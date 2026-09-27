package me.gaurang.sellmod.gui;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/** 1.21.11 Cloth entry hook (classic render signature). */
public class ButtonEntry extends TooltipListEntry<Object> {

    private final Button button;
    private final List<AbstractWidget> widgets;

    protected ButtonEntry(Component fieldName, Button button) {
        super(fieldName, () -> Optional.empty());
        this.button = button;
        this.widgets = ImmutableList.of(button);
    }

    public static ButtonEntry create(Component fieldName, Button button) {
        return new ButtonEntry(fieldName, button);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x,
                       int entryWidth, int entryHeight, int mouseX, int mouseY,
                       boolean isHovered, float tickDelta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, tickDelta);
        this.button.setX(x);
        this.button.setY(y);
        this.button.setWidth(entryWidth);
        this.button.render(graphics, mouseX, mouseY, tickDelta);
    }
}
