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
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;

public final class Application extends JavaPlugin {

    private Recorder recorder;
    private Initializer init;

    private TextUtils text;
    private PermUtils perms;
    private EventUtils utils;

    private Reporter reporter;
    private Amender amender;

    private FilesUtils files;
    private Executor executor;

    public String PLUGIN_VERSION, MC_FORK;
    public int MC_VERSION;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        String version = Bukkit.getBukkitVersion().split("-")[0];

        MC_VERSION = Integer.parseInt(version.split("\\.")[1]);
        MC_FORK = Bukkit.getVersion().split("-")[1] + " " + version;
        PLUGIN_VERSION = getDescription().getVersion();

        recorder = new Recorder(this);
        init = new Initializer(this);

        files = new FilesUtils(this);
        text = new TextUtils(this);
        perms = new PermUtils(this);
        utils = new EventUtils(this);

        reporter = new Reporter(this);
        amender = new Amender(this);

        init.startMetrics(); // The bStats method for Metrics class
        executor = new Executor(this); // Register the main cmd for the plugin

        pluginHeader();
        recorder.rawRecord(
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + MC_FORK,
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        files.loadFiles(true);
        init.setPluginHooks();
        init.registerListeners();

        reporter.startTask();
        recorder.doRecord("&7The announcement task has been started.");
        init.loadAdvances(true);

        recorder.doRecord("",
                "&7SIR " + PLUGIN_VERSION + " was&a loaded&7 in " +
                (System.currentTimeMillis() - start) + " ms."
        );
        recorder.rawRecord("");

        new BukkitRunnable() {
            @Override
            public void run() {
                amender.initUpdater(null);
            }
        }.runTaskLater(this, 20);
    }

    @Override
    public void onDisable() {
        pluginHeader();

        utils.getLoggedPlayers().clear();
        init.unloadAdvances(false);
        reporter.cancelTask();

        recorder.doRecord(
                "&7The announcement task has been stopped.",
                "&7SIR &c" + PLUGIN_VERSION + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );
    }

    private void pluginHeader() {
        recorder.rawRecord("" +
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + PLUGIN_VERSION, ""
        );
    }

    public List<Player> everyPlayer() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    @NotNull
    public FileConfiguration getConfig() {
        return files.getFile("config");
    }
    public FileConfiguration getAdvances() {
        return files.getFile("advances");
    }
    public FileConfiguration getChat() {
        return files.getFile("chat");
    }
    public FileConfiguration getAnnounces() {
        return files.getFile("announces");
    }
    public FileConfiguration getLang() {
        return files.getFile("lang");
    }
    public FileConfiguration getMessages() {
        return files.getFile("messages");
    }
    public FileConfiguration getMOTD() {
        return files.getFile("motd");
    }
    public FileConfiguration getDiscord() {
        return files.getFile("discord");
    }

    public Recorder getRecorder() {
        return recorder;
    }
    public Initializer getInitializer() {
        return init;
    }

    public FilesUtils getFiles() {
        return files;
    }
    public TextUtils getTextUtils() {
        return text;
    }
    public PermUtils getPermUtils() {
        return perms;
    }
    public EventUtils getEventUtils() {
        return utils;
    }

    public Amender getAmender() {
        return amender;
    }
    public Reporter getReporter() {
        return reporter;
    }
    public Executor getExecutor() {
        return executor;
    }

    public Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }

    public void registerListener(Listener listener, boolean addListener) {
        getServer().getPluginManager().registerEvents(listener, this);
        if (addListener) init.LISTENERS++;
    }

    public void registerListener(Listener listener) {
        registerListener(listener, true);
    }
}
