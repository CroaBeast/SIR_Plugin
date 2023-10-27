package me.croabeast.sir.plugin.task.object;

import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.object.AnnounceHandler;
import me.croabeast.sir.plugin.task.SIRTask;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnounceTask extends SIRTask {

    AnnounceTask() {
        super("announcer");
    }

    private static ConfigurationSection announceSection() {
        return FileCache.ANNOUNCE_CACHE.getCache("announces").getSection("announces");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
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
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return generateList(args, "start", "preview", "cancel", "reboot");

        if (args.length == 2 && args[0].matches("(?i)preview")) {
            ConfigurationSection id = announceSection();

            return id != null ?
                    generateList(args, id.getKeys(false)) :
                    generateList(args, "NOT_FOUND");
        }

        return new ArrayList<>();
    }
}
