package me.croabeast.sircore.utils;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import me.croabeast.sircore.MainClass;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EventUtils {

    private final MainClass main;
    private final LangUtils langUtils;

    public EventUtils(MainClass main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
    }

    private String color(String msg, Player player, boolean isColor) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] v = {player.getName(), player.getWorld().getName()};

        String message = StringUtils.replaceEach(msg, keys, v);
        return isColor ? langUtils.parsePAPI(player, message) : message;
    }

    private String setUp(String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    private boolean essVanish(Player player, boolean join) {
        Essentials ess = (Essentials) main.plugin("Essentials");
        if (ess == null) return false;

        boolean isJoin = join && hasPerm(player, "essentials.silentjoin.vanish");
        return (ess.getUser(player).isVanished()) || isJoin;
    }

    private boolean cmiVanish(Player player) {
        if (main.plugin("CMI") == null) return false;
        return CMIUser.getUser(player).isVanished();
    }

    public boolean isVanished(Player player, boolean join) {
        return essVanish(player, join) || cmiVanish(player);
    }

    private boolean hasPerm(Player player, String perm) {
        return !perm.matches("(?i)DEFAULT") &&
                (main.hasVault ?
                        main.getPerms().playerHas(null, player, perm) :
                        player.hasPermission(perm));
    }

    public ConfigurationSection lastSection(Player player, String path) {
        String maxPerm = ""; int highest = 0; ConfigurationSection finalId = null;
        ConfigurationSection section = main.getMessages().getConfigurationSection(path);
        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");
            int before = perm.matches("(?i)DEFAULT") ? 0 : 1;
            int priority = id.getInt("priority", before);

            if (priority > highest) { highest = priority; maxPerm = perm; }
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");

            if (hasPerm(player, maxPerm)) finalId = id;
            else if (hasPerm(player, perm)) finalId = id;
            else if (perm.matches("(?i)DEFAULT")) finalId = id;
        }

        return finalId;
    }

    public ConfigurationSection lastSection(Player player, boolean join) {
        String path = join ? (!player.hasPlayedBefore() ? "first-join" : "join") : "quit";
        return lastSection(player, path);
    }

    private void sound(Player player, String sound) {
        if (sound == null) return;
        try {
            Enum.valueOf(Sound.class, sound);
        } catch (IllegalArgumentException ex) {
            return;
        }
        player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
    }

    private void send(Player player, List<String> list, boolean priv) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : list) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = color(message, player, true);

            String prefix = "&e&lSIR &8> &f";
            if (main.getConfig().getBoolean("options.send-console", true))
                main.logger(prefix + message.replace(split, "&r" + split));

            if (message.startsWith("[ACTION-BAR]")) {
                message = setUp("[ACTION-BAR]", message);
                if (priv) langUtils.actionBar(player, message);
                else for (Player p : players) langUtils.actionBar(p, message);
            }

            else if (message.startsWith("[TITLE]")) {
                String[] array = setUp("[TITLE]", message).split(Pattern.quote(split));
                if (priv) langUtils.title(player, array);
                else for (Player p : players) langUtils.title(p, array);
            }

            else {
                if (priv) langUtils.sendMixed(player, message);
                else for (Player p : players) langUtils.sendMixed(p, message);
            }
        }
    }

    private void command(Player player, List<String> list, boolean join) {
        for (String message : list) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = color(message, player, false);

            if (message.startsWith("[PLAYER]") && join)
                Bukkit.dispatchCommand(player, setUp("[PLAYER]", message));

            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    private void god(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable", 0) ;
        if (main.getLangUtils().getVersion <= 8 || godTime <= 0) return;
        godTime = godTime * 20; player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, () -> player.setInvulnerable(false), godTime);
    }

    public void spawn(ConfigurationSection id, Player player) {
        Location location; String[] coordinates;
        String path = id.getString("spawn.x-y-z", "");
        World world = Bukkit.getWorld(id.getString("spawn.world",""));
        if (world == null) return;

        if (path.trim().equals("")) location = world.getSpawnLocation();
        else {
            coordinates = path.split(",");
            if (coordinates.length == 3) {
                int x = Integer.parseInt(coordinates[0]);
                int y = Integer.parseInt(coordinates[1]);
                int z = Integer.parseInt(coordinates[2]);
                location = new Location(world, x, y, z);
            }
            else location = world.getSpawnLocation();
        }
        player.teleport(location);
    }

    public void runEvent(ConfigurationSection id, Player player, boolean join, boolean spawn, boolean login) {
        Runnable event = () -> {
            if (id == null) {
                String prefix = "&7 &4&lSIR-DEBUG &8> ";
                langUtils.sendMixed(player, prefix + "&cA valid message group isn't found...");
                langUtils.sendMixed(player, prefix + "&7Please check your &messages.yml &7file.");
                main.logger(prefix + "&cA valid message group isn't found...");
                main.logger(prefix + "&7Please check your &messages.yml &7file.");
                return;
            }

            String soundString = id.getString("sound");
            if (join && soundString != null) sound(player, soundString);
            if (join) god(id, player); if (join && spawn) spawn(id, player);

            send(player, id.getStringList("public"), false);
            if (join) send(player, id.getStringList("private"), true);
            command(player, id.getStringList("commands"), join);
        };
        
        int delay = main.getConfig().getInt("login.ticks-after", 0);
        Bukkit.getScheduler().runTaskLater(main, event, login ? delay : 4);
    }
}
