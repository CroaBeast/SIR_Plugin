package me.croabeast.sir.plugin;

import lombok.experimental.UtilityClass;
import lombok.var;
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
import org.bukkit.plugin.ServicesManager;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class SIRInitializer {

    private Permission permProvider;
    private Chat chatProvider;

    private boolean hasPAPI() {
        return Exceptions.isPluginEnabled("PlaceholderAPI");
    }
    public boolean hasVault() {
        return Exceptions.isPluginEnabled("Vault");
    }

    public boolean hasDiscord() {
        return Exceptions.isPluginEnabled("DiscordSRV") && ModuleName.DISCORD_HOOK.isEnabled();
    }

    void startMetrics() {
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
    }

    String pluginVersion(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null ? plugin.getDescription().getVersion() : "";
    }

    void setPluginHooks() {
        LogUtils.doLog("&bChecking all compatible hooks...");
        int logLines = 0;

        if (hasPAPI()) {
            LogUtils.doLog("&7PlaceholderAPI: &e" + pluginVersion("PlaceholderAPI"));
            logLines++;
        }

        if (hasVault()) {
            var servMngr = Bukkit.getServer().getServicesManager();

            var rsp = servMngr.getRegistration(Permission.class);
            var rsc = servMngr.getRegistration(Chat.class);

            if (rsp != null) permProvider = rsp.getProvider();
            if (rsc != null) chatProvider = rsc.getProvider();
        }

        if (hasDiscord()) {
            LogUtils.doLog("&7DiscordSRV: " + "&e" + pluginVersion("DiscordSRV"));
            logLines++;
        }

        if (LoginHook.isEnabled()) {
            LoginHook.loadHook();
            Plugin p = LoginHook.getHook();

            String name = p != null ? p.getName() : "";
            String pV = p != null ?
                    p.getDescription().getVersion() : "";

            LogUtils.doLog("&7Login Plugin: " + "&e" + name + " v. " + pV);
            logLines++;
        }

        if (VanishHook.isEnabled()) {
            VanishHook.loadHook();
            Plugin p = VanishHook.getHook();

            String name = p != null ? p.getName() : "";
            String pV = p != null ?
                    p.getDescription().getVersion() : "";

            LogUtils.doLog("&7Vanish Plugin: " + "&e" + name + " v. " + pV);
            logLines++;
        }

        if (logLines == 0)
            LogUtils.doLog("&cThere is no compatible hooks available.");
    }

    public Permission getPerms() {
        return permProvider;
    }

    public Chat getChatMeta() {
        return chatProvider;
    }
}
