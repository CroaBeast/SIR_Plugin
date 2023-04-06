package me.croabeast.sirplugin;

import lombok.Getter;
import lombok.var;
import me.croabeast.beanslib.builder.BossbarBuilder;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.module.Announcer;
import me.croabeast.sirplugin.module.listener.JoinQuit;
import me.croabeast.sirplugin.object.analytic.Amender;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.object.instance.SIRModule;
import me.croabeast.sirplugin.object.instance.SIRTask;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class SIRPlugin extends JavaPlugin {

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static LangUtils utils;

    @Getter
    private static String version, author;

    @Override
    public void onEnable() {
        var start = System.currentTimeMillis();

        instance = this;
        author = getDescription().getAuthors().get(0);
        version = getDescription().getVersion();

        utils = new LangUtils(this);

        FileCache.loadFiles();

        LogUtils.rawLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + version, "",
                "&0* &7Developer: " + author,
                "&0* &7Software: " + LibUtils.serverFork(),
                "&0* &7Java Version: " + SystemUtils.JAVA_VERSION, ""
        );

        Initializer.startMetrics();
        Initializer.setPluginHooks();

        SIRTask.registerCommands();
        SIRModule.registerModules();

        if (SIRModule.isEnabled("announces")) {
            ((Announcer) SIRModule.get("announces")).startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.mixLog("",
                "&7SIR " + version + " was&a loaded&7 in &e" +
                (System.currentTimeMillis() - start) + " ms.",
                "true::"
        );

        if (LoginHook.isEnabled())
            JoinQuit.LOGGED_PLAYERS.addAll(Bukkit.getOnlinePlayers());

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Initializer.loadAdvances(true);
            Amender.initUpdater(null);
        });
    }

    @Override
    public void onDisable() {
        LogUtils.rawLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &fv" + version, ""
        );

        Initializer.unloadAdvances(false);
        ((Announcer) SIRModule.get("announces")).cancelTask();

        for (var player : Bukkit.getOnlinePlayers()) {
            var bar = BossbarBuilder.getBuilder(player);
            if (bar != null) bar.unregister();
        }

        LogUtils.mixLog(
                "&7The announcement task has been stopped.", "true::",
                "&7SIR &c" + version + "&7 was totally disabled.", "true::"
        );

        VanishHook.unloadHook();
        LoginHook.unloadHook();

        HandlerList.unregisterAll(this);

        utils = null;
        instance = null;
    }
}
