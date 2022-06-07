package me.croabeast.sirplugin.tasks;

import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;

import java.util.*;

public class BroadCmd extends SIRTask {

    @Override
    public String getName() {
        return "announcer";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            Announcer announcer = (Announcer) SIRModule.getModule(Identifier.ANNOUNCES);

            if (hasNoPerm("announcer.*")) return true;
            if (args.length == 0) return oneMessage("commands.announcer.help");
            if (args.length > 2) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "start":
                    if (hasNoPerm("announcer.start")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (announcer.isRunning())
                        return oneMessage("commands.announcer.cant-start");

                    announcer.startTask();
                    return oneMessage("commands.announcer.started");

                case "cancel":
                    if (hasNoPerm("announcer.cancel")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!announcer.isRunning())
                        return oneMessage("commands.announcer.cant-stop");

                    announcer.cancelTask();
                    return oneMessage("commands.announcer.stopped");

                case "reboot":
                    if (hasNoPerm("announcer.reboot")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!announcer.isRunning()) announcer.startTask();

                    return oneMessage("commands.announcer.rebooted");

                case "preview":
                    if (hasNoPerm("announcer.preview")) return true;

                    if (sender instanceof ConsoleCommandSender) {
                        LogUtils.doLog("&cYou can't preview an announce in console.");
                        return true;
                    }

                    if (args.length == 1 || announcer.getSection() == null)
                        return oneMessage("commands.announcer.select");

                    ConfigurationSection id = announcer.getSection().getConfigurationSection(args[1]);
                    if (id == null) return oneMessage("commands.announcer.select");

                    announcer.runSection(id);
                    return true;

                default: return notArgument(args[args.length - 1]);
            }
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1) return resultList("start", "preview", "cancel", "reboot");

            if(args.length == 2 && args[0].matches("(?i)preview")) {
                ConfigurationSection id = FileCache.ANNOUNCES.get().getConfigurationSection("announces");
                return id == null ? resultList("NOT_FOUND") : resultList(id.getKeys(false));
            }

            return new ArrayList<>();
        };
    }
}
