package me.croabeast.sir.plugin.module.object;

import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;

public class ChatColorHandler extends SIRModule implements CacheHandler {

    ChatColorHandler() {
        super(ModuleName.CHAT_COLORS);
    }
}
