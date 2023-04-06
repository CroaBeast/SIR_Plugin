package me.croabeast.sirplugin.task;

import lombok.var;
import me.croabeast.sirplugin.module.Announcer;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.object.instance.SIRModule;
import me.croabeast.sirplugin.object.instance.SIRTask;
import me.croabeast.sirplugin.utility.LogUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnounceTask extends SIRTask {

    public AnnounceTask() {
        super("announcer");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        var announcer = (Announcer) SIRModule.get("announces");

        if (isProhibited(sender, "announcer.*")) return true;
        if (args.length == 0) return fromSender(sender, "commands.announcer.help");
        if (args.length > 2) return isWrongArgument(sender, args[args.length - 1]);

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "start":
                if (isProhibited(sender, "announcer.start")) return true;
                if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                if (announcer.isRunning())
                    return fromSender(sender, "commands.announcer.cant-start");

                announcer.startTask();
                return fromSender(sender, "commands.announcer.started");

            case "cancel":
                if (isProhibited(sender, "announcer.cancel")) return true;
                if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                if (!announcer.isRunning())
                    return fromSender(sender, "commands.announcer.cant-stop");

                announcer.cancelTask();
                return fromSender(sender, "commands.announcer.stopped");

            case "reboot":
                if (isProhibited(sender, "announcer.reboot")) return true;
                if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
                if (!announcer.isRunning()) announcer.startTask();

                return fromSender(sender, "commands.announcer.rebooted");

            case "preview":
                if (isProhibited(sender, "announcer.preview")) return true;

                if (sender instanceof ConsoleCommandSender) {
                    LogUtils.doLog("&cYou can't preview an announce in console.");
                    return true;
                }

                var section = Announcer.getSection();

                if (args.length == 1 || section == null)
                    return fromSender(sender, "commands.announcer.select");

                var id = section.getConfigurationSection(args[1]);
                if (id == null) return fromSender(sender, "commands.announcer.select");

                announcer.runSection(id);
                return true;

            default: return isWrongArgument(sender, args[args.length - 1]);
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return generateList(args, "start", "preview", "cancel", "reboot");

        if(args.length == 2 && args[0].matches("(?i)preview")) {
            var id = FileCache.ANNOUNCEMENTS.getSection("announces");

            return id != null ?
                    generateList(args, id.getKeys(false)) :
                    generateList(args, "NOT_FOUND");
        }

        return new ArrayList<>();
    }
}
