package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;

public class FormatListener implements Listener {

    private final Application main;
    private final TextUtils text;
    private final EventUtils utils;

    private ConfigurationSection id;

    private String playerName;
    private String playerPrefix;
    private String playerSuffix;

    public FormatListener(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    private String parseFormat(String line) {
        String[] keys = {"{PREFIX}", "{PLAYER}", "{SUFFIX}"};
        String[] values = {playerPrefix, playerName, playerSuffix};
        return StringUtils.replaceEach(line, keys, values);
    }

    private String parseMessage(String line) {
        if (!id.getBoolean("color.normal", false)) line = stripBukkit(line);
        if (!id.getBoolean("color.special", false)) line = stripSpecial(line);
        if (!id.getBoolean("color.rgb", false)) line = stripRGB(line);
        return line;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChatFormatter(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        this.id = utils.lastSection(main.getChat(), player, "formats");
        String[] format = id.getString("format", "").split("(?i)\\{MESSAGE}");

        this.playerPrefix = id.getString("prefix", "");
        this.playerSuffix = id.getString("suffix", "");
        this.playerName = player.getName();

        if (!main.getChat().getBoolean("enabled")) return;

        if (id == null || format.length > 2) {
            main.getRecords().doRecord(player,
                    "&cCouldn't found any valid chat format, check your chat.yml");
            return;
        }

        String start = parseFormat(format[0]);
        String end = parseFormat(format.length == 1 ? "" : format[1]);
        String message = parseMessage(event.getMessage());

        if (message.length() == 0) {
            main.getRecords().doRecord(player,
                    "&cEmpty messages are not allowed in this module.");
            return;
        }

        event.setFormat(text.parsePAPI(player,start + message + end));
    }
}
