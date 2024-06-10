package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PaneCreatable<P extends Pane> {

    protected final P pane;

    @NotNull
    public PaneCreatable<P> modifyPane(Consumer<P> consumer) {
        Objects.requireNonNull(consumer).accept(pane);
        return this;
    }

    @NotNull
    public PaneCreatable<P> setAction(Consumer<InventoryClickEvent> consumer) {
        pane.setOnClick(consumer);
        return this;
    }

    @NotNull
    public PaneCreatable<P> setAction(Function<P, Consumer<InventoryClickEvent>> function) {
        return setAction(function.apply(pane));
    }

    @NotNull
    public P create() {
        return pane;
    }

    public boolean compare(P pane) {
        return Objects.equals(this.pane.getUUID(), pane.getUUID());
    }
}
