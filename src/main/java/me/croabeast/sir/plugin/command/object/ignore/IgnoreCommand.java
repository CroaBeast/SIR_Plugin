package me.croabeast.sir.plugin.command.object.ignore;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IgnoreCommand extends SIRCommand {

    private static final String MAIN_PATH = "commands.ignore.";

    public IgnoreCommand() {
        super("ignore");
    }

    @SuppressWarnings("all")
    private boolean changeSettings(IgnoreSettings settings, Player player, String token, boolean isMsg) {
        final String[] keys = {"{target}", "{type}"};
        final String type = isMsg ? "msg" : "chat";

        IgnoreSettings.Entry cache = !isMsg ?
                settings.getChatCache() : settings.getMsgCache();

        final UUID uuid = player.getUniqueId();

        String t = YAMLCache.getLang().get(
                MAIN_PATH + "channels." + type, String.class);

        if (token.matches("(?i)@a")) {
            boolean b = !cache.isForAll();
            cache.setForAll(b);

            YAMLFile file = YAMLCache.fromData("ignore");

            file.set("data." + uuid, settings);
            file.save();

            String path = MAIN_PATH + (b ? "success" : "remove") + ".all";

            return MessageSender.fromLoaded().setTargets(player)
                    .addKeysValues(keys, null, t)
                    .send(YAMLCache.getLang().toList(path));
        }

        Player target = PlayerUtils.getClosestPlayer(token);
        if (target == null) return
                fromSender(player, "{target}", token, MAIN_PATH + "not-player");

        if (!cache.remove(target)) cache.add(target);

        YAMLFile file = YAMLCache.fromData("ignore");

        file.set("data." + uuid, settings);
        file.save();

        String path = MAIN_PATH +
                (cache.contains(target) ? "success" : "remove") + ".all";

        return MessageSender.fromLoaded().setTargets(player)
                .addKeysValues(keys, target, t)
                .send(YAMLCache.getLang().toList(path));
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
            if (args.length > 2)
                return isWrongArgument(sender, args[args.length - 1]);

            Player player = ((Player) sender);
            IgnoreSettings settings = getSettings(player);

            if (args.length == 2 && args[1].matches("(?i)-chat"))
                return changeSettings(settings, player, args[1], false);

            if (args.length == 1)
                return changeSettings(settings, player, args[0], true);

            return isWrongArgument(sender, args[0]);
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(getPlayersNames())
                .addArgument(1, "-chat");
    }

    public static IgnoreSettings getSettings(Player player) {
        String path = "data." + player.getUniqueId();

        IgnoreSettings i = YAMLCache.fromData("ignore").get(path, IgnoreSettings.class);
        return i == null ? new IgnoreSettings(player) : i;
    }
}
