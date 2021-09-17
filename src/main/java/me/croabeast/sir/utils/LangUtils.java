package me.croabeast.sir.utils;

import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sir.SIR;
import me.croabeast.sir.handlers.ActBarNew;
import me.croabeast.sir.handlers.ActBarOld;
import me.croabeast.sir.handlers.TitleNew;
import me.croabeast.sir.handlers.TitleOld;
import me.croabeast.sir.interfaces.ActionBar;
import me.croabeast.sir.interfaces.TitleMain;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class LangUtils {

    private final SIR main;

    public boolean hasPAPI;
    public boolean hasUserLogin;
    public int getVersion;
    public String serverName;

    private final ActionBar actionBar;
    private final TitleMain titleMain;

    public LangUtils(SIR main) {
        this.main = main;
        String version = Bukkit.getBukkitVersion().split("-")[0];
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.hasUserLogin = Bukkit.getPluginManager().isPluginEnabled("UserLogin");
        this.getVersion = Integer.parseInt(version.split("\\.")[1]);
        this.serverName = Bukkit.getVersion().split("-")[1] + " " + version;
        actionBar = this.getVersion < 11 ? new ActBarOld(main) : new ActBarNew(main);
        titleMain = this.getVersion < 10 ? new TitleOld(main) : new TitleNew(main);
    }

    public String parseColor(String message) {
        return IridiumAPI.process(message);
    }

    public String parsePAPI(Player player, String message) {
        String papi = PlaceholderAPI.setPlaceholders(player, message);
        return parseColor((hasPAPI && player != null) ? papi : message);
    }

    public void sendCentered(Player player, String message) {
        message = parsePAPI(player, message);
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        for (char c : message.toCharArray()) {
            if (c == '\u00A7') previousCode = true;
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
        String center = main.getConfig().getString("options.center-prefix");
        if (center == null) center = "";
        if (message.startsWith(center)) sendCentered(player, message.replace(center, ""));
        else player.sendMessage(parsePAPI(player, message));
    }

    private List<String> toList(String path) {
        if(main.getLang().isList(path)) return main.getLang().getStringList(path);
        return new ArrayList<>(Collections.singletonList(main.getLang().getString(path)));
    }

    public void send(CommandSender sender, String path, String... values) {
        String key = main.getConfig().getString("values.messages-prefix");
        String prefix = main.getLang().getString("messages.main-prefix");
        if (key == null) key = ""; if (prefix == null) prefix = "";
        for (String msg : toList(path)) {
            if (msg == null || msg.equals("")) continue;
            msg = msg.startsWith(key) ? msg.replace(key, prefix) : msg;
            String[] keys = {"{ARG}", "{PERM}", "{PLAYER}", "{VERSION}"};
            msg = StringUtils.replaceEach(msg, keys, values);
            sendMixed((Player) sender, msg);
        }
    }

    public void actionBar(Player player, String message) {
        actionBar.send(player, message);
    }

    public void title(Player player, String[] message) {
        if (message.length == 0 || message.length > 2) return;
        String subtitle = message.length == 1 ? null : message[1];
        titleMain.send(player, message[0], subtitle);
    }

    public <E extends Enum<E>> boolean isEnum(Class<E> enumClass, String enumName) {
        if (enumName == null) return false;
        try {
            Enum.valueOf(enumClass, enumName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void sound(Player player, String sound) {
        if (!isEnum(Sound.class, sound)) return;
        player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
    }

    public void eventSend(Player player, String path) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] values = {player.getName(), player.getWorld() + ""};
        String split = main.getConfig().getString("options.line-splitter");
        for (String message : toList(path)) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            if (main.getConfig().getBoolean("options.send-console")) {
                if (path.contains("motd")) continue;
                message = parsePAPI(player, message);
                Bukkit.getConsoleSender().sendMessage("&e&lSIR &8> &f" + message);
            }
            if (message.startsWith("[ACTION-BAR]")) {
                message = message.substring(12);
                if (message.startsWith(" ")) message = message.substring(1);
                for (Player p : Bukkit.getOnlinePlayers()) actionBar(p, message);
            }
            else if (message.startsWith("[TITLE]")) {
                message = message.substring(7);
                if (message.startsWith(" ")) message = message.substring(1);
                if (split == null) split = ""; String sp = split;
                for (Player p : Bukkit.getOnlinePlayers())
                    title(p, message.split(Pattern.quote(sp)));
            }
            else for (Player p : Bukkit.getOnlinePlayers()) sendMixed(p, message);
        }
    }

    public void eventCommand(Player player, String path) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] values = {player.getName(), player.getWorld() + ""};
        for (String command : toList(path)) {
            if (command == null || command.equals("")) continue;
            command = StringUtils.replaceEach(command, keys, values);
            if (command.startsWith(" ")) command = command.substring(1);
            if (command.startsWith("[PLAYER]")) {
                command = command.substring(8);
                if (command.startsWith(" ")) command = command.substring(1);
                player.performCommand(command);
            }
            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
