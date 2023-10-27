package me.croabeast.sir.plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public final class SIRCollector {

    private final List<Class<?>> classes = new LinkedList<>();

    SIRCollector() {
        for (String s : SIRPlugin.JAR_ENTRIES) {
            if (s == null || !s.endsWith(".class")) continue;
            s = s.replace('/', '.').replace(".class", "");

            try {
                classes.add(Class.forName(s));
            } catch (Exception ignored) {}
        }
    }

    public SIRCollector filter(Predicate<Class<?>> predicate) {
        classes.removeIf(predicate.negate());
        return this;
    }

    public List<Class<?>> collect() {
        return new LinkedList<>(classes);
    }

    public static SIRCollector from() {
        return new SIRCollector();
    }

    public static SIRCollector from(String packagePath) {
        return from().filter(c -> c.getName().startsWith(packagePath));
    }
}
