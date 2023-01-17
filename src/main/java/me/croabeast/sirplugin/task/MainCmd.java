package me.croabeast.sirplugin.task;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.module.*;
import me.croabeast.sirplugin.object.instance.*;
import org.bukkit.command.*;

import java.util.*;

import static me.croabeast.sirplugin.object.instance.Identifier.*;

public class MainCmd extends SIRTask {

    private final SIRPlugin main = SIRPlugin.getInstance();

    @Override
    public String getName() {
        return "sir";
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (hasNoPerm(sender, "admin.*")) return true;
        if (args.length == 0)
            return oneMessage(sender, "commands.sir.help", "version", SIRPlugin.pluginVersion());
        if (args.length > 1) return notArgument(sender, args[args.length - 1]);

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "help":
                if (hasNoPerm(sender, "admin.help")) return true;
                return oneMessage(sender, "commands.sir.help", "version", SIRPlugin.pluginVersion());

            case "reload":
                if (hasNoPerm(sender, "admin.reload")) return true;
                long start = System.currentTimeMillis();

                main.getFiles().loadFiles(false);

                SIRModule.getModule(EMOJIS).registerModule();
                SIRModule.getModule(ANNOUNCES).registerModule();

                Initializer.unloadAdvances(true);
                Initializer.loadAdvances(false);

                Announcer announcer = (Announcer) SIRModule.getModule(ANNOUNCES);
                if (announcer.isEnabled() && !announcer.isRunning()) announcer.startTask();
                if (!announcer.isEnabled()) announcer.cancelTask();

                sendMessage(sender, "commands.sir.reload", "time",
                        (System.currentTimeMillis() - start) + "");
                return true;

            case "support":
                if (hasNoPerm(sender, "admin.support")) return true;
                return oneMessage(sender, "commands.sir.support", "link",
                        "https://discord.gg/s9YFGMrjyF");

            default: return notArgument(sender, args[0]);
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        return args.length == 1 ? resultList(args, "reload", "help", "support") : new ArrayList<>();
    }
}
