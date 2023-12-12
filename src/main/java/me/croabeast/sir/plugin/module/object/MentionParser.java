package me.croabeast.sir.plugin.module.object;

import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser extends SIRModule implements CacheHandler {

    private static final Map<Integer, Set<Mention>> MENTIONS_MAP = new LinkedHashMap<>();

    MentionParser() {
        super(ModuleName.MENTIONS);
    }

    @Priority(level = 1)
    static void loadCache() {
        if (!ModuleName.MENTIONS.isEnabled()) return;
        MENTIONS_MAP.clear();

        FileCache.MENTIONS_CACHE
                .getUnitsByPriority("mentions")
                .forEach((k, v) -> {
                    Set<Mention> mentions = MENTIONS_MAP.get(k);
                    if (mentions == null)
                        mentions = new LinkedHashSet<>();

                    Set<Mention> result = mentions;
                    v.forEach(c -> result.add(new Mention(c)));

                    MENTIONS_MAP.put(k, result);
                });
    }

    static Mention getMention(Player player) {
        for (Map.Entry<Integer, Set<Mention>> e : MENTIONS_MAP.entrySet())
            for (Mention m : e.getValue())
                if (m.hasPerm(player)) return m;

        return null;
    }

    static Set<Mention> getMentions(Player player) {
        final Set<Mention> list = new LinkedHashSet<>();

        for (Map.Entry<Integer, Set<Mention>> e : MENTIONS_MAP.entrySet())
            for (Mention m : e.getValue())
                if (m.hasPerm(player)) list.add(m);

        return list;
    }

    static String stripJoiner(StringJoiner joiner) {
        return joiner.toString().replaceAll("\\\\[QE]", "");
    }

    static Mention from(String word, Map<String, String> map, Set<Mention> mentions) {
        String result = map.get(word);

        for (final Mention mention : mentions) {
            String prefix = mention.prefix;
            if (StringUtils.isBlank(prefix)) continue;

            if (result.startsWith(prefix))
                return mention;
        }

        return null;
    }

    public static String parsing(Player player, String string) {
        if (StringUtils.isBlank(string) ||
                !ModuleName.MENTIONS.isEnabled())
            return string;

        Set<Mention> mentions = getMentions(player);
        if (mentions.isEmpty()) return string;

        final String name = player.getName(),
                split = Beans.getLineSeparator();

        String[] words = string.split(" ");

        Map<String, String> map = new LinkedHashMap<>();
        for (String s : words) map.put(s, s);

        int count = 0;

        StringJoiner joiner = new StringJoiner(" ");

        for (String word : words) {
            Mention mention = from(word, map, mentions);
            if (mention == null) {
                joiner.add(word);
                continue;
            }

            Matcher matcher = Pattern
                    .compile("(?i)" + mention.prefix)
                    .matcher(word);

            if (!matcher.find()) {
                joiner.add(word);
                continue;
            }

            final String match = matcher.group();
            int index = word.lastIndexOf(match) + match.length();

            String raw = word.substring(index);
            Player target = PlayerUtils.getClosestPlayer(raw);

            if ((target == null || player == target) ||
                    PlayerUtils.isIgnoring(target, player, true)) {
                joiner.add(word);
                continue;
            }

            joiner.add(word);
        }

        return stripJoiner(joiner);
    }

    public static String parse(Player player, String string) {
        if (StringUtils.isBlank(string)) return string;
        if (!ModuleName.MENTIONS.isEnabled()) return string;

        final String plName = player.getName();
        String split = Beans.getLineSeparator();

        Mention mention = getMention(player);
        if (mention == null) return string;

        String prefix = mention.prefix;
        if (StringUtils.isBlank(prefix)) return string;

        Entry sound = mention.sound;

        final String[] stringArray = string.split(" ");
        boolean atLeastOne = false;
        Player firstTarget = null;

        String[] keys = {"{sender}", "{receiver}", "{prefix}"};
        MessageSender sender = MessageSender.fromLoaded().setKeys(keys);

        for (int i = 0; i < stringArray.length; i++) {
            final String word = stringArray[i];

            Matcher matcher = Pattern.compile("(?i)" + prefix).matcher(word);
            if (!matcher.find()) continue;

            final String match = matcher.group();
            int index = word.lastIndexOf(match) + match.length();

            String raw = word.substring(index);
            Player target = PlayerUtils.getClosestPlayer(raw);

            if (target == null || player == target) continue;
            if (PlayerUtils.isIgnoring(target, player, true)) continue;

            String[] values = {plName, target.getName(), prefix};
            if (firstTarget == null) firstTarget = target;

            atLeastOne = true;
            String output = mention.value;

            List<String> hover = mention.hover;
            String click = mention.click;

            boolean hasHover = !hover.isEmpty();
            boolean hasClick = StringUtils.isNotBlank(click);

            if (hasHover || hasClick) {
                StringBuilder builder = new StringBuilder();

                if (hasHover) {
                    StringJoiner joiner = new StringJoiner(split);
                    hover.forEach(joiner::add);

                    builder.append("<hover:")
                            .append('"')
                            .append(stripJoiner(joiner))
                            .append('"');
                }

                if (hasClick) {
                    builder.append(hasHover ? '|' : '<');

                    String[] array = click.split(":", 2);

                    builder.append(array[0].toLowerCase(Locale.ENGLISH))
                            .append(":\"")
                            .append(array[1]).append("\">");
                }
                else builder.append('>');

                if (builder.length() > 0) output = builder + output + "</text>";
                stringArray[i] = ValueReplacer.forEach(keys, values, output);
            }

            sender.clone().setValues(values)
                    .setTargets(target)
                    .send(mention.messages.receiver);

            if (!sound.receiver.isEmpty())
                PlayerUtils.playSound(target, sound.receiver.get(0));
        }

        if (atLeastOne) {
            sender.clone().setTargets(player)
                    .setValues(plName, firstTarget.getName(), prefix)
                    .send(mention.messages.sender);

            if (!sound.sender.isEmpty())
                PlayerUtils.playSound(player, sound.sender.get(0));
        }

        StringJoiner joiner = new StringJoiner(" ");
        for (String s : stringArray) joiner.add(s);

        return stripJoiner(joiner);
    }

    static class Mention implements ConfigUnit {

        ConfigUnit unit;
        String prefix, value;

        String click;
        List<String> hover;

        Entry sound = Entry.EMPTY, messages = Entry.EMPTY;

        Mention(ConfigUnit unit) {
            this.unit = unit;

            ConfigurationSection s = getSection();

            prefix = s.getString("prefix", "");
            value = s.getString("value", "");

            click = s.getString("click", "");
            hover = TextUtils.toList(s, "hover");

            try {
                sound = new Entry(s, "sound");
            } catch (Exception ignored) {}
            try {
                messages = new Entry(s, "messages");
            } catch (Exception ignored) {}
        }

        @Override
        public @NotNull ConfigurationSection getSection() {
            return unit.getSection();
        }
    }

    static class Entry {

        static final Entry EMPTY = new Entry() {
            @Override
            boolean isEmpty() {
                return true;
            }
        };

        private List<String> sender = new ArrayList<>(),
                receiver = new ArrayList<>();

        Entry() {}

        Entry(ConfigurationSection s, String path) {
            Objects.requireNonNull(s);

            s = s.getConfigurationSection(path);
            Objects.requireNonNull(s);

            sender = TextUtils.toList(s, "sender");
            receiver = TextUtils.toList(s, "receiver");
        }

        boolean isEmpty() {
            return sender.isEmpty() && receiver.isEmpty();
        }
    }
}
