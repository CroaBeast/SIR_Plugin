package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.plugin.SIRPlugin;

@Setter @Getter
abstract class ListenerModule extends SIRModule implements CustomListener {

    private boolean registered = false;

    protected ListenerModule(String name) {
        super(name);
    }

    @Override
    public boolean register() {
        return CustomListener.super.register(SIRPlugin.getInstance());
    }

    @Override
    public boolean unregister() {
        return CustomListener.super.unregister();
    }
}
