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
import me.croabeast.sirplugin.utility.LogUtils;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Initializer {

    public static final Map<Advancement, AdvancementInfo> ADVANCEMENT_MAP = new HashMap<>();
    private static final Map<String, List<AdvancementInfo>> ADV_INFO_MAP = new HashMap<>();

    public static final List<Advancement> ADV_LIST;

    private static void forList(Set<AdvancementInfo> set, String frame) {
        set.stream().
                filter(info -> info.getFrameType().matches("(?i)" + frame)).
                forEach(info -> {
                    var s = ADV_INFO_MAP.getOrDefault(frame, new ArrayList<>());
                    s.add(info);

                    ADV_INFO_MAP.put(frame, s);
                });
    }

    static {
        ADV_LIST = Lists.newArrayList(Bukkit.advancementIterator()).
                stream().filter(a -> {
                    var key = a.getKey().toString();

                    if (key.contains("recipes"))
                        return false;

                    return !key.contains("root");
                }).
                collect(Collectors.toList());

        var infoSet = ADV_LIST.stream().map(AdvancementInfo::new).collect(Collectors.toSet());

        infoSet.forEach(info -> ADVANCEMENT_MAP.put(info.getBukkit(), info));

        forList(infoSet, "task");
        forList(infoSet, "goal");
        forList(infoSet, "challenge");
        forList(infoSet, "unknown");
    }

    private static List<AdvancementInfo> getList(String frame) {
        return ADV_INFO_MAP.getOrDefault(frame, new ArrayList<>());
    }

    private static Set<AdvancementInfo> toTypeSet(String frame) {
        return new HashSet<>(getList(frame));
    }

    private static final Set<AdvancementInfo> TASK_ADVANCEMENTS = toTypeSet("task");
    private static final Set<AdvancementInfo> GOAL_ADVANCEMENTS = toTypeSet("goal");
    private static final Set<AdvancementInfo> CHALLENGE_ADVANCEMENTS = toTypeSet("challenge");
    private static final Set<AdvancementInfo> UNKNOWN_ADVANCEMENTS = toTypeSet("unknown");

    private Initializer() {}

    private static Permission permProvider;

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

    private static Consumer<AdvancementInfo> fromInfo(Set<Advancement> keys, String type) {
        return info -> {
            var advances = FileCache.ADVANCE_LANG.get();
            if (advances == null) return;

            var adv = info.getBukkit();

            final String k = adv.getKey().toString();
            var key = k.replaceAll("[/:]", ".");

            if (advances.contains(key)) return;

            String title = info.getTitle();
            if (title == null) {
                String temp = k.substring(k.lastIndexOf('/') + 1);
                temp = temp.replace('_', ' ');

                char f = temp.toCharArray()[0];
                String first = (f + "").toUpperCase(Locale.ENGLISH);

                title = first + temp.substring(1);
            }

            advances.set(key + ".path", "type." + type);

            advances.set(key + ".frame", info.getFrameType());
            advances.set(key + ".name", title);
            advances.set(key + ".description", info.getDescription());

            final var item = info.getItem();
            advances.set(key + ".item",
                    item == null ? null : item.getType().toString());

            keys.add(adv);
        };
    }

    @SuppressWarnings("deprecation")
    public static void loadAdvances(boolean debug) {
        var version = LibUtils.getMainVersion();
        if (version < 12) return;

        var time = System.currentTimeMillis();
        if (debug) {
            LogUtils.rawLog("");
            LogUtils.doLog("&bRegistering all the advancement values...");
        }

        if (SIRModule.isEnabled("advances")) {
            for (var world : Bukkit.getWorlds())
                world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "false");
            LogUtils.doLog("&eAll worlds have default advancements disabled.");
        }

        Set<Advancement> loadedKeys = new HashSet<>();

        TASK_ADVANCEMENTS.forEach(fromInfo(loadedKeys, "task"));
        GOAL_ADVANCEMENTS.forEach(fromInfo(loadedKeys, "goal"));
        CHALLENGE_ADVANCEMENTS.forEach(fromInfo(loadedKeys, "challenge"));
        UNKNOWN_ADVANCEMENTS.forEach(fromInfo(loadedKeys, "custom"));

        if (loadedKeys.size() > 0) {
            var file = FileCache.ADVANCE_LANG.getFile();
            if (file != null) file.saveFile();
        }

        if (!debug) return;

        String advancements = "&7Tasks: &a" + TASK_ADVANCEMENTS.size() +
                "&7 - Goals: &b" + GOAL_ADVANCEMENTS.size() +
                "&7 - &7Challenges: &d" + CHALLENGE_ADVANCEMENTS.size();
        LogUtils.doLog(advancements);

        if (!UNKNOWN_ADVANCEMENTS.isEmpty())
            LogUtils.doLog(
                    "&7Unknowns: &c" + UNKNOWN_ADVANCEMENTS.size() +
                    "&7. Check your advances.yml file!"
            );

        LogUtils.mixLog(
                "&7Loaded advancements in &e" + (System.currentTimeMillis() - time) +
                "&7 ms.", "true::"
        );
    }

    @SuppressWarnings("deprecation")
    public static void unloadAdvances(boolean reload) {
        var version = LibUtils.getMainVersion();
        if (version < 12) return;

        if (SIRModule.isEnabled("advances") && reload) return;

        for (var world : Bukkit.getWorlds())
            world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "true");

        LogUtils.doLog("&eAll worlds have default advancements enabled.");
    }

    public static Permission getPerms() {
        return permProvider;
    }
}
