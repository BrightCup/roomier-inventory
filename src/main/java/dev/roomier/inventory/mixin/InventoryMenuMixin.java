package dev.roomier.inventory.mixin;

import dev.roomier.inventory.RoomierInventory;
import dev.roomier.inventory.menu.ToolSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Appends 23 grid slots and 6 tool slots to the player's container menu.
 *
 * <p>Vanilla slots keep their order, so every vanilla slot constant
 * ({@code INV_SLOT_START}, {@code USE_ROW_SLOT_START}, ...) stays valid. Only the hotbar
 * row is pushed down to make room for the two extra inventory rows.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    private InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roomier$addExtraSlots(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        int index = RoomierInventory.EXTRA_START;

        // Tenth column of the three vanilla rows.
        for (int row = 0; row < 3; row++) {
            this.addSlot(new Slot(playerInventory, index++, 170, 84 + row * 18));
        }

        // Two brand new rows, ten wide.
        for (int row = 3; row < RoomierInventory.MAIN_ROWS; row++) {
            for (int col = 0; col < RoomierInventory.COLUMNS; col++) {
                this.addSlot(new Slot(playerInventory, index++, 8 + col * 18, 84 + row * 18));
            }
        }

        // Floating tool rack to the left of the panel.
        for (int i = 0; i < RoomierInventory.TOOL_COUNT; i++) {
            this.addSlot(new ToolSlot(
                    playerInventory,
                    RoomierInventory.TOOL_START + i,
                    RoomierInventory.TOOL_X,
                    84 + i * 18
            ));
        }
    }

    /**
     * Vanilla builds the hotbar row at y=142. {@code Slot.y} is final, so the y argument of
     * every {@code new Slot(container, index, x, y)} is inspected instead: 142 is unique to
     * the hotbar, and the armour/offhand slots keep their own values.
     */
    @ModifyArgs(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"
            )
    )
    private void roomier$lowerHotbar(Args args) {
        if ((int) args.get(3) == 142) {
            args.set(3, RoomierInventory.HOTBAR_Y);
        }
    }

    /**
     * Replaces vanilla shift-click routing so the extra slots take part in it.
     * Mirrors vanilla behaviour, including auto-equipping armour and the offhand.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void roomier$quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        Slot slot = this.getSlot(index);
        if (!slot.hasItem()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int extraStart = RoomierInventory.MENU_EXTRA_START;
        int extraEnd = RoomierInventory.MENU_TOOL_START;
        int end = RoomierInventory.MENU_END;

        if (index == 0) {
            // Crafting result: spread across the whole inventory.
            if (!this.moveItemStackTo(stack, 9, end, true)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            slot.onQuickCraft(stack, original);
        } else if (index >= 1 && index < 9) {
            // Crafting grid and armour slots.
            if (!this.moveItemStackTo(stack, 9, end, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else {
            EquipmentSlot equipmentSlot = player.getEquipmentSlotForItem(stack);
            if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                    && !this.getSlot(8 - equipmentSlot.getIndex()).hasItem()) {
                int target = 8 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(stack, target, target + 1, false)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.getSlot(45).hasItem()) {
                if (!this.moveItemStackTo(stack, 45, 46, false)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            } else if (index >= 9 && index < 36) {
                // Main inventory: hotbar first, then the extra grid.
                if (!this.moveItemStackTo(stack, 36, 45, false)
                        && !this.moveItemStackTo(stack, extraStart, end, false)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            } else if (index >= 36 && index < 45) {
                // Hotbar: main inventory first, then the extra grid.
                if (!this.moveItemStackTo(stack, 9, 36, false)
                        && !this.moveItemStackTo(stack, extraStart, extraEnd, false)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            } else {
                // Offhand, extra grid and tool rack go back to the normal inventory.
                if (!this.moveItemStackTo(stack, 9, 45, false)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        slot.onTake(player, stack);
        if (index == 0) {
            player.drop(stack, false);
        }

        cir.setReturnValue(original);
    }
}
