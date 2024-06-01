package me.croabeast.sir.plugin;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.module.hook.VanishHook;
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

    private boolean hasPAPI() {
        return Exceptions.isPluginEnabled("PlaceholderAPI");
    }

    public boolean hasVault() {
        return Exceptions.isPluginEnabled("Vault");
    }

    public boolean hasDiscord() {
        return Exceptions.isPluginEnabled("DiscordSRV");
    }

    public Chat getChatMeta() {
        return hasVault() ? chatProvider : null;
    }

    public Permission getPermsMeta() {
        return hasVault() ? permProvider : null;
    }

    void startMetrics() {
        try {
            Metrics metrics = new Metrics(SIRPlugin.getInstance(), 12806);

            metrics.addCustomChart(new SimplePie("hasPAPI", () -> hasPAPI() + ""));
            metrics.addCustomChart(new SimplePie("hasVault", () -> hasVault() + ""));

            metrics.addCustomChart(new SimplePie("hasDiscord", () -> (hasDiscord()) + ""));

            metrics.addCustomChart(new DrilldownPie("loginPlugins", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>();
                Map<String, Integer> entry = new HashMap<>();

                entry.put("Login Plugins", 1);

                if (LoginHook.isHookEnabled()) {
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

                if (VanishHook.isHookEnabled()) {
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
        SIRPlugin.DELAY_LOGGER.add(true, "&bLoading all available hooks...");
        int logLines = 0;

        if (hasPAPI()) {
            SIRPlugin.DELAY_LOGGER.add(true, "&7PlaceholderAPI: &e" + pluginVersion("PlaceholderAPI"));
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
            SIRPlugin.DELAY_LOGGER.add(true, "&7DiscordSRV: " + "&e" + pluginVersion("DiscordSRV"));
            logLines++;
        }

        if (LoginHook.isHookEnabled()) {
            StringBuilder builder = new StringBuilder("&7Login Plugin: &e");
            final Plugin p = LoginHook.getHook();

            builder.append(pluginProperty(p, Plugin::getName))
                    .append(' ')
                    .append(pluginVersion(p));

            SIRPlugin.DELAY_LOGGER.add(true, builder.toString());
            logLines++;
        }

        if (VanishHook.isHookEnabled()) {
            StringBuilder builder = new StringBuilder("&7Vanish Plugin: &e");
            final Plugin p = VanishHook.getHook();

            builder.append(pluginProperty(p, Plugin::getName))
                    .append(' ')
                    .append(pluginVersion(p));

            SIRPlugin.DELAY_LOGGER.add(true, builder.toString());
            logLines++;
        }

        if (logLines == 0)
            SIRPlugin.DELAY_LOGGER.add(true, "&cThere is no compatible hooks available.");
    }
}
