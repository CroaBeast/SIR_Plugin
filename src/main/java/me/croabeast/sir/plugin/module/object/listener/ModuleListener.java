package me.croabeast.sir.plugin.module.object.listener;

import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;

abstract class ModuleListener extends SIRModule implements CustomListener {

    ModuleListener(ModuleName moduleName) {
        super(moduleName);
    }

    public void register() {
        registerOnSIR();
    }
}
