package me.croabeast.sir.api.misc;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

/**
 * Represents a Java-Spigot JAR loader to load files, functions, and others.
 */
public interface JavaLoader {

    /**
     * Returns the name of the loader.
     * @return the loader's name
     */
    @NotNull String getName();

    /**
     * Returns the folder that the loader data's files are located in.
     * The folder may not yet exist.
     *
     * @return the loader's folder
     */
    @NotNull File getDataFolder();

    /**
     * Returns the resource inside the loader.
     *
     * @param name the file's name
     * @return the requested resource
     */
    @Nullable
    default InputStream getResource(String name) {
        if (StringUtils.isBlank(name)) return null;

        try {
            URL url = getClass().getClassLoader().getResource(name);
            if (url == null) return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            return connection.getInputStream();
        }
        catch (IOException ex) {
            return null;
        }
    }

    /**
     * Saves a resource in the loader's data folder.
     *
     * @param path the path of the file/resource
     * @param replace if the file should be replaced
     *
     */
    default void saveResource(String path, boolean replace) {
        if (StringUtils.isBlank(path)) return;

        path = path.replace('\\', '/');

        try {
            saveResourceFrom(getResource(path), getDataFolder(), path, replace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveResourceFrom(
            InputStream resource, File dataFolder, String path, boolean replace
    ) {
        if (path == null || resource == null)
            throw new NullPointerException("Path or resource is null");

        path = path.replace('\\', '/');
        int lastIndex = path.lastIndexOf('/');

        File out = new File(dataFolder, path);
        File dir = new File(dataFolder,
                path.substring(0, Math.max(lastIndex, 0)));

        if (!dir.exists()) dir.mkdirs();
        if (out.exists() && !replace)
            throw new UnsupportedOperationException("File already exists");

        try {
            OutputStream o = Files.newOutputStream(out.toPath());
            byte[] buf = new byte[1024];

            int len;
            while ((len = resource.read(buf)) > 0)
                o.write(buf, 0, len);

            o.close();
            resource.close();
        }
        catch (IOException e) {
            throw new UnsupportedOperationException(e.getLocalizedMessage());
        }
    }
}
