package me.croabeast.sir.plugin.task.object;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.ModuleGUI;
import me.croabeast.sir.plugin.task.SIRTask;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainTask extends SIRTask {

    MainTask() {
        super("sir");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "admin.*")) return true;
        String version = SIRPlugin.getVersion();

        if (args.length == 0)
            return fromSender(sender, "{version}", version, "commands.sir.help");

        if (args.length > 1)
            return isWrongArgument(sender, args[args.length - 1]);

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "modules":
                Player player = sender instanceof Player ? (Player) sender : null;

                if (player == null)
                    return getClonedSender(sender).
                            setLogger(true).
                            send("[SIR] &cThis command is only for players.");

                ModuleGUI.getModulesGUI().show(player);
                return true;

            case "about":
                return getClonedSender(sender).setLogger(true).
                        send(
                            "", " &eSIR &7- &f" + version + "&7:",
                            "   &8• &7Server Software: &f" + LibUtils.serverFork(),
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
                    CacheHandler.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    CacheHandler.load();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final FileCache config = FileCache.MAIN_CONFIG;

                MessageSender.setLoaded(
                        MessageSender.fromLoaded().
                        setLogger(
                                config.getValue("options.send-console", true)
                        ).
                        setNoFirstSpaces(
                                config.getValue("options.strip-spaces", false)
                        )
                );

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
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        return args.length == 1 ?
                generateList(args, "modules", "reload", "help", "support", "about") :
                new ArrayList<>();
    }
}
