package me.croabeast.sircore;

import me.croabeast.sircore.command.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utils.*;
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

    public String version;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        main = this; // The plugin instance initializing...

        String author = getDescription().getAuthors().get(0);
        String header = "&7SIR " + version + " was&a loaded&7 in ";

        version = getDescription().getVersion();

        records = new Records(main);
        init = new Initializer(main);
        text = new TextUtils(main);
        utils = new EventUtils(main);

        init.startMetrics(); // The bStats method for Metrics.
        new Executor(main); // Register the main cmd for the plugin

        records.rawRecord(
                "&e   ____   ___   ____",
                "&e  (___     |    |___)",
                "&e  ____) . _|_ . |   \\ . &fv" + version,
                "  &7Developer: " + author,
                "  &7Software: "+ text.serverName, ""
        );

        init.loadSavedFiles();
        init.setPluginHooks();
        init.registerListeners();

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
        records.doRecord(
                "&7SIR &c" + version + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );
        main = null; // This will prevent any memory leaks.
    }

    public FileConfiguration getLang() { return init.lang.getFile(); }
    public FileConfiguration getMessages() { return init.messages.getFile(); }

    public Records getRecords() { return records; }
    public Initializer getInitializer() { return init; }
    public TextUtils getTextUtils() { return text; }
    public EventUtils getEventUtils() { return utils; }

    public Plugin getPlugin(String name) {
        if (name == null || name.equals("")) return main;
        return Bukkit.getPluginManager().getPlugin(name);
    }
}
