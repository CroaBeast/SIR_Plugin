package me.croabeast.sir.plugin.module.object;

import lombok.var;
import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser extends SIRModule implements CacheHandler {

    private static final Map<Integer, List<Mention>> MENTIONS_MAP = new LinkedHashMap<>();

    MentionParser() {
        super(ModuleName.MENTIONS);
    }

    @Override
    public void register() {
        loadCache();
    }

    @Priority(level = 1)
    static void loadCache() {
        if (!ModuleName.MENTIONS.isEnabled()) return;
        MENTIONS_MAP.clear();

        FileCache.MENTIONS_CACHE.getPermSections("mentions").forEach((k, v) -> {
            List<Mention> mentions = MENTIONS_MAP.get(k);
            if (mentions == null) mentions = new LinkedList<>();

            List<Mention> result = mentions;
            v.values().forEach(c -> result.add(new Mention(c)));

            MENTIONS_MAP.put(k, result);
        });
    }

    static Mention getMention(Player player) {
        for (var entries : MENTIONS_MAP.entrySet())
            for (var m : entries.getValue()) {
                if (PlayerUtils.hasPerm(player, m.permission))
                    return m;
            }

        return null;
    }

    static String stripJoiner(StringJoiner joiner) {
        return joiner.toString().replaceAll("\\\\Q", "").replaceAll("\\\\E", "");
    }

    public static String parseMentions(Player player, String string) {
        if (StringUtils.isBlank(string)) return string;
        if (!ModuleName.MENTIONS.isEnabled()) return string;

        final String plName = player.getName();
        String split = Beans.getLineSeparator();

        Mention mention = getMention(player);
        if (mention == null) return string;

        String prefix = mention.prefix;
        if (StringUtils.isBlank(prefix)) return string;

        var sound = mention.sound;

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

    static class Mention {

        private final String permission;
        private final String prefix, value;

        private final String click;
        private final List<String> hover;

        private Two sound = Two.EMPTY, messages = Two.EMPTY;

        Mention(ConfigurationSection s) {
            permission = s.getString("permission", "DEFAULT");

            prefix = s.getString("prefix", "");
            value = s.getString("value", "");

            click = s.getString("click", "");
            hover = TextUtils.toList(s, "hover");

            try {
                sound = new Two(s.getConfigurationSection("sound"));
            } catch (Exception ignored) {}
            try {
                messages = new Two(s.getConfigurationSection("messages"));
            } catch (Exception ignored) {}
        }
    }

    static class Two {

        static final Two EMPTY = new Two() {
            @Override
            boolean isEmpty() {
                return true;
            }
        };

        private List<String> sender = new ArrayList<>(),
                receiver = new ArrayList<>();

        Two() {}

        Two(ConfigurationSection s) {
            if (s == null)
                throw new NullPointerException();

            sender = TextUtils.toList(s, "sender");
            receiver = TextUtils.toList(s, "receiver");
        }

        boolean isEmpty() {
            return sender.isEmpty() && receiver.isEmpty();
        }
    }
}
