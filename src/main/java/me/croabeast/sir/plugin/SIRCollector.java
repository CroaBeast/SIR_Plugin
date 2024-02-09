package me.croabeast.sir.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SIRCollector {

    private final List<Class<?>> classes = new ArrayList<>();

    private SIRCollector() {
        classes.addAll(SIRPlugin.JAR_ENTRIES);
    }

    public SIRCollector filter(Predicate<Class<?>> predicate) {
        classes.removeIf(predicate.negate());
        return this;
    }

    public List<Class<?>> collect() {
        return new ArrayList<>(classes);
    }

    public static SIRCollector from() {
        return new SIRCollector();
    }

    public static SIRCollector from(String packagePath) {
        return from().filter(c -> c.getName().startsWith(packagePath));
    }
}
