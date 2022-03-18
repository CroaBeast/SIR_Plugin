package me.croabeast.sirplugin.tasks.extensions;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.tasks.BaseCmd;
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
                return oneMessage("commands.sir.help", "version", SIRPlugin.PLUGIN_VERSION);
            if (args.length > 1) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "help":
                    if (hasNoPerm("admin.help")) return true;
                    return oneMessage("commands.sir.help", "version", SIRPlugin.PLUGIN_VERSION);

                case "reload":
                    if (hasNoPerm("admin.reload")) return true;
                    long start = System.currentTimeMillis();

                    main.getFiles().loadFiles(false);
                    main.getEmParser().registerModule();
                    main.getReporter().registerModule();

                    main.getInitializer().unloadAdvances(true);
                    main.getInitializer().loadAdvances(false);

                    if (!main.getReporter().isRunning()) main.getReporter().startTask();

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
            if (args.length == 1) return resultTab(args, "reload", "help", "support");
            return new ArrayList<>();
        };
    }
}
