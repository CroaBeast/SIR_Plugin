package me.croabeast.sir.plugin.module.hook;

import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.SIRModule;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

abstract class HookModule extends SIRModule {

    protected final ConfigurableFile config;

    protected HookModule(Name name, YAMLData.Module.Hook hook) {
        super("hook." + name);
        this.config = hook.from();
    }

    public @NotNull File getDataFolder() {
        return config.getFile().getParentFile();
    }

    protected enum Name {
        DISCORD,
        LOGIN,
        VANISH;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }
}
