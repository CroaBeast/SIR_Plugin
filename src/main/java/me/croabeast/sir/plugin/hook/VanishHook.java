package me.croabeast.sir.plugin.hook;

import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import com.earth2me.essentials.Essentials;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sir.api.event.hook.SIRVanishEvent;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.file.YAMLCache;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class VanishHook {

    private final Set<LoadedListener> LISTENER_SET = new LinkedHashSet<>();

    private final String[] SUPPORTED_PLUGINS = {
            "Essentials", "CMI", "SuperVanish", "PremiumVanish"
    };

    private final List<Plugin> ENABLED_HOOKS = new ArrayList<>();
    private boolean areHooksRegistered = false;

    public boolean isEnabled() {
        if (ENABLED_HOOKS.size() != 1) return false;
        return YAMLCache.fromJoinQuit("config").get("vanish.enabled", true);
    }

    @Nullable
    public Plugin getHook() {
        return isEnabled() ? ENABLED_HOOKS.get(0) : null;
    }

    void loadHook() {
        if (!areHooksRegistered) {
            for (String s : SUPPORTED_PLUGINS) {
                Plugin p = Bukkit.getPluginManager().getPlugin(s);
                if (p == null || !p.isEnabled()) continue;

                if (ENABLED_HOOKS.isEmpty()) ENABLED_HOOKS.add(p);
            }

            areHooksRegistered = true;
        }

        if (!isEnabled()) return;

        if (Exceptions.isPluginEnabled("Essentials"))
            new LoadedListener() {
                @EventHandler
                private void onVanish(VanishStatusChangeEvent event) {
                    IUser user = event.getAffected();
                    new SIRVanishEvent(user.getBase(), user.isVanished()).call();
                }
            }.registerOnSIR();

        if (Exceptions.isPluginEnabled("CMI")) {
            new LoadedListener() {
                @EventHandler
                private void onVanish(CMIPlayerVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), false).call();
                }
                @EventHandler
                private void onUnVanish(CMIPlayerUnVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), true).call();
                }
            }.registerOnSIR();
        }

        if (Exceptions.arePluginsEnabled(false, "SuperVanish", "PremiumVanish"))
            new LoadedListener() {
                @EventHandler
                private void onVanish(PlayerVanishStateChangeEvent event) {
                    Player player = Bukkit.getPlayer(event.getUUID());
                    new SIRVanishEvent(player, !event.isVanishing()).call();
                }
            }.registerOnSIR();
    }

    void unloadHook() {
        if (isEnabled()) LISTENER_SET.forEach(LoadedListener::unregister);
    }

    public boolean isVanished(Player player) {
        if (!isEnabled()) return false;
        if (player == null) return false;

        Plugin e = Bukkit.getPluginManager().getPlugin("Essentials"),
                c = Bukkit.getPluginManager().getPlugin("CMI");

        if (e != null)
            return ((Essentials) e).getUser(player).isVanished();

        if (c != null)
            return CMIUser.getUser(player).isVanished();

        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;

        return false;
    }

    public boolean isVisible(Player player) {
        return !isVanished(player);
    }

    static class LoadedListener implements CustomListener {

        LoadedListener() {
            LISTENER_SET.add(this);
        }

        public void unregister() {
            CustomListener.super.unregister();
            LISTENER_SET.remove(this);
        }
    }
}
