package me.croabeast.sirplugin.modules;

import me.croabeast.sirplugin.*;
import org.bukkit.configuration.file.*;

/**
 * This class represents a module used for each feature.
 */
public abstract class BaseModule {

    /**
     * The modules.yml file of this plugin.
     * @return the modules.yml file
     */
    private static FileConfiguration modulesFile() {
        return SIRPlugin.getInstance().getModules();
    }

    /**
     * Checks if the module is enabled in modules.yml
     * @param identifier the module's respective identifier.
     * @return if the specified module is enabled.
     */
    public static boolean isEnabled(Identifier identifier) {
        String name = identifier.toString().toLowerCase();
        return modulesFile().getStringList("modules").contains(name);
    }

    /**
     * Checks if the module is enabled in modules.yml
     * @return if the specified module is enabled.
     */
    protected boolean isEnabled() {
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
        JOIN_QUIT("join-quit"),
        /**
         * For global announcements.
         */
        ANNOUNCES("announces"),
        /**
         * Manages the server MOTD.
         */
        MOTD("motd"),
        /**
         * Changes the default chat format.
         */
        FORMATS("formats"),
        /**
         * Enables the DiscordSRV hook.
         */
        DISCORD("discord"),
        /**
         * For custom advance messages.
         */
        ADVANCES("advances"),
        /**
         * Parses all the custom emoticons.
         */
        EMOJIS("emojis"),
        /**
         * Denies some words in the chat.
         */
        FILTERS("filters");

        /**
         * The module's name.
         */
        private final String name;

        /**
         * Basic Identifier constructor.
         * @param name the module's name.
         */
        Identifier(String name) {
            this.name = name;
        }

        /**
         * Converts the identifier to its name.
         * @return the module's name.
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
