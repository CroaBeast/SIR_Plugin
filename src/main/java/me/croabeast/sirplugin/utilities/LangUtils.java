package me.croabeast.sirplugin.utilities;

import me.croabeast.beanslib.*;
import me.croabeast.beanslib.utilities.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.files.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.*;

import java.util.regex.*;

public class LangUtils extends BeansLib {

    private static SIRPlugin main;

    public LangUtils(SIRPlugin instance) {
        main = instance;
    }

    @Override
    protected @NotNull JavaPlugin getPlugin() {
        return main;
    }

    private String isSet(@Nullable YMLFile section, String path, String def) {
        return section == null ? def : section.getFile().getString(path, def);
    }

    @Override
    public @NotNull String langPrefixKey() {
        return isSet(FileCache.CONFIG.initialSource(), "values.lang-prefix-key", "<P>");
    }

    @Override
    public @NotNull String langPrefix() {
        return isSet(FileCache.LANG.initialSource(), "main-prefix", " &e&lSIR &8>");
    }

    @Override
    public @NotNull String centerPrefix() {
        return isSet(FileCache.CONFIG.initialSource(), "values.center-prefix", "<C>");
    }

    @Override
    public @NotNull String lineSeparator() {
        return Pattern.quote(isSet(FileCache.CONFIG.initialSource(), "values.line-separator", "<n>"));
    }

    @Override
    public boolean fixColorLogger() {
        return FileCache.CONFIG.initialSource() != null &&
                FileCache.CONFIG.get().getBoolean("options.fix-logger");
    }

    @Override
    public boolean isHardSpacing() {
        return FileCache.CONFIG.get().getBoolean("options.hard-spacing", true);
    }

    @Override
    public boolean isStripPrefix() {
        return !FileCache.CONFIG.get().getBoolean("options.show-prefix", true);
    }

    @Override
    public String colorize(@Nullable Player player, String line) {
        return super.colorize(player, EmParser.parseEmojis(line));
    }

    private boolean checkInts(@Nullable String[] array) {
        if (array == null) return false;
        for (String integer : array) {
            if (integer == null) return false;
            if (!integer.matches("\\d+")) return false;
        }
        return true;
    }

    public void sendTitle(Player player, String[] message, String[] times) {
        int[] i;
        if (checkInts(times) && times.length == 3) {
            i = new int[times.length];
            for (int x = 0; x < times.length; x++) {
                assert times[x] != null;
                i[x] = Integer.parseInt(times[x]);
            }
        }
        else i = defaultTitleTicks();
        sendTitle(player, message, i[0], i[1], i[2]);
    }

    public static String parseInternalKeys(String line, String[] keys, String[] values) {
        String[] resultKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) resultKeys[i] = "{" + keys[i] + "}";
        return TextUtils.replaceInsensitiveEach(line, resultKeys, values);
    }

    public static String parseInternalKeys(String line, String key, String value) {
        return parseInternalKeys(line, new String[] {key}, new String[] {value});
    }

    public static String stringKey(@Nullable String key) {
        return key == null ? "empty" : key.replace("/", ".").replace(":", ".");
    }

    public String parsePrefix(String type, String message) {
        return removeSpace(message.substring(("[" + type.toUpperCase() + "]").length()));
    }

    public static boolean isStarting(String prefix, String line) {
        return line.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    @NotNull
    public static String getLastColor(@NotNull String string, @Nullable String key, boolean checkSpecial) {
        if (string.length() < 1)
            throw new IndexOutOfBoundsException("String can not be empty");

        boolean hasKey = key != null && key.length() >= 1;
        if (hasKey) key = Pattern.quote(key);

        String rgb = "\\{#[\\dA-F]{6}}|<#[\\dA-F]{6}>|&#[\\dA-F]{6}|#[\\dA-F]{6}",
                special = checkSpecial ? "([&§][k-or])*" : "",
                regex = "(?i)(([&§][a-f\\d]|" + rgb + ")" + special + ")";

        String input = hasKey ? string.split(regex + "?" + key)[0] : string,
                lastColor = "";

        Matcher match = Pattern.compile(regex).matcher(input);
        while (match.find()) lastColor = match.group();

        return lastColor;
    }
}
