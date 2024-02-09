package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.misc.CollectionBuilder;
import me.croabeast.beanslib.misc.UpdateChecker;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.object.AnnounceHandler;
import me.croabeast.sir.plugin.module.object.EmojiParser;
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
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class SIRPlugin extends JavaPlugin {

    static final List<Class<?>> JAR_ENTRIES = new ArrayList<>();
    static final String EMPTY_LINE = "true::";

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static LangUtils utils;

    @Getter
    private static String version, author;

    static String[] pluginHeader() {
        return new String[] {
                "", "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
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
            final String prefix = "me/croabeast/sir/plugin";

            JAR_ENTRIES.addAll(
                    CollectionBuilder.of(j.entries())
                            .map(ZipEntry::getName)
                            .filter(s -> {
                                if (s.contains("$")) return false;

                                if (s.startsWith(prefix + "/hook"))
                                    return false;

                                if (!s.startsWith(prefix))
                                    return false;

                                return s.endsWith(".class");
                            })
                            .map(s -> {
                                s = s.replace('/', '.')
                                        .replace(".class", "");

                                try {
                                    return Class.forName(s);
                                } catch (Exception ignored) {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull).toList()
            );
        } catch (Exception ignored) {}

        try {
            SIRCache.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final YAMLFile config = YAMLCache.getMainConfig();

        MessageSender sender = new MessageSender() {
            @Override
            public boolean shouldTrimSpaces() {
                return config.get("options.strip-spaces", false);
            }
        };

        MessageSender.setLoaded(sender
                .setLogger(config.get("options.send-console", true))
                .setSensitive(false)
                .addFunctions(EmojiParser::parse)
        );

        LogUtils.rawLog(pluginHeader());

        LogUtils.rawLog(
                "&0* &7Developer: " + author,
                "&0* &7Software: " + Bukkit.getVersion(),
                "&0* &7Java Version: " +
                        SystemUtils.JAVA_VERSION, ""
        );

        SIRInitializer.startMetrics();
        SIRInitializer.setPluginHooks();

        if (ModuleName.ANNOUNCEMENTS.isEnabled()) {
            AnnounceHandler.startTask();
            LogUtils.doLog("&7The announcement task has been started.");
        }

        LogUtils.mixLog("",
                "&7SIR " + version + " was&a loaded&7 in &e" +
                (System.currentTimeMillis() - start) + " ms.",
                EMPTY_LINE
        );

        if (LoginHook.isEnabled())
            Bukkit.getOnlinePlayers().forEach(LoginHook::addPlayer);

        runTaskWhenLoaded(() -> checkUpdater(null));
    }

    @Override
    public void onDisable() {
        LogUtils.rawLog(pluginHeader());

        try {
            SIRCache.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AnnounceHandler.cancelTask();

        LogUtils.mixLog(
                "&7The announcement task has been stopped.", "",
                "&7SIR &c" + version + "&7 was totally disabled.",
                EMPTY_LINE
        );

        HandlerList.unregisterAll(this);

        utils = null;
        instance = null;
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

    @SneakyThrows
    public static void checkUpdater(@Nullable Player player) {
        if (player == null) {
            if (!YAMLCache.getMainConfig().get("updater.plugin.on-start", false))
                return;
            runUpdater(null);
        }
        else {
            if (!YAMLCache.getMainConfig().get("updater.plugin.send-op", false) ||
                    !PlayerUtils.hasPerm(player, "sir.admin.updater")) return;
            runUpdater(player);
        }
    }

    @SneakyThrows
    public static void runTaskWhenLoaded(Runnable runnable) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, runnable);
    }

    public static File getSIRFolder() {
        return instance.getDataFolder();
    }
}
