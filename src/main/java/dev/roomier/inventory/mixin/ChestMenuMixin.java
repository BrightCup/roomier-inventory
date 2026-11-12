package dev.roomier.inventory.mixin;

import dev.roomier.inventory.RoomierInventory;
import dev.roomier.inventory.menu.ToolSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Gives chest-like menus the same widened player grid as the inventory screen.
 *
 * <p>Vanilla lays the player inventory out as 9x3 at {@code 103 + (rows - 4) * 18} plus a
 * hotbar at {@code 161 + offset}. The hotbar is pushed down two rows and 23 grid slots plus
 * 6 tool slots are appended after the vanilla ones.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin extends AbstractContainerMenu {

    @Shadow
    @Final
    private int containerRows;

    private ChestMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    /** Hotbar y is unique per menu, so it identifies the row without tracking locals. */
    @ModifyArgs(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;<init>(Lnet/minecraft/world/Container;III)V"
            )
    )
    private void roomier$lowerHotbar(Args args) {
        Object container = args.get(0);
        int y = args.get(3);
        if (container instanceof Inventory && y == 161 + (this.containerRows - 4) * 18) {
            args.set(3, 197 + (this.containerRows - 4) * 18);
        }
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At("TAIL")
    )
    private void roomier$addExtraSlots(MenuType<?> type, int containerId, Inventory playerInventory,
                                       Container container, int rows, CallbackInfo ci) {
        int offset = (this.containerRows - 4) * 18;
        int baseY = 103 + offset;

        int index = RoomierInventory.EXTRA_START;

        for (int row = 0; row < 3; row++) {
            this.addSlot(new Slot(playerInventory, index++, 170, baseY + row * 18));
        }

        for (int row = 3; row < RoomierInventory.MAIN_ROWS; row++) {
            for (int col = 0; col < RoomierInventory.COLUMNS; col++) {
                this.addSlot(new Slot(playerInventory, index++, 8 + col * 18, baseY + row * 18));
            }
        }

        for (int i = 0; i < RoomierInventory.TOOL_COUNT; i++) {
            this.addSlot(new ToolSlot(
                    playerInventory,
                    RoomierInventory.TOOL_START + i,
                    RoomierInventory.TOOL_X,
                    baseY + i * 18
            ));
        }
    }
}
