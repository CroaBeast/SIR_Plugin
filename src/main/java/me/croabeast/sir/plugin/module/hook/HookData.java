package me.croabeast.sir.plugin.module.hook;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.DataHandler;

@UtilityClass
class HookData implements DataHandler {

    void loadData() {
        LoginHook.loadHook();
        VanishHook.loadHook();
    }

    void saveData() {
        LoginHook.unloadHook();
        VanishHook.unloadHook();
    }
}
