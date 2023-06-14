package me.croabeast.sirplugin;

import lombok.Getter;
import lombok.var;
import me.croabeast.beanslib.analytic.UpdateChecker;
import me.croabeast.beanslib.builder.BossbarBuilder;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.instance.SIRModule;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.module.AnnounceViewer;
import me.croabeast.sirplugin.module.listener.JoinQuitHandler;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.jar.JarFile;

public final class SIRPlugin extends JavaPlugin {

    private static final String EMPTY_LINE = "true::";

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
            ((AnnounceViewer) SIRModule.get("announces")).startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.mixLog(EMPTY_LINE,
                "&7SIR " + version + " was&a loaded&7 in &e" +
                (System.currentTimeMillis() - start) + " ms.",
                EMPTY_LINE
        );

        if (LoginHook.isEnabled())
            JoinQuitHandler.LOGGED_PLAYERS.addAll(Bukkit.getOnlinePlayers());

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Initializer.loadAdvances(true);
            checkUpdater(null);
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
        ((AnnounceViewer) SIRModule.get("announces")).cancelTask();

        Bukkit.getOnlinePlayers().
                stream().map(BossbarBuilder::getBuilder).
                filter(Objects::nonNull).
                forEach(BossbarBuilder::unregister);

        LogUtils.mixLog(
                "&7The announcement task has been stopped.", EMPTY_LINE,
                "&7SIR &c" + version + "&7 was totally disabled.", EMPTY_LINE
        );

        VanishHook.unloadHook();
        LoginHook.unloadHook();

        HandlerList.unregisterAll(this);

        utils = null;
        instance = null;
    }

    private static void updaterLog(Player player, String... strings) {
        if (player != null) {
            String[] array = new String[strings.length];

            for (int i = 0; i < strings.length; i++) {
                var s = strings[i];
                array[i] = s.equals(EMPTY_LINE) ? " " : s;
            }

            LogUtils.playerLog(player, array);
            return;
        }

        LogUtils.mixLog(strings);
    }

    private static void runUpdater(@Nullable Player player) {
        UpdateChecker.of(instance, 96378).requestUpdateCheck().whenComplete((result, e) -> {
            String latest = result.getNewestVersion();

            switch (result.getReason()) {
                case NEW_UPDATE:
                    updaterLog(player,
                            EMPTY_LINE, "&4NEW UPDATE!",
                            "&cYou don't have the latest version of S.I.R. installed.",
                            "&cRemember, older versions won't receive any support.",
                            "&7New Version: &a" + latest +
                                    "&7 - Your Version: &e" + version,
                            "&7Link:&b https://www.spigotmc.org/resources/96378/",
                            EMPTY_LINE
                    );
                    break;

                case UP_TO_DATE:
                    updaterLog(player,
                            EMPTY_LINE,
                            "&eYou have the latest version of S.I.R. &7(" + latest + ")",
                            "&7I would appreciate if you keep updating &c<3",
                            EMPTY_LINE
                    );
                    break;

                case UNRELEASED_VERSION:
                    updaterLog(player,
                            EMPTY_LINE, "&4DEVELOPMENT BUILD:",
                            "&cYou have a newer version of S.I.R. installed.",
                            "&cErrors might occur in this build.",
                            "Spigot Version: &a" + result.getSpigotVersion() +
                                    "&7 - Your Version: &e" + version,
                            EMPTY_LINE
                    );
                    break;

                default:
                    updaterLog(player,
                            EMPTY_LINE, "&4WARNING!",
                            "&cCould not check for a new version of S.I.R.",
                            "&7Please check your connection and restart the server.",
                            "&7Possible reason: &e" + result.getReason(),
                            EMPTY_LINE
                    );
                    break;
            }
        });
    }

    public static void checkUpdater(@Nullable Player player) {
        if (player == null) {
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.on-start", false)) return;
            runUpdater(null);
        }
        else {
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.send-op", false) ||
                    !PlayerUtils.hasPerm(player, "sir.admin.updater")) return;
            runUpdater(player);
        }
    }

    public static String getSIRFilePath() {
        return SIRPlugin.class.getProtectionDomain().
                getCodeSource().
                getLocation().getPath();
    }

    @SuppressWarnings("deprecation")
    public static File getSIRFileObject() {
        return new File(URLDecoder.decode(getSIRFilePath()));
    }

    @Nullable
    public static JarFile getSIRJarFile() {
        try {
            return new JarFile(getSIRFileObject());
        } catch (Exception e) {
            return null;
        }
    }
}
