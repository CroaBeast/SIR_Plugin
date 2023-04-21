package me.croabeast.sirplugin.hook;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sirplugin.event.SIRLoginEvent;
import me.croabeast.sirplugin.instance.SIRListener;
import me.croabeast.sirplugin.utility.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.auth.api.event.AuthPlayerLoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerRegisterEvent;

import java.util.ArrayList;
import java.util.List;

import static me.croabeast.sirplugin.file.FileCache.JOIN_QUIT;

public final class LoginHook {

    private LoginHook() {}

    private static final String[] SUPPORTED_PLUGINS = {
            "AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"
    };

    private static final List<Plugin> ENABLED_HOOKS = new ArrayList<>();
    private static final List<SIRListener> REGISTERED_LISTENERS = new ArrayList<>();

    private static boolean areHooksRegistered = false;

    public static void loadHook() {
        if (!isEnabled()) return;

        if (Exceptions.isPluginEnabled("AuthMe")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(LoginEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("UserLogin")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(AuthenticationEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("nLogin")) {
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");

                SIRListener s = new SIRListener() {
                    @EventHandler
                    private void onEvent(AuthenticateEvent event) {
                        new SIRLoginEvent(event.getPlayer()).call();
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
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(AsyncLoginEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
                }

                @EventHandler
                private void onOther(AsyncRegisterEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
                }
            };

            s.register();
            REGISTERED_LISTENERS.add(s);
        }

        if (Exceptions.isPluginEnabled("NexAuth")) {
            SIRListener s = new SIRListener() {
                @EventHandler
                private void onEvent(AuthPlayerLoginEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
                }

                @EventHandler
                private void onOther(AuthPlayerRegisterEvent event) {
                    new SIRLoginEvent(event.getPlayer()).call();
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
                JOIN_QUIT.getValue("login.enabled", true);
    }

    public static void unloadHook() {
        if (!isEnabled()) return;
        REGISTERED_LISTENERS.forEach(SIRListener::unregister);
    }

    @Nullable
    public static Plugin getHook() {
        return isEnabled() ? ENABLED_HOOKS.get(0) : null;
    }
}
