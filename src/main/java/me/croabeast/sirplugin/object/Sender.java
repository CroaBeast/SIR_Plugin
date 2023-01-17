package me.croabeast.sirplugin.object;

import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.listener.Formats;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class Sender {

    private List<String> stringList;

    private String[] keys, values;
    private boolean isRegistered = false;

    private Sender(List<String> stringList) {
        this.stringList = stringList;
    }

    private String removeSpace(String string) {
        return TextUtils.removeSpace(string);
    }

    private String replace(String line, String[]... arrays) {
        return TextUtils.replaceInsensitiveEach(line, arrays[0], arrays[1]);
    }

    private void display(Player target, Player parser, String line) {
        LangUtils.create(target, parser, Collections.singletonList(line)).display();
    }

    public Sender setKeys(String... array) {
        keys = array;
        return this;
    }

    public Sender setValues(String... array) {
        values = array;
        return this;
    }

    public static boolean isStarting(String prefix, String line) {
        return line.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private String parsePlayerKeys(Player parser, String line) {
        if (parser == null) return line;
        String[] k = {"{prefix}", "{suffix}", "{player}", "{world}"},
                v = {
                        Formats.getTag(parser, true),
                        Formats.getTag(parser, false),
                };
        return SIRPlugin.getUtils().
                parsePlayerKeys(parser, replace(line, k, v), false);
    }

    private void registerKeys(Player parser) {
        if (isRegistered) return;
        if (stringList.isEmpty()) return;

        List<String> list = new ArrayList<>();

        for (String s : stringList) {
            if (s == null) continue;
            s = parsePlayerKeys(parser, s);
            list.add(replace(s, keys, values));
        }

        isRegistered = true;
        stringList = list;
    }

    private void messageLogger(String line) {
        if (!FileCache.CONFIG.value("options.send-console", true)) return;
        if (!isStarting("[cmd]", line)) LogUtils.doLog(line);
    }

    private void messageLogger(List<String> list) {
        if (!list.isEmpty()) list.forEach(this::messageLogger);
    }

    private void execute(Player parser, List<String> stringList, boolean onlyPrefix) {
        if (stringList.isEmpty()) return;

        for (String s : stringList) {
            if (StringUtils.isBlank(s)) continue;

            s = parsePlayerKeys(parser, s);
            if (onlyPrefix && !isStarting("[cmd]", s)) continue;

            if (isStarting("[cmd]", s)) s = s.substring(5);

            if (parser != null && isStarting("[player]", s)) {
                Bukkit.dispatchCommand(parser,
                        removeSpace(s.substring(8)));
                return;
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
        }
    }

    public void execute(Player parser, boolean onlyPrefix) {
        registerKeys(parser);
        execute(parser, stringList, onlyPrefix);
    }

    public void execute(Player parser) {
        execute(parser, false);
    }

    private void send(Player target, Player parser, boolean doLog) {
        registerKeys(parser);
        if (stringList.isEmpty()) return;

        if (target == null) target = parser;

        for (String s : stringList) {
            if (doLog) messageLogger(s);
            if (isStarting("[cmd]", s)) continue;

            if (isStarting("[player]", s))
                s = s.substring(8);
            display(target, parser, removeSpace(s));
        }
    }

    public void send(Player parser) {
        send(null, parser, true);
    }

    public void send(Collection<? extends Player> targets, Player parser) {
        targets = targets.stream().
                filter(Objects::nonNull).
                collect(Collectors.toList());

        if (!targets.isEmpty()) {
            for (Player player : targets) {
                if (parser == null) send(null, player, false);
                else send(player, parser, false);
            }
        }
        messageLogger(stringList);
    }

    public static Sender to(ConfigurationSection section, String path) {
        return new Sender(TextUtils.toList(section, path));
    }
}
