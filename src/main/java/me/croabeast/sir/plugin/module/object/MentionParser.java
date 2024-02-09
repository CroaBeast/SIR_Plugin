package me.croabeast.sir.plugin.module.object;

import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.ChatMessageBuilder;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser extends SIRModule implements CacheManageable {

    private static final Map<Integer, Set<Mention>> MENTIONS_MAP = new LinkedHashMap<>();

    MentionParser() {
        super(ModuleName.MENTIONS);
    }

    @Priority(1)
    static void loadCache() {
        if (!ModuleName.MENTIONS.isEnabled()) return;
        MENTIONS_MAP.clear();

        YAMLCache.getMentions()
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

    static Mention from(String word, Set<Mention> mentions) {
        for (final Mention mention : mentions) {
            String prefix = mention.prefix;

            if (StringUtils.isBlank(prefix)) continue;
            if (word.startsWith(prefix)) return mention;
        }

        return null;
    }

    public static String parse(Player player, ChatChannel channel, String string) {
        if (StringUtils.isBlank(string) ||
                !ModuleName.MENTIONS.isEnabled())
            return string;

        Set<Mention> mentions = new LinkedHashSet<>();
        MENTIONS_MAP.forEach((k, v) -> mentions.addAll(v));

        if (mentions.isEmpty()) return string;

        String[] words = string.split(" ");

        UnaryOperator<String> op = null;

        List<String> messages = null;
        String firstSound = null;

        List<String> keys = ArrayUtils.toList(
                "{prefix}", "{sender}", "{receiver}"
        );

        StringBuilder builder = new StringBuilder();

        String lastColor = null;
        int modCount = 0;

        for (int i = 0; i < words.length; i++)  {
            String space = i == words.length - 1 ? "" : " ";

            String word = words[i];
            String temp = NeoPrismaticAPI.stripAll(word);

            String rawResult = word + space;

            Mention mention = from(temp, mentions);
            if (mention == null) {
                builder.append(rawResult);
                continue;
            }

            String prefix = mention.prefix;
            Pattern pattern = Pattern.compile("(?i)" + prefix);

            Matcher matcher = pattern.matcher(word);
            if (!matcher.find()) {
                builder.append(rawResult);
                continue;
            }

            final String match = matcher.group();
            int index = word.lastIndexOf(match) + match.length();

            String raw = word.substring(index);
            Player target = PlayerUtils.getClosestPlayer(raw);

            if ((target == null || player == target) ||
                    PlayerUtils.isIgnoring(target, player, true)) {
                builder.append(rawResult);
                continue;
            }

            if (!channel.getRecipients(player).contains(target)) {
                builder.append(rawResult);
                continue;
            }

            if (modCount < 2)
                try {
                    if (modCount == 0)
                        lastColor = NeoPrismaticAPI.getLastColor(word, word);
                    modCount++;
                } catch (Exception ignored) {}
            else lastColor = null;

            List<String> values = ArrayUtils.toList(
                    prefix, player.getName(), target.getName());

            if (op == null)
                op = s -> ValueReplacer.forEach(keys, values, s);

            MessageSender.fromLoaded().addFunctions(op)
                    .setLogger(false)
                    .setTargets(target)
                    .send(mention.messages.receiver);

            if (messages == null)
                messages = mention.messages.sender;

            final Entry e = mention.sound;

            if (!e.receiver.isEmpty())
                PlayerUtils.playSound(target, e.receiver.get(0));

            if (firstSound == null)
                firstSound = e.sender.get(0);

            List<String> hover = mention.hover;
            hover.replaceAll(op);

            String click = op.apply(mention.click);
            String[] c = click.split(":", 2);

            if (lastColor != null) rawResult = lastColor + rawResult;

            ChatMessageBuilder b =
                    new ChatMessageBuilder(rawResult)
                            .setHover(hover)
                            .setClick(c[0], c[1].replace("\"", ""));

            builder.append(b.toPatternString());
        }

        if (messages != null && !messages.isEmpty())
            MessageSender.fromLoaded().addFunctions(op)
                    .setLogger(false)
                    .setTargets(player)
                    .send(messages);

        PlayerUtils.playSound(player, firstSound);
        return builder.toString();
    }

    static class Mention implements ConfigUnit {

        ConfigUnit unit;
        String prefix, value;

        String click;
        List<String> hover;

        Entry sound = Entry.empty(), messages = Entry.empty();

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

        static Entry empty() {
            return new Entry();
        }
    }
}
