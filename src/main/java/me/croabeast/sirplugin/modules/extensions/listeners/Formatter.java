package me.croabeast.sirplugin.modules.extensions.listeners;

import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.hooks.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import net.md_5.bungee.api.chat.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.modules.extensions.listeners.Formatter.KeysHandler.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class Formatter extends BaseModule implements Listener {

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
        line = line.replace("%", "%%");
        return removeSpace(line.replace("$", "\\$"));
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        ConfigurationSection id =
                utils.getSection(main.getFormats(), player, "formats");

        if (id == null) {
            sendFileMsg(player, "chat.invalid-format");
            return;
        }

        String message = parseMessage(id, event.getMessage()), name = player.getName(),
                format = (String) getValue(id, "format", ""),
                prefix = (String) getValue(id, "prefix", ""),
                suffix = (String) getValue(id, "suffix", ""),
                world = (String) getValue(id, "world", null),
                click = (String) getValue(id, "click", null);

        ConfigurationSection hoverID = isDef(id, "hover");
        List<String> hover = hoverID == null ? Collections.emptyList() :
                hoverID.getStringList("hover");

        String[] keys = {"prefix", "suffix", "player", "message"},
                values = {prefix, suffix, name, message};

        if (!hover.isEmpty()) {
            for (int i = 0; i < hover.size(); i++)
                hover.set(i, parseInsensitiveEach(hover.get(i), keys, values));
        }

        if (click != null)
            click = parsePAPI(player, parseInsensitiveEach(click, keys, values));

        if (StringUtils.isBlank(message) &&
                !main.getModules().getBoolean("chat.allow-empty")) {
            event.setCancelled(true);
            sendFileMsg(player, "chat.empty-message");
            return;
        }

        if (Initializer.hasLogin() && !utils.getLoggedPlayers().contains(player)) return;

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
            List<String> list = main.getIgnore().getStringList(s + "chat");

            if (main.getIgnore().getBoolean(s + "all-chat") ||
                    (!list.isEmpty() && list.contains(x))) players.remove(target);
        }

        Integer timer = (Integer) getValue(id, "cooldown.time", 0);

        if (timer != null && timer > 0 && timedPlayers.containsKey(player)) {
            long rest = System.currentTimeMillis() - timedPlayers.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));
                String path = "cooldown.message";

                List<String> list = fileList(isDef(id, path), path);
                sendFileMsg(player, list, "time", time + "");
                return;
            }
        }

        String result = parseInsensitiveEach(removeSpace(format), keys, values);

        boolean isDefault = main.getModules().getBoolean("chat.default-format");

        if (isDefault && !JsonMsg.isValidJson(result) && hover.isEmpty() && click == null &&
                world == null && (radius == null || radius <= 0) && !Initializer.hasIntChat()) {
            event.setFormat(JsonMsg.centeredText(player, result));
            return;
        }

        event.setCancelled(true);

        String path = "chat.simple-logger.", s = main.getConfig().getString(path + "format");
        LogUtils.doLog(
                parsePAPI(player, !main.getModules().getBoolean(path + "enabled")
                        ? result : parseInsensitiveEach(s, keys, values))
        );

        if (Initializer.hasDiscord())
            new Message(player, "chat", keys, values).sendMessage();

        BaseComponent[] component = new JsonMsg(player, result, click, hover).build();
        players.forEach(p -> p.spigot().sendMessage(component));

        if (timer != null && timer > 0 && !timedPlayers.containsKey(player)) {
            timedPlayers.put(player, System.currentTimeMillis());
            new BukkitRunnable() {
                @Override
                public void run() {
                    timedPlayers.remove(player);
                }
            }.runTaskLater(main, timer * 20);
        }
    }

    public static class KeysHandler {

        private static final SIRPlugin main = SIRPlugin.getInstance();
        private static final EventUtils utils = main.getEventUtils();

        @Nullable
        public static ConfigurationSection isDef(ConfigurationSection id, String path) {
            ConfigurationSection d = main.getModules().getConfigurationSection("chat.default");
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
                    main.getFormats(), player, "formats"), path, def);
            return StringUtils.isBlank(value) ? null : value;
        }
    }
}
