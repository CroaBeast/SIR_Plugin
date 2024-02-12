package me.croabeast.sir.plugin.module;

import lombok.SneakyThrows;
import me.croabeast.sir.plugin.SIRPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a module used for each feature.
 */
public abstract class SIRModule {

    private final ModuleName name;

    @SneakyThrows
    protected SIRModule(ModuleName name) {
        this.name = name;
    }

    /**
     * Registers the module in the server.
     */
    public void register() {}

    /**
     * Checks if the module is enabled in the GUI.
     *
     * @return if the specified module is enabled.
     */
    protected boolean isEnabled() {
        return name.isEnabled();
    }

    public String toString() {
        return "SIRModule{" + name + ", " + name.isEnabled() + "}";
    }
}
