package me.croabeast.sirplugin;

import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.modules.extensions.*;
import me.croabeast.sirplugin.modules.extensions.listeners.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.objects.analytics.*;
import me.croabeast.sirplugin.tasks.extensions.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.*;

public final class SIRPlugin extends JavaPlugin {

    private static SIRPlugin instance;

    private Initializer init;

    private FilesUtils files;
    private EventUtils utils;

    private Amender amender;

    private static final String MC_VERSION = Bukkit.getBukkitVersion().split("-")[0];
    public static String PLUGIN_VERSION;

    public static final String MC_FORK = Bukkit.getVersion().split("-")[1] + " " + MC_VERSION;
    public static final int MAJOR_VERSION = Integer.parseInt(MC_VERSION.split("\\.")[1]);

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;
        PLUGIN_VERSION = instance.getDescription().getVersion();

        this.init = new Initializer(this);
        this.files = new FilesUtils(this);

        TextUtils.initializeClass();
        this.utils = new EventUtils(this);
        this.amender = new Amender(this);

        pluginHeader();
        LogUtils.rawLog(
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + MC_FORK,
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        init.startMetrics();

        files.loadFiles(true);
        init.setPluginHooks();

        init.registerCommands(
                new MainCmd(this), new Announcer(this), new PrintCmd(),
                new MsgCmd(this), new ReplyCmd(this), new IgnoreCmd(this)
        );

        init.registerModules(
                new EmParser(this), new Reporter(this), new JoinQuit(this),
                new Advances(this), new ServerList(this), new Formatter(this),
                new ChatFilter(this)
        );

        if (getReporter().isEnabled()) getReporter().startTask();
        LogUtils.doLog("&7The announcement task has been started.");

        LogUtils.doLog("",
                "&7SIR " + PLUGIN_VERSION + " was&a loaded&7 in " +
                        (System.currentTimeMillis() - start) + " ms."
        );
        LogUtils.rawLog("");

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            init.loadAdvances(true);
            amender.initUpdater(null);
        });
    }

    @Override
    public void onDisable() {
        pluginHeader();

        init.unloadAdvances(false);
        getReporter().cancelTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bar = Bossbar.getBossbar(player);
            if (bar != null) bar.removePlayer(player);
        }

        LogUtils.doLog(
                "&7The announcement task has been stopped.",
                "&7SIR &c" + PLUGIN_VERSION + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );

        instance = null;
    }

    private void pluginHeader() {
        LogUtils.rawLog("" +
                        "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + PLUGIN_VERSION, ""
        );
    }

    public static SIRPlugin getInstance() {
        return instance;
    }

    public Initializer getInitializer() {
        return init;
    }
    public Amender getAmender() {
        return amender;
    }

    public FilesUtils getFiles() {
        return files;
    }
    public EventUtils getEventUtils() {
        return utils;
    }

    public Reporter getReporter() {
        return (Reporter) Initializer.getModules().get(BaseModule.Identifier.ANNOUNCES);
    }
    public EmParser getEmParser() {
        return (EmParser) Initializer.getModules().get(BaseModule.Identifier.EMOJIS);
    }

    @NotNull
    public FileConfiguration getConfig() {
        return files.getFile("config");
    }
    public FileConfiguration getLang() {
        return files.getFile("lang");
    }
    public FileConfiguration getModules() {
        return files.getFile("modules");
    }

    public FileConfiguration getIgnore() {
        return files.getFile("ignore");
    }

    public FileConfiguration getAnnounces() {
        return files.getFile("announces");
    }
    public FileConfiguration getAdvances() {
        return files.getFile("advances");
    }
    public FileConfiguration getJoinQuit() {
        return files.getFile("join-quit");
    }

    public FileConfiguration getFormats() {
        return files.getFile("formats");
    }
    public FileConfiguration getEmojis() {
        return files.getFile("emojis");
    }
    public FileConfiguration getFilters() {
        return files.getFile("filters");
    }

    public FileConfiguration getDiscord() {
        return files.getFile("discord");
    }
    public FileConfiguration getMOTD() {
        return files.getFile("motd");
    }

    public static void registerListener(Listener... listeners) {
        for (Listener listener : listeners) if (listener != null)
            Bukkit.getPluginManager().registerEvents(listener, instance);
    }
}
