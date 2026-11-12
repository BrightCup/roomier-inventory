package dev.roomier.inventory.mixin;

import com.google.common.collect.ImmutableList;
import dev.roomier.inventory.RoomierInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a fourth compartment to {@link Inventory} for the new slots.
 *
 * <p>{@code Inventory} resolves a global index by walking {@code compartments} in order:
 * items(36) -> armor(4) -> offhand(1). Appending a list keeps vanilla indices 0..40 pointing
 * at exactly what they pointed at before, so armor (36..39) and offhand (40) are untouched.
 * The extra slots start at 41.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Unique
    private NonNullList<ItemStack> roomier$extra;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"
            )
    )
    private ImmutableList<NonNullList<ItemStack>> roomier$addCompartment(Object items, Object armor, Object offhand) {
        NonNullList<ItemStack> extra = NonNullList.withSize(
                RoomierInventory.EXTRA_COMPARTMENT_SIZE, ItemStack.EMPTY);
        this.roomier$extra = extra;
        return ImmutableList.of(
                (NonNullList<ItemStack>) items,
                (NonNullList<ItemStack>) armor,
                (NonNullList<ItemStack>) offhand,
                extra
        );
    }

    @Inject(method = "getContainerSize", at = @At("RETURN"), cancellable = true)
    private void roomier$containerSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValueI() + RoomierInventory.EXTRA_COMPARTMENT_SIZE);
    }

    @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
    private void roomier$isEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        for (ItemStack stack : this.roomier$extra) {
            if (!stack.isEmpty()) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    /** Pickups fill the 36 vanilla slots first, then the extra grid. The tool rack stays out of it. */
    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void roomier$freeSlot(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() != -1) {
            return;
        }
        for (int i = 0; i < RoomierInventory.EXTRA_SLOTS; i++) {
            if (this.roomier$extra.get(i).isEmpty()) {
                cir.setReturnValue(RoomierInventory.EXTRA_START + i);
                return;
            }
        }
    }

    /** Partial stacks merge into the extra grid once the vanilla slots are full. */
    @Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
    private void roomier$stackExtra(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() != -1 || stack.isEmpty()) {
            return;
        }
        for (int i = 0; i < RoomierInventory.EXTRA_SLOTS; i++) {
            ItemStack destination = this.roomier$extra.get(i);
            if (!destination.isEmpty()
                    && ItemStack.isSameItemSameComponents(destination, stack)
                    && destination.isStackable()
                    && destination.getCount() < destination.getMaxStackSize()) {
                cir.setReturnValue(RoomierInventory.EXTRA_START + i);
                return;
            }
        }
    }

    @Inject(method = "save", at = @At("RETURN"), cancellable = true)
    private void roomier$saveExtra(ListTag listTag, CallbackInfoReturnable<ListTag> cir) {
        ListTag out = cir.getReturnValue();
        for (int i = 0; i < this.roomier$extra.size(); i++) {
            ItemStack stack = this.roomier$extra.get(i);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) (RoomierInventory.SAVE_BASE + i));
                out.add(stack.save(this.player.registryAccess(), tag));
            }
        }
        cir.setReturnValue(out);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void roomier$loadExtra(ListTag listTag, CallbackInfo ci) {
        for (int i = 0; i < this.roomier$extra.size(); i++) {
            this.roomier$extra.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = listTag.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            int index = slot - RoomierInventory.SAVE_BASE;
            if (index >= 0 && index < this.roomier$extra.size()) {
                this.roomier$extra.set(index,
                        ItemStack.parse(this.player.registryAccess(), tag).orElse(ItemStack.EMPTY));
            }
        }
    }
}
