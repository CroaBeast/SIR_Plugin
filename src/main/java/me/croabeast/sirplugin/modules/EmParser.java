package me.croabeast.sirplugin.modules;

import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.LangUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmParser extends SIRModule {

    static List<Emoji> emojiList = new ArrayList<>();

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.EMOJIS;
    }

    @Override
    public void registerModule() {
        if (!isEnabled()) return;
        if (!emojiList.isEmpty()) emojiList.clear();

        ConfigurationSection section =
                FileCache.EMOJIS.get().getConfigurationSection("emojis");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            emojiList.add(new Emoji(
                    id.getString("key"),
                    id.getString("value"))
            );
        }
    }

    public static String parseEmojis(String line) {
        if (!Identifier.EMOJIS.isEnabled()) return line;
        if (emojiList.isEmpty()) return line;

        for (Emoji e : emojiList) line = e.parseEmoji(line);
        return line;
    }

    public static class Emoji {

        private final String key, value;

        public Emoji(@Nullable String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        public String parseEmoji(String line) {
            if (key != null && value != null) {
                Matcher match = Pattern.compile(Pattern.quote(key)).matcher(line);
                if (!match.find()) return line;

                line = line.replace(match.group(), value +
                        LangUtils.getLastColor(line, key, true));
            }
            return line;
        }
    }
}
