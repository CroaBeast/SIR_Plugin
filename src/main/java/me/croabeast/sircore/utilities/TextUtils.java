package me.croabeast.sircore.utilities;

import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.handlers.*;
import me.croabeast.sircore.terminals.*;
import org.apache.commons.lang.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.util.*;

public class TextUtils {

    private final Application main;

    private final PAPI papi;
    private final ActionBar actionBar;
    private final TitleMain titleMain;

    // Initializer for PAPI
    public interface PAPI {
        String parsePAPI(Player player, String message);
    }

    public TextUtils(Application main) {
        this.main = main;

        papi = (player, message) -> !main.getInitializer().HAS_PAPI || player == null ?
                message : PlaceholderAPI.setPlaceholders(player, message);
        actionBar = main.GET_VERSION < 11 ? new ActBar10() : new ActBar17();
        titleMain = main.GET_VERSION < 10 ? new Title9() : new Title17();
    }

    public boolean getOption(int i, String path) {
        String P;

        if (i == 1) P = "options.";
        else if (i == 2) P = "login.";
        else if (i == 3) P = "vanish.";
        else if (i == 4) P = "updater.plugin.";
        else P = "";

        return main.getConfig().getBoolean(P + path);
    }

    private String getValue(String key) {
        return main.getConfig().getString("values." + key);
    }

    public String getSplit() { return getValue("line-separator"); }

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
                messagePxSize += isBold ?
                        dFI.getBoldLength() : dFI.getLength();
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
        String center = getValue("center-prefix");

        if (message.startsWith(center)) {
            sendCentered(player, message.replace(center, ""));
        }
        else player.sendMessage(parsePAPI(player, message));
    }

    public int getSections(String path) {
        int messages = 0;
        ConfigurationSection ids = main.getMessages().getConfigurationSection(path);
        if (ids == null) return 0;

        for (String key : ids.getKeys(false))
            if (ids.getConfigurationSection(key) != null) messages++;

        return messages;
    }

    public List<String> fileList(FileConfiguration file, String path) {
        return  !file.isList(path) ?
                Collections.singletonList(file.getString(path)) :
                file.getStringList(path);
    }

    private List<String> toList(String path) { return fileList(main.getLang(), path); }

    public void send(CommandSender sender, String path, String[] keys, String... values) {
        String prefix = main.getLang().getString("main-prefix", ""),
                key = getValue("config-prefix"),
                center = getValue("center-prefix");

        for (String line : toList(path)) {
            if (line == null || line.equals("")) continue;
            line = StringUtils.replaceEach(
                    line.startsWith(key) ? line.replace(key, prefix) : line,
                    keys, values
            );

            if (!(sender instanceof Player)) {
                if (line.startsWith(center)) line = line.replace(center,"");
                main.getRecords().rawRecord(line);
            }
            else sendMixed((Player) sender, line);
        }
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null, (String[]) null);
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
}
