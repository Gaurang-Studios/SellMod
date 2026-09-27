package me.gaurang.sellmod.mixin;

import me.gaurang.sellmod.gui.ProtectedSlotOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ProtectedSlotOverlayMixin {
    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void sellmod$renderProtectedSlotOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        ProtectedSlotOverlay.render((AbstractContainerScreen<?>) (Object) this, graphics);
    }
}
