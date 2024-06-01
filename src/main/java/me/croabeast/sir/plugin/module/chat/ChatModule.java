package me.croabeast.sir.plugin.module.chat;

import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.SIRModule;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

abstract class ChatModule extends SIRModule {

    protected final ConfigurableFile config;

    protected ChatModule(Name name, YAMLData.Module.Chat chat) {
        super("chat." + name);

        config = chat.from();
    }

    public @NotNull File getDataFolder() {
        return config.getFile().getParentFile();
    }

    protected enum Name {
        CHANNELS,
        COOLDOWNS,
        TAGS,
        FILTERS,
        EMOJIS,
        MENTIONS;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }
}
