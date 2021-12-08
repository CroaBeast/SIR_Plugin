package me.croabeast.sircore;

import me.croabeast.sircore.command.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;

import java.util.*;

public final class Application extends JavaPlugin {

    private Application main;
    private Records records;
    private Initializer init;
    private TextUtils text;
    private PermUtils perms;
    private EventUtils utils;
    private Announcer announcer;
    private DoUpdate doUpdate;

    public String PLUGIN_VERSION;
    public String MC_FORK;
    public int MC_VERSION;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        main = this; // The plugin instance initializing...

        String version = Bukkit.getBukkitVersion().split("-")[0];

        MC_VERSION = Integer.parseInt(version.split("\\.")[1]);
        MC_FORK = Bukkit.getVersion().split("-")[1] + " " + version;
        PLUGIN_VERSION = getDescription().getVersion();

        records = new Records(main);
        init = new Initializer(main);
        text = new TextUtils(main);
        perms = new PermUtils(main);
        utils = new EventUtils(main);
        announcer = new Announcer(main);
        doUpdate = new DoUpdate(main);

        init.startMetrics(); // The bStats method for Metrics class
        new Executor(main); // Register the main cmd for the plugin

        records.rawRecord("" +
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + PLUGIN_VERSION,
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + MC_FORK,
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        init.loadSavedFiles();
        init.setPluginHooks();
        init.registerListeners();
        announcer.startTask();
        records.doRecord("&7The announcement task has been started.");
        init.checkFeatures(null);

        records.doRecord("",
                "&7SIR " + PLUGIN_VERSION + " was&a loaded&7 in " +
                        (System.currentTimeMillis() - start) + " ms."
        );
        records.rawRecord("");

        doUpdate.initUpdater(null);
    }

    @Override
    public void onDisable() {
        records.rawRecord("" +
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + PLUGIN_VERSION, ""
        );
        announcer.cancelTask();
        records.doRecord(
                "&7The announcement task has been stopped.",
                "&7SIR &c" + PLUGIN_VERSION + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );
        main = null; // This will prevent any memory leaks.
    }

    public List<Player> everyPlayer() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    public FileConfiguration getChat() { return init.chat.getFile(); }
    public FileConfiguration getAnnounces() { return init.announces.getFile(); }
    public FileConfiguration getLang() { return init.lang.getFile(); }
    public FileConfiguration getMessages() { return init.messages.getFile(); }
    public FileConfiguration getMOTD() { return init.motd.getFile(); }

    public Records getRecords() { return records; }
    public Initializer getInitializer() { return init; }
    public TextUtils getTextUtils() { return text; }
    public PermUtils getPermUtils() { return perms; }
    public EventUtils getEventUtils() { return utils; }
    public DoUpdate getDoUpdate() { return doUpdate; }
    public Announcer getAnnouncer() { return announcer; }

    public Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }

    public void registerListener(Listener listener, boolean addListener) {
        main.getServer().getPluginManager().registerEvents(listener, main);
        if (addListener) init.LISTENERS++;
    }

    public void registerListener(Listener listener) {
        registerListener(listener, true);
    }
}
