package me.croabeast.sirplugin.task;

import me.croabeast.sirplugin.module.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;

import java.util.*;

public class BroadCmd extends SIRTask {

    @Override
    public String getName() {
        return "announcer";
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Announcer announcer = (Announcer) SIRModule.getModule(Identifier.ANNOUNCES);

        if (hasNoPerm(sender, "announcer.*")) return true;
        if (args.length == 0) return oneMessage(sender, "commands.announcer.help");
        if (args.length > 2) return notArgument(sender, args[args.length - 1]);

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "start":
                if (hasNoPerm(sender, "announcer.start")) return true;
                if (args.length > 1) return notArgument(sender, args[args.length - 1]);
                if (announcer.isRunning())
                    return oneMessage(sender, "commands.announcer.cant-start");

                announcer.startTask();
                return oneMessage(sender, "commands.announcer.started");

            case "cancel":
                if (hasNoPerm(sender, "announcer.cancel")) return true;
                if (args.length > 1) return notArgument(sender, args[args.length - 1]);
                if (!announcer.isRunning())
                    return oneMessage(sender, "commands.announcer.cant-stop");

                announcer.cancelTask();
                return oneMessage(sender, "commands.announcer.stopped");

            case "reboot":
                if (hasNoPerm(sender, "announcer.reboot")) return true;
                if (args.length > 1) return notArgument(sender, args[args.length - 1]);
                if (!announcer.isRunning()) announcer.startTask();

                return oneMessage(sender, "commands.announcer.rebooted");

            case "preview":
                if (hasNoPerm(sender, "announcer.preview")) return true;

                if (sender instanceof ConsoleCommandSender) {
                    LogUtils.doLog("&cYou can't preview an announce in console.");
                    return true;
                }

                if (args.length == 1 || announcer.getSection() == null)
                    return oneMessage(sender, "commands.announcer.select");

                ConfigurationSection id = announcer.getSection().getConfigurationSection(args[1]);
                if (id == null) return oneMessage(sender, "commands.announcer.select");

                announcer.runSection(id);
                return true;

            default: return notArgument(sender, args[args.length - 1]);
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return resultList(args, "start", "preview", "cancel", "reboot");

        if(args.length == 2 && args[0].matches("(?i)preview")) {
            ConfigurationSection id = FileCache.ANNOUNCES.getSection("announces");
            return id != null ?
                    resultList(args, id.getKeys(false)) :
                    resultList(args, "NOT_FOUND");
        }

        return new ArrayList<>();
    }
}
