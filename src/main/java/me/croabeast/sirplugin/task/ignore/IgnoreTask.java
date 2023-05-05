package me.croabeast.sirplugin.task.ignore;

import com.google.common.collect.Lists;
import lombok.var;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IgnoreTask extends SIRTask {

    private static final String MAIN_PATH = "commands.ignore.";

    public IgnoreTask() {
        super("ignore");
    }

    @SuppressWarnings("all")
    private boolean changeSettings(IgnoreSettings settings, Player player, String type, String token) {
        final String[] keys = {"{target}", "{type}"};

        var cache = type.matches("(?i)CHAT") ?
                settings.getChatCache() : settings.getMsgCache();

        var uuid = player.getUniqueId();

        String t = FileCache.LANG.getValue(
                MAIN_PATH + "channels." + type.toLowerCase(Locale.ENGLISH), String.class);

        if (token.matches("(?i)@a")) {
            var b = !cache.isForAll();
            cache.setForAll(b);

            FileCache.IGNORE_DATA.get().set("data." + uuid, settings);
            FileCache.IGNORE_DATA.getFile().saveFile(false);

            String path = MAIN_PATH + (b ? "success" : "remove") + ".all";

            return getClonedSender(player).setKeys(keys).
                    setValues(null, t).
                    send(FileCache.LANG.toList(path));
        }

        var target = PlayerUtils.getClosestPlayer(token);
        if (target == null) return
                fromSender(player, "{target}", token, MAIN_PATH + "not-player");

        if (!cache.remove(target)) cache.add(target);

        FileCache.IGNORE_DATA.get().set("data." + uuid, settings);
        FileCache.IGNORE_DATA.getFile().saveFile(false);

        String path = MAIN_PATH +
                (cache.contains(target) ? "success" : "remove") + ".all";

        return getClonedSender(player).setKeys(keys).
                setValues(target, t).
                send(FileCache.LANG.toList(path));
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            LogUtils.doLog("&cYou can not ignore players in the console.");
            return true;
        }

        if (isProhibited(sender, "ignore")) return true;

        if (args.length == 0) return fromSender(sender, MAIN_PATH + "help");
        if (args.length == 1) return fromSender(sender, MAIN_PATH + "need-player");

        if (args.length > 2)
            return isWrongArgument(sender, args[args.length - 1]);

        final var player = ((Player) sender);

        var settings = getSettings(player);
        if (settings == null) settings = new IgnoreSettings(player);

        switch (args[0].toUpperCase(Locale.ENGLISH)) {
            case "CHAT": return changeSettings(settings, player, "CHAT", args[1]);
            case "MSG": return changeSettings(settings, player, "MSG", args[1]);

            default: return isWrongArgument(sender, args[0]);
        }
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1: return generateList(args, "chat", "msg");
            case 2: return generateList(args,
                    getPlayersNames(), Lists.newArrayList("@a"));
        }

        return new ArrayList<>();
    }

    public static IgnoreSettings getSettings(Player player) {
        return FileCache.IGNORE_DATA.getValue("data." + player.getUniqueId(), IgnoreSettings.class);
    }
}
