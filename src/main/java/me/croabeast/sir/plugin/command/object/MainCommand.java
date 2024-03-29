package me.croabeast.sir.plugin.command.object;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sir.plugin.utility.CacheUtils;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.module.ModuleGUI;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.entity.Player;

import java.util.Locale;

class MainCommand extends SIRCommand {

    MainCommand() {
        super("sir", false);
    }

    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "admin.*")) return true;
            String version = SIRPlugin.getVersion();

            if (args.length == 0)
                return fromSender(sender, "{version}", version, "commands.sir.help");

            if (args.length > 1)
                return isWrongArgument(sender, args[args.length - 1]);

            MessageSender message = MessageSender.fromLoaded().setTargets(sender)
                    .setLogger(true);

            switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "modules":
                    Player player = sender instanceof Player ? (Player) sender : null;

                    if (player == null)
                        return message.send("&cThis command is only for players.");

                    if (LibUtils.MAIN_VERSION < 14.0)
                        return message.send(
                                "<P> &cModules GUI is not supported on this version.",
                                "<P> &7Enable/disable modules in data/modules.yml file"
                        );

                    if (isProhibited(player, "admin.modules")) return true;

                    ModuleGUI.showGUI(player);
                    return true;

                case "about":
                    return message.send(
                            "", " &eSIR &7- &f" + version + "&7:",
                            "   &8• &7Server Software: &f" + LibUtils.SERVER_FORK,
                            "   &8• &7Developer: &f" + SIRPlugin.getAuthor(),
                            "   &8• &7Java Version: &f" + SystemUtils.JAVA_VERSION, ""
                    );

                case "help":
                    return isProhibited(sender, "admin.help") ||
                            fromSender(sender, "{version}", version, "commands.sir.help");

                case "reload":
                    if (isProhibited(sender, "admin.reload")) return true;
                    long start = System.currentTimeMillis();

                    try {
                        CacheUtils.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        CacheUtils.load();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean b = YAMLCache.getMainConfig().get("options.send-console", true);
                    MessageSender.setLoaded(MessageSender.fromLoaded().setLogger(b));

                    return fromSender(sender,
                            "{time}", System.currentTimeMillis() - start,
                            "commands.sir.reload"
                    );

                case "support":
                    return isProhibited(sender, "admin.support") ||
                            fromSender(
                                    sender, "{link}",
                                    "https://discord.gg/s9YFGMrjyF",
                                    "commands.sir.support"
                            );

                default: return isWrongArgument(sender, args[0]);
            }
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArgument("sir.admin.modules", "modules")
                .addArgument("sir.admin.reload", "reload")
                .addArgument("sir.admin.support", "support")
                .addArgument("sir.admin.help", "help")
                .addArgument("sir.admin.about", "about");
    }
}
