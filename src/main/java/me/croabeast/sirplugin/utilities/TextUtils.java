package me.croabeast.sirplugin.utilities;

import me.croabeast.beanslib.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.*;

import java.util.regex.*;

import static me.croabeast.sirplugin.objects.FileCatcher.*;

public class TextUtils extends BeansLib {

    private static SIRPlugin main;

    public TextUtils(SIRPlugin instance) {
        main = instance;
    }

    @Override
    protected @NotNull JavaPlugin getPlugin() {
        return main;
    }

    private String tryString(@Nullable YMLFile section, String path, String def) {
        if (section == null) return def;
        return section.getFile().getString(path, def);
    }

    @Override
    public @NotNull String langPrefixKey() {
        return tryString(CONFIG.initialSource(), "values.lang-prefix-key", "<P>");
    }

    @Override
    public @NotNull String langPrefix() {
        return tryString(LANG.initialSource(), "main-prefix", " &e&lSIR &8>");
    }

    @Override
    public @NotNull String centerPrefix() {
        return tryString(CONFIG.initialSource(), "values.center-prefix", "<C>");
    }

    @Override
    public @NotNull String lineSeparator() {
        return Pattern.quote(tryString(CONFIG.initialSource(), "values.line-separator", "<n>"));
    }

    @Override
    public boolean isHardSpacing() {
        return CONFIG.toFile().getBoolean("options.hard-spacing", true);
    }

    @Override
    public boolean isStripPrefix() {
        return CONFIG.toFile().getBoolean("options.show-prefix", true);
    }

    @Override
    public String colorize(@Nullable Player player, String line) {
        if (Module.isEnabled(Module.Identifier.EMOJIS))
            line = main.getEmParser().parseEmojis(line);
        return IridiumAPI.process(parsePAPI(player, parseChars(line)));
    }

    private boolean checkInts(@Nullable String[] array) {
        if (array == null) return false;
        for (String integer : array) {
            if (integer == null) return false;
            if (!integer.matches("-?\\d+")) return false;
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

    public static String parseInsensitiveEach(String line, String[] keys, String[] values) {
        String[] resultKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) resultKeys[i] = "{" + keys[i] + "}";
        return BeansLib.replaceInsensitiveEach(line, resultKeys, values);
    }

    public static String parseInsensitiveEach(String line, String key, String value) {
        return parseInsensitiveEach(line, new String[] {key}, new String[] {value});
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
}
