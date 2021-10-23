package me.croabeast.sircore;

import me.croabeast.sircore.command.*;
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

    public String version;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        main = this; // The plugin instance initializing...
        version = getDescription().getVersion();

        initializer = new Initializer(main);
        textUtils = new TextUtils(main);
        eventUtils = new EventUtils(main);

        initializer.startMetrics(); // The bStats method for Metrics.
        new Executor(main); // Register the main command for the plugin

        rawLogger(
                "&e   ____   ___   ____",
                "&e  (___     |    |___)",
                "&e  ____) . _|_ . |   \\ . &fv" + version,
                "  &7Developer: " + getDescription().getAuthors().get(0),
                "  &7Software: "+ textUtils.serverName, ""
        );

        initializer.savedFiles();
        initializer.setPluginHooks();
        initializer.registerListeners();

        doLogger("",
                "&7SIR " + version + " was&a loaded successfully&7 in " +
                        (System.currentTimeMillis() - start) + " ms.");
        rawLogger("");

        initializer.startUpdater();
    }

    @Override
    public void onDisable() {
        rawLogger(
                "&e   ____   ___   ____",
                "&e  (___     |    |___)",
                "&e  ____) . _|_ . |   \\ . &fv" + version, ""
        );
        doLogger("&7SIR &c" + version + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn");
        main = null; // This will prevent any memory leaks.
    }

    public FileConfiguration getLang() { return initializer.lang.getFile(); }
    public FileConfiguration getMessages() { return initializer.messages.getFile(); }

    public Initializer getInitializer() { return initializer; }
    public TextUtils getTextUtils() { return textUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void rawLogger(String... lines) {
        for (String s : lines) getServer().getLogger().info(color(s));
    }

    public void playerLogger(Player player, String... lines) {
        for (String line : lines) player.sendMessage(color(line));
    }

    public void doLogger(Player player, String... lines) {
        for (String msg : lines) {
            getLogger().info(color(msg));
            if (player != null) playerLogger(player, "&6[SIR] " + msg);
        }
    }

    public void doLogger(String... lines) { doLogger(null, lines); }

    public void reloadFiles() {
        initializer.config.reloadFile();
        initializer.lang.reloadFile();
        initializer.messages.reloadFile();
    }

    public Plugin plugin(String name) { return Bukkit.getPluginManager().getPlugin(name); }

    public int sections(String path) {
        int messages = 0;
        ConfigurationSection ids = getMessages().getConfigurationSection(path);
        if (ids == null) return 0;

        for (String key : ids.getKeys(false)) {
            ConfigurationSection id = ids.getConfigurationSection(key);
            if (id != null) messages++;
        }
        return messages;
    }

    public boolean choice(String key) {
        switch (key) {
            case "console": return getConfig().getBoolean("options.send-console");
            case "format": return getConfig().getBoolean("options.format-logger");
            case "after": return getConfig().getBoolean("login.send-after");
            case "login": return getConfig().getBoolean("login.spawn-before");
            case "trigger": return getConfig().getBoolean("vanish.trigger");
            case "silent": return getConfig().getBoolean("vanish.silent");
            case "vanish": return getConfig().getBoolean("vanish.do-spawn");
            case "logger" : return getConfig().getBoolean("updater.plugin.on-enabling");
            case "toOp" : return getConfig().getBoolean("updater.plugin.send-to-op");
            default: return false;
        }
    }
}
