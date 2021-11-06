package me.croabeast.sircore;

import me.croabeast.sircore.command.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

public final class Application extends JavaPlugin {

    private Application main;
    private Records records;
    private Initializer init;
    private TextUtils text;
    private EventUtils utils;
    private Announcer announcer;

    public String version;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        main = this; // The plugin instance initializing...

        version = getDescription().getVersion();
        String header = "&7SIR " + version + " was&a loaded&7 in ";

        records = new Records(main);
        init = new Initializer(main);
        text = new TextUtils(main);
        utils = new EventUtils(main);
        announcer = new Announcer(main);

        init.startMetrics(); // The bStats method for Metrics class
        new Executor(main); // Register the main cmd for the plugin

        records.rawRecord(
                "&e &e &e ____ &e &e ___ &e &e ____",
                "&e &e (___ &e &e &e &e | &e &e &e |___)",
                "&e &e ____) . _|_ . | &e &e \\ . &fv" + version,
                "&e &e &7Developer: " + getDescription().getAuthors().get(0),
                "&e &e &7Software: "+ text.serverName, ""
        );

        init.loadSavedFiles();
        init.setPluginHooks();
        init.registerListeners();
        announcer.startTask();
        records.doRecord(
                "&7The announcement task has been started."
        );

        records.doRecord(
                "",
                header + (System.currentTimeMillis() - start) + " ms."
        );
        records.rawRecord("");

        init.startUpdater();
    }

    @Override
    public void onDisable() {
        records.rawRecord(
                "&e   ____   ___   ____",
                "&e  (___     |    |___)",
                "&e  ____) . _|_ . |   \\ . &fv" + version, ""
        );
        announcer.cancelTask();
        records.doRecord(
                "&7The announcement task has been stopped."
        );
        records.doRecord(
                "&7SIR &c" + version + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );
        main = null; // This will prevent any memory leaks.
    }

    public FileConfiguration getAnnounces() {
        return init.announces.getFile();
    }
    public FileConfiguration getLang() {
        return init.lang.getFile();
    }
    public FileConfiguration getMessages() {
        return init.messages.getFile();
    }

    public Records getRecords() { return records; }
    public Initializer getInitializer() { return init; }
    public TextUtils getTextUtils() { return text; }
    public EventUtils getEventUtils() { return utils; }
    public Announcer getAnnouncer() { return announcer; }

    public Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }
}
