package me.croabeast.sirplugin.object.instance;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.command.*;

import java.util.List;

/**
 * This class represents the base of every command used in this plugin.
 */
public abstract class SIRTask extends CmdUtils {

    /**
     * The command's name from plugin.yml file
     * @return the command's name.
     */
    protected abstract String getName();

    /**
     * Defines the executor for this command.
     * @return the command's executor
     */
    protected abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Defines the completer for this command.
     * @return the command's completer
     */
    protected abstract List<String> complete(CommandSender sender, String[] args);

    /**
     * Registers the command in the server.
     * @throws NullPointerException if command is null
     */
    public final void registerCommand() {
        PluginCommand command = SIRPlugin.getInstance().getCommand(getName());
        if (command == null)
            throw new NullPointerException("Command \"/" + getName() + "\" is null");

        command.setExecutor((s, c, l, a) -> execute(s, a));
        command.setTabCompleter((s, c, l, a) -> complete(s, a));
    }
}
