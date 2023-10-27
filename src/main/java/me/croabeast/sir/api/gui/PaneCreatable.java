package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public interface PaneCreatable<P extends Pane> {

    PaneCreatable<P> setLength(int length);

    PaneCreatable<P> setHeight(int height);

    PaneCreatable<P> modify(Consumer<P> consumer);

    PaneCreatable<P> onClick(Consumer<InventoryClickEvent> event);

    PaneCreatable<P> onClick(Function<P, Consumer<InventoryClickEvent>> function);

    @NotNull P create();
}
