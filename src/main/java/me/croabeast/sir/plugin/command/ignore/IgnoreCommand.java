package me.croabeast.sir.plugin.command.ignore;

import lombok.Getter;
import me.croabeast.beans.BeansLib;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IgnoreCommand extends SIRCommand {

    static final String[] KEYS = {"{target}", "{type}"};

    @NotNull @Getter
    private final ConfigurableFile lang, data;

    IgnoreCommand() {
        super("ignore");

        this.lang = YAMLData.Command.Multi.IGNORE.from(true);
        this.data = YAMLData.Command.Multi.IGNORE.from(false);
    }

    class SettingsChanger {

        private final IgnoreSettings settings;
        private final boolean isChat;
        private final String channelType;

        SettingsChanger(IgnoreSettings settings, boolean isChat) {
            this.settings = settings;
            this.isChat = isChat;
            this.channelType = getLang().get(
                    "lang.channels." + (isChat ? "chat" : "msg"),
                    String.class
            );
        }

        boolean change(Player player, String token) {
            Matcher matcher = Pattern.compile("(?i)@a").matcher(token);

            if (matcher.find()) {
                boolean value = !settings.isForAll(isChat);
                settings.setForAll(isChat, value);

                getData().set("data." + player.getUniqueId(), settings);
                getData().save();

                String[] values = {null, channelType};

                return fromSender(player).addKeysValues(KEYS, values)
                        .send((value ? "success" : "remove") + ".all");
            }

            Player target = PlayerUtils.getClosest(token);
            if (target == null)
                return fromSender(player)
                        .addKeyValue("{target}", token)
                        .send("not-player");

            final Set<UUID> uuids = settings.getCache(isChat);

            UUID uuid = target.getUniqueId();
            if (!uuids.remove(uuid)) uuids.add(uuid);

            getData().set("data." + player.getUniqueId(), settings);
            getData().save();

            String[] values = {target.getName(), channelType};
            boolean value = uuids.contains(uuid);

            return fromSender(player).addKeysValues(KEYS, values)
                    .send((value ? "success" : "remove") + ".player");
        }
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            BeansLib.logger().log("&cYou can not ignore players in the console.");
            return true;
        }

        if (isProhibited(sender)) return true;
        if (args.length == 0) return fromSender(sender).send("help");

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
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return null;
    }

    public static IgnoreSettings getSettings(Player player) {
        String path = "data." + player.getUniqueId();

        IgnoreSettings i = YAMLData.Command.Multi.IGNORE.from(false).get(path, IgnoreSettings.class);
        return i == null ? new IgnoreSettings(player) : i;
    }

    public static boolean isIgnoring(Player source, Player target, boolean isChat) {
        IgnoreSettings s = getSettings(source);
        Set<UUID> cache = s.getCache(isChat);

        return s.isForAll(isChat) || (target != null && cache.contains(target.getUniqueId()));
    }
}
