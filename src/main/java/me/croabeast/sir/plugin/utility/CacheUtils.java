package me.croabeast.sir.plugin.utility;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.file.CacheManageable;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@UtilityClass
public class CacheUtils {

    int getLevel(Method method) {
        CacheManageable.Priority a = method.getAnnotation(CacheManageable.Priority.class);
        return a != null ? a.value() : 0;
    }

    void loadMethodFromClasses(boolean isLoad) {
        Map<Integer, Set<Method>> map = new TreeMap<>(Comparator.reverseOrder());
        String name = isLoad ? "loadCache" : "saveCache";

        SIRCollector.from()
                .filter(CacheManageable.class::isAssignableFrom)
                .filter(c -> c != CacheManageable.class)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect()
                .forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod(name);
                        int level = getLevel(m);

                        Set<Method> set = map.getOrDefault(level, new HashSet<>());
                        set.add(m);

                        map.put(level, set);
                    }
                    catch (Exception ignored) {}
                });

        String prefix = "[SIR] " + (isLoad ? "Loading " : "Saving ");
        String temp = isLoad ? " loaded." : " saved.";

        map.forEach((k, v) -> {
            if (!v.isEmpty())
                Bukkit.getLogger().info(prefix + "data with priority " + k + ':');

            v.forEach(m -> {
                String clazz = m.getDeclaringClass().getSimpleName();
                try {
                    m.setAccessible(true);
                    m.invoke(null);

                    Bukkit.getLogger().info(" • " + clazz + " was being" + temp);
                } catch (Exception e) {
                    Bukkit.getLogger().info(
                            " • " + clazz + " can't be" +
                                    temp + "(" +
                                    e.getLocalizedMessage()+ ")"
                    );
                }
            });
        });
    }

    /**
     * It will invoke all "loadCache" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link CacheManageable.Priority} annotation, from all
     * {@link CacheManageable} sub non-abstract classes loaded in the SIR plugin.
     */
    public void load() {
        loadMethodFromClasses(true);
    }

    /**
     * It will invoke all "saveCache" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link CacheManageable.Priority} annotation, from all
     * {@link CacheManageable} sub non-abstract classes loaded in the SIR plugin.
     */
    public void save() {
        loadMethodFromClasses(false);
    }
}
