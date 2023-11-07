package me.croabeast.sir.plugin.module;

import lombok.SneakyThrows;
import me.croabeast.sir.plugin.SIRPlugin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class ModuleName {

    private static final List<ModuleName> VALUES = new ArrayList<>();

    public static final ModuleName JOIN_QUIT = new ModuleName("join-quit");
    public static final ModuleName MOTD = new ModuleName("motd");
    public static final ModuleName CHAT_CHANNELS = new ModuleName("chat-channels");
    public static final ModuleName ADVANCEMENTS = new ModuleName("advancements");
    public static final ModuleName EMOJIS = new ModuleName("emojis");
    public static final ModuleName ANNOUNCEMENTS = new ModuleName("announcements");
    public static final ModuleName MENTIONS = new ModuleName("mentions");
    public static final ModuleName CHAT_FILTERS = new ModuleName("chat-filters");
    public static final ModuleName CHAT_COLORS = new ModuleName("chat-colors");
    public static final ModuleName DISCORD_HOOK = new ModuleName("discord-hook");

    private final String name;
    final String toFolder;

    @SneakyThrows
    private ModuleName(String name) {
        SIRPlugin.checkAccess(ModuleName.class);

        this.name = name;
        toFolder = name.replace('-', '_');

        VALUES.add(this);
    }

    public boolean isEnabled() {
        return ModuleGUI.MODULE_STATUS_MAP.getOrDefault(this, false);
    }

    public String folderName() {
        return toFolder;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<ModuleName> values() {
        return new LinkedList<>(VALUES);
    }
}
