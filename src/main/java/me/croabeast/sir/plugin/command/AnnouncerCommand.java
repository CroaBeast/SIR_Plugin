package me.croabeast.sir.plugin.command;

import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.AnnounceHandler;
import me.croabeast.sir.plugin.module.SIRModule;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class AnnouncerCommand extends SIRCommand {

    private final ConfigurableFile announces = YAMLData.Module.ANNOUNCEMENT.fromName("announces");

    AnnouncerCommand() {
        super("announcer");

        final AnnounceHandler handler = getParent();

        editSubCommand("start", (sender, args) -> {
            if (args.length > 0)
                return isWrongArgument(sender, args[args.length - 1]);

            if (handler.isRunning())
                return fromSender(sender).send("cant-start");

            handler.start();
            return fromSender(sender).send("started");
        });

        editSubCommand("cancel", (sender, args) -> {
            if (args.length > 0)
                return isWrongArgument(sender, args[args.length - 1]);

            if (!handler.isRunning())
                return fromSender(sender).send("cant-stop");

            handler.stop();
            return fromSender(sender).send("stopped");
        });

        editSubCommand("reboot", (sender, args) -> {
            if (args.length > 0)
                return isWrongArgument(sender, args[args.length - 1]);

            if (!handler.isRunning()) handler.start();
            return fromSender(sender).send("rebooted");
        });

        editSubCommand("preview", (sender, args) -> {
            if (!(sender instanceof Player)) {
                BeansLogger.getLogger().log("&cYou can't preview an announce in console.");
                return true;
            }

            return (args.length == 1 && handler.displayAnnounce(args[0])) ||
                    fromSender(sender).send("select");
        });
    }

    @NotNull
    protected AnnounceHandler getParent() {
        return SIRModule.ANNOUNCEMENTS.getData();
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return YAMLData.Command.Single.ANNOUNCER.from();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return fromSender(sender).send("help");
    }

    @NotNull
    protected TabBuilder completer() {
        final TabBuilder builder = TabBuilder.of();

        for (SubCommand sub : subCommands.values())
            builder.addArguments((s, a) -> sub.isPermitted(s), sub.getNames());

        ConfigurationSection id = announces.getSection("announces");
        return id != null ?
                builder.addArguments(1,
                        (s, a) -> a[0].matches("(?i)preview"),
                        id.getKeys(false)
                ) :
                builder;
    }
}
