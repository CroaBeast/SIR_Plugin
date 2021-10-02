package me.croabeast.sircore;

import me.croabeast.sircore.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.Plugin;
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

        main.logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
        main.logger("&6[SIR] ");
        main.logger("&6[SIR] &7Checking &e"+ Bukkit.getVersion()+"&7...");
        main.logger("&6[SIR] &e" + textUtils.serverName + " &7detected.");

        initializer.setBooleans();

        initializer.savedFiles();
        initializer.setPluginHooks();
        initializer.registerEvents();

        logger("&6[SIR] ");
        logger("&6[SIR] &fSIR " + version + "&7 was&a loaded&7 successfully&7.");
        logger("&6[SIR] ");
        logger(" &7---- > Simple In-game Receptionist by CroaBeast < ---- ");
    }

    @Override
    public void onDisable() {
        main = null; // This will prevent any memory leaks.
        logger("&4[SIR] &7SIR &f" + version + "&7 was totally disabled.");
        logger("&4[SIR] &7Hope we can see you again&c nwn");
    }

    public FileConfiguration getLang() { return initializer.getLang().getFile(); }
    public FileConfiguration getMessages() { return initializer.getMessages().getFile(); }

    public Initializer getInitializer() { return initializer; }
    public TextUtils getTextUtils() { return textUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

    public void logger(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getConsoleSender().sendMessage(msg);
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
