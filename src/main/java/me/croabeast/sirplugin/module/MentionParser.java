package me.croabeast.sirplugin.module;

import lombok.var;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.object.instance.SIRModule;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MentionParser extends SIRModule {

    private static final List<Mention> MENTION_LIST = new ArrayList<>();

    public MentionParser() {
        super("mentions");
    }

    @Override
    public void registerModule() {
        if (!isEnabled()) return;
        if (!MENTION_LIST.isEmpty()) MENTION_LIST.clear();

        var section = FileCache.MENTIONS.getSection("mentions");
        if (section == null) return;

        for (var key : section.getKeys(false)) {
            var c = section.getConfigurationSection(key);

            if (c != null)
                MENTION_LIST.add(new Mention(c));
        }
    }

    public static String parseMention(Player player, String line) {
        if (!SIRModule.isEnabled("mentions")) return line;

        var id = FileCache.MENTIONS.permSection(player, "mentions");
        if (id == null) return line;

        Mention mention = null;
        for (var m : MENTION_LIST) if (m.section == id) mention = m;

        if (mention == null) return line;

        var prefix = mention.prefix;
        if (StringUtils.isBlank(prefix)) return line;

        Player target = null;

        for (var word : line.split(" ")) {
            var matcher = Pattern.compile("(?i)" + prefix).matcher(word);
            if (!matcher.find()) continue;

            var match = matcher.group();
            target = PlayerUtils.getClosestPlayer(
                    word.substring(word.
                    lastIndexOf(match) + match.length())
            );
        }

        if (target == null || player == target) return line;
        if (PlayerUtils.isIgnoring(target, player, true)) return line;

        String[] keys = {"{sender}", "{receiver}", "{prefix}"},
                values = {player.getName(), target.getName(), prefix};

        var sender = LangUtils.getSender().
                setKeys(keys).
                setValues(values);

        sender.clone().setTargets(player).
                send(mention.messages.sender);
        sender.clone().setTargets(target).
                send(mention.messages.receiver);

        var sound = mention.sound;

        if (!sound.sender.isEmpty())
            PlayerUtils.playSound(player, sound.sender.get(0));
        if (!sound.receiver.isEmpty())
            PlayerUtils.playSound(target, sound.receiver.get(0));

        var output = mention.value;

        var hover = mention.hover;
        var click = mention.click;

        var not = !hover.isEmpty();

        if (not || StringUtils.isNotBlank(click)) {
            var format = "";

            var split = SIRPlugin.getUtils().getLineSeparator();
            if (not)
                format += "<hover:\"" + String.join(split, hover).
                        replaceAll("\\\\Q", "").
                        replaceAll("\\\\E", "") + "\"";

            if (StringUtils.isNotBlank(click)) {
                String[] array = click.split(":", 2);
                format += (not ? "|" : "<") +
                        array[0] + ":\"" + array[1] + "\">";
            }
            else format += ">";

            if (StringUtils.isNotBlank(format))
                output = format + output + "</text>";
        }

        String result = ValueReplacer.forEach(output, keys, values),
                regex = prefix + target.getName();

        var match = Pattern.compile("(?i)" + regex).matcher(line);
        if (match.find())
            line = line.replace(match.group(), result +
                    IridiumAPI.getLastColor(line, regex, true, true));

        return line;
    }

    static class Mention {

        private final ConfigurationSection section;
        private final String prefix, value;

        private final String click;
        private final List<String> hover;

        private DoubleObject sound = DoubleObject.DEFAULT,
                messages = DoubleObject.DEFAULT;

        Mention(ConfigurationSection s) {
            section = s;

            prefix = s.getString("prefix", "");
            value = s.getString("value", "");

            click = s.getString("click", "");
            hover = TextUtils.toList(s, "hover");

            try {
                sound = new DoubleObject(s.getConfigurationSection("sound"));
            } catch (Exception ignored) {}
            try {
                messages = new DoubleObject(s.getConfigurationSection("messages"));
            } catch (Exception ignored) {}
        }

        static class DoubleObject {

            static final DoubleObject DEFAULT = new DoubleObject();

            private List<String> sender = new ArrayList<>(),
                    receiver = new ArrayList<>();

            DoubleObject() {}

            DoubleObject(ConfigurationSection s) {
                if (s == null)
                    throw new NullPointerException();

                sender = TextUtils.toList(s, "sender");
                receiver = TextUtils.toList(s, "receiver");
            }
        }
    }
}
