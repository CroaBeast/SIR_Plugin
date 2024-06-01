package me.croabeast.sir.api.addon;

import me.croabeast.sir.api.ResourceIOUtils;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.plugin.SIRPlugin;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public abstract class SIRAddon implements SIRExtension {

    private ClassLoader loader;

    private File file;
    private File dataFolder;

    private AddonFile description;

    boolean loaded = false;
    boolean enabled = false;

    public SIRAddon() {
        ClassLoader loader = getClass().getClassLoader();
        if (!(loader instanceof AddonClassLoader))
            throw new IllegalStateException("This addon requires " + AddonClassLoader.class.getName());

        ((AddonClassLoader) loader).initialize(this);
    }

    public final ClassLoader getClassLoader() {
        return loader;
    }

    public final AddonFile getDescriptionFile() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public final String getName() {
        return description.getName();
    }

    @NotNull
    public final String getFullName() {
        return description.getName() + ' ' + description.getVersion();
    }

    public final File getFile() {
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public final File getDataFolder() {
        return dataFolder;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isLoaded() {
        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isEnabled() {
        return enabled;
    }

    protected abstract boolean enable();

    protected abstract boolean disable();

    @Nullable
    public final InputStream getResource(String name) {
        if (StringUtils.isBlank(name)) return null;

        try {
            URL url = getClassLoader().getResource(name);
            if (url == null) return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            return connection.getInputStream();
        }
        catch (IOException ex) {
            return null;
        }
    }

    public final void saveResource(String path, boolean replace) {
        if (StringUtils.isBlank(path)) return;

        try {
            path = path.replace('\\', '/');
            ResourceIOUtils.saveResource(getResource(path), getDataFolder(), path, replace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, SIRPlugin.getInstance());
    }

    final void initialize(AddonClassLoader loader, File file, File folder, AddonFile description) {
        this.loader = loader;
        this.file = file;
        this.description = description;
        this.dataFolder = folder;
    }
}
