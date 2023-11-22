package me.croabeast.sir.plugin.command.object.ignore;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.Locale;

public class IgnoreTask extends SIRCommand {

    private static final String MAIN_PATH = "commands.ignore.";

    public IgnoreTask() {
        super("ignore");
    }

    @SuppressWarnings("all")
    private boolean changeSettings(IgnoreSettings settings, Player player, String type, String token) {
        final String[] keys = {"{target}", "{type}"};

        IgnoreSettings.DoubleObject cache = type.matches("(?i)CHAT") ?
                settings.getChatCache() : settings.getMsgCache();

        java.util.UUID uuid = player.getUniqueId();

        String t = FileCache.getLang().getValue(
                MAIN_PATH + "channels." + type.toLowerCase(Locale.ENGLISH), String.class);

        if (token.matches("(?i)@a")) {
            boolean b = !cache.isForAll();
            cache.setForAll(b);

            FileCache.IGNORE_DATA.get().set("data." + uuid, settings);
            FileCache.IGNORE_DATA.getFile().save(false);

            String path = MAIN_PATH + (b ? "success" : "remove") + ".all";

            return MessageSender.fromLoaded().setTargets(player)
                    .addKeysValues(keys, null, t)
                    .send(FileCache.getLang().toList(path));
        }

        Player target = PlayerUtils.getClosestPlayer(token);
        if (target == null) return
                fromSender(player, "{target}", token, MAIN_PATH + "not-player");

        if (!cache.remove(target)) cache.add(target);

        FileCache.IGNORE_DATA.get().set("data." + uuid, settings);
        FileCache.IGNORE_DATA.getFile().save(false);

        String path = MAIN_PATH +
                (cache.contains(target) ? "success" : "remove") + ".all";

        return MessageSender.fromLoaded().setTargets(player)
                .addKeysValues(keys, target, t)
                .send(FileCache.getLang().toList(path));
    }

    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
            if (!(sender instanceof Player)) {
                LogUtils.doLog("&cYou can not ignore players in the console.");
                return true;
            }

            if (isProhibited(sender, "ignore")) return true;

            if (args.length == 0) return fromSender(sender, MAIN_PATH + "help");
            if (args.length == 1) return fromSender(sender, MAIN_PATH + "need-player");

            if (args.length > 2)
                return isWrongArgument(sender, args[args.length - 1]);

            final Player player = ((Player) sender);

            IgnoreSettings settings = getSettings(player);
            if (settings == null) settings = new IgnoreSettings(player);

            switch (args[0].toUpperCase(Locale.ENGLISH)) {
                case "CHAT": return changeSettings(settings, player, "CHAT", args[1]);
                case "MSG": return changeSettings(settings, player, "MSG", args[1]);

                default: return isWrongArgument(sender, args[0]);
            }
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArguments("chat", "msg")
                .addArguments(1, getPlayersNames())
                .addArgument(1, "@a");
    }

    public static IgnoreSettings getSettings(Player player) {
        return FileCache.IGNORE_DATA.getValue("data." + player.getUniqueId(), IgnoreSettings.class);
    }
}
