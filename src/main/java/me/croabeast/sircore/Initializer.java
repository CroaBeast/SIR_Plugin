package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.objects.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.plugin.*;

import java.util.*;

public class Initializer {

    private final Application main;
    private final Records records;
    public static Permission Perms;

    public SavedFile config;
    public SavedFile announces;
    public SavedFile lang;
    public SavedFile messages;

    public int listeners = 0;
    public int files = 0;

    public boolean hasPAPI;
    public boolean hasVault;

    public boolean hasLogin;
    public boolean authMe;
    public boolean userLogin;

    public boolean hasVanish;
    public boolean hasCMI;
    public boolean essentials;
    public boolean srVanish;
    public boolean prVanish;

    public Initializer(Application main) {
        this.main = main;
        records = main.getRecords();

        hasPAPI = main.getPlugin("PlaceholderAPI") != null;
        hasVault = main.getPlugin("Vault") != null;
        authMe = main.getPlugin("AuthMe") != null;
        userLogin = main.getPlugin("UserLogin") != null;

        hasCMI = main.getPlugin("CMI") != null;
        essentials = main.getPlugin("Essentials") != null;
        srVanish = main.getPlugin("SuperVanish") != null;
        prVanish = main.getPlugin("PremiumVanish") != null;
    }

    public void loadSavedFiles() {
        records.doRecord("&bLoading plugin's files...");
        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");
        announces = new SavedFile(main, "announces");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        announces.updateInitFile();

        for (String key : main.getMessages().getKeys(false)) {
            int sections = main.getTextUtils().getSections(key);
            if (sections == 0) continue;
            String section = sections + "&7 groups in the &e'" + key;
            records.doRecord("&7Found &e" + section + "'&7 section.");
        }
        records.doRecord("&7Loaded &e" + files + "&7 files in the plugin's folder.");
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> hasPAPI + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> hasVault + ""));

        metrics.addCustomChart(new Metrics.DrilldownPie("loginPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Login Plugins", 1);

            if (hasLogin) {
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

            if (hasVault) {
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
        records.doRecord("", "&bChecking all the available hooks...");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        if (!hasVault)
            records.doRecord("&7Vault&c isn't installed&7, using default system.");
        else {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
            if (rsp != null) {
                Perms = rsp.getProvider();
                records.doRecord("&7Vault&a installed&7, hooking in a perm plugin...");
            }
            else records.doRecord("&7Unknown perm provider&7, using default system.");
        }

        // Login hook
        String loginPlugin = "Login Plugin";
        int i = 0;

        if (authMe) {
            i++;
            loginPlugin = "AuthMe";
        }
        if (userLogin) {
            i++;
            loginPlugin = "UserLogin";
        }

        records.doRecord("> &7Checking if a login plugin is enabled...");

        if (i == 1) {
            hasLogin = true;
            showPluginInfo(loginPlugin);
        } else {
            hasLogin = false;
            if (i > 1) {
                records.doRecord(
                        "&cTwo or more compatible login plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            }
            else records.doRecord("&cNo login plugin installed. &7Unhooking...");
        }

        // Vanish hook
        String vanishPlugin = "Vanish Plugin";
        int x = 0;

        if (essentials) {
            x++;
            vanishPlugin = "Essentials";
        }
        if (hasCMI) {
            x++;
            vanishPlugin = "CMI";
        }
        if (srVanish) {
            x++;
            vanishPlugin = "SuperVanish";
        }
        if (prVanish) {
            x++;
            vanishPlugin = "PremiumVanish";
        }

        records.doRecord("> &7Checking if a vanish plugin is enabled...");

        if (x == 1) {
            hasVanish = true;
            showPluginInfo(vanishPlugin);
        } else {
            hasVanish = false;
            if (x > 1) {
                records.doRecord(
                        "&cTwo or more compatible vanish plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            }
            else records.doRecord("&cNo vanish plugin installed. &7Unhooking...");
        }
    }

    public void startUpdater() {
        Updater.init(main, 96378).updateCheck().whenComplete(((updateResult, throwable) -> {
            if (!main.getTextUtils().getOption(4, "on-start")) return;

            String latest = updateResult.getNewestVersion();

            records.rawRecord("");
            switch (updateResult.getReason()) {
                case NEW_UPDATE:
                    records.doRecord(
                            "&4BIG WARNING!",
                            "&cYou don't have the latest version of S.I.R. installed.",
                            "&cRemember, older versions won't receive any support.",
                            "&7New Version: &a" + latest + "&7 - Your Version: &e" + main.version,
                            "&7Link:&b https://www.spigotmc.org/resources/96378/"
                    );
                    break;
                case UP_TO_DATE:
                    records.doRecord(
                            "&eYou have the latest version of S.I.R. &7(" + latest + ")",
                            "&7I would appreciate if you keep updating &c<3"
                    );
                    break;
                case UNRELEASED_VERSION:
                    records.doRecord(
                            "&4DEVELOPMENT BUILD:",
                            "&cYou have a newer version of S.I.R. installed.",
                            "&cErrors might occur in this build.",
                            "Spigot Version: &a" + updateResult.getSpigotVersion()
                                    + "&7 - Your Version: &e" + main.version
                    );
                    break;
                default:
                    records.rawRecord(
                            "&4WARNING!",
                            "&cCould not check for a new version of S.I.R.",
                            "&7Please check your connection and restart the server.",
                            "&7Possible reason: &e" + updateResult.getReason()
                    );
                    break;
            }
            records.rawRecord("");
        }));
    }

    public void registerListeners() {
        records.doRecord("", "&bLoading all the listeners...");
        new PlayerListener(main);
        new LoginListener(main);
        new VanishListener(main);
        records.doRecord("&7Registered &e" + listeners + "&7 plugin's listeners.");
    }

    public void reloadFiles() {
        config.reloadFile();
        lang.reloadFile();
        messages.reloadFile();
        announces.reloadFile();
    }

    private void showPluginInfo(String name) {
        String pluginVersion;
        String isHooked;

        if (main.getPlugin(name) != null) {
            pluginVersion = main.getPlugin(name).getDescription().getVersion();
            isHooked = " &aenabled&7. Hooking...";
        } else {
            pluginVersion = "";
            isHooked = "&cnot found&7. Unhooking...";
        }

        records.doRecord("&7" + name + " " + pluginVersion + isHooked);
    }
}
