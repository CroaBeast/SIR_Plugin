package me.croabeast.sirplugin.instance;

import lombok.Getter;
import lombok.var;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.file.FileCache;

import java.io.File;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

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

    static void createInstances(String pack) {
        @SuppressWarnings("deprecation")
        var file = new File(URLDecoder.decode(SIRPlugin.class.getProtectionDomain().
                getCodeSource().getLocation().getPath()));

        final var packPath = pack.replace(".", "/");

        try (var jarFile = new JarFile(file)) {
            var entries = Collections.list(jarFile.entries());

            for (var entry : entries) {
                var name = entry.getName();
                if (!name.startsWith(packPath)) continue;

                if (name.endsWith(".class")) {
                    String className = name.replace("/", ".").replace(".class", "");
                    var clazz = Class.forName(className);

                    var sup = clazz.getSuperclass();
                    if (sup != SIRModule.class && sup != SIRViewer.class)
                        continue;

                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);

                    var instance = constructor.newInstance();
                    ((SIRModule) instance).registerModule();
                    continue;
                }

                if (name.endsWith("/")) {
                    var subName = name.replace("/", ".").replaceFirst(packPath + "\\.", "");
                    createInstances(pack + "." + subName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerModules() {
        if (areRegistered)
            throw new IllegalStateException("Modules are already registered.");

        try {
            createInstances("me.croabeast.sirplugin.module");
            areRegistered = true;
        } catch (Exception e) {
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
