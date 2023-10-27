package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ButtonCreator implements PaneCreatable<ToggleButton> {

    private final ToggleButton button;

    private ButtonCreator(int x, int y, boolean value) {
        button = new ToggleButton(x, y, 1, 1, value);
    }

    public ButtonCreator setLength(int length) {
        button.setLength(length);
        return this;
    }

    public ButtonCreator setHeight(int height) {
        button.setHeight(height);
        return this;
    }

    public ButtonCreator modify(Consumer<ToggleButton> consumer) {
        consumer.accept(button);
        return this;
    }

    public ButtonCreator setItem(GuiItem item, boolean isEnabled) {
        if (isEnabled)
            button.setEnabledItem(item);
        else
            button.setDisabledItem(item);
        return this;
    }

    public ButtonCreator setItem(ItemCreator item, boolean isEnabled) {
        return setItem(item.create(), isEnabled);
    }

    public ButtonCreator onClick(Consumer<InventoryClickEvent> event) {
        button.setOnClick(event);
        return this;
    }

    public ButtonCreator onClick(Function<ToggleButton, Consumer<InventoryClickEvent>> function) {
        return onClick(function.apply(button));
    }

    public @NotNull ToggleButton create() {
        return button;
    }

    public static ButtonCreator of(int x, int y, boolean value) {
        return new ButtonCreator(x, y, value);
    }
}
