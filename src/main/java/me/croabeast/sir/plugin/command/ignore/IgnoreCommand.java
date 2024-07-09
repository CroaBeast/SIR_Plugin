package me.croabeast.sir.plugin.command.ignore;

import lombok.Getter;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.command.Executable;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.lib.map.MapBuilder;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public final class IgnoreCommand extends SIRCommand {

    private static final YAMLData.Command.Multi IG_DATA = YAMLData.Command.Multi.IGNORE;
    static final String[] KEYS = {"{target}", "{type}"};

    @NotNull
    private final ConfigurableFile lang, data;

    IgnoreCommand() {
        super("ignore");

        this.lang = IG_DATA.from(true);
        this.data = IG_DATA.from(false);
    }

    static final Map<String, Object> DEFAULTS = new MapBuilder<String, Object>()
            .put("chat.for-all", false)
            .put("msg.for-all", false)
            .put("chat.list", new ArrayList<String>())
            .put("msg.list", new ArrayList<String>())
            .build();

    @NotNull
    public Executable getExecutable() {
        return Executable.from((sender, args) -> {
            if (!(sender instanceof Player)) {
                BeansLogger.getLogger()
                        .log("&cYou can not ignore players in the console.");
                return true;
            }

            if (!isPermitted(sender)) return true;
            if (args.length == 0) return createSender(sender).send("help");

            if (args.length > 2)
                return isWrongArgument(sender, args[args.length - 1]);

            final Player player = ((Player) sender);

            boolean isChat = args.length == 2 && args[1].matches("(?i)-chat");
            String channel = getLang().get(
                    "lang.channels." + (isChat ? "chat" : "msg"),
                    String.class
            );

            final ConfigurationSection data = getData(player);

            if (args[0].matches("(?i)@a")) {
                String path = (isChat ? "chat" : "msg") + ".for-all";

                data.set(path, !data.getBoolean(path));
                this.data.save();

                return createSender(player)
                        .addKeysValues(KEYS, null, channel)
                        .send((data.getBoolean(path) ?
                                "success" :
                                "remove") + ".all");
            }

            Player target = PlayerUtils.getClosest(args[0]);
            if (target == null)
                return createSender(player)
                        .addKeyValue("{target}", args[0]).send("not-player");

            String path = (isChat ? "chat" : "msg") + ".list";
            List<String> uuids = data.getStringList(path);

            String uuid = target.getUniqueId().toString();
            if (!uuids.remove(uuid)) uuids.add(uuid);

            data.set(path, uuids);
            this.data.save();

            return createSender(player)
                    .addKeysValues(KEYS, target.getName(), channel)
                    .send((uuids.contains(uuid) ?
                            "success" :
                            "remove") + ".player");
        });
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return PlayerUtils.createBuilder()
                .addArguments(0, getOnlineNames())
                .addArgument(0, "@a")
                .addArgument(1, "-chat");
    }

    public static ConfigurationSection getData(Player player) {
        ConfigurationSection s =
                IG_DATA.from(false).getSection("data." + player.getUniqueId());

        return s == null ?
                IG_DATA.from(false).getConfiguration()
                        .createSection(
                                "data." + player.getUniqueId(),
                                DEFAULTS
                        ) : s;
    }

    public static boolean isIgnoring(Player source, Player target, boolean isChat) {
        ConfigurationSection data = getData(source);

        String path = (isChat ? "chat" : "msg");
        String uuid = target == null ? null : (target.getUniqueId() + "");

        return data.getBoolean(path + ".for-all") ||
                (uuid != null && data
                        .getStringList(path + ".list")
                        .contains(uuid)
                );
    }
}
