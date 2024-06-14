package me.croabeast.sir.plugin;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.CollectionBuilder;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@UtilityClass
class SIRLoader {

    final List<Class<?>> JAR_CLASSES = new ArrayList<>();
    final List<String> JAR_FILE_PATHS = new ArrayList<>();

    JarFile pluginJar = null;

    @SuppressWarnings("deprecation")
    void loadAllJarEntries() {
        final String path = SIRPlugin.class.getProtectionDomain()
                .getCodeSource()
                .getLocation().getPath();

        Set<Class<?>> classes = new LinkedHashSet<>();
        final Set<String> filePaths;

        try (JarFile jar = new JarFile(new File(URLDecoder.decode(path)))) {
            if (pluginJar == null) pluginJar = jar;

            CollectionBuilder<String> builder =
                    CollectionBuilder.of(pluginJar.entries()).map(ZipEntry::getName);

            for (String s : CollectionBuilder.of(builder)
                    .filter(s -> !s.contains("$") &&
                            s.startsWith("me/croabeast/sir/plugin") &&
                            s.endsWith(".class"))
                    .filter(s -> !s.contains("sir/plugin/util"))
                    .apply(s -> s.replace('/', '.').replace(".class", "")))
            {
                try {
                    classes.add(Class.forName(s));
                } catch (Exception ignored) {}
            }

            filePaths = CollectionBuilder.of(builder)
                    .filter(s -> s.startsWith("resources") && s.endsWith(".yml"))
                    .apply(s -> s.substring(10))
                    .apply(s -> s.replace('/', File.separatorChar))
                    .apply(s -> s.replace(".yml", ""))
                    .collect(new LinkedHashSet<>());
        } catch (Exception e) {
            return;
        }

        JAR_CLASSES.addAll(classes);
        JAR_FILE_PATHS.addAll(filePaths);
    }
}
