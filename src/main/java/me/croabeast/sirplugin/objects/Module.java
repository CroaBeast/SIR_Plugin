package me.croabeast.sirplugin.objects;

import static me.croabeast.sirplugin.objects.FileCache.MODULES;

/**
 * This class represents a module used for each feature.
 */
public abstract class Module {

    /**
     * Checks if the module is enabled in modules.yml
     * @param identifier the module's respective identifier.
     * @return if the specified module is enabled.
     */
    public static boolean isEnabled(Identifier identifier) {
        return MODULES.toFile().getStringList("modules").contains(identifier + "");
    }

    /**
     * Checks if the module is enabled in modules.yml
     * @return if the specified module is enabled.
     */
    public boolean isEnabled() {
        return isEnabled(getIdentifier());
    }

    /**
     * The name of the module identifier in modules.yml file.
     * @return the identifier's name
     */
    public abstract Identifier getIdentifier();

    /**
     * Registers the module in the server.
     */
    public abstract void registerModule();

    /**
     * The Identifier enum class identifies the module based on its feature.
     */
    public enum Identifier {
        /**
         * For join and quit messages.
         */
        JOIN_QUIT,
        /**
         * For global announcements.
         */
        ANNOUNCES,
        /**
         * Manages the server MOTD.
         */
        MOTD,
        /**
         * Changes the default chat format.
         */
        FORMATS,
        /**
         * Enables the DiscordSRV hook.
         */
        DISCORD,
        /**
         * For custom advance messages.
         */
        ADVANCES,
        /**
         * Parses all the custom emoticons.
         */
        EMOJIS,
        /**
         * Denies some words in the chat.
         */
        FILTERS;

        /**
         * Converts the identifier to its name.
         * @return the module's name.
         */
        @Override
        public String toString() {
            return name().toLowerCase().replace("_", "-");
        }
    }
}
