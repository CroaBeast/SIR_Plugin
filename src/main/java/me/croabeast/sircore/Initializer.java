package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.listeners.login.*;
import me.croabeast.sircore.listeners.vanish.*;
import me.croabeast.sircore.utils.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.plugin.*;

public class Initializer {

    private final Application main;
    private Permission perms = null;

    public Initializer(Application main) {
        this.main = main;
        hasPAPI = main.plugin("PlaceholderAPI") != null;
        authMe = main.plugin("AuthMe") != null;
        userLogin = main.plugin("UserLogin") != null;
        hasCMI = main.plugin("CMI") != null;
        essentials = main.plugin("Essentials") != null;
    }

    private SavedFile config;
    private SavedFile lang;
    private SavedFile messages;

    public int events = 0;
    private int modules = 0;

    public boolean hasPAPI;
    public boolean hasVault;

    public boolean hasLogin;
    public boolean authMe;
    public boolean userLogin;

    public boolean hasVanish;
    public boolean hasCMI;
    public boolean essentials;

    public void savedFiles() {
        moduleHeader("Plugin Files");
        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        sendSectionsLog();
        main.logger("&6[SIR] &7Loaded 3 files in the plugin directory.");
    }

    public void setPluginHooks() {
        // PlaceholderAPI
        moduleHeader("PlaceholderAPI");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        moduleHeader("Permissions");
        main.logger("&6[SIR] &7Checking if Vault Permission System is integrated...");

        ServicesManager servMngr = main.getServer().getServicesManager();
        RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
        Plugin vaultPlugin = main.plugin("Vault");
        hasVault = vaultPlugin != null && rsp != null;

        if (!hasVault) {
            main.logger("&6[SIR] &7Vault&c isn't installed&7, using the default system.");
        } else {
            perms = rsp.getProvider();
            String vault = "Vault " + vaultPlugin.getDescription().getVersion();
            main.logger("&6[SIR] &7" + vault + "&a installed&7, hooking in a permission plugin...");
        }

        // Login hook
        String loginPlugin = "No login plugin enabled";
        int i = 0;
        if (authMe) {
            i++;
            loginPlugin = "AuthMe";
        }
        if (userLogin) {
            i++;
            loginPlugin = "UserLogin";
        }

        moduleHeader("Login Plugin Hook");
        main.logger("&6[SIR] &7Checking if a compatible login plugin is installed...");

        if (i > 1) {
            hasLogin = false;
            main.logger("&6[SIR] &cTwo or more compatible login plugins are installed.");
            main.logger("&6[SIR] &cPlease delete the extra ones and leave one of them.");
        } else if (i == 1) {
            hasLogin = true;
            showPluginInfo(loginPlugin);
        } else {
            hasLogin = false;
            main.logger("&6[SIR] &cThere is no login plugin installed. &7Unhooking...");
        }

        // Vanish hook
        String vanishPlugin = "No vanish plugin enabled";
        int x = 0;
        if (hasCMI) {
            x++;
            vanishPlugin = "CMI";
        }
        if (essentials) {
            x++;
            vanishPlugin = "Essentials";
        }

        moduleHeader("Vanish Plugin Hook");
        main.logger("&6[SIR] &7Checking if a compatible vanish plugin is installed...");

        if (x > 1) {
            hasVanish = false;
            main.logger("&6[SIR] &cTwo or more compatible vanish plugins are installed.");
            main.logger("&6[SIR] &cPlease delete the extra ones and leave one of them.");
        } else if (x == 1) {
            hasVanish = true;
            showPluginInfo(vanishPlugin);
        } else {
            hasVanish = false;
            main.logger("&6[SIR] &cThere is no vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerEvents() {
        moduleHeader("Events Registering");
        new PlayerListener(main);
        if (hasLogin) {
            new AuthMe(main);
            new UserLogin(main);
        }
        if (hasVanish) {
            new CMI(main);
            new Essentials(main);
        }
        main.logger("&6[SIR] &7Registered &e" + events + "&7 plugin events.");
    }

    private void showPluginInfo(String name) {
        boolean isPlugin = main.plugin(name) != null;

        String version = isPlugin ? main.plugin(name).getDescription().getVersion() + " " : "";
        String hook = isPlugin ? "&aenabled&7. Hooking..." : "&cnot found&7. Unhooking...";

        main.logger("&6[SIR] &7" + name + " " + version + hook);
    }

    private void moduleHeader(String moduleName) {
        modules++;
        main.logger("&6[SIR] ");
        main.logger("&6[SIR] &bModule " + modules + ": &3" + moduleName);
    }

    private void sendSectionsLog() {
        for (String id : main.getMessages().getKeys(false))
            main.logger("&6[SIR] &7Loaded &b" + main.sections(id) + "&7 groups in the &b" + id + "&7 section.");
    }

    public Permission getPerms() { return perms; }

    public SavedFile getConfig() { return config; }
    public SavedFile getLang() { return lang; }
    public SavedFile getMessages() { return messages; }
}
