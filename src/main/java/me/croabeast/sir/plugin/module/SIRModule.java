package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.chat.*;
import me.croabeast.sir.plugin.module.hook.DiscordHook;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

@Getter
public abstract class SIRModule implements SIRExtension {

    public static final Data<JoinQuitHandler> JOIN_QUIT = new Data<>("join-quit", JoinQuitHandler.class);
    public static final Data<AdvanceHandler> ADVANCEMENTS = new Data<>("advancements", AdvanceHandler.class);
    public static final Data<MotdHandler> MOTD = new Data<>("motd", MotdHandler.class);
    public static final Data<AnnounceHandler> ANNOUNCEMENTS = new Data<>("announcements", AnnounceHandler.class);
    public static final Data<FilterHandler> FILTERS = new Data<>(Type.CHAT, "filters", FilterHandler.class);
    public static final Data<MentionParser> MENTIONS = new Data<>(Type.CHAT, "mentions", MentionParser.class);
    public static final Data<TagsParser> TAGS = new Data<>(Type.CHAT, "tags", TagsParser.class);
    public static final Data<ChannelHandler> CHANNELS = new Data<>(Type.CHAT, "channels", ChannelHandler.class);
    public static final Data<EmojiParser> EMOJIS = new Data<>(Type.CHAT, "emojis", EmojiParser.class);
    public static final Data<CooldownHandler> COOLDOWNS = new Data<>(Type.CHAT, "cooldowns", CooldownHandler.class);
    public static final Data<VanishHook> VANISH = new Data<>(Type.HOOK, "vanish", VanishHook.class);
    public static final Data<LoginHook> LOGIN = new Data<>(Type.HOOK, "login", LoginHook.class);
    public static final Data<DiscordHook> DISCORD = new Data<>(Type.HOOK, "discord", DiscordHook.class);

    private enum Type {
        DEFAULT(null),
        CHAT("chat."),
        HOOK("hook.");

        private final String name;

        Type(String name) {
            this.name = name;
        }
    }

    public static class Data<T extends SIRModule> {

        private final String name;
        private final Class<T> clazz;

        private Data(Type type, String name, Class<T> clazz) {
            this.clazz = clazz;

            if (type != Type.DEFAULT)
                name = type.name + name;

            this.name = name;
        }

        private Data(String name, Class<T> clazz) {
            this(Type.DEFAULT, name, clazz);
        }

        public SIRModule getModule() {
            return ModuleData.MODULE_MAP.getOrDefault(name, null);
        }

        public T getData() {
            try {
                return getModule() == null ? null : clazz.cast(getModule());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean isEnabled() {
            return getModule() != null && getModule().isEnabled();
        }
    }

    private final String name;
    final ConfigurableFile file = YAMLData.Module.getMain();

    protected SIRModule(String name) {
        this.name = name;
        ModuleData.MODULE_MAP.put(name, this);

        boolean enabled = file.get("modules." + name, false);
        ModuleData.STATUS_MAP.put(name, enabled);
    }

    @NotNull
    public File getDataFolder() {
        throw new UnsupportedOperationException("File can't be got");
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ModuleData.STATUS_MAP.getOrDefault(name, false);
    }

    public boolean register() {
        return true;
    }

    @Override
    public String toString() {
        return "SIRModule{name='" + name + '}';
    }

    public static void showGUI(Player player) {
        ModuleData.MODULES_MENU.showGUI(Objects.requireNonNull(player));
    }
}
