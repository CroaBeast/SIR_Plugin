package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.hooks.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import net.md_5.bungee.api.chat.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.SIRPlugin.*;
import static me.croabeast.sirplugin.modules.listeners.Formatter.KeysHandler.*;
import static me.croabeast.sirplugin.objects.FileCatcher.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class Formatter extends Module implements Listener {

    private final SIRPlugin main;
    private final EventUtils utils;
    
    HashMap<Player, Long> timedPlayers = new HashMap<>();

    public Formatter(SIRPlugin main) {
        this.main = main;
        this.utils = main.getEventUtils();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.FORMATS;
    }

    @Override
    public void registerModule() {
        SIRPlugin.registerListener(this);
    }

    @NotNull
    private Boolean getColored(ConfigurationSection id, String s) {
        Object object = getValue(id, "color." + s);
        return object != null && (Boolean) object;
    }

    private String parseMessage(ConfigurationSection id, String line) {
        if (!getColored(id, "normal")) line = IridiumAPI.stripBukkit(line);
        if (!getColored(id, "rgb")) line = IridiumAPI.stripRGB(line);
        if (!getColored(id, "special")) line = IridiumAPI.stripSpecial(line);
        line = line.replace("\\", "\\\\");
        return textUtils().removeSpace(line.replace("$", "\\$"));
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        ConfigurationSection id =
                utils.getSection(FORMATS.toFile(), player, "formats");

        if (id == null) {
            textUtils().sendMessageList(player, toList(LANG.toFile(), "chat.invalid-format"));
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

        if (!hover.isEmpty()) hover.replaceAll(line -> parseInsensitiveEach(line, keys, values));

        if (click != null)
            click = parsePAPI(player, parseInsensitiveEach(click, keys, values));

        if (StringUtils.isBlank(message) &&
                !MODULES.toFile().getBoolean("chat.allow-empty")) {
            event.setCancelled(true);
            textUtils().sendMessageList(player, toList(LANG.toFile(), "chat.empty-message"));
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

            String s = "data." + target.getUniqueId() + ".", x = player.getUniqueId() + "";
            List<String> list = IGNORE.toFile().getStringList(s + "chat");

            if (IGNORE.toFile().getBoolean(s + "all-chat") ||
                    (!list.isEmpty() && list.contains(x))) players.remove(target);
        }

        Integer timer = (Integer) getValue(id, "cooldown.time", 0);

        if (timer != null && timer > 0 && timedPlayers.containsKey(player)) {
            long rest = System.currentTimeMillis() - timedPlayers.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));
                String path = "cooldown.message";

                List<String> list = toList(isDef(id, path), path);
                textUtils().sendMessageList(player, list,
                        new String[] {"time"}, new String[] {time + ""});
                return;
            }
        }

        String result = parseInsensitiveEach(textUtils().removeSpace(format), keys, values);
        boolean isDefault = MODULES.toFile().getBoolean("chat.default-format");

        if (isDefault && !IS_JSON.apply(result) && hover.isEmpty() && click == null &&
                world == null && (radius == null || radius <= 0) && !Initializer.hasIntChat()) {
            event.setFormat(textUtils().centeredText(player, result.replace("%", "%%")));
            return;
        }

        event.setCancelled(true);

        String path = "chat.simple-logger.", s = main.getConfig().getString(path + "format");
        LogUtils.doLog(
                parsePAPI(player, !MODULES.toFile().getBoolean(path + "enabled")
                        ? result : parseInsensitiveEach(s, keys, values))
        );

        if (Initializer.hasDiscord())
            new Message(player, "chat", keys, values).sendMessage();

        BaseComponent[] component = textUtils().stringToJson(player, result, click, hover);
        players.forEach(p -> p.spigot().sendMessage(component));

        if (timer != null && timer > 0) timedPlayers.put(player, System.currentTimeMillis());
    }

    public static class KeysHandler {

        private static final SIRPlugin main = SIRPlugin.getInstance();
        private static final EventUtils utils = main.getEventUtils();

        @Nullable
        public static ConfigurationSection isDef(ConfigurationSection id, String path) {
            ConfigurationSection d = MODULES.toFile().getConfigurationSection("chat.default");
            return (d != null && d.getBoolean("enabled") && !id.contains(path)) ? d : id;
        }

        @Nullable
        public static Object getValue(ConfigurationSection id, String path) {
            ConfigurationSection d = isDef(id, path);
            return d != null && d.isSet(path) ? d.get(path) : null;
        }

        @Nullable
        public static Object getValue(ConfigurationSection id, String path, @Nullable Object def) {
            ConfigurationSection d = isDef(id, path);
            return d != null && d.isSet(path) ? d.get(path, def) : def;
        }

        @Nullable
        public static String getChatValue(Player player, String path, Object def) {
            String value = (String) getValue(utils.getSection(
                    FORMATS.toFile(), player, "formats"), path, def);
            return StringUtils.isBlank(value) ? null : value;
        }
    }
}
