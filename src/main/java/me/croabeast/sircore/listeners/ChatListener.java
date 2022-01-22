package me.croabeast.sircore.listeners;

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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;

public class ChatListener implements Listener {

    private final Application main;
    private final Recorder recorder;

    private final TextUtils text;
    private final EventUtils utils;

    private ConfigurationSection id;

    private String playerName,
            playerPrefix, playerSuffix;

    public ChatListener(Application main) {
        this.main = main;
        this.recorder = main.getRecorder();

        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();

        main.registerListener(this);
    }

    private List<String> hoverList() {
        String line = id.getString("hover");
        List<String> result = !id.isList("hover") ?
                (line == null ? null : Collections.singletonList(line)) :
                id.getStringList("hover");
        if (result == null || result.isEmpty()) return null;

        List<String> color = new ArrayList<>();
        for (String s : result) color.add(parseFormat(s));
        return color;
    }

    private String parseFormat(String line) {
        String[] keys = {"{PREFIX}", "{PLAYER}", "{SUFFIX}"};
        String[] values = {playerPrefix, playerName, playerSuffix};
        return StringUtils.replaceEach(line, keys, values);
    }

    @Nullable
    private String parseMessage(String line) {
        if (!id.getBoolean("color.normal")) line = stripBukkit(line);
        if (!id.getBoolean("color.special")) line = stripSpecial(line);
        if (!id.getBoolean("color.rgb")) line = stripRGB(line);

        try {
            while (line.charAt(0) == ' ') line = line.substring(1);
        } catch (IndexOutOfBoundsException e) {
            line = null;
        }
        return parseFormat(line);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onChatFormatter(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!main.getChat().getBoolean("enabled")) return;

        Player player = event.getPlayer();
        this.id = utils.resultSection(main.getChat(), player, "formats");

        assert id != null;
        String rawFormat = id.getString("format", "");
        String[] format = rawFormat.split("(?i)\\{MESSAGE}", 2);

        this.playerPrefix = id.getString("prefix", "");
        this.playerSuffix = id.getString("suffix", "");
        this.playerName = player.getName();

        String start = parseFormat(format[0]);
        String end = format.length == 1 ? "" : parseFormat(format[1]);
        String message = parseMessage(event.getMessage());

        if (StringUtils.isBlank(message) &&
                !main.getConfig().getBoolean("chat.allow-empty")) {
            text.send(null, "chat.empty-message", (String) null, null);
            text.send(player, "chat.empty-message", (String) null, null);
            return;
        }

        String resultFormat = text.colorize(player, start + message + end);

        TextComponent result = new Message(main, player, resultFormat).
                setExecutor(parseFormat(id.getString("click.execute"))).
                setSuggestion(parseFormat(id.getString("click.suggest"))).
                setURL(parseFormat(id.getString("click.openURL"))).
                setHover(hoverList()).getBuilder();

        if (id == null || result == null) {
            text.send(null, "chat.invalid-format", (String) null, null);
            text.send(player, "chat.invalid-format", (String) null, null);
            return;
        }

        event.setCancelled(true);

        String path = "chat.simple-logger.";
        if (main.getConfig().getBoolean(path + "enabled")) {
            String simple = main.getConfig().getString(path + "format");
            message = message == null ? "" : message;
            simple = parseFormat(simple).replace("{MESSAGE}", message);
            recorder.rawRecord(text.parsePAPI(player, simple));
        }
        else recorder.rawRecord(resultFormat);

        if (main.getInitializer().DISCORD) {
            String[] values = {stripAll(playerPrefix), stripAll(playerSuffix)};
            DiscordMsg msg = new DiscordMsg(main, player, "chat",
                    new String[]{"{PREFIX}", "{SUFFIX}"}, values).
                    setMessage(stripAll(event.getMessage()));
            if (main.getInitializer().discordServer() != null) msg.sendMessage();
        }

        main.everyPlayer().forEach(p -> p.spigot().sendMessage(result));
    }
}
