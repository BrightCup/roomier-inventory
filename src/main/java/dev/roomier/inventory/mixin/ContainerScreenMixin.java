package dev.roomier.inventory.mixin;

import dev.roomier.inventory.RoomierInventory;
import dev.roomier.inventory.client.PanelRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Widens and heightens chest screens, then repaints the background to match the new size. */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin extends AbstractContainerScreen<ChestMenu> {

    @Shadow
    @Final
    private int containerRows;

    private ContainerScreenMixin(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roomier$resize(ChestMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
        this.imageWidth = RoomierInventory.CHEST_WIDTH;
        this.imageHeight = 114 + this.containerRows * 18 + RoomierInventory.CHEST_HEIGHT_BONUS;
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void roomier$renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        PanelRenderer.draw(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        int baseY = 103 + (this.containerRows - 4) * 18;
        for (int i = 0; i < RoomierInventory.TOOL_COUNT; i++) {
            PanelRenderer.draw(
                    graphics,
                    this.leftPos + RoomierInventory.TOOL_X - 2,
                    this.topPos + baseY + i * 18 - 2,
                    20, 20
            );
        }

        ci.cancel();
    }
}
