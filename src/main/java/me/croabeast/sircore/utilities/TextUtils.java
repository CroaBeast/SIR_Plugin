package me.croabeast.sircore.utilities;

import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.terminals.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.util.*;

public class TextUtils {

    private final Application main;
    private final ActionBar actionBar;
    private final Title title;

    private final PAPI papi;

    // Initializer for PAPI
    public interface PAPI {
        String parsePAPI(Player player, String message);
    }

    public TextUtils(Application main) {
        this.main = main;

        papi =  (p, line) -> !main.getInitializer().HAS_PAPI ? line :
                (p != null ? PlaceholderAPI.setPlaceholders(p, line) : line);
        actionBar = new ActionBar(main);
        title = new Title(main);
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
        return papi.parsePAPI(player, message);
    }

    public String parse(Player player, String message) {
        return IridiumAPI.process(parsePAPI(player, message));
    }

    public void sendCentered(Player player, String message) {
        message = parse(player, message);

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
        if (!message.startsWith(center)) player.sendMessage(parse(player, message));
        else sendCentered(player, message.replace(center, ""));
    }

    public List<String> fileList(FileConfiguration file, String path) {
        return  !file.isList(path) ?
                Collections.singletonList(file.getString(path)) :
                file.getStringList(path);
    }

    private List<String> toList(String path) { return fileList(main.getLang(), path); }

    public void send(CommandSender sender, String path, String key, String value) {
        String prefix = main.getLang().getString("main-prefix", ""),
                prKey = getValue("config-prefix"),
                center = getValue("center-prefix");

        for (String line : toList(path)) {
            if (line == null || line.equals("")) continue;
            line = line.startsWith(prKey) ? line.replace(prKey, prefix) : line;
            if (key != null && value != null)
                line = line.replace("{" + key + "}", value);

            if (sender instanceof ConsoleCommandSender) {
                line = line.replace(center, "");
                main.getRecords().rawRecord(line);
            }
            else {
                Player player = (Player) sender;
                line = line.replace("{PLAYER}", player.getName());
                sendMixed(player, line);
            }
        }
    }

    public void actionBar(Player player, String message) {
        actionBar.getMethod().send(player, message);
    }

    private boolean checkInts(String[] array) {
        if (array == null) return false;
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
        int[] i = checkInts(times) ? intArray(times) : new int[]{10, 50, 10};
        title.getMethod().send(player, message[0], subtitle, i[0], i[1], i[2]);
    }
}
