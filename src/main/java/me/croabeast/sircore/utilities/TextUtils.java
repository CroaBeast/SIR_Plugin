package me.croabeast.sircore.utilities;

import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.handlers.*;
import me.croabeast.sircore.terminals.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.util.*;

public class TextUtils {

    private final Application main;

    public int getVersion;
    public String serverName;

    private final PAPI papi;
    private final ActionBar actionBar;
    private final TitleMain titleMain;

    public TextUtils(Application main) {
        this.main = main;

        String version = Bukkit.getBukkitVersion().split("-")[0];
        getVersion = Integer.parseInt(version.split("\\.")[1]);
        serverName = Bukkit.getVersion().split("-")[1] + " " + version;

        papi = main.getInitializer().hasPAPI ? new HasPAPI() : new NotPAPI();
        actionBar = getVersion < 11 ? new ActBar10() : new ActBar17();
        titleMain = getVersion < 10 ? new Title9() : new Title17();
    }

    public boolean getOption(int i, String path) {
        String P = "";
        switch (i) {
            case 1: P = "options."; break;
            case 2: P = "login."; break;
            case 3: P = "vanish."; break;
            case 4: P = "updater.plugin."; break;
        }
        return main.getConfig().getBoolean(P + path);
    }

    public String getValue(String key) {
        switch (key) {
            case "prefix":
                return main.getLang().getString("main-prefix", "");
            case "split":
                return main.getConfig().getString("values.line-separator");
            case "config":
                return main.getConfig().getString("values.config-prefix");
            case "center":
                return main.getConfig().getString("values.center-prefix");
            default: return "";
        }
    }

    public String parsePAPI(Player player, String message) {
        return IridiumAPI.process(papi.parsePAPI(player, message));
    }

    public void sendCentered(Player player, String message) {
        message = parsePAPI(player, message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') previousCode = true;
            else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                FontInfo dFI = FontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = FontInfo.SPACE.getLength() + 1;
        int compensated = 0;

        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        player.sendMessage(sb + message);
    }

    public void sendMixed(Player player, String message) {
        String center = getValue("center");

        if (message.startsWith(center)) {
            sendCentered(player, message.replace(center, ""));
        }
        else player.sendMessage(parsePAPI(player, message));
    }

    public int getSections(String path) {
        int messages = 0;
        ConfigurationSection ids = main.getMessages().getConfigurationSection(path);
        if (ids == null) return 0;

        for (String key : ids.getKeys(false)) {
            ConfigurationSection id = ids.getConfigurationSection(key);
            if (id != null) messages++;
        }
        return messages;
    }

    public List<String> fileList(FileConfiguration file, String path) {
        return  !file.isList(path) ?
                Collections.singletonList(file.getString(path)) :
                file.getStringList(path);
    }

    private List<String> toList(String path) { return fileList(main.getLang(), path); }

    public void send(CommandSender sender, String path, String[] keys, String... values) {
        String key = getValue("config"), center = getValue("center");

        for (String msg : toList(path)) {
            if (msg == null || msg.equals("")) continue;
            msg = msg.startsWith(key) ? msg.replace(key, getValue("prefix")) : msg;
            msg = StringUtils.replaceEach(msg, keys, values);

            if (!(sender instanceof Player)) {
                if (msg.startsWith(center)) msg = msg.replace(center,"");
                main.getRecords().rawRecord(msg);
            }
            else sendMixed((Player) sender, msg);
        }
    }

    public void actionBar(Player player, String message) { actionBar.send(player, message); }

    private boolean checkInts(String[] array) {
        for (String integer : array)
            if (!integer.matches("-?\\d+")) return false;
        return true;
    }

    private int[] intArray(String[] array) {
        int[] ints = new int[array.length];
        for (int i = 0; i < array.length; i++)
            ints[i] = Integer.parseInt(array[i]);
        return ints;
    }

    public void title(Player player, String[] message, String[] times) {
        if (message.length == 0 || message.length > 2) return;
        String subtitle = message.length == 1 ? "" : message[1];
        if (!checkInts(times)) return;
        int[] ints = intArray(times);
        titleMain.send(player, message[0], subtitle, ints[0], ints[1], ints[2]);
    }

    // Initializer for PAPI
    public interface PAPI {
        String parsePAPI(Player player, String message);
    }

    public static class NotPAPI implements PAPI{
        @Override
        public String parsePAPI(Player player, String message) {
            return message;
        }
    }

    public static class HasPAPI implements PAPI{
        @Override
        public String parsePAPI(Player player, String message) {
            return player != null ? PlaceholderAPI.setPlaceholders(player, message) : message;
        }
    }
}
