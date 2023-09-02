package me.croabeast.sir.plugin.module.instance;

import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionParser extends SIRModule {

    private static final List<Mention> MENTION_LIST = new ArrayList<>();

    public MentionParser() {
        super(ModuleName.MENTIONS);
    }

    @Override
    public void registerModule() {
        loadCache();
    }

    static void loadCache() {
        if (!ModuleName.isEnabled(ModuleName.MENTIONS)) return;
        if (!MENTION_LIST.isEmpty()) MENTION_LIST.clear();

        ConfigurationSection section = FileCache.MENTIONS_CACHE.getSection("mentions");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(key);
            if (c != null) MENTION_LIST.add(new Mention(c));
        }
    }

    public static String parse(Player player, String line) {
        if (StringUtils.isBlank(line)) return line;
        if (!ModuleName.isEnabled(ModuleName.MENTIONS)) return line;

        ConfigurationSection id = FileCache.MENTIONS_CACHE.permSection(player, "mentions");
        if (id == null) return line;

        Mention mention = null;
        for (Mention m : MENTION_LIST) if (m.section == id) mention = m;

        if (mention == null) return line;

        String prefix = mention.prefix;
        if (StringUtils.isBlank(prefix)) return line;

        Player target = null;

        for (String word : line.split(" ")) {
            Matcher matcher = Pattern.compile("(?i)" + prefix).matcher(word);
            if (!matcher.find()) continue;

            String match = matcher.group();
            target = PlayerUtils.getClosestPlayer(
                    word.substring(word.
                    lastIndexOf(match) + match.length())
            );
        }

        if (target == null || player == target) return line;
        if (PlayerUtils.isIgnoring(target, player, true)) return line;

        String[] keys = {"{sender}", "{receiver}", "{prefix}"},
                values = {player.getName(), target.getName(), prefix};

        MessageSender sender = MessageSender.fromLoaded().
                setKeys(keys).
                setValues(values);

        sender.clone().setTargets(player).
                send(mention.messages.sender);
        sender.clone().setTargets(target).
                send(mention.messages.receiver);

        DoubleObject sound = mention.sound;

        if (!sound.sender.isEmpty())
            PlayerUtils.playSound(player, sound.sender.get(0));
        if (!sound.receiver.isEmpty())
            PlayerUtils.playSound(target, sound.receiver.get(0));

        String output = mention.value;

        List<String> hover = mention.hover;
        String click = mention.click;

        boolean not = !hover.isEmpty();

        if (not || StringUtils.isNotBlank(click)) {
            String format = "";

            String split = SIRPlugin.getUtils().getLineSeparator();
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

        String result = ValueReplacer.forEach(keys, values, output),
                regex = prefix + target.getName();

        Matcher match = Pattern.compile("(?i)" + regex).matcher(line);
        if (match.find())
            line = line.replace(match.group(), result +
                    NeoPrismaticAPI.getLastColor(line, regex));

        return line;
    }

    static class Mention {

        private final ConfigurationSection section;
        private final String prefix, value;

        private final String click;
        private final List<String> hover;

        private DoubleObject sound = DoubleObject.EMPTY,
                messages = DoubleObject.EMPTY;

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
    }

    static class DoubleObject {

        static final DoubleObject EMPTY = new DoubleObject();

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
