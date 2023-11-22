package me.croabeast.sir.plugin.command.object;

import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.object.AnnounceHandler;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public class AnnounceTask extends SIRCommand {

    AnnounceTask() {
        super("announcer", false);
    }

    private static ConfigurationSection announceSection() {
        return FileCache.ANNOUNCE_CACHE.getCache("announces").getSection("announces");
    }

    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "announcer.*")) return true;

            if (args.length == 0) return fromSender(sender, "commands.announcer.help");
            if (args.length > 2) return isWrongArgument(sender, args[args.length - 1]);

            switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "start":
                    if (isProhibited(sender, "announcer.start")) return true;
                    if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                    if (AnnounceHandler.isRunning())
                        return fromSender(sender, "commands.announcer.cant-start");

                    AnnounceHandler.startTask();
                    return fromSender(sender, "commands.announcer.started");

                case "cancel":
                    if (isProhibited(sender, "announcer.cancel")) return true;
                    if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                    if (!AnnounceHandler.isRunning())
                        return fromSender(sender, "commands.announcer.cant-stop");

                    AnnounceHandler.cancelTask();
                    return fromSender(sender, "commands.announcer.stopped");

                case "reboot":
                    if (isProhibited(sender, "announcer.reboot")) return true;
                    if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                    if (!AnnounceHandler.isRunning()) AnnounceHandler.startTask();

                    return fromSender(sender, "commands.announcer.rebooted");

                case "preview":
                    if (isProhibited(sender, "announcer.preview")) return true;

                    if (!(sender instanceof Player)) {
                        LogUtils.doLog("&cYou can't preview an announce in console.");
                        return true;
                    }

                    ConfigurationSection section = announceSection();

                    if (args.length == 1 || section == null)
                        return fromSender(sender, "commands.announcer.select");

                    ConfigurationSection id = section.getConfigurationSection(args[1]);
                    if (id == null) return fromSender(sender, "commands.announcer.select");

                    AnnounceHandler.displayAnnounce(id);
                    return true;

                default: return isWrongArgument(sender, args[args.length - 1]);
            }
        };
    }

    @Override
    protected TabBuilder completer() {
        TabBuilder builder = TabBuilder.of()
                .addArgument("sir.announcer.preview", "preview")
                .addArgument("sir.announcer.start", "start")
                .addArgument("sir.announcer.cancel", "cancel")
                .addArgument("sir.announcer.reboot", "reboot");

        ConfigurationSection id = announceSection();
        return id != null ?
                builder.addArguments(1,
                        (s, a) -> a[0].matches("(?i)preview"),
                        id.getKeys(false)
                ) :
                builder;
    }
}
