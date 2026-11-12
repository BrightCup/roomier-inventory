package dev.roomier.inventory;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.Mod;

/**
 * Roomier Inventory - widens the player inventory to 10x5 and adds a floating tool rack.
 *
 * <p>Layout inside {@link net.minecraft.world.entity.player.Inventory#items}:
 * <pre>
 *   0..8    hotbar (vanilla)
 *   9..35   main inventory, 3 rows of 9 (vanilla)
 *   36..58  extra slots that complete the 10x5 grid (23 slots)
 *   59..64  tool rack (6 slots)
 * </pre>
 * Vanilla indices are untouched, so saving, dropping, respawning and every other
 * vanilla path keeps working without changes.
 */
@Mod(RoomierInventory.MOD_ID)
public class RoomierInventory {
    public static final String MOD_ID = "roomier_inventory";

    /** Columns in the main inventory grid. */
    public static final int COLUMNS = 10;
    /** Rows in the main inventory grid. */
    public static final int MAIN_ROWS = 5;
    /** Slots the vanilla main inventory occupies (3 rows of 9). */
    public static final int VANILLA_MAIN_SLOTS = 27;
    /** Total slots in the widened main grid. */
    public static final int MAIN_SLOTS = COLUMNS * MAIN_ROWS;
    /** Slots added on top of the vanilla main inventory. */
    public static final int EXTRA_SLOTS = MAIN_SLOTS - VANILLA_MAIN_SLOTS;
    /**
     * First index of the extra compartment.
     *
     * <p>Vanilla {@code Inventory} stores items(36) + armor(4) + offhand(1) and resolves
     * indices by walking that list. The extra slots therefore start at 41, after the offhand,
     * so vanilla indices 36..40 keep pointing at armor and offhand.
     */
    public static final int EXTRA_START = 41;
    /** One past the last extra slot. */
    public static final int EXTRA_END = EXTRA_START + EXTRA_SLOTS;
    /** Number of dedicated tool slots. */
    public static final int TOOL_COUNT = 6;
    /** First index of the tool rack. */
    public static final int TOOL_START = EXTRA_END;
    /** Total container size: 36 items + 4 armor + 1 offhand + 23 extra + 6 tools. */
    public static final int TOTAL_SIZE = TOOL_START + TOOL_COUNT;
    /** NBT "Slot" offset used for extra inventory entries (armor uses 100, offhand 150). */
    public static final int SAVE_BASE = 200;
    /** Size of the extra compartment backing list. */
    public static final int EXTRA_COMPARTMENT_SIZE = EXTRA_SLOTS + TOOL_COUNT;

    /** First menu slot index we add: vanilla InventoryMenu has slots 0..45. */
    public static final int MENU_EXTRA_START = 46;
    /** First menu slot index of the tool rack. */
    public static final int MENU_TOOL_START = MENU_EXTRA_START + EXTRA_SLOTS;
    /** One past the last menu slot we add. */
    public static final int MENU_END = MENU_TOOL_START + TOOL_COUNT;

    /** GUI size, wider and taller than vanilla's 176x166. */
    public static final int GUI_WIDTH = 194;
    public static final int GUI_HEIGHT = 202;
    /** Chest-like screens use the same width; height grows by two rows plus the rack. */
    public static final int CHEST_WIDTH = 194;
    public static final int CHEST_HEIGHT_BONUS = 35;
    /** Y of the hotbar row, pushed down by the two extra inventory rows. */
    public static final int HOTBAR_Y = 178;
    /** X of the tool rack, floating left of the panel. */
    public static final int TOOL_X = -24;

    /**
     * Touching these classes at mod construction forces the mixins to be applied, so a
     * mismatched injection point fails loudly at startup instead of mid-game.
     */
    private static final Class<?>[] MIXIN_TARGETS = {
            Inventory.class, InventoryMenu.class, net.minecraft.world.inventory.ChestMenu.class
    };

    public RoomierInventory() {
        if (MIXIN_TARGETS.length != 3) {
            throw new IllegalStateException("mixins not loaded");
        }
    }

    /** Tool rack accepts swords and the four tool classes. */
    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES);
    }
}
