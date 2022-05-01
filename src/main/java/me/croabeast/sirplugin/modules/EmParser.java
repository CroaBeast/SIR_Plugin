package me.croabeast.sirplugin.modules;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.configuration.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.utilities.Files.*;

public class EmParser extends BaseModule {

    protected List<Emoticon> emoticonList = new ArrayList<>();

    @Override
    public Identifier getIdentifier() {
        return Identifier.EMOJIS;
    }

    @Override
    public void registerModule() {
        if (!isEnabled()) return;
        if (!emoticonList.isEmpty()) emoticonList.clear();

        ConfigurationSection section =
                EMOJIS.toFile().getConfigurationSection("emojis");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            emoticonList.add(new Emoticon(
                    id.getString("key"),
                    id.getString("value"))
            );
        }
    }

    public String parseEmojis(String line) {
        for (Emoticon emoticon : emoticonList)
            line = emoticon.parseEmoji(line);
        return line;
    }

    public static class Emoticon {

        private final String key, value;

        public Emoticon(@Nullable String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        public String parseEmoji(String line) {
            if (key != null && value != null)
                line = line.replace(key, value);
            return line;
        }
    }
}
