package me.croabeast.sirplugin.module;

import lombok.*;
import me.croabeast.beanslib.utility.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

public class EmParser extends SIRModule {

    public static final List<Emoji> EMOJI_LIST = new ArrayList<>();

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.EMOJIS;
    }

    @Override
    public void registerModule() {
        if (!isEnabled()) return;
        if (!EMOJI_LIST.isEmpty()) EMOJI_LIST.clear();

        ConfigurationSection section = FileCache.EMOJIS.getSection("emojis");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Emoji emoji;

            try {
                emoji = new Emoji(section.getConfigurationSection(key));
            } catch (NullPointerException e) {
                continue;
            }

            EMOJI_LIST.add(emoji);
        }
    }

    public static String parseEmojis(Player player, String line) {
        try {
            if (!Identifier.EMOJIS.isEnabled()) return line;
            if (EMOJI_LIST.isEmpty()) return line;

            for (Emoji e : EMOJI_LIST)
                line = e.parseEmoji(player, line);
            return line;
        }
        catch (Exception e) {
            return line;
        }
    }

    static class Emoji {

        private final String permission, key, value;
        private final Checks checks;

        public Emoji(ConfigurationSection section) {
            if (section == null) throw new NullPointerException();

            permission = section.getString("permission", "DEFAULT");

            key = section.getString("key");
            value = section.getString("value");

            Checks checks = new Checks(false, false, true);
            try {
                checks = new Checks(section);
            }
            catch (NullPointerException ignored) {}

            this.checks = checks;
        }

        private String convertValue(String line) {
            String v = value == null ? "" : value;
            return v + IridiumAPI.getLastColor(line, key, true, true);
        }

        private Matcher getMatcher(String line, boolean add) {
            if (key == null) return null;
            String inCase = !checks.isSensitive() ? "(?i)" : "",
                    k = checks.isRegex() ? key : Pattern.quote(key);
            if (add) k = "^" + k + "$";
            return Pattern.compile(inCase + k).matcher(line);
        }

        public String parseEmoji(Player player, String line) {
            if (key == null) return line;

            if (player != null &&
                    !permission.matches("(?i)DEFAULT") &&
                    !player.hasPermission(permission)) return line;

            if (checks.isWord()) {
                StringBuilder builder = new StringBuilder();
                String[] words = line.split(" ");

                for (int i = 0; i < words.length; i++) {
                    String w = words[i];
                    Matcher match = getMatcher(IridiumAPI.stripAll(w), true);

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
            return TextUtils.classFormat(this, ":", true, key, value, checks);
        }

        @RequiredArgsConstructor
        @Getter
        private static class Checks {

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
                return TextUtils.classFormat(this, ":", true, regex, isWord, sensitive);
            }
        }
    }
}
