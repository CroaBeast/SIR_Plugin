package me.croabeast.sirplugin;

import me.croabeast.beanslib.terminals.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.modules.listeners.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.objects.analytics.*;
import me.croabeast.sirplugin.tasks.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.plugin.java.*;

public final class SIRPlugin extends JavaPlugin {

    private static SIRPlugin instance;

    private Initializer init;

    private FilesUtils files;
    private EventUtils utils;

    private Amender amender;

    private static TextUtils text;

    private static final String MC_VERSION = Bukkit.getBukkitVersion().split("-")[0];
    private static String pluginVersion;

    public static final String MC_FORK = Bukkit.getVersion().split("-")[1] + " " + MC_VERSION;
    public static final int MAJOR_VERSION = Integer.parseInt(MC_VERSION.split("\\.")[1]);

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;
        pluginVersion = getDescription().getVersion();

        text = new TextUtils(this);
        files = new FilesUtils(this);
        utils = new EventUtils(this);

        init = new Initializer(this);
        amender = new Amender(this);

        pluginHeader();
        LogUtils.rawLog(
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + MC_FORK, // haha empty spaces goes brr
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        init.startMetrics();

        files.loadFiles(true);
        init.setPluginHooks();

        registerCommands(
                new MainCmd(this), new Announcer(this), new PrintCmd(),
                new MsgCmd(), new ReplyCmd(), new IgnoreCmd()
        );

        init.registerModules(
                new EmParser(), new Reporter(this), new JoinQuit(this),
                new Advances(this), new ServerList(this), new Formatter(this),
                new ChatFilter(this)
        );

        if (getReporter().isEnabled()) getReporter().startTask();
        LogUtils.doLog("&7The announcement task has been started.");

        LogUtils.doLog("",
                "&7SIR " + pluginVersion + " was&a loaded&7 in " +
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
                "&7SIR &c" + pluginVersion + "&7 was totally disabled.",
                "&7Hope we can see you again&c nwn"
        );

        instance = null;
    }

    private void pluginHeader() {
        LogUtils.rawLog("" +
                        "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + pluginVersion, ""
        );
    }

    public static SIRPlugin getInstance() {
        return instance;
    }
    public static TextUtils textUtils() {
        return text;
    }
    public static String pluginVersion() {
        return pluginVersion;
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
        return (Reporter) Initializer.getModules().get(Module.Identifier.ANNOUNCES);
    }
    public EmParser getEmParser() {
        return (EmParser) Initializer.getModules().get(Module.Identifier.EMOJIS);
    }

    public static void registerListener(Listener... listeners) {
        for (Listener listener : listeners) if (listener != null)
            Bukkit.getPluginManager().registerEvents(listener, instance);
    }

    public void registerCommands(BaseCmd... cmds) {
        for (BaseCmd cmd : cmds) cmd.registerCommand();
    }
}
