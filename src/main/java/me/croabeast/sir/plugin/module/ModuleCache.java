package me.croabeast.sir.plugin.module;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.SIRCollector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

@UtilityClass
class ModuleCache implements CacheManageable {

    private boolean modulesRegistered = false;

    void loadCache() {
        if (modulesRegistered) return;

        SIRCollector.from("me.croabeast.sir.plugin.module.object")
                .filter(SIRModule.class::isAssignableFrom)
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect().forEach(c -> {
                    try {
                        Constructor<?> ct = c.getDeclaredConstructor();
                        ct.setAccessible(true);

                        SIRModule module = (SIRModule) ct.newInstance();
                        module.register();
                    } catch (Exception ignored) {}
                });

        new SIRModule(ModuleName.DISCORD_HOOK) {};

        modulesRegistered = true;
    }
}
