package me.croabeast.sir.plugin.file;

import lombok.var;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.SIRPlugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;

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

    /**
     * Returns the loaded classes that has implemented the {@link CacheHandler}
     * interface in the SIR plugin .jar file.
     *
     * @return all the loaded handlers in SIR plugin.
     * @throws IllegalAccessException if this method is being called by another plugin
     */
    static List<Class<?>> loadedHandlers() throws IllegalAccessException {
        SIRPlugin.checkAccess(CacheHandler.class);

        return SIRCollector.from()
                .filter(CacheHandler.class::isAssignableFrom)
                .filter(c -> c != CacheHandler.class)
                .collect();
    }

    /**
     * It will invoke all "loadCache" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link Priority} annotation, from all CacheHandler
     * instances loaded in the SIR plugin.
     *
     * @throws IllegalAccessException if this method is being called by another plugin
     */
    static void load() throws IllegalAccessException {
        final Map<Integer, List<Method>> methodsMap = new HashMap<>();

        loadedHandlers()
                .forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod("loadCache");
                        int level = 0;

                        Priority a = m.getAnnotation(Priority.class);
                        if (a != null) level = a.level();

                        List<Method> methods = methodsMap.get(level);
                        if (methods == null)
                            methods = new ArrayList<>();

                        methods.add(m);
                        methodsMap.put(level, methods);
                    }
                    catch (Exception ignored) {}
                });

        var entries = new ArrayList<>(methodsMap.entrySet());
        entries.sort((e1, e2) -> e2.getKey().compareTo(e1.getKey()));

        entries.stream().map(Map.Entry::getValue).forEach(l -> l
                .forEach(m -> {
                    try {
                        m.setAccessible(true);
                        m.invoke(null);
                        m.setAccessible(false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                })
        );
    }

    /**
     * It will invoke all "saveCache" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link Priority} annotation, from all CacheHandler
     * instances loaded in the SIR plugin.
     *
     * @throws IllegalAccessException if this method is being called by another plugin
     */
    static void save() throws IllegalAccessException {
        final Map<Integer, List<Method>> methodsMap = new HashMap<>();

        loadedHandlers()
                .forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod("saveCache");
                        int level = 0;

                        Priority a = m.getAnnotation(Priority.class);
                        if (a != null) level = a.level();

                        List<Method> methods = methodsMap.get(level);
                        if (methods == null)
                            methods = new ArrayList<>();

                        methods.add(m);
                        methodsMap.put(level, methods);
                    }
                    catch (Exception ignored) {}
                });

        var entries = new ArrayList<>(methodsMap.entrySet());
        entries.sort((e1, e2) -> e2.getKey().compareTo(e1.getKey()));

        entries.stream().map(Map.Entry::getValue).forEach(mList -> mList
                .forEach(m -> {
                    try {
                        m.setAccessible(true);
                        m.invoke(null);
                        m.setAccessible(false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                })
        );
    }

    /**
     * Indicates if the "load" and/or "save" methods have higher or lower priority.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Priority {
        /**
         * The level of priority, can be negative.
         * @return priority level
         */
        int level() default 0;
    }
}
