package me.croabeast.sirplugin.module.listener;

import com.Zrips.CMI.Containers.CMIUser;
import lombok.var;
import me.croabeast.beanslib.builder.JsonBuilder;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.module.EmojiParser;
import me.croabeast.sirplugin.module.MentionParser;
import me.croabeast.sirplugin.object.chat.ChatFormat;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.object.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import static me.croabeast.beanslib.utility.TextUtils.*;

@SuppressWarnings("deprecation")
public class Formats extends SIRViewer {

    private static final HashMap<Player, Long> TIMED_PLAYERS = new HashMap<>();

    public Formats() {
        super("formats");
    }

    @Nullable
    public static String getTag(Player player, boolean isPrefix) {
        ChatFormat format = new ChatFormat(player);
        String value = isPrefix ? format.getPrefix() : format.getSuffix();
        return StringUtils.isBlank(value) ? null : value;
    }

    static String parseMessage(ChatFormat format, String line) {
        if (StringUtils.isBlank(line)) return line;

        if (!format.isNormalColored()) line = IridiumAPI.stripBukkit(line);
        if (!format.isSpecialColored()) line = IridiumAPI.stripRGB(line);
        if (!format.isRgbColored()) line = IridiumAPI.stripSpecial(line);

        return TextUtils.removeSpace(line);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        final var player = event.getPlayer();

        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            var id = UUIDManager.get().getUUID(player.getName());
            if (PunishmentManager.get().isMuted(id)) {
                event.setCancelled(true);
                return;
            }
        }

        if (Exceptions.isPluginEnabled("CMI") &&
                CMIUser.getUser(player).isMuted()) {
            event.setCancelled(true);
            return;
        }

        if (LoginHook.isEnabled() &&
                !JoinQuit.LOGGED_PLAYERS.contains(player)) {
            event.setCancelled(true);
            return;
        }

        ChatFormat chatFormat;

        try {
            chatFormat = new ChatFormat(player);
        } catch (Exception e) {
            var list = TextUtils.toList(FileCache.LANG.get(), "chat.invalid-format");
            event.setCancelled(true);

            LangUtils.getSender().setTargets(player).send(list);
            return;
        }

        var message = parseMessage(chatFormat, event.getMessage());

        String[] keys = {"{prefix}", "{suffix}", "{player}", "{message}"},
                values = {chatFormat.getPrefix(), chatFormat.getSuffix(), player.getName(), message};

        var hover = chatFormat.getHover();
        if (hover != null && !hover.isEmpty())
            hover.replaceAll(s -> ValueReplacer.forEach(s, keys, values));

        var click = chatFormat.getClick();
        if (click != null)
            click = parsePAPI(player, ValueReplacer.forEach(click, keys, values));

        if (StringUtils.isBlank(message) &&
                !FileCache.MODULES.getValue("chat.allow-empty", false))
        {
            var list = TextUtils.toList(
                    FileCache.LANG.get(),
                    "chat.empty-message"
            );
            event.setCancelled(true);

            LangUtils.getSender().setTargets(player).send(list);
            return;
        }

        int r = chatFormat.getRadius();
        var worldName = chatFormat.getWorld();

        var players = new ArrayList<Player>();
        var world = worldName != null ? Bukkit.getWorld(worldName) : null;

        if (r > 0) {
            for (var ent : player.getNearbyEntities(r, r, r))
                if (ent instanceof Player) players.add((Player) ent);
        }
        else players.addAll(world != null ?
                world.getPlayers() : Bukkit.getOnlinePlayers());

        for (var t : new ArrayList<>(players)) {
            if (t == player) continue;
            if (PlayerUtils.isIgnoring(t, player, true))
                players.remove(t);
        }

        int timer = chatFormat.cooldownTime();

        if (timer > 0 && TIMED_PLAYERS.containsKey(player)) {
            long rest = System.currentTimeMillis() - TIMED_PLAYERS.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));

                LangUtils.getSender().setTargets(player).
                        setKeys("{time}").setValues(time).
                        send(chatFormat.cooldownMessage());
                return;
            }
        }

        var format = TextUtils.STRIP_FIRST_SPACES.apply(chatFormat.getFormat());

        String result = MentionParser.parseMention(player,
                EmojiParser.parseEmojis(player,
                ValueReplacer.forEach(format, keys, values))
        );

        if (FileCache.MODULES.getValue("chat.default-format", false) ||
                chatFormat.isDefault())
        {
            result = result.replace("\\", "\\\\").replace("$", "\\$");
            result = parsePAPI(player, stripJson(result));

            event.setFormat(SIRPlugin.getUtils().
                    centerMessage(null, player, result.replace("%", "%%")));
            return;
        }

        String loggerPath = "chat.simple-logger.", logger = result;
        event.setCancelled(true);

        if (FileCache.MODULES.getValue(loggerPath + "enabled", false)) {
            logger = ValueReplacer.forEach(
                    FileCache.MODULES.getValue(loggerPath + "format", ""),
                    keys, values
            );
        }

        LogUtils.doLog(parsePAPI(player, logger));

        if (Initializer.hasDiscord())
            new DiscordSender(player, "chat").setKeys(keys).setValues(values).send();

        if (!result.matches("(?i)^\\[json]")) {
            String rt = result, c = click;
            players.forEach(p -> new JsonBuilder(p, player, rt).send(c, hover));
        }
        else Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), removeSpace(result.substring(6)));

        if (timer > 0) TIMED_PLAYERS.put(player, System.currentTimeMillis());
    }
}
