package me.gaurang.sellmod.mixin;

import me.gaurang.sellmod.gui.ProtectedSlotOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.11 overlay hook: the classic render method (the 26.x extractTooltip does not exist here). */
@Mixin(AbstractContainerScreen.class)
public abstract class ProtectedSlotOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void sellmod$renderProtectedSlotOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ProtectedSlotOverlay.render((AbstractContainerScreen<?>) (Object) this, graphics);
    }
}
