package me.croabeast.sir.plugin.command;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.utility.LogUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@UtilityClass
class CommandCache implements CacheManageable {

    private boolean areCommandsLoaded = false;

    @Priority(1)
    void loadCache() {
        if (areCommandsLoaded) return;

        SIRCollector.from("me.croabeast.sir.plugin.command.object")
                .filter(SIRCommand.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect().forEach(c -> {
                    try {
                        Constructor<?> cons = c.getDeclaredConstructor();

                        cons.setAccessible(true);
                        cons.newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        SIRCommand.COMMAND_SET.forEach(SIRCommand::register);
        areCommandsLoaded = true;
    }
}
