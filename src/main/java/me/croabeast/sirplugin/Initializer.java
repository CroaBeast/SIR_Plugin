package me.croabeast.sirplugin;

import com.google.common.collect.*;
import me.croabeast.advancementinfo.*;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.object.analytic.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.*;

import java.util.*;

public final class Initializer {

    private final SIRPlugin MAIN = SIRPlugin.getInstance();
    private static Permission permProvider;

    private final boolean
            userLogin = isHooked("UserLogin", LOGIN_HOOKS),
            authMe = isHooked("AuthMe", LOGIN_HOOKS),
            hasCMI = isHooked("CMI", VANISH_HOOKS),
            essentials = isHooked("Essentials", VANISH_HOOKS),
            srVanish = isHooked("SuperVanish", VANISH_HOOKS),
            prVanish = isHooked("PremiumVanish", VANISH_HOOKS);

    private static final List<String>
            LOGIN_HOOKS = new ArrayList<>(),
            VANISH_HOOKS = new ArrayList<>();

    private static final Map<Advancement, AdvancementInfo>
            ADVANCEMENT_KEYS = new HashMap<>();

    private boolean isHooked(String name, List<String> hookList) {
        if (Bukkit.getPluginManager().getPlugin(name) == null) return false;
        hookList.add(name);
        return true;
    }

    private boolean hasPAPI() {
        return Exceptions.isPluginEnabled("PlaceholderAPI");
    }
    public static boolean hasVault() {
        return Exceptions.isPluginEnabled("Vault");
    }

    public static boolean hasDiscord() {
        return Exceptions.isPluginEnabled("DiscordSRV") && Identifier.DISCORD.isEnabled();
    }

    public static boolean hasLogin() {
        return LOGIN_HOOKS.size() == 1;
    }
    public static boolean hasVanish() {
        return VANISH_HOOKS.size() == 1;
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(MAIN, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> hasPAPI() + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> hasVault() + ""));

        metrics.addCustomChart(new Metrics.SimplePie("hasDiscord", () ->
                (Exceptions.isPluginEnabled("DiscordSRV")) + ""));

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
            ServicesManager servMngr = MAIN.getServer().getServicesManager();
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
                    "&eFound " + LOGIN_HOOKS.get(0) + " v. " + pluginVersion(LOGIN_HOOKS.get(0)));
            logLines++;
        }

        if (hasVanish()) {
            LogUtils.doLog("&7Vanish Plugin: " +
                    "&eFound " + VANISH_HOOKS.get(0) + " v. " + pluginVersion(VANISH_HOOKS.get(0)));
            logLines++;
        }

        if (logLines == 0) LogUtils.doLog("&cThere is no compatible hooks available.");
    }

    @SuppressWarnings("deprecation")
    public static void loadAdvances(boolean debug) {
        if (LibUtils.majorVersion() < 12) return;
        if (!ADVANCEMENT_KEYS.isEmpty()) ADVANCEMENT_KEYS.clear();

        long time = System.currentTimeMillis();
        if (debug) {
            LogUtils.rawLog("");
            LogUtils.doLog("&bRegistering all the advancement values...");
        }

        if (Identifier.ADVANCES.isEnabled()) {
            for (World world : Bukkit.getServer().getWorlds()) {
                if (LibUtils.majorVersion() == 12) {
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
            Initializer.ADVANCEMENT_KEYS.put(adv, new AdvancementInfo(adv));

            String key = LangUtils.stringKey(adv.getKey().toString());
            final String type = Initializer.ADVANCEMENT_KEYS.get(adv).getFrameType();

            if (key.contains("root") || key.contains("recipes")) continue;

            FileConfiguration advances = FileCache.ADVANCES.get();
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

        if (keys.size() > 0) FileCache.ADVANCES.source().saveFile();

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
        if (LibUtils.majorVersion() < 12) return;
        if (Identifier.ADVANCES.isEnabled() && reload) return;

        for (World world : Bukkit.getServer().getWorlds()) {
            if (LibUtils.majorVersion() == 12) {
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
