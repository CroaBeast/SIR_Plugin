package me.croabeast.sirplugin.objects.extensions;

import me.croabeast.sirplugin.objects.files.FileCache;

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
    FILTERS,
    /**
     * Mentions a player in chat.
     */
    MENTIONS,
    /**
     * Creates prefixes and suffixes.
     */
    TAGS;

    /**
     * Converts the identifier to its name.
     * @return the module's name.
     */
    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }

    /**
     * Checks if the module is enabled in modules.yml
     * @return if the specified module is enabled.
     */
    public boolean isEnabled() {
        return FileCache.MODULES.get().getStringList("modules").contains(toString());
    }
}
