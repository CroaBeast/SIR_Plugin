package me.croabeast.sirplugin.instance;

import lombok.Getter;
import lombok.var;
import me.croabeast.sirplugin.file.FileCache;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a module used for each feature.
 */
public abstract class SIRModule {

    /**
     * The Map that stores all the plugin's modules.
     */
    public static final Map<String, SIRModule> MODULE_MAP = new HashMap<>();

    static {
        new SIRModule("discord") {
            @Override
            public void registerModule() {}
        };
    }

    @Getter
    private final String name;

    public SIRModule(String name) {
        this.name = name;
        MODULE_MAP.put(this.name, this);
    }

    /**
     * Registers the module in the server.
     */
    public abstract void registerModule();

    /**
     * Checks if the module is enabled in modules.yml
     *
     * @return if the specified module is enabled.
     */
    public boolean isEnabled() {
        return FileCache.MODULES.toList("modules").contains(name);
    }

    public String toString() {
        return "SIRModule{" + name + ", " + isEnabled() + "}";
    }

    private static boolean areRegistered = false;

    public static void registerModules() {
        if (areRegistered)
            throw new IllegalStateException("Modules are already registered.");

        try {
            for (var c : ClassCollector.
                    SIR_COLLECTOR.apply("me.croabeast.sirplugin.module").
                    getCollectedClasses(SIRModule.class)
            )
                c.getConstructor().newInstance().registerModule();

            areRegistered = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SIRModule get(String name) {
        return MODULE_MAP.getOrDefault(name, null);
    }

    public static boolean isEnabled(String name) {
        var m = get(name);
        return m != null && m.isEnabled();
    }
}
