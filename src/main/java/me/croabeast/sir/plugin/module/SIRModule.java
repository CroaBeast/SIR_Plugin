package me.croabeast.sir.plugin.module;

import me.croabeast.sir.plugin.SIRPlugin;

import java.lang.reflect.Constructor;
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

    protected final ModuleName<?> name;

    public SIRModule(ModuleName<?> name) {
        this.name = name;

        if (MODULE_MAP.containsKey(name))
            throw new UnsupportedOperationException("This module already exists");

        MODULE_MAP.put(name, this);
    }

    /**
     * Registers the module in the server.
     */
    public abstract void registerModule();

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

    private static boolean areRegistered = false;

    public static void registerModules() {
        if (areRegistered)
            throw new IllegalStateException("Modules are already registered.");

        try {
            SIRPlugin.fromCollector("me.croabeast.sir.plugin.module.instance").
                    filter(c -> !c.getName().contains("$")).
                    filter(c -> c.getSuperclass() == SIRModule.class).
                    collect().
                    forEach(c -> {
                        try {
                            Constructor<?> co = c.getDeclaredConstructor();
                            ((SIRModule) co.newInstance()).registerModule();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            new SIRModule(ModuleName.DISCORD_HOOK) {
                @Override
                public void registerModule() {}
            };

            new SIRModule(ModuleName.CHAT_COLORS) {
                @Override
                public void registerModule() {}
            };

            areRegistered = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
