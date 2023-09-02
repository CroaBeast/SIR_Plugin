package me.croabeast.sir.plugin.hook;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.auth.api.event.AuthPlayerLoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerRegisterEvent;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LoginHook {

    private final String[] SUPPORTED_PLUGINS = {
            "AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"
    };

    private final List<Plugin> ENABLED_HOOKS = new ArrayList<>();
    private final List<CustomListener> REGISTERED_LISTENERS = new ArrayList<>();

    private boolean areHooksRegistered = false;

    public void loadHook() {
        if (!isEnabled()) return;

        if (Exceptions.isPluginEnabled("AuthMe")) {
            CustomListener s = new CustomListener() {
                @EventHandler
                private void onEvent(LoginEvent event) {
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("UserLogin")) {
            CustomListener s = new CustomListener() {
                @EventHandler
                private void onEvent(AuthenticationEvent event) {
                    if (event.isCancelled()) return;
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("nLogin")) {
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");

                CustomListener s = new CustomListener() {
                    @EventHandler
                    private void onEvent(AuthenticateEvent event) {
                        new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                    }
                };

                s.register();
                REGISTERED_LISTENERS.add(s);
            }
            catch (Exception e) {
                LogUtils.doLog("&cnLogin isn't updated, try v10.");
            }
        }

        if (Exceptions.isPluginEnabled("OpeNLogin")) {
            CustomListener s = new CustomListener() {
                @EventHandler
                private void onEvent(AsyncLoginEvent event) {
                    if (event.isCancelled()) return;
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }

                @EventHandler
                private void onOther(AsyncRegisterEvent event) {
                    if (event.isCancelled()) return;
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("NexAuth")) {
            CustomListener s = new CustomListener() {
                @EventHandler
                private void onEvent(AuthPlayerLoginEvent event) {
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }

                @EventHandler
                private void onOther(AuthPlayerRegisterEvent event) {
                    new SIRLoginEvent(event.getPlayer(), event.isAsynchronous()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }
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
                FileCache.JOIN_QUIT_CACHE.getConfig().getValue("login.enabled", true);
    }

    public void unloadHook() {
        if (!isEnabled()) return;
        REGISTERED_LISTENERS.forEach(CustomListener::unregister);
    }

    @Nullable
    public Plugin getHook() {
        return isEnabled() ? ENABLED_HOOKS.get(0) : null;
    }
}
