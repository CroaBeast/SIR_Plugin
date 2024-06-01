package me.croabeast.sir.api.file;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

final class FileLoader {

    @Getter
    private final Object loader;

    private final Method resourceMethod;
    private final Method folderMethod;

    <T> FileLoader(T loader) throws IOException {
        Class<?> clazz = loader.getClass();

        try {
            resourceMethod = clazz.getMethod("getResource", String.class);
            folderMethod = clazz.getMethod("getDataFolder");

            this.loader = loader;
        } catch (Exception e) {
            throw new IOException("Loader object isn't valid");
        }
    }

    InputStream getResource(String name) {
        try {
            return (InputStream) resourceMethod.invoke(loader, name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    File getDataFolder() {
        try {
            return (File) folderMethod.invoke(loader);
        } catch (Exception e) {
            return null;
        }
    }
}
