package me.croabeast.sir.plugin.command;

import java.io.File;
import java.util.Locale;

public enum CommandName {
    ANNOUNCER,
    CHAT_VIEW,
    IGNORE,
    MESSAGE,
    REPLY,
    TEMP_MUTE,
    MUTE,
    UN_MUTE,
    PRINT, SIR;

    private final String name;
    final String toFolder;

    CommandName() {
        name = name().toLowerCase(Locale.ENGLISH);
        toFolder = name.replace('-', '_');
    }

    public boolean isEnabled() {
        return true;
    }

    public String getFolderName() {
        return toFolder;
    }

    public String getFolderPath() {
        return "commands" + File.separator + toFolder;
    }

    @Override
    public String toString() {
        return name;
    }
}
