package me.croabeast.sirplugin.instance;

import lombok.var;
import me.croabeast.sirplugin.SIRPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.JarFile;

public class ClassCollector {

    private final List<Class<?>> entries = new ArrayList<>();

    private ClassCollector(JarFile file) {
        Objects.requireNonNull(file);

        Collections.list(file.entries()).forEach(e -> {
            var name = e.getName().replace('/', '.');
            if (!name.endsWith(".class")) return;

            name = name.replace(".class", "");

            try {
                entries.add(Class.forName(name));
            } catch (Exception ignored) {}
        });
    }

    public ClassCollector filter(Predicate<Class<?>> skip) {
        entries.removeIf(skip.negate());
        return this;
    }

    public List<Class<?>> collect() {
        return entries;
    }

    public static ClassCollector from(JarFile jar) {
        return new ClassCollector(jar);
    }

    public static ClassCollector fromPackage(JarFile jar, String packagePath) {
        return from(jar).filter(c -> c.getName().startsWith(packagePath));
    }

    public static ClassCollector from(File file) throws IOException {
        return from(new JarFile(file));
    }

    public static ClassCollector fromPackage(File file, String packagePath) throws IOException {
        return from(file).filter(c -> c.getName().startsWith(packagePath));
    }

    public static ClassCollector fromSIR(String packageName) {
        return fromPackage(SIRPlugin.getSIRJarFile(), packageName);
    }
}
