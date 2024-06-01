package me.croabeast.sir.plugin.module.chat;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.plugin.file.YAMLData;

public final class FilterHandler extends ChatModule implements CustomListener {

    @Getter @Setter
    private boolean registered = false;

    FilterHandler() {
        super(Name.FILTERS, YAMLData.Module.Chat.FILTERS);
    }
}

