package me.croabeast.sir.api.extension;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class SIRAddon extends SIRExtension {

    private static final Map<String, SIRAddon> ADDON_MAP = new HashMap<>();

    protected SIRAddon(@NotNull String name) {
        super(name, "addons");

        if (ADDON_MAP.containsValue(this))
            throw new UnsupportedOperationException();

        ADDON_MAP.put(this.name, this);
    }
}
