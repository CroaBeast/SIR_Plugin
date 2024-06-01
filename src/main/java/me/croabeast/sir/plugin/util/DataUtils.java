package me.croabeast.sir.plugin.util;

import lombok.experimental.UtilityClass;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRCollector;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@UtilityClass
public class DataUtils {

    int getLevel(Method method) {
        DataHandler.Priority a = method.getAnnotation(DataHandler.Priority.class);
        return a != null ? a.value() : 0;
    }

    void loadMethodFromClasses(boolean isLoad) {
        Map<Integer, LinkedHashSet<Method>> map = new TreeMap<>(Comparator.reverseOrder());
        String name = isLoad ? "loadData" : "saveData";

        SIRCollector.from()
                .filter(DataHandler.class::isAssignableFrom)
                .filter(c -> c != DataHandler.class)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect()
                .forEach(c -> {
                    try {
                        Method m = c.getDeclaredMethod(name);
                        int level = getLevel(m);

                        LinkedHashSet<Method> set = map.getOrDefault(level, new LinkedHashSet<>());
                        set.add(m);

                        map.put(level, set);
                    }
                    catch (Exception ignored) {}
                });

        String prefix = isLoad ? "Loading" : "Saving";
        BeansLogger.DEFAULT.log("[SIR] " + prefix + " data to cache...");

        DataHandler.Counter fails = new DataHandler.Counter();
        String suffix = isLoad ? "loaded" : "saved";

        map.forEach((k, v) -> {
            if (v.isEmpty()) return;

            v.forEach(m -> {
                String clazz = m.getDeclaringClass().getSimpleName();
                String be = " was";

                try {
                    m.setAccessible(true);
                    m.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    be += "n't";
                    fails.add();
                }

                BeansLogger.DEFAULT.log(" • " + clazz + be + ' ' + suffix);
            });
        });

        if (fails.get() > 0)
            BeansLogger.DEFAULT.log(
                    "&c[SIR] Some classes weren't " + suffix + " correctly.",
                    "&c[SIR] Please report this ASAP to CroaBeast."
            );
    }

    /**
     * It will invoke all "loadData" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link DataHandler.Priority} annotation, from all
     * {@link DataHandler} sub non-abstract classes loaded in the SIR plugin.
     */
    public void load() {
        loadMethodFromClasses(true);
    }

    /**
     * It will invoke all "saveData" methods (if they exist), starting from the highest
     * to the lowest priority using the {@link DataHandler.Priority} annotation, from all
     * {@link DataHandler} sub non-abstract classes loaded in the SIR plugin.
     */
    public void save() {
        loadMethodFromClasses(false);
    }
}
