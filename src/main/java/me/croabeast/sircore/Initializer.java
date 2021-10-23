package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.objects.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.plugin.*;

import java.util.*;

public class Initializer {

    private final Application main;
    public static Permission Perms = null;

    public SavedFile config;
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
    public boolean superVanish;
    public boolean prVanish;

    public Initializer(Application main) {
        this.main = main;

        hasPAPI = main.plugin("PlaceholderAPI") != null;
        hasVault = main.plugin("Vault") != null;
        authMe = main.plugin("AuthMe") != null;
        userLogin = main.plugin("UserLogin") != null;

        hasCMI = main.plugin("CMI") != null;
        essentials = main.plugin("Essentials") != null;
        superVanish = main.plugin("SuperVanish") != null;
        prVanish = main.plugin("PremiumVanish") != null;
    }

    public void savedFiles() {
        main.doLogger("&bLoading plugin's files...");
        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        for (String s : main.getMessages().getKeys(false)) {
            if (main.sections(s) == 0) continue;
            String section = main.sections(s) + "&7 groups in the &e'" + s;
            main.doLogger("&7Found &e" + section + "'&7 section.");
        }
        main.doLogger("&7Loaded &e" + files + "&7 files in the plugin's folder.");
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("listeners", () -> listeners + ""));
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
                if (userLogin) map.put("CMI", entry);
                else if (authMe) map.put("EssentialsX", entry);
                else if (superVanish) map.put("SuperVanish", entry);
                else if (prVanish) map.put("PremiumVanish", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));
    }

    public void setPluginHooks() {
        // PlaceholderAPI
        main.doLogger("", "&bChecking all the available hooks...");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        if (!hasVault) main.doLogger("&7Vault&c isn't installed&7, using default system.");
        else {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
            if (rsp != null) {
                Perms = rsp.getProvider();
                main.doLogger("&7Vault&a installed&7, hooking in a perm plugin...");
            }
            else main.doLogger("&7Unknown perm provider&7, using default system.");
        }

        // Login hook
        String loginPlugin = "";
        int i = 0;
        if (authMe) {
            i++;
            loginPlugin = "AuthMe";
        }
        if (userLogin) {
            i++;
            loginPlugin = "UserLogin";
        }

        main.doLogger("> &7Checking if a login plugin is enabled...");

        if (i == 1) {
            hasLogin = true;
            showPluginInfo(loginPlugin);
        } else {
            hasLogin = false;
            if (i > 1) {
                main.doLogger(
                        "&cTwo or more compatible login plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            }
            else main.doLogger("&cThere is no login plugin installed. &7Unhooking...");
        }

        // Vanish hook
        String vanishPlugin = "";
        int x = 0;
        if (essentials) {
            x++;
            vanishPlugin = "Essentials";
        }
        if (hasCMI) {
            x++;
            vanishPlugin = "CMI";
        }
        if (superVanish) {
            x++;
            vanishPlugin = "SuperVanish";
        }
        if (prVanish) {
            x++;
            vanishPlugin = "PremiumVanish";
        }

        main.doLogger("> &7Checking if a vanish plugin is enabled...");

        if (x == 1) {
            hasVanish = true;
            showPluginInfo(vanishPlugin);
        } else {
            hasVanish = false;
            if (x > 1) {
                main.doLogger(
                        "&cTwo or more compatible vanish plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            }
            else main.doLogger("&cThere is no vanish plugin installed. &7Unhooking...");
        }
    }

    public void startUpdater() {
        new Updater(main, 96378).getVersion(latest -> {
            if (!main.choice("logger")) return;
            if (main.version.equals(latest)) {
                main.rawLogger("");
                main.doLogger(
                        "&eYou have the latest version of S.I.R. &7(" + main.version + ")",
                        "&7I would appreciate if you keep updating &c<3"
                );
                main.rawLogger("");
            } else {
                main.rawLogger("");
                main.doLogger(
                        "&4BIG WARNING!",
                        "&cYou don't have the latest version of S.I.R. installed.",
                        "&cRemember, older versions won't receive any support.",
                        "&7New Version: &e" + latest + "&7 - Your Version: &e" + main.version,
                        "&7Link:&b https://www.spigotmc.org/resources/96378/"
                );
                main.rawLogger("");
            }
        });
    }

    public void registerListeners() {
        main.doLogger("", "&bLoading all the listeners...");
        new PlayerListener(main);
        new LoginListener(main);
        new VanishListener(main);
        main.doLogger("&7Registered &e" + listeners + "&7 plugin's listeners.");
    }

    private void showPluginInfo(String name) {
        boolean isPlugin = main.plugin(name).isEnabled();
        main.doLogger("&7" + name + " " +
                (isPlugin ? main.plugin(name).getDescription().getVersion() +
                        " &aenabled&7. Hooking..." : "&cnot found&7. Unhooking...")
        );
    }
}
