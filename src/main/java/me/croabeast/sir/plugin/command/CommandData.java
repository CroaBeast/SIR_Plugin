package me.croabeast.sir.plugin.command;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.reflect.Craft;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.gui.MenuCreator;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.logger.DelayLogger;

import java.lang.reflect.Modifier;
import java.util.Set;

@UtilityClass
class CommandData implements DataHandler {

    final MenuCreator COMMANDS_MENU = MenuCreator.of(4, "");
    private boolean loaded = false;

    Set<SIRCommand> getCommands() {
        try {
            return Reflector.of(SIRCommand.class).get("COMMAND_SET");
        } catch (Exception e) {
            return null;
        }
    }

    @Priority(1)
    void loadData() {
        final DelayLogger logger = DelayLogger.simplified();
        logger.add("Loading commands...", true);

        Counter not = new Counter(), failed = new Counter();
        Counter registered = new Counter();

        if (!CommandData.loaded) {
            SIRCollector.from("me.croabeast.sir.plugin.command")
                    .filter(SIRCommand.class::isAssignableFrom)
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect().forEach(c -> {
                        SIRCommand command = null;
                        try {
                            command = Reflector.of(c).create();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (command == null) failed.add();
                    });

            CommandData.loaded = true;
        }

        final Set<SIRCommand> commands = getCommands();
        if (commands != null) {
            commands.forEach(c -> (c.register() ? registered : not).add());

            Reflector reflector = Reflector.of(SIRCommand.class);
            String path = "registerCalled";

            boolean updated = false;
            for (SIRCommand command : commands)
                try {
                    if (reflector.get(command, path)) {
                        reflector.set(command, path, false);
                        if (!updated) updated = true;
                    }
                } catch (Exception ignored) {}

            if (updated) Craft.Server.INSTANCE.call("syncCommands");
        }

        logger.add(
                "Registered: " + registered.get() +
                        ", Not= " + not.get() +
                        ", Failed= " + failed.get(),
                true
        );

        if (failed.get() > 0)
            logger.add(true,
                    "&cSome commands were not loaded correctly.",
                    "&cReport it to CroaBeast ASAP!"
            );

        logger.sendLines();
    }

    @Priority(1)
    void saveData() {
        final DelayLogger logger = DelayLogger.simplified();
        logger.add("Saving commands...", true);

        Counter yes = new Counter(), not = new Counter();

        Set<SIRCommand> commands = getCommands();
        if (commands != null) {
            commands.forEach(c -> (c.unregister() ? yes : not).add());

            Reflector reflector = Reflector.of(SIRCommand.class);
            boolean updated = false;
            String path = "unregisterCalled";

            for (SIRCommand command : commands)
                try {
                    if (reflector.get(command, path)) {
                        reflector.set(command, path, false);
                        if (!updated) updated = true;
                    }
                } catch (Exception ignored) {}

            if (updated)
                Craft.Server.INSTANCE.call("syncCommands");
        }

        logger.add("Un-registered: " + yes.get() + ", Not= " + not.get(), true)
                .sendLines();
    }
}
