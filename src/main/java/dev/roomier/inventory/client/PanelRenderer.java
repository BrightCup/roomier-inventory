package dev.roomier.inventory.client;

import net.minecraft.client.gui.GuiGraphics;

/** Draws a flat vanilla-ish panel without depending on a texture with a fixed height. */
public final class PanelRenderer {

    private PanelRenderer() {
    }

    public static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFC6C6C6);
    }
}
