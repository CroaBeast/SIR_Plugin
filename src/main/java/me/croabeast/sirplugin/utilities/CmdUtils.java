package me.croabeast.sirplugin.utilities;

import com.google.common.collect.*;
import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;

import java.util.*;
import java.util.stream.*;

public class CmdUtils {

    private final SIRPlugin main = SIRPlugin.getInstance();

    private CommandSender sender;
    private String[] args;
    protected static HashMap<CommandSender, CommandSender> receivers = new HashMap<>();

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }
    public void setArgs(String[] args) {
        this.args = args;
    }

    public static HashMap<CommandSender, CommandSender> getReceivers() {
        return receivers;
    }
    
    protected void sendMessage(String path, String key, String value) {
        TextUtils.sendFileMsg(sender, path, key, value);
    }

    protected boolean oneMessage(String path, String[] keys, String[] values) {
        TextUtils.sendFileMsg(sender, path, keys, values);
        return true;
    }

    protected boolean oneMessage(String path, String key, String value) {
        sendMessage(path, key, value);
        return true;
    }

    protected boolean oneMessage(String path) {
        sendMessage(path, null, null);
        return true;
    }

    protected boolean hasNoPerm(String perm) {
        if (PermUtils.hasPerm(sender, "sir." + perm)) return false;
        sendMessage("no-permission", "perm", "sir." + perm);
        return true;
    }

    protected boolean notArgument(String arg) {
        sendMessage("wrong-arg", "arg", arg);
        return true;
    }

    protected Set<Player> targets(String input) {
        Player player = Bukkit.getPlayer(input);
        Set<Player> players = new HashSet<>();

        if (player == sender || player != null) return Collections.singleton(player);
        if (input.matches("(?i)@a")) return new HashSet<>(Bukkit.getOnlinePlayers());

        input = input.toUpperCase();

        if (input.startsWith("WORLD:")) {
            World w = Bukkit.getWorld(input.substring(6));
            if (w == null) return new HashSet<>();
            return new HashSet<>(w.getPlayers());
        }

        if (input.startsWith("PERM:")) {
            String perm = input.substring(5);
            Bukkit.getOnlinePlayers().stream().filter(p ->
                    PermUtils.hasPerm(p, perm)).forEach(players::add);
            return players;
        }

        if (input.startsWith("GROUP:")) {
            String group = input.substring(6);
            Bukkit.getOnlinePlayers().stream().filter(
                    p -> Initializer.hasVault() && Initializer.getPerms().
                            getPrimaryGroup(null, p).
                            matches("(?i)" + group)
            ).forEach(players::add);
            return players;
        }

        return new HashSet<>();
    }

    protected String rawMessage(String[] args, int initial) {
        StringBuilder builder = new StringBuilder();

        for (int i = initial; i < args.length; i++)
            builder.append(args[i]).append(" ");

        return builder.substring(0, builder.toString().length() - 1);
    }

    protected void sendReminder(String input) {
        Set<Player> set = targets(input);
        if (sender instanceof Player && set.size() == 1 &&
                set.contains((Player) sender)) return;

        if (set.isEmpty()) sendMessage("reminder.empty", "TARGET", input);
        else if (set.size() == 1) {
            String playerName = set.toArray(new Player[1])[0].getName();
            sendMessage("reminder.success", "TARGET", playerName);
        }
        else sendMessage("reminder.success", "TARGET", input);
    }

    protected void messageLogger(String type, String line) {
        String start = main.getLang().getString("logger.header");
        if (start == null || start.equals("")) return;
        line = TextUtils.colorize(null, line);
        LogUtils.doLog(start, "&7[" + type + "] " + line);
    }

    protected String isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ?
                main.getLang().getString("commands.msg-reply.console-name") : sender.getName();
    }

    @SafeVarargs
    protected final List<String> resultTab(Collection<String>... lists) {
        List<String> tab = new ArrayList<>();
        for (Collection<String> list : lists) tab.addAll(list);
        return StringUtil.copyPartialMatches(
                args[args.length - 1], tab, new ArrayList<>());
    }

    protected List<String> resultTab(String... array) {
        return resultTab(Lists.newArrayList(array));
    }

    protected List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
