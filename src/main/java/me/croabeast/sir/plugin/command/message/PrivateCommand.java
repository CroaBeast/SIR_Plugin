package me.croabeast.sir.plugin.command.message;

import lombok.RequiredArgsConstructor;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.lib.file.Configurable;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.command.ignore.IgnoreCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

abstract class PrivateCommand extends SIRCommand {

    static final CommandValues INITIATOR_VALUES = new CommandValues(true);
    static final CommandValues RECEIVER_VALUES = new CommandValues(false);

    @RequiredArgsConstructor
    static class CommandValues {

        private final ConfigurableFile lang = YAMLData.Command.Single.MSG_REPLY.from();
        private final boolean sender;

        String getPath() {
            return "lang.for-" + (sender ? "sender" : "receiver") + '.';
        }

        void playSound(CommandSender sender) {
            PlayerUtils.playSound(sender, lang.get(getPath() + "sound", ""));
        }

        List<String> getOutput() {
            return lang.toStringList(getPath() + "message");
        }
    }

    protected PrivateCommand(String name) {
        super(name);
    }

    String isConsoleValue(CommandSender sender) {
        return !(sender instanceof Player) ?
                getLang().get("lang.console-formatting.name", String.class) :
                sender.getName();
    }

    boolean isIgnoring(Player source, Player target) {
        if (!IgnoreCommand.isIgnoring(source, target, false)) return false;

        Configurable file = YAMLData.Command.Multi.IGNORE.from(true);
        boolean b = source == null ||
                IgnoreCommand.getData(source).getBoolean("msg.for-all");

        createSender(target)
                .addKeyValue("{type}", file.get("lang.channels.msg", ""))
                .send("ignoring." + (b ? "all" : "player"));
        return true;
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return YAMLData.Command.Single.MSG_REPLY.from();
    }

    @NotNull
    public abstract Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments);

    public final TabBuilder getCompletionBuilder() {
        return null;
    }
}
