package me.croabeast.sircore;

import github.scarsz.discordsrv.*;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.plugin.*;

import java.util.*;

public class Initializer {

    private final Application main;
    private final Recorder recorder;
    public static Permission Perms;

    public int LISTENERS = 0;

    public boolean HAS_PAPI, HAS_VAULT, DISCORD, HAS_LOGIN, HAS_VANISH,
            authMe, userLogin, hasCMI, essentials, srVanish, prVanish;

    protected List<String> LOGIN_HOOKS = new ArrayList<>(),
            VANISH_HOOKS = new ArrayList<>();

    public Initializer(Application main) {
        this.main = main;
        recorder = main.getRecorder();

        HAS_PAPI = isPlugin("PlaceholderAPI");
        HAS_VAULT = isPlugin("Vault");
        DISCORD = isPlugin("DiscordSRV");

        authMe = isHooked("AuthMe", LOGIN_HOOKS);
        userLogin = isHooked("UserLogin", LOGIN_HOOKS);

        hasCMI = isHooked("CMI", VANISH_HOOKS);
        essentials = isHooked("Essentials", VANISH_HOOKS);
        srVanish = isHooked("SuperVanish", VANISH_HOOKS);
        prVanish = isHooked("PremiumVanish", VANISH_HOOKS);
    }

    private boolean isPlugin(String name) {
        return main.getPlugin(name) != null;
    }

    private boolean isHooked(String name, List<String> hookList) {
        if (!isPlugin(name)) return false;
        hookList.add(name);
        return true;
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> HAS_PAPI + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> HAS_VAULT + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasDiscord", () -> DISCORD + ""));

        metrics.addCustomChart(new Metrics.DrilldownPie("loginPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Login Plugins", 1);

            if (HAS_LOGIN) {
                if (userLogin) map.put("UserLogin", entry);
                else if (authMe) map.put("AuthMe", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));

        metrics.addCustomChart(new Metrics.DrilldownPie("vanishPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Vanish Plugins", 1);

            if (HAS_VANISH) {
                if (hasCMI) map.put("CMI", entry);
                else if (essentials) map.put("EssentialsX", entry);
                else if (srVanish) map.put("SuperVanish", entry);
                else if (prVanish) map.put("PremiumVanish", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));
    }

    public void setPluginHooks() {
        // PlaceholderAPI
        recorder.doRecord("", "&bChecking all simple plugin hooks...");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        if (!HAS_VAULT)
            recorder.doRecord("&7Vault&c isn't installed&7, using default system.");
        else {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
            if (rsp != null) {
                Perms = rsp.getProvider();
                recorder.doRecord("&7Vault&a installed&7, hooking in a perm plugin...");
            }
            else recorder.doRecord("&7Unknown perm provider&7, using default system.");
        }

        // DiscordSRV Hook
        recorder.doRecord("", "&bChecking if DiscordSRV is enabled...");
        showPluginInfo("DiscordSRV");

        if (DISCORD) {
            if (discordServer() != null)
                recorder.doRecord("" +
                        "&7Server name: &e" + discordServer().getName() + " by " +
                        Objects.requireNonNull(discordServer().getOwner()).getEffectiveName()
                );
            else recorder.doRecord("" +
                    "&cInvalid SERVER_ID, change it ASAP",
                    "&7After that, use &b/sir reload"
            );
        }

        // Login hook
        recorder.doRecord("", "&bChecking if a login plugin is enabled...");

        if (LOGIN_HOOKS.size() == 1) {
            HAS_LOGIN = true;
            showPluginInfo(LOGIN_HOOKS.get(0));
        }
        else {
            HAS_LOGIN = false;
            if (LOGIN_HOOKS.size() > 1)
                recorder.doRecord(
                        "&cTwo or more compatible login plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            else recorder.doRecord("&cNo login plugin installed. &7Unhooking...");
        }

        recorder.doRecord("", "&bChecking if a vanish plugin is enabled...");

        if (VANISH_HOOKS.size() == 1) {
            HAS_VANISH = true;
            showPluginInfo(VANISH_HOOKS.get(0));
        }
        else {
            HAS_VANISH = false;
            if (VANISH_HOOKS.size() > 1)
                recorder.doRecord(
                        "&cTwo or more compatible vanish plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            else recorder.doRecord("&cNo vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerListeners() {
        recorder.doRecord("", "&bLoading all the listeners...");

        new PlayerListener(main);
        new MOTDListener(main);
        new FormatListener(main);
        new Advancements(main);

        if (HAS_LOGIN) new LoginListener(main);
        if (HAS_VANISH) new VanishListener(main);

        recorder.doRecord("&7Registered &e" + LISTENERS + "&7 plugin's listeners.");
    }

    @SuppressWarnings("deprecation")
    public void loadAdvances() {
        if (main.MC_VERSION < 12) return;

        long time = System.currentTimeMillis();
        recorder.doRecord("", "&bLoading all the advancements...");

        for (World world : main.getServer().getWorlds()) {
            if (main.MC_VERSION == 12) {
                world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "false");
                continue;
            }
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }

        Set<String> tasks = new HashSet<>(), goals = new HashSet<>(),
                challenges = new HashSet<>(), keys = new HashSet<>();
        Iterator<Advancement> progress = main.getServer().advancementIterator();

        while (progress.hasNext()) {
            Advancement adv = progress.next();
            String key = main.getTextUtils().stringKey(adv.getKey().toString());
            String type = new Advancements.ReflectKeys(adv).getFrameType();

            if (key.contains("root") || key.contains("recipes")) continue;
            boolean notContained = !main.getAdvances().contains(key);

            if (type.matches("(?i)CHALLENGE")) {
                challenges.add(key);
                if (notContained) {
                    main.getAdvances().set(key, "type.challenge");
                    keys.add(key);
                }
            }
            else if (type.matches("(?i)TASK")) {
                tasks.add(key);
                if (notContained) {
                    main.getAdvances().set(key, "type.task");
                    keys.add(key);
                }
            }
            else if (type.matches("(?i)GOAL")) {
                goals.add(key);
                if (notContained) {
                    main.getAdvances().set(key, "type.goal");
                    keys.add(key);
                }
            }
        }

        if (keys.size() > 0) main.getFiles().getObject("advances").saveFile();
        recorder.doRecord(
                "&7Tasks: &a" + tasks.size() + "&7 - Goals: &b" + goals.size() + "&7 - " +
                "&7Challenges: &d" + challenges.size(), // haha, I hate those empty gaps lmao
                "&7Registered advancements in &e" + (System.currentTimeMillis() - time) + "&7 ms."
        );
    }

    @SuppressWarnings("deprecation")
    public void unloadAdvances() {
        if (main.MC_VERSION < 12) return;

        for (World world : main.getServer().getWorlds()) {
            if (main.MC_VERSION == 12) {
                world.setGameRuleValue("ANNOUNCE_ADVANCEMENTS", "true");
                continue;
            }
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        }
    }

    private void showPluginInfo(String name) {
        String pluginVersion;
        String isHooked;

        if (isPlugin(name)) {
            pluginVersion = main.getPlugin(name).getDescription().getVersion();
            isHooked = " &aenabled&7. Hooking...";
        }
        else {
            pluginVersion = "";
            isHooked = "&cnot found&7. Unhooking...";
        }

        recorder.doRecord("&7" + name + " " + pluginVersion + isHooked);
    }

    public Guild discordServer() {
        try {
            String server = main.getDiscord().getString("server-id", "");
            return DiscordSRV.getPlugin().getJda().getGuildById(server);
        }
        catch (Exception e) {
            return null;
        }
    }
}
