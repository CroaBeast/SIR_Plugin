package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.beans.builder.BossbarBuilder;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.api.ResourceIOUtils;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.logger.DelayLogger;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.module.chat.EmojiParser;
import me.croabeast.sir.plugin.module.chat.TagsParser;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.util.DataUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

public final class SIRPlugin extends JavaPlugin {

    public static final DelayLogger DELAY_LOGGER = DelayLogger.simplified();

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static String version, author;

    static class SIRSender extends MessageSender {

        private SIRSender() {
            addFunctions(TagsParser::parse, EmojiParser::parse);
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
                "&0 * &e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &f" +
                        getVersion(), "",
                "&0 * &e• Server: " + ServerInfoUtils.SERVER_FORK,
                "&0 * &e• Java Version: " + SystemUtils.JAVA_VERSION,
                "&0 * &e• Developer: " + author, ""
        );

        SIRInitializer.startMetrics();

        try {
            DataUtils.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SIRInitializer.setPluginHooks();
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

    @NotNull
    public Logger getLogger() {
        return BeansLogger.getLogger();
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

        BeansLogger.doLog(
                "&0* *&e____ &0* &e___ &0* &e____",
                "&0* &e(___&0 * * &e|&0* * &e|___)",
                "&0* &e____) . _|_ . | &0* &e\\ . &f" +
                        getVersion(), ""
        );

        BeansLogger.getLogger().log("&7SIR &c" + version + "&7 was totally disabled.");
        BeansLogger.doLog("");

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
