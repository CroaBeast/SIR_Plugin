package me.croabeast.sircore;

import me.croabeast.sircore.listeners.PlayerListener;
import me.croabeast.sircore.listeners.login.AuthMe;
import me.croabeast.sircore.listeners.login.UserLogin;
import me.croabeast.sircore.listeners.vanish.CMI;
import me.croabeast.sircore.listeners.vanish.Essentials;
import me.croabeast.sircore.utils.SavedFile;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

public class MainCore {

    private final SIRPlugin main;
    private Permission perms = null;

    public MainCore(SIRPlugin main) {
        this.main = main;
    }

    private SavedFile config;
    private SavedFile lang;
    private SavedFile messages;

    private String version;

    public int events = 0;

    public boolean hasPAPI;
    public boolean hasVault;

    public boolean hasLogin;
    public boolean authMe;
    public boolean userLogin;

    public boolean hasVanish;
    public boolean hasCMI;
    public boolean essentials;

    public void setBooleans() {
        version = main.getDescription().getVersion();
        hasPAPI = plugin("PlaceholderAPI") != null;
        authMe = plugin("AuthMe") != null;
        userLogin = plugin("UserLogin") != null;
        hasCMI = plugin("CMI") != null;
        essentials = plugin("Essentials") != null;
    }

    public void enablingHeader(String server) {
        logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
        logger("&6[SIR] ");
        logger("&6[SIR] &7Checking &e"+ Bukkit.getVersion()+"&7...");
        logger("&6[SIR] &e" + server + " &7detected.");
    }

    public void setSavedFiles() {
        moduleHeader(1, "Plugin Files");
        config = new SavedFile(main, "config");
        lang = new SavedFile(main, "lang");
        messages = new SavedFile(main, "messages");

        config.updateInitFile();
        lang.updateInitFile();
        messages.updateInitFile();
        sendSectionsLog("first-join", "join", "quit");
        logger("&6[SIR] &7Loaded 3 files in the plugin directory.");
    }

    public void setPAPI() {
        moduleHeader(2, "PlaceholderAPI");
        showPluginInfo("PlaceholderAPI");
    }

    public void setPermissions() {
        moduleHeader(3, "Permissions");
        logger("&6[SIR] &7Checking if Vault Permission System is integrated...");

        ServicesManager servMngr = main.getServer().getServicesManager();
        RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
        Plugin vaultPlugin = plugin("Vault");
        hasVault = vaultPlugin != null && rsp != null;

        if (!hasVault) {
            logger("&6[SIR] &7Vault&c isn't installed&7, using the default system.");
        } else {
            perms = rsp.getProvider();
            String vault = "Vault " + vaultPlugin.getDescription().getVersion();
            logger("&6[SIR] &7" + vault + "&a installed&7, hooking in a permission plugin...");
        }
    }

    public void setLoginHook() {
        int i = 0; String loginPlugin = "No login plugin enabled";
        if (authMe) { i++; loginPlugin = "AuthMe"; }
        if (userLogin) { i++; loginPlugin = "UserLogin"; }

        moduleHeader(4, "Login Plugin Hook");
        logger("&6[SIR] &7Checking if a compatible login plugin is installed...");

        if (i > 1) { hasLogin = false;
            logger("&6[SIR] &cTwo or more compatible login plugins are installed.");
            logger("&6[SIR] &cPlease delete the extra ones and leave one of them.");
        } else if (i == 1) { hasLogin = true;
            showPluginInfo(loginPlugin);
        } else { hasLogin = false;
            logger("&6[SIR] &cThere is no login plugin installed. &7Unhooking...");
        }
    }

    public void setVanishHook() {
        int x = 0; String vanishPlugin = "No vanish plugin enabled";
        if (hasCMI) { x++; vanishPlugin = "CMI"; }
        if (essentials) { x++; vanishPlugin = "Essentials"; }

        moduleHeader(5, "Vanish Plugin Hook");
        logger("&6[SIR] &7Checking if a compatible vanish plugin is installed...");

        if (x > 1) { hasVanish = false;
            logger("&6[SIR] &cTwo or more compatible vanish plugins are installed.");
            logger("&6[SIR] &cPlease delete the extra ones and leave one of them.");
        } else if (x == 1) { hasVanish = true;
            showPluginInfo(vanishPlugin);
        } else { hasVanish = false;
            logger("&6[SIR] &cThere is no vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerEvents() {
        moduleHeader(6, "Events Registering");
        new PlayerListener(main);
        if (hasLogin) {
            new AuthMe(main);
            new UserLogin(main);
        }
        if (hasVanish) {
            new CMI(main);
            new Essentials(main);
        }
        logger("&6[SIR] &7Registered &e" + events + "&7 plugin events.");
    }

    public void enablingFooter() {
        logger("&6[SIR] ");
        logger("&6[SIR] &fSIR " + version + "&7 was&a loaded&7 successfully&7.");
        logger("&6[SIR] ");
        logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
    }

    public void disableMessage() {
        logger("&4[SIR] &7SIR &f" + version + "&7 was totally disabled.");
        logger("&4[SIR] &7Hope we can see you again&c nwn");
    }

    public void logger(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private void showPluginInfo(String name) {
        String version = plugin(name) != null ? plugin(name).getDescription().getVersion() + " " : "";
        String hook = plugin(name) != null ? "&aenabled&7. Hooking..." : "&cnot found&7. Unhooking...";
        logger("&6[SIR] &7" + name + " " + version + hook);
    }

    private void moduleHeader(int i, String moduleName) {
        logger("&6[SIR] ");
        logger("&6[SIR] &bModule " + i + ": &3" + moduleName);
    }

    public Plugin plugin(String name) { return Bukkit.getPluginManager().getPlugin(name); }

    public int sections(String path) {
        int messages = 0;
        ConfigurationSection ids = main.getMessages().getConfigurationSection(path);
        if (ids == null) return 0;

        for (String key : ids.getKeys(false)) {
            ConfigurationSection id = ids.getConfigurationSection(key);
            if (id != null) messages++;
        }
        return messages;
    }

    private void sendSectionsLog(String... sections) {
        for (String id : sections)
            logger("&6[SIR] &7Loaded &b" + sections(id) + "&7 groups in the &b" + id + "&7 section.");
    }

    public Permission getPerms() { return perms; }
    public SavedFile getConfig() { return config; }
    public SavedFile getLang() { return lang; }
    public SavedFile getMessages() { return messages; }
}
