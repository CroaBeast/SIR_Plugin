package me.croabeast.sircore;

import me.croabeast.sircore.others.*;
import me.croabeast.sircore.utils.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

public final class Application extends JavaPlugin {

    private Application main;
    private Initializer initializer;
    private TextUtils textUtils;
    private EventUtils eventUtils;

    private String version;

    @Override
    public void onEnable() {
        main = this; // The plugin instance initializing...

        initializer = new Initializer(main);
        textUtils = new TextUtils(main);
        eventUtils = new EventUtils(main);
        textUtils.loadLangClasses(); // Loading Title and Action Bar

        new Metrics(main, 12806); // The bStats class.
        new CmdUtils(main); // Register the main command for the plugin

        version = main.getDescription().getVersion();

        sendLimiter();
        logger("");
        logger("&7Server Software: &e"+ textUtils.serverName);

        initializer.savedFiles();
        initializer.setPluginHooks();
        initializer.registerEvents();

        logger("");
        logger("&7SIR " + version + " was&a loaded successfully&7.");
        logger("");
        sendLimiter();
    }

    @Override
    public void onDisable() {
        logger("&7SIR &c" + version + "&7 was totally disabled.");
        logger("&7Hope we can see you again&c nwn");
        main = null; // This will prevent any memory leaks.
    }

    public FileConfiguration getLang() { return initializer.getLang().getFile(); }
    public FileConfiguration getMessages() { return initializer.getMessages().getFile(); }

    public Initializer getInitializer() { return initializer; }
    public TextUtils getTextUtils() { return textUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void logger(Player player, String msg) {
        getLogger().info(color(msg));
        if (player != null) player.sendMessage(color("&6[SIR] " + msg));
    }

    public void logger(String msg) { logger(null, msg); }

    private void sendLimiter() {
        String limiter = " &7---- > Simple In-game Receptionist by CroaBeast < ---- ";
        Bukkit.getConsoleSender().sendMessage(color(limiter));
    }

    public void reloadFiles() {
        initializer.getConfig().reloadFile();
        initializer.getLang().reloadFile();
        initializer.getMessages().reloadFile();
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

    public boolean choice(String key) {
        switch (key) {
            case "console":
                return main.getConfig().getBoolean("options.send-console");
            case "after":
                return main.getConfig().getBoolean("login.send-after");
            case "lSpawn":
                return main.getConfig().getBoolean("login.spawn-before");
            case "trigger":
                return main.getConfig().getBoolean("vanish.trigger");
            case "silent":
                return main.getConfig().getBoolean("vanish.silent");
            case "vSpawn":
                return main.getConfig().getBoolean("vanish.do-spawn");
            default:
                return false;
        }
    }
}
