package me.croabeast.sirplugin.hook;

import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import com.earth2me.essentials.Essentials;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sirplugin.event.hook.SIRVanishEvent;
import me.croabeast.sirplugin.instance.SIRListener;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static me.croabeast.sirplugin.file.FileCache.JOIN_QUIT;

public final class VanishHook {

    private VanishHook() {}

    private static final String[] SUPPORTED_PLUGINS = {
            "Essentials", "CMI", "SuperVanish", "PremiumVanish"
    };

    private static final List<Plugin> ENABLED_HOOKS = new ArrayList<>();
    private static final List<SIRListener> REGISTERED_LISTENERS = new ArrayList<>();

    private static boolean areHooksRegistered = false;

    public static void loadHook() {
        if (!isEnabled()) return;

        if (Exceptions.isPluginEnabled("Essentials")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(VanishStatusChangeEvent event) {
                    IUser user = event.getAffected();
                    new SIRVanishEvent(user.getBase(), user.isVanished()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("CMI")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(CMIPlayerUnVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), true).call();
                }

                @EventHandler
                private void onOther(CMIPlayerVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), false).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.arePluginsEnabled(false, "SuperVanish", "PremiumVanish")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(PlayerVanishStateChangeEvent event) {
                    Player player = Bukkit.getPlayer(event.getUUID());
                    new SIRVanishEvent(player, !event.isVanishing()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }
    }

    public static void unloadHook() {
        if (!isEnabled()) return;
        REGISTERED_LISTENERS.forEach(SIRListener::unregister);
    }

    public static boolean isEnabled() {
        if (!areHooksRegistered) {
            for (String s : SUPPORTED_PLUGINS) {
                Plugin p = Bukkit.getPluginManager().getPlugin(s);
                if (p == null || !p.isEnabled()) continue;

                if (ENABLED_HOOKS.isEmpty()) ENABLED_HOOKS.add(p);
            }
            areHooksRegistered = true;
        }

        return ENABLED_HOOKS.size() == 1 &&
                JOIN_QUIT.getValue("vanish.enabled", true);
    }

    public static boolean isVanished(Player player) {
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

    @Nullable
    public static Plugin getHook() {
        return isEnabled() ? ENABLED_HOOKS.get(0) : null;
    }
}
