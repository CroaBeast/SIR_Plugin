package me.croabeast.sirplugin.utility;

import com.google.common.collect.*;
import me.croabeast.beanslib.object.display.Displayer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.object.file.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;

import java.util.*;
import java.util.stream.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

public class CmdUtils {

    protected void oneMessage(CommandSender sender, String path, String[] k, String[] v, boolean b) {
        LangUtils.create(sender, null, FileCache.LANG.toList(path)).
                setKeys(k).setValues(v).setLogger(b).display();
    }

    protected void oneMessage(CommandSender sender, String path, String[] keys, String[] values) {
        oneMessage(sender, path, keys, values, true);
    }

    protected void sendMessage(CommandSender sender, String path, String key, String value) {
        oneMessage(sender, path, new String[] {"{" + key + "}"}, new String[] {value});
    }

    protected boolean oneMessage(CommandSender sender, String path, String key, String value) {
        sendMessage(sender, path, key, value);
        return true;
    }

    protected boolean oneMessage(CommandSender sender, String path) {
        return oneMessage(sender, path, (String) null, null);
    }

    protected boolean hasNoPerm(CommandSender sender, String perm) {
        if (PlayerUtils.hasPerm(sender, "sir." + perm)) return false;
        sendMessage(sender, "no-permission", "perm", "sir." + perm);
        return true;
    }

    protected boolean notArgument(CommandSender sender, String arg) {
        sendMessage(sender, "wrong-arg", "arg", arg);
        return true;
    }

    protected String rawMessage(String[] args, int initial) {
        if (initial >= args.length) return null;

        StringBuilder b = new StringBuilder();
        for (int i = initial; i < args.length; i++) b.append(args[i]).append(" ");

        return b.substring(0, b.toString().length() - 1);
    }

    @SafeVarargs
    protected final List<String> resultList(String[] args, Collection<String>... lists) {
        List<String> tab = new ArrayList<>();
        for (Collection<String> list : lists) tab.addAll(list);
        return StringUtil.copyPartialMatches(
                args[args.length - 1], tab, new ArrayList<>());
    }

    protected List<String> resultList(String[] args, String... array) {
        return resultList(args, Lists.newArrayList(array));
    }

    protected List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
