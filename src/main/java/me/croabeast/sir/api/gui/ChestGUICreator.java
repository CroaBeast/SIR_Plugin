package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import org.jetbrains.annotations.NotNull;

public final class ChestGUICreator implements GUICreatable<ChestGui> {

    private final ChestGui gui;

    private ChestGUICreator(int rows, String name) {
        gui = new ChestGui(rows, NeoPrismaticAPI.colorize(name));
    }

    public ChestGUICreator addPane(Pane pane) {
        gui.addPane(pane);
        return this;
    }

    @NotNull
    public ChestGui create() {
        return gui;
    }

    public static ChestGUICreator of(int rows, String name) {
        return new ChestGUICreator(rows, name);
    }
}
