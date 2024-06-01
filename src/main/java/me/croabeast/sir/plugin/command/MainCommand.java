package me.croabeast.sir.plugin.command;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.util.DataUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class MainCommand extends SIRCommand {

    MainCommand() {
        super("sir", false);
        MessageSender sender = MessageSender.loaded().setLogger(true);

        editSubCommand("reload", (s, strings) -> {
            final long start = System.currentTimeMillis();

            try {
                DataUtils.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                DataUtils.load();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return fromSender(s)
                    .addKeyValue("{time}", System.currentTimeMillis() - start)
                    .send("reload");
        });

        editSubCommand("modules", (s, strings) -> {
            Player player = s instanceof Player ? (Player) s : null;

            if (player == null)
                return sender.send("&cThis command is only for players.");

            if (ServerInfoUtils.SERVER_VERSION < 14.0)
                return sender.setTargets(player).send(
                        "<P> &cModules GUI is not supported on this version.",
                        "<P> &7Enable/disable modules in modules/modules.yml file"
                );

            SIRModule.showGUI(player);
            return true;
        });

        editSubCommand("about", (s, strings) -> {
            Player player = s instanceof Player ? (Player) s : null;

            return sender.setTargets(player).send(
                            "", " &eSIR &7- &f" + SIRPlugin.getVersion() + "&7:",
                            "   &8• &7Server Software: &f" + ServerInfoUtils.SERVER_FORK,
                            "   &8• &7Developer: &f" + SIRPlugin.getAuthor(),
                            "   &8• &7Java Version: &f" + SystemUtils.JAVA_VERSION, ""
                    );
        });

        editSubCommand("help", (s, strings) ->
                fromSender(s)
                        .addKeyValue("{version}", SIRPlugin.getVersion())
                        .send("help"));

        editSubCommand("support", (s, strings) ->
                fromSender(s)
                        .addKeyValue("{link}", "https://discord.gg/s9YFGMrjyF")
                        .send("support"));
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return YAMLData.Command.Single.SIR.from();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return findSubCommandByNameOrAlias("help").execute(sender, args);
    }

    @Override
    protected TabBuilder completer() {
        TabBuilder builder = TabBuilder.of();

        for (SubCommand sub : subCommands.values())
            builder.addArguments((s, a) -> sub.isPermitted(s), sub.getNames());

        return builder;
    }
}
