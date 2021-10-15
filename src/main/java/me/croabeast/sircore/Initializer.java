package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.others.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.plugin.*;

public class Initializer {

    private final Application main;
    private Permission perms = null;

    private SavedFile config;
    private SavedFile lang;
    private SavedFile messages;

    public int events = 0;
    public int files = 0;
    private int modules = 0;

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
        moduleHeader("Plugin Files");
        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        for (String id : main.getMessages().getKeys(false)) {
            main.logger("&7Loaded &e" + main.sections(id) +
                    "&7 groups in the &e" + id + "&7 section.");
        }
        main.logger("&7Loaded &e" + files + "&7 files in plugin's folder.");
    }

    public void setPluginHooks() {
        // PlaceholderAPI
        moduleHeader("PlaceholderAPI");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        moduleHeader("Permissions");
        main.logger("&7Checking if Vault is enabled...");

        if (!hasVault) main.logger("&7Vault&c isn't installed&7, using default system.");
        else {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
            if (rsp != null) {
                perms = rsp.getProvider();
                main.logger("&7Vault&a installed&7, hooking in a perm plugin...");
            }
            else main.logger("&7Unknown perm provider&7, using default system.");
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

        moduleHeader("Login Plugin Hook");
        main.logger("&7Checking if a login plugin is enabled...");

        if (i == 1) {
            hasLogin = true;
            showPluginInfo(loginPlugin);
        } else {
            hasLogin = false;
            if (i > 1) {
                main.logger("&cTwo or more compatible login plugins are installed.");
                main.logger("&cPlease leave one of them installed.");
            }
            else main.logger("&cThere is no login plugin installed. &7Unhooking...");
        }

        // Vanish hook
        String vanishPlugin = "";
        int x = 0;
        if (hasCMI) {
            x++;
            vanishPlugin = "CMI";
        }
        if (essentials) {
            x++;
            vanishPlugin = "Essentials";
        }
        if (superVanish) {
            x++;
            vanishPlugin = "SuperVanish";
        }
        if (prVanish) {
            x++;
            vanishPlugin = "PremiumVanish";
        }

        moduleHeader("Vanish Plugin Hook");
        main.logger("&7Checking if a vanish plugin is enabled...");

        if (x == 1) {
            hasVanish = true;
            showPluginInfo(vanishPlugin);
        } else {
            hasVanish = false;
            if (x > 1) {
                main.logger("&cTwo or more compatible vanish plugins are installed.");
                main.logger("&cPlease leave one of them installed.");
            }
            else main.logger("&cThere is no vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerEvents() {
        moduleHeader("Events Registering");
        new PlayerListener(main);
        new LoginListener(main);
        new VanishListener(main);
        main.logger("&7Registered &e" + events + "&7 plugin events.");
    }

    private void showPluginInfo(String name) {
        boolean isPlugin = main.plugin(name) != null;
        main.logger("&7" + name + " " +
                (isPlugin ? main.plugin(name).getDescription().getVersion() + " " : "") +
                (isPlugin ? "&aenabled&7. Hooking..." : "&cnot found&7. Unhooking..."));
    }

    private void moduleHeader(String moduleName) {
        modules++;
        main.logger("");
        main.logger("&bModule " + modules + ": &3" + moduleName);
    }

    public Permission getPerms() { return perms; }

    public SavedFile getConfig() { return config; }
    public SavedFile getLang() { return lang; }
    public SavedFile getMessages() { return messages; }
}
