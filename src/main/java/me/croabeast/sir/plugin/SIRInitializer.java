package me.croabeast.sir.plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.utility.LogUtils;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class SIRInitializer {

    private Permission permProvider;
    private Chat chatProvider;

    ProtocolManager manager;

    private boolean hasPAPI() {
        return Exceptions.isPluginEnabled("PlaceholderAPI");
    }

    public boolean hasVault() {
        return Exceptions.isPluginEnabled("Vault");
    }

    boolean hasProtocolLib() {
        return Exceptions.isPluginEnabled("ProtocolLib");
    }

    public boolean hasDiscord() {
        return Exceptions.isPluginEnabled("DiscordSRV") && ModuleName.DISCORD_HOOK.isEnabled();
    }

    void startMetrics() {
        try {
            Metrics metrics = new Metrics(SIRPlugin.getInstance(), 12806);

            metrics.addCustomChart(new SimplePie("hasPAPI", () -> hasPAPI() + ""));
            metrics.addCustomChart(new SimplePie("hasVault", () -> hasVault() + ""));

            metrics.addCustomChart(new SimplePie("hasDiscord", () ->
                    (Exceptions.isPluginEnabled("DiscordSRV")) + ""));

            metrics.addCustomChart(new DrilldownPie("loginPlugins", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                Map<String, Integer> entry = new HashMap<>();

                entry.put("Login Plugins", 1);

                if (LoginHook.isEnabled()) {
                    Plugin p = LoginHook.getHook();
                    map.put(p != null ? p.getName() : "None / Other", entry);
                }
                else map.put("None / Other", entry);

                return map;
            }));

            metrics.addCustomChart(new DrilldownPie("vanishPlugins", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                Map<String, Integer> entry = new HashMap<>();

                entry.put("Vanish Plugins", 1);

                if (VanishHook.isEnabled()) {
                    Plugin p = VanishHook.getHook();
                    map.put(p != null ? p.getName() : "None / Other", entry);
                }
                else map.put("None / Other", entry);

                return map;
            }));
        } catch (Exception ignored) {}
    }

    String pluginProperty(Plugin plugin, Function<Plugin, String> function) {
        return plugin == null ? "" : function.apply(plugin);
    }

    String pluginVersion(Plugin plugin) {
        return pluginProperty(plugin, p -> p.getDescription().getVersion());
    }

    String pluginVersion(String name) {
        return pluginVersion(Bukkit.getPluginManager().getPlugin(name));
    }

    void setPluginHooks() {
        LogUtils.doLog("&bChecking all compatible hooks...");
        int logLines = 0;

        if (hasProtocolLib()) {
            LogUtils.doLog("&7ProtocolLib: &e" + pluginVersion("ProtocolLib"));
            logLines++;
        }

        if (hasPAPI()) {
            LogUtils.doLog("&7PlaceholderAPI: &e" + pluginVersion("PlaceholderAPI"));
            logLines++;
        }

        if (hasVault()) {
            ServicesManager servMngr = Bukkit.getServer().getServicesManager();

            RegisteredServiceProvider<Permission> rsp =
                    servMngr.getRegistration(Permission.class);
            RegisteredServiceProvider<Chat> rsc =
                    servMngr.getRegistration(Chat.class);

            if (rsp != null) permProvider = rsp.getProvider();
            if (rsc != null) chatProvider = rsc.getProvider();
        }

        if (hasDiscord()) {
            LogUtils.doLog("&7DiscordSRV: " + "&e" + pluginVersion("DiscordSRV"));
            logLines++;
        }

        if (LoginHook.isEnabled()) {
            StringBuilder builder = new StringBuilder("&7Login Plugin: &e");
            final Plugin p = LoginHook.getHook();

            builder.append(pluginProperty(p, Plugin::getName))
                    .append(' ')
                    .append(pluginVersion(p));

            LogUtils.doLog(builder.toString());
            logLines++;
        }

        if (VanishHook.isEnabled()) {
            StringBuilder builder = new StringBuilder("&7Vanish Plugin: &e");
            final Plugin p = VanishHook.getHook();

            builder.append(pluginProperty(p, Plugin::getName))
                    .append(' ')
                    .append(pluginVersion(p));

            LogUtils.doLog(builder.toString());
            logLines++;
        }

        if (logLines == 0)
            LogUtils.doLog("&cThere is no compatible hooks available.");
    }

    public Permission getPermsMeta() {
        return hasVault() ? permProvider : null;
    }

    public Chat getChatMeta() {
        return hasVault() ? chatProvider : null;
    }

    ProtocolManager initProtocolManager() {
        return manager == null ? (manager = ProtocolLibrary.getProtocolManager()) : manager;
    }

    public ProtocolManager getProtocolManager() {
        return hasProtocolLib() ? initProtocolManager() : null;
    }
}
