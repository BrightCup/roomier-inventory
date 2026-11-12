package dev.roomier.inventory.menu;

import dev.roomier.inventory.RoomierInventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A rack slot that only accepts swords, axes, pickaxes, shovels and hoes. */
public class ToolSlot extends Slot {

    public ToolSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return RoomierInventory.isTool(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
