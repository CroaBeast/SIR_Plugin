package me.croabeast.sir.plugin.hook;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class LoginHook {

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

    public void loadHook() {
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
            new CustomListener() {
                @EventHandler
                private void onLogin(LoginEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();

        if (Exceptions.isPluginEnabled("UserLogin"))
            new CustomListener() {
                @EventHandler
                private void onLogin(AuthenticationEvent event) {
                    createLoginEventCall(event);
                }
            }.registerOnSIR();

        if (Exceptions.isPluginEnabled("nLogin"))
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");
                new CustomListener() {
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
            new CustomListener() {
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
            new CustomListener() {
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

    public boolean isEnabled() {
        FileCache file = FileCache.JOIN_QUIT_CACHE.getConfig();
        return ENABLED_HOOKS.size() == 1 && file.getValue("login.enabled", true);
    }

    @Nullable
    public Plugin getHook() {
        return isEnabled() ? ENABLED_HOOKS.get(0) : null;
    }

    @SneakyThrows
    private void checkAccess() {
        SIRPlugin.checkAccess(LoginHook.class);
    }

    public void addPlayer(Player player) {
        checkAccess();
        LOGGED_PLAYERS.add(player);
    }

    public void removePlayer(Player player) {
        checkAccess();
        LOGGED_PLAYERS.remove(player);
    }

    @SuppressWarnings("all")
    public boolean isLogged(Player player) {
        return isEnabled() && LOGGED_PLAYERS.contains(player);
    }
}
