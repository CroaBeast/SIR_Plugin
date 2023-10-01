package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.var;
import me.croabeast.beanslib.analytic.UpdateChecker;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.module.object.AnnounceHandler;
import me.croabeast.sir.plugin.module.object.EmojiParser;
import me.croabeast.sir.plugin.module.object.listener.JoinQuitHandler;
import me.croabeast.sir.plugin.task.SIRTask;
import me.croabeast.sir.plugin.task.object.message.DirectTask;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class SIRPlugin extends JavaPlugin {

    static final List<String> JAR_ENTRIES = new ArrayList<>();
    static final String EMPTY_LINE = "true::";

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static LangUtils utils;

    @Getter
    private static String version, author;

    static String[] pluginHeader() {
        return new String[] {
                "", "&0* *&e____ &0* &e___ &0* &e____", "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &f" + getVersion(), ""
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        final long start = System.currentTimeMillis();

        instance = this;

        author = getDescription().getAuthors().get(0);
        version = getDescription().getVersion();

        utils = new LangUtils(this);

        String path = getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation().getPath();

        try (JarFile j = new JarFile(new File(URLDecoder.decode(path)))) {
            String prefix = "me/croabeast/sir/plugin";

            JAR_ENTRIES.addAll(
                    Collections.list(j.entries()).stream()
                            .map(ZipEntry::getName)
                            .filter(s -> s.startsWith(prefix))
                            .collect(Collectors.toList())
            );
        } catch (Exception ignored) {}

        try {
            CacheHandler.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final FileCache config = FileCache.MAIN_CONFIG;

        MessageSender.setLoaded(new MessageSender()
                .setLogger(config.getValue("options.send-console", true))
                .setCaseSensitive(false)
                .setNoFirstSpaces(config.getValue("options.strip-spaces", false))
                .addFunctions(EmojiParser::parse)
        );

        LogUtils.rawLog(pluginHeader());

        LogUtils.rawLog(
                "&0* &7Developer: " + author,
                "&0* &7Software: " + LibUtils.serverFork(),
                "&0* &7Java Version: " + SystemUtils.JAVA_VERSION, ""
        );

        SIRInitializer.startMetrics();
        SIRInitializer.setPluginHooks();

        registerCommands();
        registerModules();

        ModuleName<AnnounceHandler> name = ModuleName.ANNOUNCEMENTS;

        if (name.isEnabled()) {
            name.get().startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.mixLog("",
                "&7SIR " + version + " was&a loaded&7 in &e" +
                (System.currentTimeMillis() - start) + " ms.",
                EMPTY_LINE
        );

        if (LoginHook.isEnabled())
            JoinQuitHandler.LOGGED_PLAYERS.addAll(Bukkit.getOnlinePlayers());

        runTaskWhenLoaded(() -> checkUpdater(null));
    }

    @Override
    public void onDisable() {
        LogUtils.rawLog(pluginHeader());

        try {
            CacheHandler.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ModuleName.ANNOUNCEMENTS.get().cancelTask();

        LogUtils.mixLog(
                "&7The announcement task has been stopped.", "",
                "&7SIR &c" + version + "&7 was totally disabled.", EMPTY_LINE
        );

        VanishHook.unloadHook();
        LoginHook.unloadHook();

        HandlerList.unregisterAll(this);

        utils = null;
        instance = null;
    }

    private boolean commandsRegistered = false, modulesRegistered = false;

    private void registerCommands() {
        if (commandsRegistered)
            throw new IllegalStateException("Commands are already registered.");

        SIRCollector.from("me.croabeast.sir.plugin.task.object")
                .filter(c -> !c.getName().contains("$"))
                .filter(SIRTask.class::isAssignableFrom)
                .filter(c -> c != SIRTask.class && c != DirectTask.class)
                .collect()
                .forEach(c -> {
                    try {
                        var co = c.getDeclaredConstructor();
                        co.setAccessible(true);
                        ((SIRTask) co.newInstance()).register();
                        co.setAccessible(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        commandsRegistered = true;
    }

    private void registerModules() {
        if (modulesRegistered)
            throw new IllegalStateException("Modules are already registered.");

        SIRCollector.from("me.croabeast.sir.plugin.module.object")
                .filter(c -> !c.getName().contains("$"))
                .filter(SIRModule.class::isAssignableFrom)
                .filter(c -> c != SIRModule.class)
                .collect()
                .forEach(c -> {
                    try {
                        var co = c.getDeclaredConstructor();
                        co.setAccessible(true);
                        ((SIRModule) co.newInstance()).register();
                        co.setAccessible(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        new SIRModule(ModuleName.DISCORD_HOOK) {
            @Override
            public void register() {}
        };

        new SIRModule(ModuleName.CHAT_COLORS) {
            @Override
            public void register() {}
        };

        modulesRegistered = true;
    }

    private static void updaterLog(Player player, String... strings) {
        if (player != null) {
            String[] array = new String[strings.length];

            for (int i = 0; i < strings.length; i++) {
                String s = strings[i];
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
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.on-start", false))
                return;
            runUpdater(null);
        }
        else {
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.send-op", false) ||
                    !PlayerUtils.hasPerm(player, "sir.admin.updater")) return;
            runUpdater(player);
        }
    }

    @SneakyThrows
    public static void runTaskWhenLoaded(Runnable runnable) {
        checkAccess(SIRPlugin.class);
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, runnable);
    }

    public static File getSIRFolder() {
        return instance.getDataFolder();
    }

    /**
     * Checks if the providing plugin of the class is SIR, otherwise will
     * throw an Exception.
     *
     * @param clazz a class
     * @throws IllegalAccessException if the plugin of the class is not SIR
     */
    public static void checkAccess(Class<?> clazz) throws IllegalAccessException {
        JavaPlugin plugin = null;

        try {
            plugin = JavaPlugin.getProvidingPlugin(clazz);
            if (plugin != instance) plugin = null;
        }
        catch (Exception ignored) {}

        if (plugin != null) return;

        String e = " is only accessible using the SIR plugin";
        throw new IllegalAccessException(clazz.getSimpleName() + e);
    }
}
