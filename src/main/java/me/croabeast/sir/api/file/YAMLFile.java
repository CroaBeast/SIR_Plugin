package me.croabeast.sir.api.file;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.api.misc.JavaLoader;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
@Accessors(chain = true)
public class YAMLFile {

    private final Loader loader;

    @Getter
    private final String name;
    @Getter @Nullable
    private String folder;

    @Getter
    private final String location;
    @Getter @NotNull
    private final File file;

    private InputStream resource;
    private FileConfiguration configuration;

    @Getter @Setter
    private boolean updatable = true;
    @Getter @Setter
    private boolean debug = false;

    public <T> YAMLFile(T loaderObject, @Nullable String folder, String name) throws IOException {
        loader = new Loader(loaderObject);
        this.name = StringUtils.isBlank(name) ? ("file-" + hashCode()) : name;

        String location = name + ".yml";

        if (StringUtils.isNotBlank(folder)) {
            this.folder = folder;

            File file = new File(loader.getDataFolder(), folder);
            if (!file.exists()) file.mkdirs();

            location = folder + File.separator + location;
        }

        this.location = location;
        file = new File(loader.getDataFolder(), location);

        try {
            setResource(location);
        } catch (Exception ignored) {}
    }

    public YAMLFile setResource(InputStream resource) {
        this.resource = Objects.requireNonNull(resource, "There is no resource in plugin's jar file");
        return this;
    }

    public YAMLFile setResource(String resourcePath) {
        if (StringUtils.isBlank(resourcePath))
            throw new NullPointerException("The resource path is blank or empty");

        this.resource = loader.getResource(resourcePath.replace('\\', '/'));
        Objects.requireNonNull(resource, "There is no resource in " + resourcePath);

        return this;
    }

    @NotNull
    public FileConfiguration reload() {
        return configuration = YamlConfiguration.loadConfiguration(file);
    }

    public boolean saveDefaults() {
        if (file.exists()) return true;

        try {
            JavaLoader.saveResource(resource, loader.getDataFolder(), location);
        } catch (Exception e) {
            if (isDebug()) e.printStackTrace();
            return false;
        }

        if (isDebug())
            LogUtils.doLog("&cFile " + location + " missing... &7Generating!");

        reload();
        return true;
    }

    @NotNull
    private FileConfiguration getConfiguration() {
        return configuration == null ? reload() : configuration;
    }

    public void acceptTo(Consumer<FileConfiguration> consumer) {
        Objects.requireNonNull(consumer).accept(getConfiguration());
    }

    public <T> T getFrom(Function<FileConfiguration, T> function) {
        return Objects.requireNonNull(function).apply(getConfiguration());
    }

    public boolean save() {
        String msg = "&7The &e" + location + "&7 file ";

        try {
            getConfiguration().save(file);

            if (isDebug())
                LogUtils.doLog(msg + "has been&a saved&7.");
            return true;
        }
        catch (Exception e) {
            if (isDebug()) {
                LogUtils.doLog(msg + "&ccouldn't be saved&7.");
                e.printStackTrace();
            }
            return false;
        }
    }

    public boolean update() {
        String msg = "&7The &e" + location + "&7 file ";
        if (!isUpdatable()) return false;

        try {
            YAMLUpdater.updateFrom(resource, file);
            if (LibUtils.MAIN_VERSION < 13)
                YAMLUpdater.updateFrom(resource, file);

            if (isDebug())
                LogUtils.doLog(msg + "has been&a updated&7.");
            return true;
        }
        catch (Exception e) {
            if (isDebug()) {
                LogUtils.doLog(msg + "&ccouldn't be updated&7.");
                e.printStackTrace();
            }
            return false;
        }
    }

    public <T> T get(String path, T def) {
        return (T) getConfiguration().get(path, def);
    }

    @Nullable
    public <T> T get(String path, Class<T> clazz) {
        try {
            return clazz.cast(getConfiguration().get(path));
        } catch (Exception e) {
            return null;
        }
    }

    public <T> void set(String path, T value) {
        getConfiguration().set(path, value);
    }

    @Nullable
    public ConfigurationSection getSection(String path) {
        return StringUtils.isBlank(path) ?
                getConfiguration() :
                getConfiguration().getConfigurationSection(path);
    }

    @NotNull
    public List<String> getKeys(String path, boolean deep) {
        ConfigurationSection section = getSection(path);

        return section != null ?
                new ArrayList<>(section.getKeys(deep)) :
                new ArrayList<>();
    }

    public Map<Integer, Set<ConfigUnit>> getUnitsByPriority(String path) {
        return getUnitsByPriority(getConfiguration(), path);
    }

    public List<String> toList(String path) {
        return TextUtils.toList(getConfiguration(), path);
    }

    @Override
    public String toString() {
        return "YAMLFile{folder='" + folder + "', name='" + name + "'}";
    }

    public boolean equals(String folder, String name) {
        return Objects.equals(this.folder, folder) && Objects.equals(this.name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (this == o) return true;
        if (getClass() != o.getClass()) return false;

        YAMLFile f = (YAMLFile) o;
        return equals(f.folder, f.name);
    }

    public static Map<Integer, Set<ConfigUnit>> getUnitsByPriority(ConfigurationSection main, String path) {
        ConfigurationSection section = StringUtils.isNotBlank(path) ?
                Objects.requireNonNull(main).getConfigurationSection(path) :
                main;

        Objects.requireNonNull(section);

        Set<String> sectionKeys = section.getKeys(false);
        if (sectionKeys.isEmpty()) throw new NullPointerException();


        Comparator<Integer> sort = Comparator.reverseOrder();
        Map<Integer, Set<ConfigUnit>> map = new TreeMap<>(sort);

        for (String key : sectionKeys) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");
            int def = perm.matches("(?i)default") ? 0 : 1;

            int priority = id.getInt("priority", def);

            Set<ConfigUnit> m = map.getOrDefault(priority, new LinkedHashSet<>());
            m.add(ConfigUnit.of(id));

            map.put(priority, m);
        }

        return map;
    }

    private static class Loader {

        private final Class<?> clazz;
        private final Object loader;

        private <T> Loader(T loader) throws IOException {
            if (loader instanceof Plugin || loader instanceof JavaLoader) {
                this.loader = loader;
                this.clazz = loader.getClass();
                return;
            }

            throw new IOException("Loader object is not valid.");
        }

        File getDataFolder() {
            try {
                return (File) clazz.getMethod("getDataFolder").invoke(loader);
            } catch (Exception e) {
                return null;
            }
        }

        InputStream getResource(String name) {
            try {
                return (InputStream) clazz.getMethod("getResource", String.class).invoke(loader, name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
