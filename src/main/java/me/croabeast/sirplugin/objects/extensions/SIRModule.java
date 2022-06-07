package me.croabeast.sirplugin.objects.extensions;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents a module used for each feature.
 */
public abstract class SIRModule {

    /**
     * The Map that stores all the plugin's modules.
     */
    static Map<Identifier, SIRModule> moduleMap = new HashMap<>();

    /**
     * The name of the module identifier in modules.yml file.
     * @return the identifier's name
     */
    @NotNull
    public abstract Identifier getIdentifier();

    /**
     * Registers the module in the server.
     */
    public abstract void registerModule();

    /**
     * Checks if the module is enabled in modules.yml
     * @return if the specified module is enabled.
     */
    public boolean isEnabled() {
        return getIdentifier().isEnabled();
    }

    /**
     * Registers all the modules of the plugin.
     * @param baseModules all modules
     */
    public static void registerModules(SIRModule... baseModules) {
        for (SIRModule module : baseModules) {
            moduleMap.put(module.getIdentifier(), module);
            module.registerModule();
        }
    }

    /**
     * Gets the module from the {@link #moduleMap} using an {@link Identifier}.
     * @param identifier the identifier
     * @return the requested module
     */
    public static SIRModule getModule(Identifier identifier) {
        return moduleMap.get(identifier);
    }
}
