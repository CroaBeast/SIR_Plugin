package me.croabeast.sir.api.addon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * The AddonManager interface provides methods for managing addons within the plugin framework.
 * Addons can be loaded, unloaded, enabled, disabled, and retrieved using this interface.
 */
public interface AddonManager {

    /**
     * Loads an addon from the specified JAR file.
     *
     * @param file the JAR file containing the addon
     * @return the loaded addon instance, or null if loading fails
     */
    @Nullable SIRAddon loadAddon(File file);

    /**
     * Unloads the specified addon.
     *
     * @param addon the addon to unload
     * @return true if the addon was successfully unloaded, otherwise false
     */
    boolean unloadAddon(SIRAddon addon);

    /**
     * Enables the specified addon.
     *
     * @param addon the addon to enable
     * @return true if the addon was successfully enabled, otherwise false
     */
    boolean enableAddon(SIRAddon addon);

    /**
     * Disables the specified addon.
     *
     * @param addon the addon to disable
     * @return true if the addon was successfully disabled, otherwise false
     */
    boolean disableAddon(SIRAddon addon);

    /**
     * Gets the addon of the specified class.
     *
     * @param clazz the class of the addon to retrieve
     * @param <A>   the type of the addon
     * @return the addon instance of the specified class, or null if not found
     */
    @Nullable <A extends SIRAddon> A getAddon(Class<A> clazz);

    /**
     * Gets the addon with the specified name.
     *
     * @param name the name of the addon to retrieve
     * @return the addon instance with the specified name, or null if not found
     */
    @Nullable SIRAddon getAddon(String name);

    /**
     * Gets a list of only available addons.
     *
     * @return a list of available addons
     */
    @NotNull List<SIRAddon> getAvailableAddons();

    /**
     * Gets a list of all loaded addons.
     *
     * @return a list of loaded addons
     */
    @NotNull List<SIRAddon> getAddons();

    /**
     * Gets the singleton instance of the AddonManager.
     *
     * @return the AddonManager instance
     */
    @NotNull
    static AddonManager getManager() {
        AddonManager manager = AddonMngrImpl.manager;
        return manager == null ? (AddonMngrImpl.manager = new AddonMngrImpl()) : manager;
    }
}

