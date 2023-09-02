package me.croabeast.sir.plugin.module;

import me.croabeast.sir.plugin.module.instance.AnnounceHandler;
import me.croabeast.sir.plugin.module.instance.EmojiParser;
import me.croabeast.sir.plugin.module.instance.MentionParser;
import me.croabeast.sir.plugin.module.instance.listener.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModuleName<T extends SIRModule> {

    private static final List<ModuleName<?>> VALUES = new ArrayList<>();

    public static final ModuleName<JoinQuitHandler> JOIN_QUIT = new ModuleName<>("join-quit");
    public static final ModuleName<MotdHandler> MOTD = new ModuleName<>("motd");
    public static final ModuleName<ChatFormatter> CHAT_CHANNELS = new ModuleName<>("chat-channels");
    public static final ModuleName<?> DISCORD_HOOK = new ModuleName<>("discord-hook");
    public static final ModuleName<AdvanceHandler> ADVANCEMENTS = new ModuleName<>("advancements");
    public static final ModuleName<EmojiParser> EMOJIS = new ModuleName<>("emojis");
    public static final ModuleName<AnnounceHandler> ANNOUNCEMENTS = new ModuleName<>("announcements");
    public static final ModuleName<MentionParser> MENTIONS = new ModuleName<>("mentions");
    public static final ModuleName<ChatFilterer> CHAT_FILTERS = new ModuleName<>("chat-filters");
    public static final ModuleName<?> CHAT_COLORS = new ModuleName<>("chat-colors");

    private final String name;

    private ModuleName(String name) {
        this.name = name;
        VALUES.add(this);
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<ModuleName<?>> values() {
        return VALUES;
    }

    @SuppressWarnings("unchecked")
    public static <V extends SIRModule> V get(ModuleName<V> name) {
        return (V) SIRModule.MODULE_MAP.get(name);
    }

    public static boolean isEnabled(ModuleName<?> name) {
        return ModuleGUI.MODULE_STATUS_MAP.getOrDefault(name, false);
    }
}
