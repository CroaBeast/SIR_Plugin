package me.croabeast.sir.plugin.file;

import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.utility.LogUtils;

import java.lang.reflect.Method;

/**
 * Represents any class that loads or save any data from file to cache or vice-versa.
 * Only will work if the providing plugin of those classes is SIR.
 *
 * <p> Those classes should have the private static "void" methods: "loadCache()" to load
 * any data from .yml files to cache, like lists, maps, etc; and "saveCache()" to save
 * the cache data to any file.
 *
 * <p> The methods "loadCache()" and/or "saveCache()" should be implemented on each class
 * that can be assigned with this interface.
 */
public interface CacheHandler {

    static void load() {
        SIRPlugin.fromCollector().
                filter(CacheHandler.class::isAssignableFrom).
                collect().
                forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod("loadCache");

                        m.setAccessible(true);
                        m.invoke(null);
                        m.setAccessible(false);
                    }
                    catch (Exception ignored) {}
                });
    }

    static void save() {
        SIRPlugin.fromCollector().
                filter(CacheHandler.class::isAssignableFrom).
                collect().
                forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod("saveCache");

                        m.setAccessible(true);
                        m.invoke(null);
                        m.setAccessible(false);
                    }
                    catch (Exception ignored) {}
                });
    }
}
