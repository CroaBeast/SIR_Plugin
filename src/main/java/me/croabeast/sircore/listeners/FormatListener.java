package me.croabeast.sircore.listeners;

import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sircore.*;
import me.croabeast.sircore.hooks.DiscordMsg;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;

public class FormatListener implements Listener {

    private final Application main;
    private final Records records;
    private final TextUtils text;
    private final EventUtils utils;

    private ConfigurationSection id;

    private String playerName;
    private String playerPrefix;
    private String playerSuffix;

    public FormatListener(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    private List<String> idList() {
        String line = id.getString("hover");
        List<String> result = !id.isList("hover") ?
                (line == null ? null : Collections.singletonList(line)) :
                id.getStringList("hover");
        if (result == null) return null;

        List<String> color = new ArrayList<>();
        for (String s : result) color.add(parseFormat(s));
        return color;
    }

    private String parseFormat(String line) {
        String[] keys = {"{PREFIX}", "{PLAYER}", "{SUFFIX}"};
        String[] values = {playerPrefix, playerName, playerSuffix};
        return StringUtils.replaceEach(line, keys, values);
    }

    private String parseMessage(String line) {
        if (!id.getBoolean("color.normal")) line = stripBukkit(line);
        if (!id.getBoolean("color.special")) line = stripSpecial(line);
        if (!id.getBoolean("color.rgb")) line = stripRGB(line);
        return parseFormat(line);
    }

    @EventHandler()
    private void onChatFormatter(AsyncPlayerChatEvent event) {
        if (!main.getChat().getBoolean("enabled")) return;

        Player player = event.getPlayer();
        this.id = utils.lastSection(main.getChat(), player, "formats");

        assert id != null;
        String rawFormat = id.getString("format", "");
        String[] format = rawFormat.split("(?i)\\{MESSAGE}");

        this.playerPrefix = id.getString("prefix", "");
        this.playerSuffix = id.getString("suffix", "");
        this.playerName = player.getName();

        String start = parseFormat(format[0]);
        String end = format.length == 1 ? "" : parseFormat(format[1]);
        String message = parseMessage(event.getMessage());

        String resultFormat = text.parse(player, start + message + end);

        TextComponent result = new Message(main, player, resultFormat)
                .setExecutor(parseFormat(id.getString("click.execute")))
                .setSuggestion(parseFormat(id.getString("click.suggest")))
                .setURL(parseFormat(id.getString("click.openURL")))
                .setHover(idList()).getBuilder();

        if (id == null || format.length > 2 || result == null) {
            main.getRecords().doRecord(player,
                    "<P> &cCouldn't found any valid chat format, check your chat.yml");
            return;
        }

        event.setCancelled(true);
        records.rawRecord(resultFormat);

        if (main.getInitializer().DISCORD) {
            String[] values = {IridiumAPI.stripAll(playerPrefix), IridiumAPI.stripAll(playerSuffix)};
            String resultMessage = IridiumAPI.stripAll(event.getMessage());
            DiscordMsg msg = new DiscordMsg(main, player, "chat",
                    new String[]{"{PREFIX}", "{SUFFIX}"}, values).setMessage(resultMessage);
            if (main.getInitializer().getDiscordServer() != null) msg.sendMessage();
        }

        main.everyPlayer().forEach(p -> p.spigot().sendMessage(result));
    }
}
