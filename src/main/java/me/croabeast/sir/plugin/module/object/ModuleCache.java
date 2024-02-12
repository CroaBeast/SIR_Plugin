package me.croabeast.sir.plugin.module.object;

import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;

abstract class ModuleCache extends SIRModule implements CacheManageable {

    protected ModuleCache(ModuleName name) {
        super(name);
    }
}
