package me.croabeast.sir.plugin.module.object;

import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.ChatMessageBuilder;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionsParser extends ModuleCache {

    private static final Set<Mention> MENTION_SET = new HashSet<>();

    MentionsParser() {
        super(ModuleName.MENTIONS);
    }

    @Priority(1)
    static void loadCache() {
        if (!ModuleName.MENTIONS.isEnabled()) return;
        MENTION_SET.clear();

        ConfigurationSection c = YAMLCache.getMentions().getSection("mentions");
        if (c == null) return;

        for (String key : c.getKeys(false)) {
            ConfigurationSection s = c.getConfigurationSection(key);
            if (s != null) MENTION_SET.add(new Mention(s));
        }
    }

    static final Pattern COLOR_PATTERN = Pattern.compile(
            "[&§][a-fk-or\\d]|[{]#([a-f\\d]{6})[}]|" +
                "<#([a-f\\d]{6})>|%#([a-f\\d]{6})%|" +
                "\\[#([a-f\\d]{6})]|&?#([a-f\\d]{6})|&x([a-f\\d]{6})"
    );

    static final String[] KEYS = {"{prefix}", "{sender}", "{receiver}"};

    public static String parse(Player player, ChatChannel channel, String string) {
        if (player == null || StringUtils.isBlank(string))
            return string;

        if (!ModuleName.MENTIONS.isEnabled()) return string;

        UnaryOperator<String> operator = null;
        String firstSound = null;
        List<String> firstMessages = null;

        for (Mention mention : MENTION_SET) {
            if (!mention.isInGroupAsNull(player)) continue;
            if (!mention.hasPerm(player)) continue;

            final String prefix = mention.prefix;
            if (StringUtils.isBlank(prefix)) continue;

            Pattern pattern = Pattern.compile(
                    Pattern.quote(prefix) + "(.[^ ]+)\\b");

            Matcher matcher = pattern.matcher(string);
            int start = 0, end;

            while (matcher.find()) {
                String rawPlayer = matcher.group(1);

                Player target = PlayerUtils.getClosest(rawPlayer);
                if ((target == null || player == target) ||
                        PlayerUtils.isIgnoring(target, player, true))
                    continue;

                if (channel != null &&
                        !channel.getRecipients(player).contains(target))
                    continue;

                end = matcher.start();

                String finder = string.substring(start, end);
                start = matcher.end();

                Matcher m = COLOR_PATTERN.matcher(finder);

                String color = null;
                while (m.find()) color = m.group();

                String[] values = {
                        prefix,
                        player.getName(), target.getName()
                };

                UnaryOperator<String> op =
                        s -> ValueReplacer.forEach(KEYS, values, s);
                if (operator == null) operator = op;

                MessageSender.fromLoaded()
                        .setTargets(target)
                        .setLogger(false)
                        .addFunctions(op)
                        .send(mention.messages.receiver);

                if (firstMessages == null)
                    firstMessages = mention.messages.sender;

                final Entry e = mention.sound;

                if (!e.receiver.isEmpty())
                    PlayerUtils.playSound(target, e.receiver.get(0));

                if (firstSound == null)
                    firstSound = e.sender.get(0);

                List<String> hover = mention.hover;
                hover.replaceAll(op);

                String click = op.apply(mention.click);
                String[] c = click.split(":", 2);

                String result = op.apply(mention.value);

                ChatMessageBuilder b = new ChatMessageBuilder(result)
                        .setHover(hover)
                        .setClick(c[0], c[1].replace("\"", ""));

                String replace = b.toPatternString();
                if (color != null) replace += color;

                string = string.replace(matcher.group(), replace);
            }
        }

        if (firstMessages != null && !firstMessages.isEmpty())
            MessageSender.fromLoaded()
                    .addFunctions(operator)
                    .setLogger(false)
                    .setTargets(player).send(firstMessages);

        PlayerUtils.playSound(player, firstSound);

        return string;
    }

    static class Mention implements ConfigUnit {

        ConfigurationSection section;
        String prefix, value;

        String click;
        List<String> hover;

        Entry sound = Entry.empty(), messages = Entry.empty();

        Mention(ConfigurationSection section) {
            this.section = section;

            prefix = section.getString("prefix", "");
            value = section.getString("value", "");

            click = section.getString("click", "");
            hover = TextUtils.toList(section, "hover");

            try {
                sound = new Entry(section, "sound");
            } catch (Exception ignored) {}
            try {
                messages = new Entry(section, "messages");
            } catch (Exception ignored) {}
        }

        @Override
        public @NotNull ConfigurationSection getSection() {
            return section;
        }

        @Override
        public String toString() {
            return "Mention{" +
                    "prefix='" + prefix + '\'' +
                    ", value='" + value + '\'' +
                    ", click='" + click + '\'' +
                    ", hover=" + hover +
                    ", sound=" + sound +
                    ", messages=" + messages +
                    '}';
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

        @Override
        public String toString() {
            return "Entry{sender=" + sender + ", receiver=" + receiver + '}';
        }
    }
}
