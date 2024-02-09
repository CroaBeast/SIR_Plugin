package me.croabeast.sir.plugin.hook;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.file.CacheManageable;

@UtilityClass
class HookHandler implements CacheManageable {

    void loadCache() {
        LoginHook.loadHook();
        VanishHook.loadHook();
    }

    void saveCache() {
        LoginHook.unloadHook();
        VanishHook.unloadHook();
    }
}
