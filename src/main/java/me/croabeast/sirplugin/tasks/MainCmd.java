package me.croabeast.sirplugin.tasks;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.command.*;

import java.util.*;

public class MainCmd extends BaseCmd {

    private final SIRPlugin main;

    public MainCmd(SIRPlugin main) {
        this.main = main;
    }

    @Override
    public String getName() {
        return "sir";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("admin.*")) return true;
            if (args.length == 0)
                return oneMessage("commands.sir.help", "version", SIRPlugin.pluginVersion());
            if (args.length > 1) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "help":
                    if (hasNoPerm("admin.help")) return true;
                    return oneMessage("commands.sir.help", "version", SIRPlugin.pluginVersion());

                case "reload":
                    if (hasNoPerm("admin.reload")) return true;
                    long start = System.currentTimeMillis();

                    main.getFiles().loadFiles(false);
                    main.getEmParser().registerModule();
                    main.getReporter().registerModule();

                    main.getInitializer().unloadAdvances(true);
                    main.getInitializer().loadAdvances(false);

                    Reporter reporter = main.getReporter();
                    if (reporter.isEnabled() && !reporter.isRunning()) reporter.startTask();
                    if (!reporter.isEnabled()) reporter.cancelTask();

                    sendMessage("commands.sir.reload", "time",
                            (System.currentTimeMillis() - start) + "");
                    return true;

                case "support":
                    if (hasNoPerm("admin.support")) return true;
                    return oneMessage("commands.sir.support", "LINK",
                            "https://discord.gg/s9YFGMrjyF");

                default: return notArgument(args[0]);
            }
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            return args.length == 1 ? resultTab("reload", "help", "support") : new ArrayList<>();
        };
    }
}
