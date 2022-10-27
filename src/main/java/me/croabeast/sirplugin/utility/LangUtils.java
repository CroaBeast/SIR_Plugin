package me.croabeast.sirplugin.utility;

import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.object.display.Displayer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.EmParser;
import me.croabeast.sirplugin.object.file.FileCache;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class LangUtils extends BeansLib {

    private static SIRPlugin main;

    public LangUtils(SIRPlugin instance) {
        main = instance;
    }

    @Override
    public @NotNull JavaPlugin getPlugin() {
        return main;
    }

    @Override
    public @NotNull String langPrefixKey() {
        return FileCache.CONFIG.value("values.lang-prefix-key", "<P>");
    }

    @Override
    public @NotNull String langPrefix() {
        return FileCache.LANG.value("main-prefix", " &e&lSIR &8>");
    }

    @Override
    public @NotNull String centerPrefix() {
        return FileCache.CONFIG.value("values.center-prefix", "<C>");
    }

    @Override
    public @NotNull String lineSeparator() {
        return Pattern.quote(FileCache.CONFIG.value("values.line-separator", "<n>"));
    }

    @Override
    public boolean fixColorLogger() {
        return FileCache.CONFIG.value("options.fix-logger", false);
    }

    @Override
    public ConfigurationSection getWebhookSection() {
        return null;
    }

    @Override
    public boolean isStripPrefix() {
        return !FileCache.CONFIG.value("options.show-prefix", true);
    }

    public static String parseInternalKeys(String line, String[] keys, String[] values) {
        String[] resultKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) resultKeys[i] = "{" + keys[i] + "}";
        return TextUtils.replaceInsensitiveEach(line, resultKeys, values);
    }

    public static String stringKey(@Nullable String key) {
        return key == null ? "empty" : key.replace("/", ".").replace(":", ".");
    }

    public static Displayer create(Collection<? extends CommandSender> targets, Player p, List<String> list) {
        return new Displayer(SIRPlugin.getUtils(), targets, p, list).
                setLogger(FileCache.CONFIG.value("options.send-console", true)).
                setCaseSensitive(false).
                setOperators(EmParser::parseEmojis);
    }

    public static Displayer create(CommandSender t, Player p, List<String> list) {
        return new Displayer(SIRPlugin.getUtils(), t, p, list).
                setLogger(FileCache.CONFIG.value("options.send-console", true)).
                setCaseSensitive(false).
                setOperators();
    }

    public static Displayer create(Player p, List<String> list) {
        return create((Collection<? extends CommandSender>) null, p, list);
    }
}
