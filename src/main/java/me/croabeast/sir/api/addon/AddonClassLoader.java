package me.croabeast.sir.api.addon;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

class AddonClassLoader extends URLClassLoader {

    private final JarFile jarFile;
    private final File file;
    private final File dataFolder;

    private SIRAddon addonInit;
    SIRAddon addon;
    private final AddonFile description;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    AddonClassLoader(File file, ClassLoader parent, File folder, AddonFile desc) throws IOException {
        super(new URL[] {file.toURI().toURL()}, parent);

        this.file = file;
        this.jarFile = new JarFile(file);

        this.dataFolder = folder;
        this.description = desc;

        final Class<?> main;
        try {
            main = Class.forName(desc.getMain(), true, this);
        } catch (Exception e) {
            throw new IOException(e);
        }

        final Class<? extends SIRAddon> clazz;
        try {
            clazz = main.asSubclass(SIRAddon.class);
        } catch (Exception e) {
            throw new IOException(e);
        }

        try {
            this.addon = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jarFile.close();
        }
    }

    synchronized void initialize(@NotNull SIRAddon addon) {
        Preconditions.checkArgument(
                addon.getClass().getClassLoader() == this,
                "Cannot initialize addon outside of this class loader"
        );

        if (this.addon != null || addonInit != null)
            throw new IllegalArgumentException("Plugin already initialized!");

        this.addonInit = addon;
        addon.initialize(this, file, dataFolder, description);
    }
}
