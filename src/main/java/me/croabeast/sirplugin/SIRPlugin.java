package me.croabeast.sirplugin;

import me.croabeast.beanslib.object.display.Bossbar;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.module.*;
import me.croabeast.sirplugin.module.Announcer;
import me.croabeast.sirplugin.module.listener.*;
import me.croabeast.sirplugin.object.analytic.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.task.*;
import me.croabeast.sirplugin.task.message.*;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.*;

import static me.croabeast.sirplugin.object.instance.Identifier.*;

public final class SIRPlugin extends JavaPlugin {

    private static SIRPlugin instance;

    private FilesUtils files;

    private Amender amender;

    private static LangUtils text;
    private static String pluginVersion;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;
        pluginVersion = getDescription().getVersion();

        text = new LangUtils(this);
        files = new FilesUtils(this);

        Initializer init = new Initializer();
        amender = new Amender(this);

        LogUtils.rawLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + pluginVersion, "",
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + LibUtils.serverFork(),
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        init.startMetrics();

        files.loadFiles(true);
        init.setPluginHooks();

        registerCommands(
                new MainCmd(), new BroadCmd(), new PrintCmd(), new MsgCmd(),
                new ReplyCmd(), new IgnCmd()
        );

        SIRModule.registerModules(
                new EmParser(), new Announcer(), new JoinQuit(), new Advances(),
                new MOTD(), new Formats(), new ChatFilter()
        );

        if (ANNOUNCES.isEnabled()) {
            ((Announcer) SIRModule.getModule(ANNOUNCES)).startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.doLog("",
                "&7SIR " + pluginVersion + " was&a loaded&7 in &e" +
                        (System.currentTimeMillis() - start) + " ms."
        );
        LogUtils.rawLog("");

        if (!Bukkit.getOnlinePlayers().isEmpty() && Initializer.hasLogin())
            Bukkit.getOnlinePlayers().stream().filter(p ->
                    !JoinQuit.LOGGED_PLAYERS.contains(p)).
                    forEach(JoinQuit.LOGGED_PLAYERS::add);

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Initializer.loadAdvances(true);
            amender.initUpdater(null);
        });
    }

    @Override
    public void onDisable() {
        LogUtils.rawLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + pluginVersion, ""
        );

        Initializer.unloadAdvances(false);
        ((Announcer) SIRModule.getModule(ANNOUNCES)).cancelTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Bossbar bar = Bossbar.getBossbar(player);
            if (bar != null) bar.unregister();
        }

        LogUtils.doLog(
                "&7The announcement task has been stopped.",
                "&7SIR &c" + pluginVersion + "&7 was totally disabled."
        );
        LogUtils.rawLog("");

        HandlerList.unregisterAll(this);
        instance = null;
    }

    public static SIRPlugin getInstance() {
        return instance;
    }
    public static LangUtils getUtils() {
        return text;
    }

    public static String pluginVersion() {
        return pluginVersion;
    }

    public Amender getAmender() {
        return amender;
    }
    public FilesUtils getFiles() {
        return files;
    }

    private void registerCommands(SIRTask... cmds) {
        for (SIRTask cmd : cmds) {
            try {
                cmd.registerCommand();
            } catch (NullPointerException e) {
                LogUtils.doLog("&c" + e.getMessage());
            }
        }
    }
}
