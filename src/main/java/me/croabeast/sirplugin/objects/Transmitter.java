package me.croabeast.sirplugin.objects;

import me.croabeast.beanslib.utilities.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.modules.listeners.Formats;
import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.LangUtils;
import me.croabeast.sirplugin.utilities.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Transmitter {

    private List<String> stringList;

    private String[] keys = null, values = null;
    private boolean notRegistered = true;

    private Transmitter(ConfigurationSection section, String path) {
        stringList = TextUtils.toList(section, path);
    }

    private String removeSpace(String string) {
        return SIRPlugin.getUtils().removeSpace(string);
    }

    private void send(Player target, Player parser, String line) {
        SIRPlugin.getUtils().sendMessage(target, parser, line);
    }

    public Transmitter setKeys(String... keys) {
        this.keys = keys;
        return this;
    }

    public Transmitter setValues(String... values) {
        this.values = values;
        return this;
    }

    private void register(Player parser) {
        List<String> stringList = new ArrayList<>();

        for (String string : this.stringList) {
            if (StringUtils.isBlank(string)) continue;

            if (parser != null) {
                String[] k = {"{prefix}", "{suffix}"},
                        v = {Formats.getTag(parser, "prefix"), Formats.getTag(parser, "suffix")};
                string = TextUtils.replaceInsensitiveEach(string, k, v);
            }

            stringList.add(TextUtils.replaceInsensitiveEach(string, keys, values));
        }

        this.stringList = stringList;
        notRegistered = false;
    }

    public void runCommands(Player sender, String string) {
        if (sender != null && string.matches("(?i)^\\[player]")) {
            Bukkit.dispatchCommand(sender, removeSpace(string.substring(8)));
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
    }

    public void runCommands(Player parser) {
        if (notRegistered) register(parser);
        for (String s : stringList) runCommands(parser, s);
    }

    public void runCommands(Collection<? extends Player> targets) {
        if (notRegistered) register(null);
        for (Player player : targets) runCommands(player);
    }

    public void display(Player target, Player parser) {
        final String plMatch = "(?i)^\\[player]", cmdMatch = "(?i)^\\[cmd]";

        if (notRegistered) register(parser);
        if (stringList.isEmpty()) return;

        if (target == null) target = parser;

        for (String string : stringList) {
            String line = LangUtils.parseInternalKeys(string, "player", parser.getName());
            line = LangUtils.parseInternalKeys(line, "world", parser.getWorld().getName());

            if (FileCache.CONFIG.get().getBoolean("options.send-console"))
                if (!line.matches(cmdMatch)) LogUtils.doLog(line);

            if (line.matches(cmdMatch)) {
                runCommands(parser, removeSpace(line.substring(5)));
                continue;
            }

            send(target, parser, line.matches(plMatch) ? removeSpace(line.substring(8)) : line);
        }
    }

    public void display(Player parser) {
        display((Player) null, parser);
    }

    public void display(Collection<? extends Player> targets, Player parser) {
        for (Player player : targets)
            if (player != null) display(player, parser);
    }

    public void display(Collection<? extends Player> targets) {
        for (Player player : targets)
            if (player != null) display(player);
    }

    public static Transmitter to(ConfigurationSection section, String path) {
        return new Transmitter(section, path);
    }

    public static Transmitter to(String path) {
        return to(FileCache.LANG.get(), path);
    }
}
