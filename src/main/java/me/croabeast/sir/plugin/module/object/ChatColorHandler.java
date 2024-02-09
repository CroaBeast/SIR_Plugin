package me.croabeast.sir.plugin.module.object;

import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;

public class ChatColorHandler extends SIRModule implements CacheManageable {

    ChatColorHandler() {
        super(ModuleName.CHAT_COLORS);
    }
}
