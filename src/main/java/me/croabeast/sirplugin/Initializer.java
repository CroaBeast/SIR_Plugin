package me.croabeast.sirplugin;

import com.google.common.collect.*;
import me.croabeast.advancementinfo.*;
import me.croabeast.sirplugin.objects.analytics.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.utilities.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCache.*;

public final class Initializer {

    private final SIRPlugin main;
    private static Permission permProvider;

    private final boolean userLogin, authMe, hasCMI,
            essentials, srVanish, prVanish;

    static List<String> loginHooks = new ArrayList<>(),
            vanishHooks = new ArrayList<>();

    static Map<Advancement, AdvancementInfo> keys = new HashMap<>();
    static Map<BaseModule.Identifier, BaseModule> moduleMap = new HashMap<>();

    public Initializer(SIRPlugin main) {
        this.main = main;

        userLogin = isHooked("UserLogin", loginHooks);
        authMe = isHooked("AuthMe", loginHooks);

        hasCMI = isHooked("CMI", vanishHooks);
        essentials = isHooked("Essentials", vanishHooks);

        srVanish = isHooked("SuperVanish", vanishHooks);
        prVanish = isHooked("PremiumVanish", vanishHooks);
    }

    private boolean isHooked(String name, List<String> hookList) {
        if (Bukkit.getPluginManager().getPlugin(name) == null) return false;
        hookList.add(name);
        return true;
    }

    private boolean hasPAPI() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }
    public static boolean hasVault() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    public static boolean hasDiscord() {
        return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV") && BaseModule.isEnabled(BaseModule.Identifier.DISCORD);
    }

    public static boolean hasLogin() {
        return loginHooks.size() == 1;
    }
    public static boolean hasVanish() {
        return vanishHooks.size() == 1;
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> hasPAPI() + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> hasVault() + ""));

        metrics.addCustomChart(new Metrics.SimplePie("hasDiscord", () ->
                (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) + ""));

        metrics.addCustomChart(new Metrics.DrilldownPie("loginPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Login Plugins", 1);

            if (hasLogin()) {
                if (userLogin) map.put("UserLogin", entry);
                else if (authMe) map.put("AuthMe", entry);
                else map.put("None / Other", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));

        metrics.addCustomChart(new Metrics.DrilldownPie("vanishPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Vanish Plugins", 1);

            if (hasVanish()) {
                if (hasCMI) map.put("CMI", entry);
                else if (essentials) map.put("EssentialsX", entry);
                else if (srVanish) map.put("SuperVanish", entry);
                else if (prVanish) map.put("PremiumVanish", entry);
                else map.put("None / Other", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));
    }

    private String pluginVersion(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null ? plugin.getDescription().getVersion() : "";
    }

    public void setPluginHooks() {
        LogUtils.doLog("", "&bChecking all compatible hooks...");
        int logLines = 0;

        if (hasPAPI()) {
            LogUtils.doLog("&7PlaceholderAPI: &eFound v. " + pluginVersion("PlaceholderAPI"));
            logLines++;
        }

        if (hasVault()) {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);

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

        if (hasLogin()) {
            LogUtils.doLog("&7Login Plugin: " +
                    "&eFound " + loginHooks.get(0) + " v. " + pluginVersion(loginHooks.get(0)));
            logLines++;
        }

        if (hasVanish()) {
            LogUtils.doLog("&7Vanish Plugin: " +
                    "&eFound " + vanishHooks.get(0) + " v. " + pluginVersion(vanishHooks.get(0)));
            logLines++;
        }

        if (logLines == 0) LogUtils.doLog("&cThere is no compatible hooks available.");
    }

    @SuppressWarnings("deprecation")
    public static void loadAdvances(boolean debug) {
        if (TextUtils.majorVersion() < 12) return;
        if (!keys.isEmpty()) keys.clear();

        long time = System.currentTimeMillis();
        if (debug) {
            LogUtils.rawLog("");
            LogUtils.doLog("&bRegistering all the advancement values...");
        }

        if (BaseModule.isEnabled(BaseModule.Identifier.ADVANCES)) {
            for (World world : Bukkit.getServer().getWorlds()) {
                if (TextUtils.majorVersion() == 12) {
                    world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "false");
                    continue;
                }
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }

            LogUtils.doLog("&eAll worlds have default advancements disabled.");
        }

        List<Advancement> tasks = new ArrayList<>(), goals = new ArrayList<>(),
                challenges = new ArrayList<>(), errors = new ArrayList<>(), keys = new ArrayList<>();

        for (Advancement adv : getAdvancements()) {
            Initializer.keys.put(adv, new AdvancementInfo(adv));

            String key = TextUtils.stringKey(adv.getKey().toString());
            final String type = Initializer.keys.get(adv).getFrameType();

            if (key.contains("root") || key.contains("recipes")) continue;

            FileConfiguration advances = ADVANCES.toFile();
            boolean notContained = !advances.contains(key);

            switch (type.toUpperCase()) {
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

        if (keys.size() > 0) ADVANCES.fromSource().saveFile();

        String error = errors.size() == 0 ? null : "&7Unknowns: &c" + errors.size() +
                "&7. Check your advances.yml file!";

        if (debug) {
            LogUtils.doLog("" +
                            "&7Tasks: &a" + tasks.size() + "&7 - Goals: &b" + goals.size() + "&7 - " +
                            "&7Challenges: &d" + challenges.size(), error, // I HATE EMPTY SPACES AAA
                    "&7Registered advancements in &e" + (System.currentTimeMillis() - time) + "&7 ms."
            );
            LogUtils.rawLog("");
        }
    }

    @SuppressWarnings("deprecation")
    public static void unloadAdvances(boolean reload) {
        if (TextUtils.majorVersion() < 12) return;
        if (BaseModule.isEnabled(BaseModule.Identifier.ADVANCES) && reload) return;

        for (World world : Bukkit.getServer().getWorlds()) {
            if (TextUtils.majorVersion() == 12) {
                world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "true");
                continue;
            }
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        }

        LogUtils.doLog("&eAll worlds have default advancements enabled.");
    }

    public void registerModules(BaseModule... baseModules) {
        for (BaseModule baseModule : baseModules) {
            moduleMap.put(baseModule.getIdentifier(), baseModule);
            baseModule.registerModule();
        }
    }

    public static List<Advancement> getAdvancements() {
        return Lists.newArrayList(Bukkit.getServer().advancementIterator());
    }

    public static Permission getPerms() {
        return permProvider;
    }

    public static Map<BaseModule.Identifier, BaseModule> getModules() {
        return moduleMap;
    }

    public static Map<Advancement, AdvancementInfo> getKeys() {
        return keys;
    }
}
