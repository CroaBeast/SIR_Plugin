package me.croabeast.sir.plugin.command;

import lombok.experimental.UtilityClass;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.gui.MenuCreator;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRCollector;

import java.lang.reflect.Modifier;
import java.util.Set;

@UtilityClass
class CommandData implements DataHandler {

    final MenuCreator COMMANDS_MENU = MenuCreator.of(4, "");
    private boolean loaded = false, registered = false;

    Set<SIRCommand> getCommands() {
        try {
            return Reflector.of(SIRCommand.class).get("COMMAND_SET");
        } catch (Exception e) {
            return null;
        }
    }

    @Priority(1)
    void loadData() {
        if (!loaded) {
            Counter loaded = new Counter(), failed = new Counter();
            final Counter total = new Counter();

            SIRCollector.from("me.croabeast.sir.plugin.command")
                    .filter(SIRCommand.class::isAssignableFrom)
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect().forEach(c -> {
                        SIRCommand command = null;
                        try {
                            command = Reflector.of(c).create();
                        } catch (Exception ignored) {}

                        (command != null ? loaded : failed).add();
                        total.add();
                    });

            CommandData.loaded = true;
            BeansLogger.getLogger().log("Loading commands...",
                    "Total: " + total.get() +
                            " [Loaded= " + loaded.get() +
                            ", Failed= " + failed.get() + "]"
            );

            if (loaded.get() < 1 || failed.get() > 0)
                BeansLogger.doLog(
                        "&cSome commands were not loaded correctly.",
                        "&cReport it to CroaBeast ASAP!"
                );
        }

        Set<SIRCommand> commands = getCommands();
        if (commands == null) return;

        if (registered) {
            commands.forEach(SIRCommand::unregister);
            registered = false;
            return;
        }

        commands.forEach(SIRCommand::register);
        registered = true;
    }
}
