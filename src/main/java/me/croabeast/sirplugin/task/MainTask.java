package me.croabeast.sirplugin.task;

import lombok.var;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.AnnounceViewer;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRModule;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.utility.LangUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainTask extends SIRTask {

    public MainTask() {
        super("sir");
    }

    @SuppressWarnings("all")
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "admin.*")) return true;
        var version = SIRPlugin.getVersion();

        if (args.length == 0)
            return fromSender(sender, "{version}", version, "commands.sir.help");

        if (args.length > 1)
            return isWrongArgument(sender, args[args.length - 1]);

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
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
                var start = System.currentTimeMillis();

                FileCache.loadFiles();
                LangUtils.setSender();

                SIRModule.get("announces").registerModule();
                SIRModule.get("emojis").registerModule();
                SIRModule.get("mentions").registerModule();

                Initializer.unloadAdvances(true);
                Initializer.loadAdvances(false);

                var a = (AnnounceViewer) SIRModule.get("announces");
                if (a.isEnabled() && !a.isRunning()) a.startTask();
                if (!a.isEnabled()) a.cancelTask();

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
                generateList(args, "reload", "help", "support", "about") :
                new ArrayList<>();
    }
}
