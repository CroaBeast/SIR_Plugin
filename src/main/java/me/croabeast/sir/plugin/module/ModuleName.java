package me.croabeast.sir.plugin.module;

import lombok.SneakyThrows;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.module.object.AnnounceHandler;
import me.croabeast.sir.plugin.module.object.EmojiParser;
import me.croabeast.sir.plugin.module.object.MentionParser;
import me.croabeast.sir.plugin.module.object.listener.*;

import java.util.ArrayList;
import java.util.List;

public final class ModuleName<T extends SIRModule> {

    private static final List<ModuleName<?>> VALUES = new ArrayList<>();

    public static final ModuleName<JoinQuitHandler> JOIN_QUIT = new ModuleName<>("join-quit");
    public static final ModuleName<MotdHandler> MOTD = new ModuleName<>("motd");
    public static final ModuleName<ChatFormatter> CHAT_CHANNELS = new ModuleName<>("chat-channels");
    public static final ModuleName<AdvanceHandler> ADVANCEMENTS = new ModuleName<>("advancements");
    public static final ModuleName<EmojiParser> EMOJIS = new ModuleName<>("emojis");
    public static final ModuleName<AnnounceHandler> ANNOUNCEMENTS = new ModuleName<>("announcements");
    public static final ModuleName<MentionParser> MENTIONS = new ModuleName<>("mentions");
    public static final ModuleName<ChatFilterer> CHAT_FILTERS = new ModuleName<>("chat-filters");

    public static final ModuleName<?> CHAT_COLORS = new ModuleName<>("chat-colors");
    public static final ModuleName<?> DISCORD_HOOK = new ModuleName<>("discord-hook");

    private final String name;

    @SneakyThrows
    private ModuleName(String name) {
        SIRPlugin.checkAccess(ModuleName.class);
        this.name = name;

        if (VALUES.contains(this))
            throw new UnsupportedOperationException();

        VALUES.add(this);
    }

    @SuppressWarnings("unchecked")
    public T get() {
        return (T) SIRModule.MODULE_MAP.get(this);
    }

    public boolean isEnabled() {
        return ModuleGUI.MODULE_STATUS_MAP.getOrDefault(this, false);
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<ModuleName<?>> values() {
        return new ArrayList<>(VALUES);
    }
}
