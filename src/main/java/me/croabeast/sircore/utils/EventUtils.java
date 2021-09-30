package me.croabeast.sircore.utils;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import me.croabeast.sircore.MainCore;
import me.croabeast.sircore.SIRPlugin;
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

    private final SIRPlugin main;
    private final MainCore mainCore;
    private final TextUtils textUtils;

    public EventUtils(SIRPlugin main) {
        this.main = main;
        this.mainCore = main.getMainCore();
        this.textUtils = main.getLangUtils();
    }

    private String format(String msg, Player player, boolean isColor) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] v = {player.getName(), player.getWorld().getName()};

        String message = StringUtils.replaceEach(msg, keys, v);
        return isColor ? textUtils.parsePAPI(player, message) : message;
    }

    private String setUp(String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    private boolean essVanish(Player player, boolean join) {
        Essentials ess = (Essentials) mainCore.plugin("Essentials");
        if (ess == null) return false;

        boolean isJoin = join && hasPerm(player, "essentials.silentjoin.vanish");
        return (ess.getUser(player).isVanished()) || isJoin;
    }

    private boolean cmiVanish(Player player) {
        if (mainCore.plugin("CMI") == null) return false;
        return CMIUser.getUser(player).isVanished();
    }

    public boolean isVanished(Player player, boolean join) {
        return essVanish(player, join) || cmiVanish(player);
    }

    private boolean hasPerm(Player player, String perm) {
        return !perm.matches("(?i)DEFAULT") &&
                (mainCore.hasVault ?
                        mainCore.getPerms().playerHas(null, player, perm) :
                        player.hasPermission(perm)
                );
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

    private void playsound(ConfigurationSection id, Player player) {
        String sound = id.getString("sound");
        if (sound == null) return;
        try {
            Enum.valueOf(Sound.class, sound);
        } catch (IllegalArgumentException ex) {
            return;
        }
        player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
    }

    private void send(ConfigurationSection id, Player player, boolean priv) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : id.getStringList(!priv ? "public" : "private")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = format(message, player, true);

            String prefix = "&e&lSIR &8> &f";
            if (main.getConfig().getBoolean("options.send-console", true))
                mainCore.logger(prefix + message.replace(split, "&r" + split));

            if (message.startsWith("[ACTION-BAR]")) {
                message = setUp("[ACTION-BAR]", message);
                if (priv) textUtils.actionBar(player, message);
                else {
                    for (Player p : players) textUtils.actionBar(p, message);
                }
            }

            else if (message.startsWith("[TITLE]")) {
                String[] array = setUp("[TITLE]", message).split(Pattern.quote(split));
                if (priv) textUtils.title(player, array);
                else {
                    for (Player p : players) textUtils.title(p, array);
                }
            }

            else if (message.startsWith("[JSON]")) {
                message = setUp("[JSON]", message);
                String one = "tellraw " + player.getName() + " " + message;
                String all = "tellraw @a " + message;
                if (priv) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), one);
                else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), all);
            }

            else {
                if (!priv) {
                    for (Player p : players) textUtils.sendMixed(p, message);
                }
                else textUtils.sendMixed(player, message);
            }
        }
    }

    private void command(ConfigurationSection id, Player player, boolean join) {
        for (String message : id.getStringList("commands")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = format(message, player, false);

            if (message.startsWith("[PLAYER]") && join)
                Bukkit.dispatchCommand(player, setUp("[PLAYER]", message));

            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    private void invulnerable(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable", 0) ;
        if (main.getLangUtils().getVersion <= 8 || godTime <= 0) return;
        godTime = godTime * 20;

        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, () -> player.setInvulnerable(false), godTime);
    }

    public void spawn(ConfigurationSection id, Player player) {
        Location location; String[] coordinates;
        String path = id.getString("spawn.x-y-z", "");
        World world = Bukkit.getWorld(id.getString("spawn.world",""));
        if (world == null) return;

        if (path.equals("")) location = world.getSpawnLocation();
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
                textUtils.sendMixed(player, prefix + "&cA valid message group isn't found...");
                textUtils.sendMixed(player, prefix + "&7Please check your &messages.yml &7file.");
                mainCore.logger(prefix + "&cA valid message group isn't found...");
                mainCore.logger(prefix + "&7Please check your &messages.yml &7file.");
                return;
            }

            if (join) playsound(id, player);
            if (join) invulnerable(id, player);
            if (join && spawn) spawn(id, player);

            send(id, player, false);
            if (join) send(id, player, true);
            command(id, player, join);
        };
        
        int delay = main.getConfig().getInt("login.ticks-after", 0);
        Bukkit.getScheduler().runTaskLater(main, event, login ? delay : 3);
    }
}
