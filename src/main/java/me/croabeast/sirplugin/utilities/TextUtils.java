package me.croabeast.sirplugin.utilities;

import com.google.common.collect.*;
import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.objects.handlers.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.sirplugin.modules.listeners.Formatter.KeysHandler.*;

public final class TextUtils {

    private static final SIRPlugin main = SIRPlugin.getInstance();

    private static ActionBar actionBar;
    private static TitleMngr titleMngr;

    public TextUtils() {
        actionBar = new ActionBar();
        titleMngr = new TitleMngr();
    }
    
    public static String centerPrefix() {
        return main.getConfig().getString("values.center-prefix", "<C>");
    }
    public static String lineSplitter() {
        return Pattern.quote(main.getConfig().getString("values.line-separator", "<n>"));
    }
    public static String langPrefix() {
        return main.getLang().getString("main-prefix");
    }
    public static String langPKey() {
        return main.getConfig().getString("values.lang-prefix-key", "<P>");
    }

    public static String parsePAPI(@Nullable Player player, String line) {
        return Initializer.hasPAPI() ? PlaceholderAPI.setPlaceholders(player, line) : line;
    }

    public static String parseChars(String line) {
        Pattern charPattern = Pattern.compile("<U:([a-fA-F0-9]{4})>");
        Matcher match = charPattern.matcher(line);

        while (match.find()) {
            char s = (char) Integer.parseInt(match.group(1), 16);
            line = line.replace(match.group(), s + "");
        }
        return line;
    }

    public static String colorize(Player player, String line) {
        if (BaseModule.isEnabled(BaseModule.Identifier.EMOJIS))
            line = main.getEmParser().parseEmojis(line);
        return IridiumAPI.process(parsePAPI(player, parseChars(line)));
    }

    @NotNull
    public static List<String> fileList(@Nullable ConfigurationSection section, String path) {
        if (section == null) return new ArrayList<>();
        return  !section.isList(path) ?
                Lists.newArrayList(section.getString(path)) :
                section.getStringList(path);
    }

    public static String stringKey(@Nullable String key) {
        return key == null ? "empty" :
                StringUtils.replaceEach(key, new String[] {"/", ":"}, new String[] {".", "."});
    }

    public static String removeSpace(String line) {
        if (SIRPlugin.getInstance().getConfig().getBoolean("options.hard-spacing")) {
            String startLine = line;
            try {
                while (line.charAt(0) == ' ') line = line.substring(1);
                return line;
            } catch (IndexOutOfBoundsException e) {
                return startLine;
            }
        }
        else return line.startsWith(" ") ? line.substring(1) : line;
    }

    public static String parseInsensitiveEach(String line, String[] keys, String[] values) {
        if (keys == null || values == null) return line;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == null | values[i] == null) continue;
            line = line.replaceAll(
                    "(?i)\\{" + keys[i] + "}", values[i]);
        }
        return line;
    }

    public static void sendFileMsg(CommandSender sender, List<String> list, String[] keys, String[] values) {
        if (list.isEmpty()) return;

        for (String line : list) {
            if (line == null || line.equals("")) continue;
            line = line.startsWith(langPKey()) ? line.replace(langPKey(), langPrefix()) : line;
            line = parseInsensitiveEach(line, keys, values);

            if (sender != null && !(sender instanceof ConsoleCommandSender)) {
                Player player = (Player) sender;

                line = parseInsensitiveEach(line, new String[] {"player", "world"},
                        new String[] {player.getName(), player.getWorld().getName()});

                selectMsgType(player, colorize(player, getChatValues(player, line)));
            }
            else LogUtils.rawLog(JsonMsg.centeredText(null, line));
        }
    }

    public static void sendFileMsg(CommandSender s, ConfigurationSection id, String p, String[] k, String[] v) {
        sendFileMsg(s, fileList(id, p), k, v);
    }

    public static void sendFileMsg(CommandSender sender, List<String> list, String key, String value) {
        sendFileMsg(sender, list, new String[] {key}, new String[] {value});
    }

    public static void sendFileMsg(CommandSender sender, String path, String[] keys, String[] values) {
        sendFileMsg(sender, SIRPlugin.getInstance().getLang(), path, keys, values);
    }

    public static void sendFileMsg(CommandSender sender, String path, String key, String value) {
        sendFileMsg(sender, SIRPlugin.getInstance().getLang(), path, new String[] {key}, new String[] {value});
    }

    public static void sendFileMsg(CommandSender sender, String path) {
        sendFileMsg(sender, SIRPlugin.getInstance().getLang(), path, null, null);
    }

    public static void sendActionBar(Player player, String message) {
        actionBar.getMethod().send(player, message);
    }

    private static boolean checkInts(String[] array) {
        if (array == null) return false;
        for (String integer : array)
            if (!integer.matches("-?\\d+")) return false;
        return true;
    }

    public static void sendTitle(Player player, String[] message, String[] times) {
        if (message.length == 0 || message.length > 2) return;
        String subtitle = message.length == 1 ? "" : message[1];

        int[] i;
        if (checkInts(times) && times.length == 3) {
            i = new int[times.length];
            for (int x = 0; x < times.length; x++)
                i[x] = Integer.parseInt(times[x]);
        }
        else i = new int[] {10, 50, 10};

        titleMngr.getMethod().send(player, message[0], subtitle, i[0], i[1], i[2]);
    }

    public static void sendJsonMsg(Player player, String line) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String cmd = "minecraft:tellraw " + player.getName() + " " + line;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }.runTask(SIRPlugin.getInstance());
    }

    public static String parsePrefix(String type, String message) {
        type = "[" + type.toUpperCase() + "]";
        message = message.substring(type.length());
        return removeSpace(message);
    }

    public static boolean isStarting(String prefix, String line) {
        return line.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static String getChatValues(Player player, String line) {
        boolean isNot = !BaseModule.isEnabled(BaseModule.Identifier.FORMATS);
        String[] values = {isNot ? null : getChatValue(player, "prefix", ""),
                isNot ? null : getChatValue(player, "suffix", "")};
        return parseInsensitiveEach(line, new String[] {"prefix", "suffix"}, values);
    }

    public static void selectMsgType(Player player, String line) {
        if (isStarting("[action-bar]", line)) {
            sendActionBar(player, JsonMsg.stripJson(parsePrefix("action-bar", line)));
        }
        else if (isStarting("[title]", line)) {
            line = JsonMsg.stripJson(parsePrefix("title", line));
            sendTitle(player, line.split(lineSplitter()), null);
        }
        else if (isStarting("[json]", line) && line.contains("{\"text\":")) {
            sendJsonMsg(player, JsonMsg.stripJson(parsePrefix("json", line)));
        }
        else if (isStarting("[bossbar", line)) {
            new Bossbar(player, line).display();
        }
        else player.spigot().sendMessage(new JsonMsg(player, line).build());
    }
}
