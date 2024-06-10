package me.croabeast.sir.api.addon;

import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.plugin.SIRPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

class AddonMngrImpl implements AddonManager {

    private final Map<String, SIRAddon> byName = new HashMap<>();
    private final Map<Class<? extends SIRAddon>, SIRAddon> byClass = new HashMap<>();

    static AddonManager manager;

    static void log(String string) {
        BeansLogger.getLogger().log(string);
    }

    AddonFile getDescription(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            return new AddonFile(jar);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Nullable
    public SIRAddon loadAddon(File file) {
        if (file == null || file.isDirectory() ||!file.exists())
            return null;

        final AddonFile description;
        try {
            description = getDescription(file);
        } catch (Exception e) {
            return null;
        }

        String name = description.getName();
        if (byName.containsKey(name)) return null;

        File folder = new File(file.getParentFile(), name);
        if (!folder.exists()) folder.mkdirs();

        try (AddonClassLoader loader = new AddonClassLoader(
                file,
                getClass().getClassLoader(),
                folder, description)
        ) {
            SIRAddon addon = loader.addon;
            addon.loaded = true;

            byName.put(addon.getName(), addon);
            byClass.put(addon.getClass(), addon);

            return addon;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean unloadAddon(SIRAddon addon) {
        if (addon == null || !addon.isLoaded())
            return true;

        String name = addon.getName();
        if (!byName.containsKey(name)) return false;

        try {
            if (addon.isEnabled() && !disableAddon(addon))
                return false;

            byName.remove(name);
            byClass.remove(addon.getClass());

            addon.loaded = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean enableAddon(SIRAddon addon) {
        if (addon == null || !addon.isLoaded()) return false;
        if (addon.isEnabled()) return true;

        String name = addon.getFullName();
        BeansLogger.getLogger().log("Enabling " + name);

        try {
            return addon.enabled = addon.enable();
        } catch (Exception e) {
            log("Error enabling " + name + ":");

            e.printStackTrace();
            return false;
        }
    }

    public boolean disableAddon(SIRAddon addon) {
        if (addon == null || !addon.isLoaded()) return false;
        if (!addon.isEnabled()) return true;

        String name = addon.getFullName();
        BeansLogger.getLogger().log("Disabling " + name);

        try {
            return !(addon.enabled = !addon.disable());
        } catch (Exception e) {
            log("Error disabling " + name + ":");

            e.printStackTrace();
            return false;
        }
    }

    @Nullable
    public <A extends SIRAddon> A getAddon(Class<A> clazz) {
        return clazz.cast(byClass.getOrDefault(clazz, null));
    }

    @Nullable
    public SIRAddon getAddon(String name) {
        return byName.getOrDefault(name, null);
    }

    @NotNull
    public List<SIRAddon> getAvailableAddons() {
        return CollectionBuilder.of(byName.values()).filter(SIRAddon::isEnabled).toList();
    }

    @NotNull
    public List<SIRAddon> getAddons() {
        return new ArrayList<>(byName.values());
    }

    void loadAllAddons() {
        File folder = SIRPlugin.fileFrom("addons");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] rawFiles = folder.listFiles();
        if (rawFiles == null) return;

        Set<SIRAddon> addons = new LinkedHashSet<>();

        for (File file : CollectionBuilder.of(rawFiles)
                .filter(File::exists)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".jar")))
        {
            SIRAddon addon = loadAddon(file);
            if (addon == null) continue;

            log("Loaded addon " + addon.getFullName());
            addons.add(addon);
        }

        for (SIRAddon addon : addons)
            if (!enableAddon(addon)) disableAddon(addon);
    }

    void unloadAllAddons() {
        File folder = SIRPlugin.fileFrom("addons");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        byName.values().forEach(this::unloadAddon);
    }
}
