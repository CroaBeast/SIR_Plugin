package me.croabeast.sir.api;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.util.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * The ResourceUtils class provides utility methods for handling resources and files.
 */
@UtilityClass
public class ResourceUtils {

    /**
     * Saves the specified resource to the given file path within the data folder.
     *
     * @param resource The input stream of the resource to save.
     * @param dataFolder The data folder where the resource will be saved.
     * @param path The file path within the data folder to save the resource to.
     * @param replace Whether to replace the existing file if it already exists.
     *
     * @throws NullPointerException if the path or resource is null.
     * @throws UnsupportedOperationException if the file already exists and replace is false, or if an IO error occurs during saving.
     */
    public void saveResource(InputStream resource, File dataFolder, String path, boolean replace) {
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
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Saves the specified resource to the given file path within the data folder.
     * Existing files will not be replaced.
     *
     * @param resource The input stream of the resource to save.
     * @param dataFolder The data folder where the resource will be saved.
     * @param path The file path within the data folder to save the resource to.
     *
     * @throws NullPointerException if the path or resource is null.
     * @throws UnsupportedOperationException if the file already exists or if an IO error occurs during saving.
     */
    public void saveResource(InputStream resource, File dataFolder, String path) {
        saveResource(resource, dataFolder, path, false);
    }

    /**
     * Creates a File object by resolving the specified child file paths against the given parent directory.
     *
     * @param parent The parent directory to resolve the child file paths against.
     * @param childPaths The relative paths of the child files.
     * @return The File object representing the specified child file paths within the parent directory.
     * @throws NullPointerException if the parent directory is null.
     */
    public File fileFrom(File parent, String... childPaths) {
        Objects.requireNonNull(parent);

        if (ArrayUtils.isArrayEmpty(childPaths))
            return parent;

        for (String child : childPaths)
            parent = new File(parent, child);

        return parent;
    }
}
