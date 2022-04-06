package me.croabeast.sirplugin.tasks.extensions;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.modules.extensions.Reporter;
import me.croabeast.sirplugin.tasks.BaseCmd;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;

import java.util.*;

public class Announcer extends BaseCmd {

    private final SIRPlugin main;

    public Announcer(SIRPlugin main) {
        this.main = main;
    }

    @Override
    public String getName() {
        return "announcer";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            Reporter reporter = main.getReporter();

            if (hasNoPerm("announcer.*")) return true;
            if (args.length == 0) return oneMessage("commands.announcer.help");
            if (args.length > 2) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "start":
                    if (hasNoPerm("announcer.start")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (reporter.isRunning())
                        return oneMessage("commands.announcer.cant-start");

                    reporter.startTask();
                    return oneMessage("commands.announcer.started");

                case "cancel":
                    if (hasNoPerm("announcer.cancel")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!reporter.isRunning())
                        return oneMessage("commands.announcer.cant-stop");

                    reporter.cancelTask();
                    return oneMessage("commands.announcer.stopped");

                case "reboot":
                    if (hasNoPerm("announcer.reboot")) return true;
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!reporter.isRunning()) reporter.startTask();

                    return oneMessage("commands.announcer.rebooted");

                case "preview":
                    if (hasNoPerm("announcer.preview")) return true;

                    if (sender instanceof ConsoleCommandSender) {
                        LogUtils.doLog("&cYou can't preview an announce in console.");
                        return true;
                    }

                    if (args.length == 1 || reporter.getSection() == null)
                        return oneMessage("commands.announcer.select");

                    ConfigurationSection id = reporter.getSection().getConfigurationSection(args[1]);
                    if (id == null) return oneMessage("commands.announcer.select");

                    reporter.runSection(id);
                    return true;

                default: return notArgument(args[args.length - 1]);
            }
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1) return resultTab("start", "preview", "cancel", "reboot");

            if(args.length == 2 && args[0].matches("(?i)preview")) {
                ConfigurationSection id = main.getAnnounces().getConfigurationSection("announces");
                return id == null ? resultTab("NOT_FOUND") : resultTab(id.getKeys(false));
            }

            return new ArrayList<>();
        };
    }
}
