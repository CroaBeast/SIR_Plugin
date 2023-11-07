package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ButtonCreator extends PaneCreatable<ToggleButton> {

    private ButtonCreator(int x, int y, boolean value) {
        super(new ToggleButton(x, y, 1, 1, value));
    }

    @NotNull
    public ButtonCreator modifyPane(Consumer<ToggleButton> consumer) {
        super.modifyPane(consumer);
        return this;
    }

    @NotNull
    public ButtonCreator setItem(GuiItem item, boolean isEnabled) {
        if (isEnabled) {
            pane.setEnabledItem(item);
            return this;
        }

        pane.setDisabledItem(item);
        return this;
    }

    @NotNull
    public ButtonCreator setItem(ItemCreator item, boolean isEnabled) {
        return setItem(item.create(), isEnabled);
    }

    @NotNull
    public ButtonCreator setAction(Consumer<InventoryClickEvent> consumer) {
        super.setAction(consumer);
        return this;
    }

    @NotNull
    public ButtonCreator setAction(Function<ToggleButton, Consumer<InventoryClickEvent>> function) {
        super.setAction(function);
        return this;
    }

    public static ButtonCreator of(int x, int y, boolean value) {
        return new ButtonCreator(x, y, value);
    }
}
