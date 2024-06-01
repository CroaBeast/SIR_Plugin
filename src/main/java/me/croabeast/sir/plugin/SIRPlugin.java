package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.builder.BossbarBuilder;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.sir.api.ResourceIOUtils;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.logger.DelayLogger;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.util.DataUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SIRPlugin extends JavaPlugin {

    public static final DelayLogger DELAY_LOGGER = DelayLogger.simplified();

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static String version, author;

    static class SIRSender extends MessageSender {

        private SIRSender() {
            addFunctions(SIRModule.TAGS.getData()::parse, SIRModule.EMOJIS.getData()::parse);
        }

        @Override
        public boolean isSensitive() {
            return false;
        }

        @Override
        public String getErrorPrefix() {
            return null;
        }

        static ConfigurableFile config() {
            return YAMLData.Main.CONFIG.from();
        }

        public boolean trimSpaces() {
            return config().get("options.strip-spaces", false);
        }

        public boolean isLogger() {
            return config().get("options.send-console", true);
        }
    }

    @Override
    public void onEnable() {
        final long start = System.currentTimeMillis();

        instance = this;
        author = getDescription().getAuthors().get(0);
        version = getDescription().getVersion();

        DELAY_LOGGER.setPlugin(this);
        SIRLoader.loadAllJarEntries();

        DELAY_LOGGER.clear();

        DELAY_LOGGER.add(false,
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &f" +
                        getVersion(), ""
        );

        SIRInitializer.startMetrics();
        SIRInitializer.setPluginHooks();

        try {
            DataUtils.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SIRLoader.initializeLangUtils(this);

        try {
            MessageSender.setLoaded(new SIRSender());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SIRModule.ANNOUNCEMENTS.getData().start();

        if (LoginHook.isHookEnabled())
            Bukkit.getOnlinePlayers().forEach(LoginHook::addPlayer);

        DELAY_LOGGER.add(false, "");
        DELAY_LOGGER.add(true,
                "&7SIR " + version + " was&a loaded&7 in &e" +
                        (System.currentTimeMillis() - start) + " ms."
        );
        DELAY_LOGGER.add(false, "");

        DELAY_LOGGER.sendLines(false);
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p ->
                BossbarBuilder.getBuilders(p).forEach(BossbarBuilder::unregister));

        try {
            DataUtils.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SIRModule.ANNOUNCEMENTS.getData().stop();

        BeansLib.logger().log(false,
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &f" +
                        getVersion(), ""
        );

        BeansLib.logger().log("&7SIR &c" + version + "&7 was totally disabled.");
        BeansLib.logger().log(false, "");

        HandlerList.unregisterAll(this);
        instance = null;
    }

    public static File getFolder() {
        return getInstance().getDataFolder();
    }

    public static File fileFrom(String... childPaths) {
        return ResourceIOUtils.fileFrom(getFolder(), childPaths);
    }

    @SneakyThrows
    public static void runTaskWhenLoaded(Runnable runnable) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), runnable);
    }
}
