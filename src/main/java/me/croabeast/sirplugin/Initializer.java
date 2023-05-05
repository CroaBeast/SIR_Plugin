package me.croabeast.sirplugin;

import com.google.common.collect.Lists;
import lombok.var;
import me.croabeast.advancementinfo.AdvancementInfo;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.instance.SIRModule;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.advancement.Advancement;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class Initializer {

    private Initializer() {}

    private static Permission permProvider;

    private static final HashMap<Advancement, AdvancementInfo> ADVANCEMENT_KEYS = new HashMap<>();

    private static boolean hasPAPI() {
        return Exceptions.isPluginEnabled("PlaceholderAPI");
    }
    public static boolean hasVault() {
        return Exceptions.isPluginEnabled("Vault");
    }

    public static boolean hasDiscord() {
        return Exceptions.isPluginEnabled("DiscordSRV") && SIRModule.isEnabled("discord");
    }

    static void startMetrics() {
        var metrics = new Metrics(SIRPlugin.getInstance(), 12806);

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

    private static String pluginVersion(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null ? plugin.getDescription().getVersion() : "";
    }

    static void setPluginHooks() {
        LogUtils.doLog("&bChecking all compatible hooks...");
        var logLines = 0;

        if (hasPAPI()) {
            LogUtils.doLog("&7PlaceholderAPI: &eFound v. " + pluginVersion("PlaceholderAPI"));
            logLines++;
        }

        if (hasVault()) {
            var servMngr = Bukkit.getServer().getServicesManager();
            var rsp = servMngr.getRegistration(Permission.class);

            String hasVault;
            if (rsp != null) {
                hasVault = "&ePermission System registered.";
                permProvider = rsp.getProvider();
            }
            else hasVault = "&cError registering permission provider!";

            LogUtils.doLog("&7Vault: " + hasVault);
            logLines++;
        }

        if (hasDiscord()) {
            LogUtils.doLog("&7DiscordSRV: " + "&eFound v. " + pluginVersion("DiscordSRV"));
            logLines++;
        }

        if (LoginHook.isEnabled()) {
            LoginHook.loadHook();
            Plugin p = LoginHook.getHook();

            String pN = p != null ? p.getName() : "";
            String pV = p != null ?
                    p.getDescription().getVersion() : "";

            LogUtils.doLog("&7Login Plugin: "
                    + "&eFound " + pN + " v. " + pV);
            logLines++;
        }

        if (VanishHook.isEnabled()) {
            VanishHook.loadHook();
            Plugin p = VanishHook.getHook();

            String pN = p != null ? p.getName() : "";
            String pV = p != null ?
                    p.getDescription().getVersion() : "";

            LogUtils.doLog("&7Vanish Plugin: "
                    + "&eFound " + pN + " v. " + pV);
            logLines++;
        }

        if (logLines == 0)
            LogUtils.doLog("&cThere is no compatible hooks available.");
    }

    @SuppressWarnings("deprecation")
    public static void loadAdvances(boolean debug) {
        var version = LibUtils.getMainVersion();

        if (version < 12) return;
        if (!ADVANCEMENT_KEYS.isEmpty()) ADVANCEMENT_KEYS.clear();

        var time = System.currentTimeMillis();
        if (debug) {
            LogUtils.rawLog("");
            LogUtils.doLog("&bRegistering all the advancement values...");
        }

        if (SIRModule.isEnabled("advances")) {
            for (var world : Bukkit.getServer().getWorlds()) {
                if (version >= 12 && version < 13) {
                    world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "false");
                    continue;
                }
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }

            LogUtils.doLog("&eAll worlds have default advancements disabled.");
        }

        List<Advancement> tasks = new ArrayList<>(), goals = new ArrayList<>(),
                challenges = new ArrayList<>(),
                errors = new ArrayList<>(), keys = new ArrayList<>();

        for (var adv : getAdvancements()) {
            ADVANCEMENT_KEYS.put(adv, new AdvancementInfo(adv));

            var key = LangUtils.stringKey(adv.getKey().toString());
            var type = ADVANCEMENT_KEYS.get(adv).getFrameType();

            if (key.contains("root") || key.contains("recipes")) continue;

            var advances = FileCache.ADVANCE_LANG.get();
            if (advances == null) continue;

            var notContained = !advances.contains(key);

            switch (type.toUpperCase(Locale.ENGLISH)) {
                case "TASK":
                    tasks.add(adv);
                    if (notContained) {
                        advances.set(key, "type.task");
                        keys.add(adv);
                    }
                    break;

                case "GOAL":
                    goals.add(adv);
                    if (notContained) {
                        advances.set(key, "type.goal");
                        keys.add(adv);
                    }
                    break;

                case "CHALLENGE":
                    challenges.add(adv);
                    if (notContained) {
                        advances.set(key, "type.challenge");
                        keys.add(adv);
                    }
                    break;

                default:
                    errors.add(adv);
                    if (notContained) {
                        advances.set(key, "type.custom");
                        keys.add(adv);
                    }
                    break;
            }
        }

        if (keys.size() > 0) {
            var file = FileCache.ADVANCE_LANG.getFile();
            if (file != null) file.saveFile();
        }

        String counts = "&7Tasks: &a" + tasks.size() +
                "&7 - Goals: &b" + goals.size() + "&7 - " + "&7Challenges: &d" + challenges.size();
        String error = errors.size() == 0 ? null :
                ("&7Unknowns: &c" + errors.size() + "&7. Check your advances.yml file!");

        if (!debug) return;

        LogUtils.doLog(counts, error,
                "&7Registered advancements in &e" + (System.currentTimeMillis() - time) + "&7 ms.");
        LogUtils.rawLog("");
    }

    @SuppressWarnings("deprecation")
    public static void unloadAdvances(boolean reload) {
        var version = LibUtils.getMainVersion();

        if (version < 12) return;
        if (SIRModule.isEnabled("advances") && reload) return;

        for (var world : Bukkit.getServer().getWorlds()) {
            if (version >= 12 && version < 13) {
                world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "true");
                continue;
            }
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        }

        LogUtils.doLog("&eAll worlds have default advancements enabled.");
    }

    public static List<Advancement> getAdvancements() {
        return Lists.newArrayList(Bukkit.getServer().advancementIterator());
    }

    public static Permission getPerms() {
        return permProvider;
    }

    public static Map<Advancement, AdvancementInfo> getKeys() {
        return ADVANCEMENT_KEYS;
    }
}
