package me.croabeast.sir.plugin.module.chat;

import lombok.Getter;
import me.croabeast.beans.builder.ChatBuilder;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.command.ignore.IgnoreCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionParser extends ChatModule {

    private static final Set<Mention> MENTION_SET = new HashSet<>();

    MentionParser() {
        super(Name.MENTIONS, YAMLData.Module.Chat.MENTIONS);
    }

    public boolean register() {
        if (!isEnabled()) return false;
        MENTION_SET.clear();

        ConfigurationSection c = config.getSection("mentions");
        if (c == null) return false;

        for (String key : c.getKeys(false)) {
            ConfigurationSection s = c.getConfigurationSection(key);
            if (s != null) MENTION_SET.add(new Mention(s));
        }

        return true;
    }

    static final String[] KEYS = {"{prefix}", "{sender}", "{receiver}"};

    public static String parse(Player player, ChatChannel channel, String string) {
        if (player == null ||
                StringUtils.isBlank(string) ||
                !MENTIONS.isEnabled())
            return string;

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
                        IgnoreCommand.isIgnoring(target, player, true))
                    continue;

                if (channel != null &&
                        !channel.getRecipients(player).contains(target))
                    continue;

                end = matcher.start();

                String finder = string.substring(start, end);
                start = matcher.end();

                String color = NeoPrismaticAPI.getLastColor(finder);

                String[] values = {
                        prefix,
                        player.getName(), target.getName()
                };

                UnaryOperator<String> op =
                        s -> ReplaceUtils.replaceEach(KEYS, values, s);
                if (operator == null) operator = op;

                MessageSender.loaded()
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

                ChatBuilder b = new ChatBuilder(result)
                        .setHover(hover)
                        .setClick(c[0], c[1].replace("\"", ""));

                String replace = b.toPatternString();
                if (color != null) replace += color;

                string = string.replace(matcher.group(), replace);
            }
        }

        if (firstMessages != null && !firstMessages.isEmpty())
            MessageSender.loaded()
                    .addFunctions(operator)
                    .setLogger(false)
                    .setTargets(player).send(firstMessages);

        PlayerUtils.playSound(player, firstSound);

        return string;
    }

    static class Mention implements ConfigUnit {

        @Getter
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
