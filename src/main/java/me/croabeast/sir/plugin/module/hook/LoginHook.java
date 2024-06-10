package me.croabeast.sir.plugin.module.hook;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.auth.api.event.AuthPlayerLoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerRegisterEvent;

import java.lang.reflect.Method;
import java.util.*;

public final class LoginHook extends JoinQuitRelated {

    private static final String[] SUPPORTED_PLUGINS = {
            "AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"
    };

    private static final List<Plugin> ENABLED_HOOKS = new ArrayList<>();

    static {
        for (String s : SUPPORTED_PLUGINS) {
            Plugin p = Bukkit.getPluginManager().getPlugin(s);
            if (p == null || !p.isEnabled()) continue;

            if (ENABLED_HOOKS.isEmpty()) ENABLED_HOOKS.add(p);
        }
    }

    private static final Set<LoadedListener> LISTENER_SET = new LinkedHashSet<>();
    private static final Set<Player> LOGGED_PLAYERS = new HashSet<>();

    LoginHook() {
        super(true);
    }

    private static <E extends Event> void createLoginEventCall(E event) {
        Player player;

        try {
            Method method = event.getClass().getMethod("getPlayer");
            player = (Player) method.invoke(event);
        } catch (Exception e) {
            throw new NullPointerException();
        }

        new SIRLoginEvent(player, event.isAsynchronous()).call();
    }

    static void loadHook() {
        if (!isHookEnabled()) return;

        if (Exceptions.isPluginEnabled("AuthMe"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(LoginEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();

        if (Exceptions.isPluginEnabled("UserLogin"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(AuthenticationEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();

        if (Exceptions.isPluginEnabled("nLogin"))
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");
                new LoadedListener() {
                    @EventHandler
                    private void onLogin(AuthenticateEvent event) {
                        createLoginEventCall(event);
                    }
                }.registerOnSIR();
            }
            catch (Exception e) {
                BeansLogger.getLogger().log("&cnLogin isn't updated, try v10.");
            }

        if (Exceptions.isPluginEnabled("OpeNLogin")) {
            new LoadedListener() {
                @EventHandler
                private void onLogin(AsyncLoginEvent event) {
                    createLoginEventCall(event);
                }
                @EventHandler
                private void onRegister(AsyncRegisterEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();
        }

        if (Exceptions.isPluginEnabled("NexAuth")) {
            new LoadedListener() {
                @EventHandler
                private void onLogin(AuthPlayerLoginEvent event) {
                    createLoginEventCall(event);
                }
                @EventHandler
                private void onRegister(AuthPlayerRegisterEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();
        }
    }

    static void unloadHook() {
        if (isHookEnabled()) LISTENER_SET.forEach(LoadedListener::unregister);
    }

    @Override
    public boolean isEnabled() {
        return isHookEnabled() && super.isEnabled();
    }

    @EventHandler
    private void onLogin(SIRLoginEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        LoginHook.addPlayer(player);

        if (VanishHook.isVanished(player)) return;

        String type = player.hasPlayedBefore() ? "JOIN" : "FIRST_JOIN";

        Object unit = getUnit(type, player);
        if (unit == null) return;

        int joinTime = jqConfig.get("cooldown.join", 0);

        if (joinTime > 0 && getJoinMap().containsKey(uuid)) {
            long rest = System.currentTimeMillis() - getJoinMap().get(uuid);
            if (rest < joinTime * 1000L) return;
        }

        performActions(unit, player);

        final long data = System.currentTimeMillis();
        if (joinTime > 0) getJoinMap().put(uuid, data);

        if (jqConfig.get("cooldown.between", 0) > 0)
            getPlayMap().put(uuid, data);
    }

    public static void addPlayer(Player player) {
        LOGGED_PLAYERS.add(player);
    }

    public static void removePlayer(Player player) {
        LOGGED_PLAYERS.remove(player);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLogged(Player player) {
        return !LOGIN.isEnabled() || LOGGED_PLAYERS.contains(player);
    }

    public static boolean isHookEnabled() {
        return ENABLED_HOOKS.size() == 1;
    }

    @Nullable
    public static Plugin getHook() {
        return isHookEnabled() ? ENABLED_HOOKS.get(0) : null;
    }

    static class LoadedListener implements CustomListener {

        @Getter @Setter
        private boolean registered = false;

        LoadedListener() {
            LISTENER_SET.add(this);
        }

        public void unregister() {
            CustomListener.super.unregister();
            LISTENER_SET.remove(this);
        }
    }
}
