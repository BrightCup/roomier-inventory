package dev.roomier.inventory.mixin;

import dev.roomier.inventory.RoomierInventory;
import dev.roomier.inventory.client.PanelRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grows the inventory screen to 194x202 and paints the wider panel.
 *
 * <p>The panel is drawn in code rather than from a texture, so the mod ships no binary assets.
 * Slot sockets are still provided by vanilla's own {@code renderSlot} sprite.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Shadow
    private float xMouse;

    @Shadow
    private float yMouse;

    private InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roomier$resize(Player player, CallbackInfo ci) {
        this.imageWidth = RoomierInventory.GUI_WIDTH;
        this.imageHeight = RoomierInventory.GUI_HEIGHT;
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void roomier$renderBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        int x = this.leftPos;
        int y = this.topPos;

        PanelRenderer.draw(graphics, x, y, this.imageWidth, this.imageHeight);

        // The tool rack floats outside the panel, so each socket gets its own backdrop.
        for (int i = 0; i < RoomierInventory.TOOL_COUNT; i++) {
            PanelRenderer.draw(
                    graphics,
                    x + RoomierInventory.TOOL_X - 2,
                    y + 84 + i * 18 - 2,
                    20, 20
            );
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                x + 26, y + 8, x + 75, y + 78,
                30, 0.0625F,
                this.xMouse, this.yMouse, this.minecraft.player
        );

        ci.cancel();
    }
}
