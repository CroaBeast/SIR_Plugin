package me.croabeast.sir.plugin.module;

import lombok.SneakyThrows;
import me.croabeast.sir.plugin.SIRPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a module used for each feature.
 */
public abstract class SIRModule {

    /**
     * The Map that stores all the plugin's modules.
     */
    static final Map<ModuleName<?>, SIRModule> MODULE_MAP = new HashMap<>();

    private final ModuleName<?> name;

    @SneakyThrows
    protected SIRModule(ModuleName<?> name) {
        SIRPlugin.checkAccess(SIRModule.class);

        this.name = name;

        if (MODULE_MAP.containsKey(name))
            throw new UnsupportedOperationException("This module already exists");

        MODULE_MAP.put(name, this);
    }

    /**
     * Registers the module in the server.
     */
    public abstract void register();

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
