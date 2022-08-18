package me.croabeast.sirplugin.utility;

import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.EmParser;
import me.croabeast.sirplugin.object.file.FileCache;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class LangUtils extends BeansLib {

    private static SIRPlugin main;

    public LangUtils(SIRPlugin instance) {
        main = instance;
    }

    @Override
    protected @NotNull JavaPlugin getPlugin() {
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
    public boolean isHardSpacing() {
        return FileCache.CONFIG.value("options.hard-spacing", true);
    }

    @Override
    public boolean isStripPrefix() {
        return !FileCache.CONFIG.value("options.show-prefix", true);
    }

    @Override
    public void sendMessage(Player target, Player parser, String string) {
        try {
            string = EmParser.parseEmojis(string);
        } catch (Exception ignored) {}

        super.sendMessage(target, parser, string);
    }

    private static boolean checkInts(@Nullable String[] array) {
        if (array == null) return false;
        for (String integer : array) {
            if (integer == null) return false;
            if (!integer.matches("\\d+")) return false;
        }
        return true;
    }

    public static void sendTitle(Player player, String[] message, String[] times) {
        int[] i = new int[] {10, 50, 10};

        if (checkInts(times) && times.length == 3) {
            for (int x = 0; x < times.length; x++) {
                if (times[x] == null) continue;
                i[x] = Integer.parseInt(times[x]);
            }
        }

        TextUtils.sendTitle(player, message, i[0], i[1], i[2]);
    }

    public static String parseInternalKeys(String line, String[] keys, String[] values) {
        String[] resultKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) resultKeys[i] = "{" + keys[i] + "}";
        return TextUtils.replaceInsensitiveEach(line, resultKeys, values);
    }

    public static String stringKey(@Nullable String key) {
        return key == null ? "empty" : key.replace("/", ".").replace(":", ".");
    }
}
