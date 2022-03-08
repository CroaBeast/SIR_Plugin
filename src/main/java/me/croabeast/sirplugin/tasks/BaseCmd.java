package me.croabeast.sirplugin.tasks;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;

import javax.annotation.*;

/**
 * This class represents the base of every command used in this plugin.
 */
public abstract class BaseCmd extends CmdUtils {

    /**
     * The command's name from plugin.yml file
     * @return the command's name.
     */
    protected abstract String getName();

    /**
     * Defines the executor for this command.
     * @return the command's executor
     */
    protected abstract CommandExecutor getExecutor();

    /**
     * Defines the completer for this command.
     * @return the command's completer
     */
    protected abstract TabCompleter getCompleter();

    /**
     * The command object from the server.
     * If the command doesn't exist in this server, it will return null.
     * @return the command itself.
     */
    @Nullable
    private PluginCommand getCommand() {
        return SIRPlugin.getInstance().getCommand(getName());
    }

    /**
     * Registers the command in the server.
     */
    public void registerCommand() {
        if (getCommand() == null) {
            LogUtils.doLog("&cError: command is null.");
            return;
        }

        getCommand().setExecutor(getExecutor());
        getCommand().setTabCompleter(getCompleter());
    }
}
