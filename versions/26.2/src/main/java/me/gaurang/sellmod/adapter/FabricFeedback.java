package me.gaurang.sellmod.adapter;

import me.gaurang.sellmod.core.port.FeedbackPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** 26.2 toast port; identical scheduling to the 26.1 adapter (toast manager moved to Gui). */
public class FabricFeedback implements FeedbackPort {
    @Override
    public void toast(String title, String message) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.toastManager().addToast(
                new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal(title), Component.literal(message))));
    }
}
