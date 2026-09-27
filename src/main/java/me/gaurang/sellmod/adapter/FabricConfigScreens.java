package me.gaurang.sellmod.adapter;

import me.gaurang.sellmod.config.ModConfigScreen;
import me.gaurang.sellmod.core.port.ConfigScreenPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** 26.x config UI port backed by the Cloth Config screen factory. */
public class FabricConfigScreens implements ConfigScreenPort {
    @Override
    public void open(Object parentScreen) {
        Minecraft.getInstance().setScreen(ModConfigScreen.create((Screen) parentScreen));
    }
}
