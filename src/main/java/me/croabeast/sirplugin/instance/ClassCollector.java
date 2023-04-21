package me.croabeast.sirplugin.instance;

import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.var;
import me.croabeast.sirplugin.SIRPlugin;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class ClassCollector {

    public static final Function<String, ClassCollector> SIR_COLLECTOR =
            s -> new ClassCollector(SIRPlugin.class, s);

    private final File file;
    private final String packageName;

    private final List<Predicate<Class<?>>> conditions = new ArrayList<>();

    @Accessors(chain = true)
    @Setter
    private boolean inclusive = false;

    public ClassCollector(File file, String packageName) {
        this.file = file;
        this.packageName = packageName;
    }

    public ClassCollector(File file) {
        this(file, "");
    }

    @SuppressWarnings("deprecation")
    public ClassCollector(String filePath, String packageName) {
        this(new File(URLDecoder.decode(filePath)), packageName);
    }

    public ClassCollector(String filePath) {
        this(filePath, "");
    }

    public ClassCollector(Class<?> fileClass, String packageName) {
        this(
                fileClass.getProtectionDomain().getCodeSource().getLocation().getPath(),
                packageName
        );
    }

    public ClassCollector(Class<?> fileClass) {
        this(fileClass, "");
    }

    @SafeVarargs
    public final ClassCollector addConditions(Predicate<Class<?>>... conditions) {
        if (conditions == null || conditions.length == 0) return this;

        this.conditions.addAll(Arrays.asList(conditions));
        return this;
    }

    static boolean notInstanceOf(Class<?> sup, Class<?> sub) {
        if (sup == null) sup = Object.class;
        return sup != Object.class && !sup.isAssignableFrom(sub);
    }

    boolean classCanSkip(Class<?> clazz) {
        return !conditions.isEmpty() && (inclusive ?
                conditions.stream().allMatch(c -> c.test(clazz)) :
                conditions.stream().anyMatch(c -> c.test(clazz))
        );
    }

    boolean noStartWith(Class<?> clazz) {
        var p = packageName;
        return StringUtils.isNotBlank(p) && !clazz.getPackage().getName().startsWith(p);
    }

    static String toClass(String string) {
        return string.replace('/', '.').replace(".class", "");
    }

    static List<Class<?>> processEntries(JarFile jar, JarEntry entry) {
        List<Class<?>> classes = new ArrayList<>();
        if (jar == null) return classes;

        JarInputStream stream;
        try (var i = new JarInputStream(jar.getInputStream(entry))) {
            stream = i;
        } catch (Exception e) {
            return classes;
        }

        JarEntry e;
        while (true) {
            try {
                if ((e = stream.getNextJarEntry()) == null) break;
            } catch (IOException ex) {
                continue;
            }

            if (e.isDirectory()) {
                classes.addAll(processEntries(jar, e));
                continue;
            }

            if (!e.getName().endsWith(".class")) continue;

            try {
                var clazz = Class.forName(toClass(e.getName()));
                classes.add(clazz);
            } catch (Exception ignored) {}
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> List<Class<T>> getCollectedClasses(Class<T> parent) {
        List<Class<?>> classes = new ArrayList<>();

        List<JarEntry> list = new ArrayList<>();
        JarFile jarFile = null;

        try (var jar = new JarFile(file)) {
            jarFile = jar;
            list = Collections.list(jar.entries());
        } catch (Exception ignored) {}

        for (var entry : list) {
            final var name = entry.getName();

            if (name.endsWith(".class")) {
                try {
                    var clazz = Class.forName(toClass(name));
                    classes.add(clazz);
                } catch (Exception ignored) {}
                continue;
            }

            if (entry.isDirectory()) continue;
            classes.addAll(processEntries(jarFile, entry));
        }

        return classes.stream().map(
                        c -> noStartWith(c) || notInstanceOf(parent, c) ||
                                classCanSkip(c) ? null : (Class<T>) c
                ).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    @NotNull
    public List<Class<?>> getCollectedClasses() {
        return new ArrayList<>(getCollectedClasses(null));
    }
}
