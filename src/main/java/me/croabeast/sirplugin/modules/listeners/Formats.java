package me.croabeast.sirplugin.modules.listeners;

import com.Zrips.CMI.Containers.*;
import me.croabeast.beanslib.utilities.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.hooks.discord.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.*;
import me.croabeast.sirplugin.utilities.*;
import me.croabeast.sirplugin.utilities.LogUtils;
import net.md_5.bungee.api.chat.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.beanslib.utilities.TextUtils.*;
import static me.croabeast.sirplugin.SIRPlugin.*;
import static me.croabeast.sirplugin.utilities.LangUtils.*;

public class Formats extends SIRViewer {

    HashMap<Player, Long> timedPlayers = new HashMap<>();

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.FORMATS;
    }

    @Nullable
    private static ConfigurationSection isDef(ConfigurationSection id, String path) {
        if (id == null) return null;

        ConfigurationSection d = FileCache.MODULES.getSection("chat.default");
        if (d == null) return id;

        return d.getBoolean("enabled") && !id.contains(path) ? d : id;
    }

    @Nullable
    private static Object getValue(ConfigurationSection id, String path, Object def) {
        ConfigurationSection d = isDef(id, path);
        return d != null && d.isSet(path) ? d.get(path, def) : def;
    }

    @Nullable
    public static String getTag(Player player, String path) {
        ConfigurationSection id = PlayerUtils.getSection(FileCache.FORMATS.get(), player, "formats");
        String value = (String) getValue(id, path, "");
        return StringUtils.isBlank(value) ? null : value;
    }

    private boolean notColor(ConfigurationSection id, String s) {
        Object object = id != null && id.isSet("color." + s) ? id.get("color." + s) : null;
        return object == null || !((Boolean) object);
    }

    private String parseMessage(ConfigurationSection id, String line) {
        if (notColor(id, "normal")) line = IridiumAPI.stripBukkit(line);
        if (notColor(id, "rgb")) line = IridiumAPI.stripRGB(line);
        if (notColor(id, "special")) line = IridiumAPI.stripSpecial(line);

        line = line.replace("\\", "\\\\");
        return getUtils().removeSpace(line.replace("$", "\\$"));
    }

    private String onMention(Player player, String line) {
        if (!Identifier.MENTIONS.isEnabled()) return line;

        ConfigurationSection id = PlayerUtils.getSection(FileCache.MENTIONS.get(), player, "mentions");
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

        getUtils().sendMessageList(player, id, "messages.sender", keys, values);
        getUtils().sendMessageList(target, id, "messages.receiver", keys, values);

        PlayerUtils.playSound(player, id.getString("sound.sender"));
        PlayerUtils.playSound(target, id.getString("sound.receiver"));

        String output = id.getString("value", "&b{prefix}{receiver}");

        List<String> hoverList = TextUtils.toList(id, "hover");
        String click = id.getString("click");

        if (!hoverList.isEmpty() || click != null) {
            String format = "";

            if (!hoverList.isEmpty()) {
                String list = String.join(getUtils().lineSeparator(), hoverList);
                format += "<hover=[" + list.replaceAll("\\\\Q", "").replaceAll("\\\\E", "") + "]";
            }

            if (click != null) {
                String[] array = click.split(":", 2);
                format += (!hoverList.isEmpty() ? "|" : "<") + array[0] + "=[" + array[1] + "]>";
            }
            else format += ">";

            if (StringUtils.isNotBlank(format)) output = format + output + "</text>";
        }

        String result = TextUtils.replaceInsensitiveEach(output, keys, values),
                regex = prefix + target.getName();

        Matcher match = Pattern.compile("(?i)" + regex).matcher(line);
        if (match.find())
            line = line.replace(match.group(), result + IridiumAPI.getLastColor(line, regex, true));

        return line;
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("CMI") &&
                CMIUser.getUser(event.getPlayer()).isMuted()) return;

        Player player = event.getPlayer();
        ConfigurationSection id =
                PlayerUtils.getSection(FileCache.FORMATS.get(), player, "formats");

        if (id == null) {
            getUtils().sendMessageList(player, toList(FileCache.LANG.get(), "chat.invalid-format"));
            return;
        }

        String message = parseMessage(id, event.getMessage()),
                format = (String) getValue(id, "format", ""),
                prefix = (String) getValue(id, "prefix", ""),
                suffix = (String) getValue(id, "suffix", ""),
                world = (String) getValue(id, "world", null),
                click = (String) getValue(id, "click", null);

        ConfigurationSection hoverID = isDef(id, "hover");
        List<String> hover = hoverID == null ? Collections.emptyList() :
                hoverID.getStringList("hover");

        String[] keys = {"prefix", "suffix", "player", "message"},
                values = {prefix, suffix, player.getName(), message};

        if (!hover.isEmpty()) hover.replaceAll(line -> parseInternalKeys(line, keys, values));

        if (click != null)
            click = parsePAPI(player, parseInternalKeys(click, keys, values));

        if (StringUtils.isBlank(message) &&
                !FileCache.MODULES.get().getBoolean("chat.allow-empty")) {
            event.setCancelled(true);
            getUtils().sendMessageList(player, toList(FileCache.LANG.get(), "chat.empty-message"));
            return;
        }

        Integer radius = (Integer) getValue(id, "radius", 0);

        List<Player> players = new ArrayList<>();
        World w = world != null ? Bukkit.getWorld(world) : null;

        if (radius != null && radius > 0) {
            for (Entity ent : player.getNearbyEntities(radius, radius, radius))
                if (ent instanceof Player) players.add((Player) ent);
        }
        else players.addAll(w != null ? w.getPlayers() : Bukkit.getOnlinePlayers());

        List<Player> targets = new ArrayList<>(players);

        for (Player target : targets) {
            if (target == player) continue;
            if (PlayerUtils.isIgnoredFrom(target, player, true))
                players.remove(target);
        }

        Integer timer = (Integer) getValue(id, "cooldown.time", 0);

        if (timer != null && timer > 0 && timedPlayers.containsKey(player)) {
            long rest = System.currentTimeMillis() - timedPlayers.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));
                String path = "cooldown.message";

                List<String> list = toList(isDef(id, path), path);
                getUtils().sendMessageList(player, list,
                        new String[] {"{time}"}, new String[] {time + ""});
                return;
            }
        }

        String result = parseInternalKeys(getUtils().removeSpace(format), keys, values);
        result = onMention(player, result);

        boolean isDefault = FileCache.MODULES.get().getBoolean("chat.default-format");

        if (isDefault && !IS_JSON.apply(result) && hover.isEmpty() && click == null &&
                world == null && (radius == null || radius <= 0) &&
                !Bukkit.getPluginManager().isPluginEnabled("InteractiveChat")) {
            event.setFormat(getUtils().centeredText(player, result.replace("%", "%%")));
            return;
        }

        event.setCancelled(true);

        String loggerPath = "chat.simple-logger.", logger = result;
        if (FileCache.MODULES.get().getBoolean(loggerPath + "enabled")) {
            logger = FileCache.MODULES.get().getString(loggerPath + "format", "");
            logger = parseInternalKeys(logger, keys, values);
        }

        LogUtils.doLog(parsePAPI(player, logger));

        if (Initializer.hasDiscord())
            new Message(player, "chat", keys, values).sendMessage();

        BaseComponent[] component = getUtils().stringToJson(player, result, click, hover);
        players.forEach(p -> p.spigot().sendMessage(component));

        if (timer != null && timer > 0) timedPlayers.put(player, System.currentTimeMillis());
    }
}
