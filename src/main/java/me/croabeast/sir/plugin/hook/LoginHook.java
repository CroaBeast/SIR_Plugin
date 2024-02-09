package me.croabeast.sir.plugin.hook;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.utility.LogUtils;
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

@UtilityClass
public class LoginHook {

    private final Set<LoadedListener> LISTENER_SET = new LinkedHashSet<>();
    private final Set<Player> LOGGED_PLAYERS = new HashSet<>();

    private final String[] SUPPORTED_PLUGINS = {
            "AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"
    };

    private final List<Plugin> ENABLED_HOOKS = new ArrayList<>();
    private boolean areHooksRegistered = false;

    private <E extends Event> void createLoginEventCall(E event) {
        Player player;

        try {
            Method method = event.getClass().getMethod("getPlayer");
            player = (Player) method.invoke(event);
        } catch (Exception e) {
            throw new NullPointerException();
        }

        new SIRLoginEvent(player, event.isAsynchronous()).call();
    }

    public boolean isEnabled() {
        YAMLFile file = YAMLCache.fromJoinQuit("config");
        return ENABLED_HOOKS.size() == 1 && file.get("login.enabled", true);
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
                LogUtils.doLog("&cnLogin isn't updated, try v10.");
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

    void unloadHook() {
        if (isEnabled()) LISTENER_SET.forEach(LoadedListener::unregister);
    }

    public void addPlayer(Player player) {
        LOGGED_PLAYERS.add(player);
    }

    public void removePlayer(Player player) {
        LOGGED_PLAYERS.remove(player);
    }

    @SuppressWarnings("all")
    public boolean isLogged(Player player) {
        return !isEnabled() || LOGGED_PLAYERS.contains(player);
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
