package me.croabeast.sirplugin;

import me.croabeast.beanslib.terminals.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.modules.Announcer;
import me.croabeast.sirplugin.modules.listeners.*;
import me.croabeast.sirplugin.objects.analytics.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.tasks.*;
import me.croabeast.sirplugin.tasks.message.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;

import static me.croabeast.sirplugin.objects.extensions.Identifier.*;

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

        Initializer init = new Initializer(this);
        amender = new Amender(this);

        LogUtils.rawLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + pluginVersion, "",
                "&0* &7Developer: " + getDescription().getAuthors().get(0),
                "&0* &7Software: " + LangUtils.serverFork(),
                "&0* &7Java Version: " + System.getProperty("java.version"), ""
        );

        init.startMetrics();

        files.loadFiles(true);
        init.setPluginHooks();

        registerCommands(
                new MainCmd(this), new BroadCmd(), new PrintCmd(), new MsgCmd(),
                new ReplyCmd(), new IgnCmd()
        );

        SIRModule.registerModules(
                new EmParser(), new Announcer(this), new JoinQuit(this), new Advances(),
                new MOTD(this), new Formats(), new ChatFilter()
        );

        if (ANNOUNCES.isEnabled()) {
            ((Announcer) SIRModule.getModule(ANNOUNCES)).startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.doLog("",
                "&7SIR " + pluginVersion + " was&a loaded&7 in " +
                        (System.currentTimeMillis() - start) + " ms."
        );
        LogUtils.rawLog("");

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Initializer.loadAdvances(true);
            amender.initUpdater(null);
        });
    }

    @Override
    public void onDisable() {
        LogUtils.rawLog("" +
                        "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + pluginVersion, ""
        );

        Initializer.unloadAdvances(false);
        ((Announcer) SIRModule.getModule(ANNOUNCES)).cancelTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bar = Bossbar.getBossbar(player);
            if (bar != null) bar.removePlayer(player);
        }

        LogUtils.doLog(
                "&7The announcement task has been stopped.",
                "&7SIR &c" + pluginVersion + "&7 was totally disabled."
        );
        LogUtils.rawLog("");

        instance = null;
    }

    public static SIRPlugin getInstance() {
        return instance;
    }
    public static LangUtils textUtils() {
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
        for (SIRTask cmd : cmds) cmd.registerCommand();
    }
}
