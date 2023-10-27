package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.jetbrains.annotations.NotNull;

public interface GUICreatable<G extends Gui> {

    GUICreatable<G> addPane(Pane pane);

    default <P extends Pane> GUICreatable<G> addPane(PaneCreatable<P> pane) {
        return addPane(pane.create());
    }

    @NotNull G create();
}
