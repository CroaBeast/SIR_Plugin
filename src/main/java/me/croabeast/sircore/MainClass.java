package me.croabeast.sircore;

import me.croabeast.sircore.listeners.OnJoin;
import me.croabeast.sircore.listeners.login.AuthMe;
import me.croabeast.sircore.listeners.login.UserLogin;
import me.croabeast.sircore.listeners.OnQuit;
import me.croabeast.sircore.listeners.vanish.CMI;
import me.croabeast.sircore.listeners.vanish.Essentials;
import me.croabeast.sircore.utils.*;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MainClass extends JavaPlugin {

    private MainClass main;
    private LangUtils langUtils;
    private EventUtils eventUtils;

    private PluginFile config;
    private PluginFile lang;
    private PluginFile messages;

    private Permission perms = null;
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

    @Override
    public void onEnable() {
        main = this; // The plugin instance initializing...

        this.version = main.getDescription().getVersion();
        this.hasPAPI = pMngr.isPluginEnabled("PlaceholderAPI");
        this.authMe = pMngr.isPluginEnabled("AuthMe");
        this.userLogin = pMngr.isPluginEnabled("UserLogin");
        this.hasCMI = pMngr.isPluginEnabled("CMI");
        this.essentials = pMngr.isPluginEnabled("Essentials");

        langUtils = new LangUtils(main);
        eventUtils = new EventUtils(main);
        langUtils.loadLangClasses(); // Loading Title and Action Bar

        new Metrics(main, 12806); // The bStats class.
        new CmdUtils(main); // Register the main command for the plugin

        logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
        logger("&6[SIR] ");
        logger("&6[SIR] &7Checking &e"+ Bukkit.getVersion()+"&7...");
        logger("&6[SIR] &e" + langUtils.serverName + " &7detected.");

        // All files related module.
        moduleHeader(1, "Plugin Files");
        config = new PluginFile(main, "config");
        lang = new PluginFile(main, "lang");
        messages = new PluginFile(main, "messages");

        config.updateRegisteredFile();
        lang.updateRegisteredFile();
        messages.updateRegisteredFile();
        logger("&6[SIR] &7Loaded 3 files in the plugin directory.");

        // PlaceholderAPI module
        moduleHeader(2, "PlaceholderAPI");
        showPluginInfo("PlaceholderAPI");

        // Permission related module
        moduleHeader(3, "Permissions");
        logger("&6[SIR] &7Checking if Vault Permission System is integrated...");

        ServicesManager servMngr = getServer().getServicesManager();
        RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
        this.hasVault = pMngr.isPluginEnabled("Vault") && rsp != null;
        Plugin vaultPlugin = pMngr.getPlugin("Vault");

        if (vaultPlugin == null || rsp == null) {
            logger("&6[SIR] &7Vault&c isn't installed&7, using the default system.");
        } else {
            perms = rsp.getProvider();
            String vault = "Vault " + vaultPlugin.getDescription().getVersion();
            logger("&6[SIR] &7" + vault + "&a installed&7, hooking in a permission plugin...");
        }

        // Login plugin Hook module
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

        // Vanish plugin Hook module
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

        // Events loading module
        moduleHeader(6, "Events Registering");
        new OldMessages(main);
        new OnJoin(main);
        new OnQuit(main);
        if (hasLogin) {
            new AuthMe(main);
            new UserLogin(main);
        }
        if (hasVanish) {
            new CMI(main);
            new Essentials(main);
        }
        logger("&6[SIR] &7Registered &e" + events + "&7 plugin events.");

        logger("&6[SIR] ");
        logger("&6[SIR] &fSIR " + version + "&7 was&a loaded&7 successfully&7.");
        logger("&6[SIR] ");
        logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
    }

    public void onDisable() {
        main = null; // This will prevent any memory leaks.
        logger("&4[SIR] &7SIR &f" + version + "&7 was totally disabled &cu-u");
    }

    PluginManager pMngr = Bukkit.getPluginManager();

    public FileConfiguration getLang() { return lang.getFile(); }
    public FileConfiguration getMessages() { return messages.getFile(); }

    public LangUtils getLangUtils() { return langUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

    public Permission getPerms() { return perms; }

    public void logger(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private void showPluginInfo(String name) {
        Plugin plugin = pMngr.getPlugin(name);
        String version = plugin != null ? plugin.getDescription().getVersion() + " " : "";
        String hook = plugin != null ? "&aenabled&7. Hooking..." : "&cnot found&7. Unhooking...";
        logger("&6[SIR] &7" + name + " " + version + hook);
    }

    private void moduleHeader(int i, String moduleName) {
        logger("&6[SIR] ");
        logger("&6[SIR] &bModule " + i + ": &3" + moduleName);
    }

    public void reloadAllFiles() {
        config.reloadFile();
        lang.reloadFile();
        messages.reloadFile();
    }

    private static class OldMessages implements Listener {

        public OldMessages(MainClass main) {
            main.events++;
            main.getServer().getPluginManager().registerEvents(this, main);
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent e) { e.setJoinMessage(""); }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) { e.setQuitMessage(""); }
    }
}
