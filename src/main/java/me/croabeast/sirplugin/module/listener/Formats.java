package me.croabeast.sirplugin.module.listener;

import com.Zrips.CMI.Containers.*;
import me.croabeast.beanslib.object.JsonMessage;
import me.croabeast.beanslib.utility.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.hook.discord.*;
import me.croabeast.sirplugin.module.EmParser;
import me.croabeast.sirplugin.object.ChatFormat;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.utility.*;
import me.croabeast.sirplugin.utility.LogUtils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.beanslib.utility.TextUtils.*;
import static me.croabeast.sirplugin.utility.LangUtils.*;

public class Formats extends SIRViewer {

    private final HashMap<Player, Long> TIMED_PLAYERS = new HashMap<>();

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.FORMATS;
    }

    @Nullable
    public static String getTag(Player player, boolean isPrefix) {
        ChatFormat format = new ChatFormat(player);
        String value = isPrefix ? format.getPrefix() : format.getSuffix();
        return StringUtils.isBlank(value) ? null : value;
    }

    private String parseMessage(ChatFormat format, String line) {
        if (StringUtils.isBlank(line)) return line;

        if (!format.isNormalColored()) line = IridiumAPI.stripBukkit(line);
        if (!format.isSpecialColored()) line = IridiumAPI.stripRGB(line);
        if (!format.isRgbColored()) line = IridiumAPI.stripSpecial(line);

        return SIRPlugin.getUtils().
                removeSpace(line.replace("\\", "\\\\").replace("$", "\\$"));
    }

    private String onMention(Player player, String line) {
        if (!Identifier.MENTIONS.isEnabled()) return line;
        LangUtils utils = SIRPlugin.getUtils();

        ConfigurationSection id = FileCache.MENTIONS.permSection(player, "mentions");
        if (id == null) return line;

        String prefix = id.getString("prefix");
        if (StringUtils.isBlank(prefix)) return line;

        Player target = null;

        for (String word : line.split(" ")) {
            Matcher matcher = Pattern.compile("(?i)" + prefix).matcher(word);
            if (!matcher.find()) continue;

            String match = matcher.group();
            word = word.substring(word.lastIndexOf(match) + match.length());
            target = PlayerUtils.getClosestPlayer(word);
        }

        if (target == null || player == target) return line;
        if (PlayerUtils.isIgnoredFrom(target, player, true)) return line;

        String[] keys = {"{sender}", "{receiver}", "{prefix}"},
                values = {player.getName(), target.getName(), prefix};

        utils.sendMessageList(player, id, "messages.sender", keys, values);
        utils.sendMessageList(target, id, "messages.receiver", keys, values);

        PlayerUtils.playSound(player, id.getString("sound.sender"));
        PlayerUtils.playSound(target, id.getString("sound.receiver"));

        String output = id.getString("value", "&b{prefix}{receiver}");

        List<String> hoverList = TextUtils.toList(id, "hover");
        String click = id.getString("click");

        if (!hoverList.isEmpty() || click != null) {
            String format = "";

            if (!hoverList.isEmpty()) {
                String list = String.join(utils.lineSeparator(), hoverList);
                format += "<hover:\"" + list.replaceAll("\\\\Q", "").replaceAll("\\\\E", "") + "\"";
            }

            if (click != null) {
                String[] array = click.split(":", 2);
                format += (!hoverList.isEmpty() ? "|" : "<") + array[0] + ":\"" + array[1] + "\">";
            }
            else format += ">";

            if (StringUtils.isNotBlank(format)) output = format + output + "</text>";
        }

        String result = TextUtils.replaceInsensitiveEach(output, keys, values),
                regex = prefix + target.getName();

        Matcher match = Pattern.compile("(?i)" + regex).matcher(line);
        if (match.find())
            line = line.replace(match.group(), result +
                    IridiumAPI.getLastColor(line, regex, true, true));

        return line;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        LangUtils utils = SIRPlugin.getUtils();
        Player player = event.getPlayer();

        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            String id = UUIDManager.get().getUUID(player.getName());
            if (PunishmentManager.get().isMuted(id)) return;
        }

        if (Exceptions.isPluginEnabled("CMI") &&
                CMIUser.getUser(player).isMuted()) return;

        ChatFormat format;

        try {
            format = new ChatFormat(player);
        } catch (Exception e) {
            utils.sendMessageList(player, FileCache.LANG.get(), "chat.invalid-format");
            return;
        }

        String message = parseMessage(format, event.getMessage());

        String[] keys = {"prefix", "suffix", "player", "message"},
                values = {format.getPrefix(), format.getSuffix(), player.getName(), message};

        List<String> hover = format.getHover();
        if (!hover.isEmpty())
            hover.replaceAll(line -> parseInternalKeys(line, keys, values));

        String click = format.getClick();
        if (click != null)
            click = parsePAPI(player, parseInternalKeys(click, keys, values));

        if (StringUtils.isBlank(message) &&
                !FileCache.MODULES.get().getBoolean("chat.allow-empty")) {
            event.setCancelled(true);
            utils.sendMessageList(player, FileCache.LANG.get(), "chat.empty-message");
            return;
        }

        int radius = format.getRadius();
        String worldName = format.getWorld();

        List<Player> players = new ArrayList<>();
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;

        if (radius > 0) {
            for (Entity ent : player.getNearbyEntities(radius, radius, radius))
                if (ent instanceof Player) players.add((Player) ent);
        }
        else players.addAll(world != null ?
                world.getPlayers() : Bukkit.getOnlinePlayers());

        List<Player> targets = new ArrayList<>(players);

        for (Player target : targets) {
            if (target == player) continue;
            if (PlayerUtils.isIgnoredFrom(target, player, true))
                players.remove(target);
        }

        int timer = format.cooldownTime();

        if (timer > 0 && TIMED_PLAYERS.containsKey(player)) {
            long rest = System.currentTimeMillis() - TIMED_PLAYERS.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));

                utils.sendMessageList(player, format.cooldownMessage(),
                        new String[] {"{time}"}, new String[] {time + ""});
                return;
            }
        }

        String result = parseInternalKeys(utils.removeSpace(format.getFormat()), keys, values);
        result = onMention(player, EmParser.parseEmojis(result));

        boolean isDefault = FileCache.MODULES.get().getBoolean("chat.default-format");

        if (isDefault && !IS_JSON.apply(result) && hover.size() == 0 &&
                click == null && world == null && radius <= 0 &&
                !Bukkit.getPluginManager().isPluginEnabled("InteractiveChat")) {

            result = parsePAPI(player, result);
            event.setFormat(utils.centerMessage(player, result.replace("%", "%%")));
            return;
        }

        event.setCancelled(true);

        String loggerPath = "chat.simple-logger.", logger = result;

        if (FileCache.MODULES.get().getBoolean(loggerPath + "enabled")) {
            logger = parseInternalKeys(
                    FileCache.MODULES.get().getString(loggerPath + "format", ""),
                    keys, values
            );
        }

        LogUtils.doLog(parsePAPI(player, logger));

        if (Initializer.hasDiscord())
            new DiscordMsg(player, "chat", keys, values).send();

        if (!result.matches("(?i)^\\[JSON]")) {
            String r = result, c = click;
            players.forEach(p -> new JsonMessage(utils, player, r).send(p, c, hover));
        }
        else Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), removeSpace(result.substring(6)));

        if (timer > 0) TIMED_PLAYERS.put(player, System.currentTimeMillis());
    }
}
