package me.croabeast.sir.plugin.module.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.plugin.file.YAMLData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmojiParser extends ChatModule {

    private static final List<Emoji> EMOJI_LIST = new ArrayList<>();

    EmojiParser() {
        super(Name.EMOJIS, YAMLData.Module.Chat.EMOJIS);
    }

    public boolean register() {
        if (!isEnabled()) return false;
        EMOJI_LIST.clear();

        ConfigurationSection section = config.getSection("emojis");
        if (section == null) return false;

        for (String key : section.getKeys(false)) {
            Emoji emoji;

            try {
                emoji = new Emoji(section.getConfigurationSection(key));
            } catch (NullPointerException e) {
                continue;
            }

            EMOJI_LIST.add(emoji);
        }

        return true;
    }

    public static String parse(Player player, String line) {
        try {
            if (!EMOJIS.isEnabled() || EMOJI_LIST.isEmpty())
                return line;

            for (Emoji e : EMOJI_LIST)
                line = e.parse(player, line);

            return line;
        }
        catch (Exception e) {
            return line;
        }
    }

    static class Emoji implements ConfigUnit {

        private final ConfigurationSection section;

        private final String key, value;
        private final Checks checks;

        Emoji(ConfigurationSection section) {
            if (section == null) throw new NullPointerException();
            this.section = section;

            key = section.getString("key");
            value = section.getString("value");

            Checks checks = new Checks(false, false, true);
            try {
                checks = new Checks(section);
            } catch (NullPointerException ignored) {}

            this.checks = checks;
        }

        @Override
        public @NotNull ConfigurationSection getSection() {
            return section;
        }

        String convertValue(String line) {
            return (value == null ? "" : value) + NeoPrismaticAPI.getLastColor(line);
        }

        Matcher getMatcher(String line, boolean add) {
            if (key == null) return null;

            String inCase = !checks.isSensitive() ? "(?i)" : "",
                    k = checks.isRegex() ? key : Pattern.quote(key);

            if (add) k = "^" + k + "$";
            return Pattern.compile(inCase + k).matcher(line);
        }

        String parse(Player player, String line) {
            if (StringUtils.isBlank(line) || key == null) return line;

            if (player != null && !hasPerm(player)) return line;

            if (checks.isWord()) {
                StringBuilder builder = new StringBuilder();
                String[] words = line.split(" ");

                for (int i = 0; i < words.length; i++) {
                    String w = words[i];
                    Matcher match = getMatcher(NeoPrismaticAPI.stripAll(w), true);

                    if (match == null) {
                        if (i > 0) builder.append(" ");
                        builder.append(w);
                        continue;
                    }

                    if (match.find()) {
                        if (i > 0) builder.append(" ");
                        builder.append(convertValue(line));
                        continue;
                    }

                    if (i > 0) builder.append(" ");
                    builder.append(w);
                }

                return builder.toString();
            }

            Matcher match = getMatcher(line, false);
            if (match == null) return line;

            while (match.find())
                line = line.replace(match.group(), convertValue(line));

            return line;
        }

        @Override
        public String toString() {
            return "Emoji{" +
                    "perm='" + getPermission() + '\'' +
                    ", key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", checks=" + checks + '}';
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class Checks {

        private final boolean regex, isWord, sensitive;

        public Checks(ConfigurationSection id) {
            if (id == null) throw new NullPointerException();

            id = id.getConfigurationSection("checks");
            if (id == null) throw new NullPointerException();

            regex = id.getBoolean("is-regex");
            isWord = id.getBoolean("is-word");
            sensitive = id.getBoolean("case-sensitive");
        }

        @Override
        public String toString() {
            return '{' + "regex=" + regex + ", isWord=" + isWord + ", sensitive=" + sensitive + '}';
        }
    }
}
