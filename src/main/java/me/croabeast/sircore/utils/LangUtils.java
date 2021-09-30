package me.croabeast.sircore.utils;

import me.clip.placeholderapi.*;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.handlers.ActBar17;
import me.croabeast.sircore.handlers.ActBar10;
import me.croabeast.sircore.handlers.Title17;
import me.croabeast.sircore.handlers.Title9;
import me.croabeast.sircore.interfaces.ActionBar;
import me.croabeast.sircore.interfaces.TitleMain;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class LangUtils {

    private final MainClass main;

    public int getVersion;
    public String serverName;

    private ActionBar actionBar;
    private TitleMain titleMain;

    public LangUtils(MainClass main) {
        this.main = main;
        String version = Bukkit.getBukkitVersion().split("-")[0];
        this.getVersion = Integer.parseInt(version.split("\\.")[1]);
        this.serverName = Bukkit.getVersion().split("-")[1] + " " + version;
    }

    public void loadLangClasses() {
        actionBar = this.getVersion < 11 ? new ActBar10() : new ActBar17();
        titleMain = this.getVersion < 10 ? new Title9() : new Title17();
    }

    public String parseColor(String message) {
        return IridiumAPI.process(message);
    }

    public String parsePAPI(Player player, String message) {
        String papi = PlaceholderAPI.setPlaceholders(player, message);
        return parseColor((main.hasPAPI && player != null) ? papi : message);
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
        String center = main.getConfig().getString("options.center-prefix", "");
        if (message.startsWith(center)) sendCentered(player, message.replace(center, ""));
        else player.sendMessage(parsePAPI(player, message));
    }

    private List<String> toList(String path) {
        if(main.getLang().isList(path)) return main.getLang().getStringList(path);
        return new ArrayList<>(Collections.singletonList(main.getLang().getString(path)));
    }

    public void send(CommandSender sender, String path, String[] keys, String... values) {
        String key = main.getConfig().getString("options.prefix-in-config", "");
        String prefix = main.getLang().getString("main-prefix", "");
        String center = main.getConfig().getString("options.center-prefix", "");

        for (String msg : toList(path)) {
            if (msg == null || msg.equals("")) continue;
            msg = msg.startsWith(key) ? msg.replace(key, prefix) : msg;
            msg = StringUtils.replaceEach(msg, keys, values);

            if (!(sender instanceof Player)) {
                if (msg.startsWith(center)) msg = msg.replace(center,"");
                sender.sendMessage(parseColor(msg));
            }
            else sendMixed((Player) sender, msg);
        }
    }

    public void actionBar(Player player, String message) { actionBar.send(player, message); }

    public void title(Player player, String[] message) {
        if (message.length == 0 || message.length > 2) return;
        String subtitle = message.length == 1 ? "" : message[1];
        titleMain.send(player, message[0], subtitle);
    }
}
