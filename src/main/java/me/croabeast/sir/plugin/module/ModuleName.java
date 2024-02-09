package me.croabeast.sir.plugin.module;

import java.io.File;
import java.util.Locale;

public enum ModuleName {
    JOIN_QUIT("join-quit"),
    MOTD,
    CHAT_CHANNELS("chat-channels"),
    CHAT_TAGS("chat-tags"),
    ADVANCEMENTS,
    EMOJIS,
    ANNOUNCEMENTS,
    MENTIONS,
    CHAT_FILTERS("chat-filters"),
    CHAT_COLORS("chat-colors"),
    DISCORD_HOOK("discord-hook");

    private final String name;
    final String toFolder;

    ModuleName(String name) {
        this.name = name;
        toFolder = name.replace('-', '_');
    }

    ModuleName() {
        name = name().toLowerCase(Locale.ENGLISH);
        toFolder = name.replace('-', '_');
    }

    public boolean isEnabled() {
        return ModuleGUI.MODULE_STATUS_MAP.getOrDefault(this, false);
    }

    public String getFolderName() {
        return toFolder;
    }

    public String getFolderPath() {
        return "modules" + File.separator + toFolder;
    }

    @Override
    public String toString() {
        return name;
    }
}
