package me.croabeast.sir.api;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents an extension for the SIR plugin.
 *
 * <p> Implementations of this interface provide methods to retrieve information about the extension's name,
 * its loading status, and whether it is enabled.
 */
public interface SIRExtension {

    /**
     * Gets the name of the extension.
     *
     * @return The name of the extension.
     */
    @NotNull String getName();

    /**
     * Gets the folder of the extension that stores all extension's data.
     *
     * @return The folder.
     */
    @NotNull File getDataFolder();

    /**
     * Checks if the extension is loaded.
     *
     * @return True if the extension is loaded, false otherwise.
     */
    boolean isLoaded();

    /**
     * Checks if the extension is enabled.
     *
     * @return True if the extension is enabled, false otherwise.
     */
    boolean isEnabled();
}
