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

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IgnoreCommand extends SIRCommand {

    private static final String MAIN_PATH = "commands.ignore.";

    IgnoreCommand() {
        super("ignore");
    }

    static String getChannelTypeName(boolean isChat) {
        return YAMLCache.getLang().get(
                MAIN_PATH + "channels." + (isChat ? "chat" : "msg"),
                String.class
        );
    }

    static final String[] KEYS = {"{target}", "{type}"};

    class SettingsChanger {

        private final IgnoreSettings settings;
        private final boolean isChat;
        private final String channelType;

        SettingsChanger(IgnoreSettings settings, boolean isChat) {
            this.settings = settings;
            this.isChat = isChat;
            this.channelType = getChannelTypeName(isChat);
        }

        YAMLFile data() {
            return YAMLCache.fromData("ignore");
        }

        boolean change(Player player, String token) {
            Matcher matcher = Pattern.compile("(?i)@a").matcher(token);

            if (matcher.find()) {
                boolean value = !settings.isForAll(isChat);
                settings.setForAll(isChat, value);

                data().set("data." + player.getUniqueId(), settings);
                data().save();

                String[] values = {null, channelType};

                return MessageSender.fromLoaded()
                        .addKeysValues(KEYS, values)
                        .setTargets(player)
                        .send(YAMLCache.getLang().toList(
                                MAIN_PATH +
                                        (value ? "success" : "remove") +
                                        ".all"
                        ));
            }

            Player target = PlayerUtils.getClosest(token);
            if (target == null) {
                final String path = MAIN_PATH + "not-player";
                return fromSender(player, "{target}", token, path);
            }

            final Set<UUID> uuids = settings.getCache(isChat);

            UUID uuid = target.getUniqueId();
            if (!uuids.remove(uuid)) uuids.add(uuid);

            data().set("data." + player.getUniqueId(), settings);
            data().save();

            String[] values = {target.getName(), channelType};
            boolean value = uuids.contains(uuid);

            return MessageSender.fromLoaded()
                    .addKeysValues(KEYS, values)
                    .setTargets(player)
                    .send(YAMLCache.getLang().toList(
                            MAIN_PATH +
                                    (value ? "success" : "remove") +
                                    ".player"
                    ));
        }
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

            final int length = args.length;
            switch (length) {
                case 1: case 2:
                    return new SettingsChanger(
                            settings,
                            length == 2 && args[1].matches("(?i)-chat")
                    ).change(player, args[0]);

                default:
                    return isWrongArgument(sender, args[0]);
            }
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(getPlayersNames())
                .addArgument("@a")
                .addArgument(1, "-chat");
    }

    public static IgnoreSettings getSettings(Player player) {
        String path = "data." + player.getUniqueId();

        IgnoreSettings i = YAMLCache.fromData("ignore").get(path, IgnoreSettings.class);
        return i == null ? new IgnoreSettings(player) : i;
    }
}
