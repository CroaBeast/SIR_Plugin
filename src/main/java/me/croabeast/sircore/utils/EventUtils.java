package me.croabeast.sircore.utils;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sircore.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.*;

public class EventUtils {

    private final Application main;
    private final Initializer initializer;
    private final TextUtils textUtils;

    public EventUtils(Application main) {
        this.main = main;
        this.initializer = main.getInitializer();
        this.textUtils = main.getTextUtils();
    }

    private String format(String msg, Player player, boolean isColor) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] v = {player.getName(), player.getWorld().getName()};

        String message = StringUtils.replaceEach(msg, keys, v);
        return isColor ? textUtils.parsePAPI(player, message) : message;
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

    public boolean isVanished(Player p, boolean join) {
        return essVanish(p, join) || cmiVanish(p);
    }

    private boolean hasPerm(Player player, String perm) {
        return !perm.matches("(?i)DEFAULT") &&
                (initializer.hasVault ?
                        initializer.getPerms().playerHas(null, player, perm) :
                        player.hasPermission(perm)
                );
    }

    public ConfigurationSection lastSection(Player player, String path) {
        ConfigurationSection finalId = null;
        String maxPerm = "";
        int highest = 0;

        ConfigurationSection section = main.getMessages().getConfigurationSection(path);
        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");
            int before = perm.matches("(?i)DEFAULT") ? 0 : 1;
            int priority = id.getInt("priority", before);

            if (priority > highest) {
                highest = priority;
                maxPerm = perm;
            }
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

    private void sendToConsole(String message, String split) {
        if (!main.choice("console")) return;
        main.logger("&e&lSIR &7> &f" + message.replace(split, "&r" + split));
    }

    private String setUp(@NotNull String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    private void typeMessage(Player player, String message) {
        if (message.startsWith("[ACTION-BAR]")) {
            textUtils.actionBar(player, setUp("[ACTION-BAR]", message));
        }
        else if (message.startsWith("[TITLE]")) {
            String split = main.getConfig().getString("options.line-separator", "<n>");
            textUtils.title(player, setUp("[TITLE]", message).split(Pattern.quote(split)));
        }
        else if (message.startsWith("[JSON]")) {
            String command = player.getName() + " " + setUp("[JSON]", message);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + command);
        }
        else textUtils.sendMixed(player, message);
    }

    private void send(ConfigurationSection id, Player player, boolean isPublic) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : id.getStringList(isPublic ? "public" : "private")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = format(message, player, true);

            sendToConsole(message, split);

            if (isPublic) for (Player p : players) typeMessage(p, message);
            else typeMessage(player, message);
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
        if (main.getTextUtils().getVersion <= 8 || godTime <= 0) return;
        godTime = godTime * 20;

        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, () -> player.setInvulnerable(false), godTime);
    }

    public void spawn(ConfigurationSection id, Player player) {
        String[] coordinates;
        Location location;
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
                main.logger(prefix + "&cA valid message group isn't found...");
                main.logger(prefix + "&7Please check your &messages.yml &7file.");
                return;
            }

            if (join) playsound(id, player);
            if (join) invulnerable(id, player);
            if (join && spawn) spawn(id, player);

            send(id, player, true);
            if (join) send(id, player, false);
            command(id, player, join);
        };
        
        int delay = main.getConfig().getInt("login.ticks-after", 0);
        Bukkit.getScheduler().runTaskLater(main, event, login ? delay : 3);
    }
}
