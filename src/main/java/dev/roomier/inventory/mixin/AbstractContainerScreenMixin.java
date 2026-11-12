package dev.roomier.inventory.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Vertical scrolling for container screens that no longer fit on screen.
 *
 * <p>Instead of moving individual slots (which would need writable slot coordinates), the whole
 * panel is shifted through {@code topPos}. Slots are positioned relative to the panel, so hits,
 * tooltips and the background all follow automatically.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageHeight;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.imageHeight <= this.height - 16 || scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int minTop = this.height - this.imageHeight - 8;
        int maxTop = 8;
        int moved = this.topPos - (int) Math.round(scrollY * 18);
        int next = Mth.clamp(moved, minTop, maxTop);
        if (next != this.topPos) {
            this.topPos = next;
            return true;
        }
        return false;
    }
}
