package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.objects.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.command.*;
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
    public SavedFile motd;

    public int LISTENERS = 0;
    public int FILES = 0;

    public boolean HAS_PAPI;
    public boolean HAS_VAULT;

    public boolean HAS_LOGIN;
    public boolean authMe;
    public boolean userLogin;

    public boolean HAS_VANISH;
    public boolean hasCMI;
    public boolean essentials;
    public boolean srVanish;
    public boolean prVanish;

    public Initializer(Application main) {
        this.main = main;
        records = main.getRecords();

        HAS_PAPI = isPlugin("PlaceholderAPI");
        HAS_VAULT = isPlugin("Vault");
        authMe = isPlugin("AuthMe");
        userLogin = isPlugin("UserLogin");

        hasCMI = isPlugin("CMI");
        essentials = isPlugin("Essentials");
        srVanish = isPlugin("SuperVanish");
        prVanish = isPlugin("PremiumVanish");
    }

    private boolean isPlugin(String name) {
        return main.getPlugin(name) != null;
    }

    public void loadSavedFiles() {
        records.doRecord("&bLoading plugin's files...");

        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");
        announces = new SavedFile(main, "announces");
        motd = new SavedFile(main, "motd");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        announces.updateInitFile();
        motd.updateInitFile();

        records.doRecord("&7Loaded &e" + FILES + "&7 files in the plugin's folder.");
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> HAS_PAPI + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> HAS_VAULT + ""));

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
        records.doRecord("", "&bChecking all the available hooks...");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        if (!HAS_VAULT)
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
            HAS_LOGIN = true;
            showPluginInfo(loginPlugin);
        } else {
            HAS_LOGIN = false;
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
            HAS_VANISH = true;
            showPluginInfo(vanishPlugin);
        } else {
            HAS_VANISH = false;
            if (x > 1) {
                records.doRecord(
                        "&cTwo or more compatible vanish plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            }
            else records.doRecord("&cNo vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerListeners() {
        records.doRecord("", "&bLoading all the listeners...");
        new PlayerListener(main);
        new MOTDListener(main);
        new LoginListener(main);
        new VanishListener(main);
        records.doRecord("&7Registered &e" + LISTENERS + "&7 plugin's listeners.");
    }

    public void reloadFiles() {
        config.reloadFile();
        lang.reloadFile();
        messages.reloadFile();
        announces.reloadFile();
        motd.reloadFile();
    }

    public void checkFeatures(CommandSender sender) {
        if (main.getTextUtils().getOption(1, "enabled") || main.getAnnouncer().getDelay() != 0 |
                main.getMOTD().getBoolean("enabled")) return;

        records.doRecord(sender,
                "", "<P> &All main features of &eS.I.R. &7are disabled.",
                "<P> &cIt's better to delete the plugin instead doing that...",
                sender != null ? "" : null
        );
    }

    private void showPluginInfo(String name) {
        String pluginVersion;
        String isHooked;

        if (isPlugin(name)) {
            pluginVersion = main.getPlugin(name).getDescription().getVersion();
            isHooked = " &aenabled&7. Hooking...";
        } else {
            pluginVersion = "";
            isHooked = "&cnot found&7. Unhooking...";
        }

        records.doRecord("&7" + name + " " + pluginVersion + isHooked);
    }
}
