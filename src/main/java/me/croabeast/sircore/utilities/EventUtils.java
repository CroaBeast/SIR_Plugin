package me.croabeast.sircore.utilities;

import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.util.*;
import java.util.regex.*;

public class EventUtils {

    private final Application main;
    private final Records records;
    private final TextUtils text;
    private final PermUtils perms;

    public EventUtils(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.text = main.getTextUtils();
        this.perms = main.getPermUtils();
    }

    protected Set<Player> LOGGED_PLAYERS = new HashSet<>();
    public Set<Player> getLoggedPlayers() { return LOGGED_PLAYERS; }

    public String doFormat(String line, Player player, boolean isColor) {
        String message = StringUtils.replaceEach(line,
                new String[] {"{PLAYER}", "{WORLD}"},
                new String[] {player.getName(), player.getWorld().getName()}
        );
        return isColor ? text.parsePAPI(player, message) : message;
    }

    public ConfigurationSection lastSection(FileConfiguration file, Player player, String path) {
        ConfigurationSection finalId = null;
        String maxPerm = "";
        int highest = 0;

        ConfigurationSection section = file.getConfigurationSection(path);
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

            if (perms.certainPerm(player, maxPerm)) finalId = id;
            else if (perms.certainPerm(player, perm)) finalId = id;
            else if (perm.matches("(?i)DEFAULT")) finalId = id;
        }

        return finalId;
    }

    public ConfigurationSection lastSection(Player player, String path) {
        return lastSection(main.getMessages(), player, path);
    }

    public ConfigurationSection lastSection(Player player, boolean isJoin) {
        return lastSection(player, isJoin ?
                (!player.hasPlayedBefore() &&
                        main.getMessages().isConfigurationSection("first-join") ?
                        "first-join" : "join"
                ) : "quit"
        );
    }

    private void sendToConsole(String message) {
        if (!text.getOption(1, "send-console")) return;
        String split = text.getSplit();
        message = message.replace(split, "&r" + split);
        records.doRecord("&7> &f" + message);
    }

    private String initLine(String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    public void typeMessage(Player player, String line) {
        if (line.startsWith("[ACTION-BAR]"))
            text.actionBar(player, initLine("[ACTION-BAR]", line));
        else if (line.startsWith("[TITLE]")) {
            String split = Pattern.quote(text.getSplit());
            text.title(player,
                    initLine("[TITLE]", line).split(split),
                    new String[] {"10", "50", "10"}
            );
        }
        else if (line.startsWith("[JSON]"))
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), "minecraft:tellraw " +
                    player.getName() + " " + initLine("[JSON]", line)
            );
        else text.sendMixed(player, line);
    }

    public void playSound(ConfigurationSection id, Player player) {
        Sound sound;
        String rawSound = id.getString("sound");
        if (rawSound == null) return;

        try {
            Enum.valueOf(Sound.class, rawSound);
            sound = Sound.valueOf(rawSound);
        }
        catch (IllegalArgumentException ex) { return; }

        player.playSound(player.getLocation(), sound, 1, 1);
    }

    private void giveGod(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable", 0) ;
        if (main.MC_VERSION <= 8 || godTime <= 0) return;

        Runnable god = () -> player.setInvulnerable(false);
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, god, godTime);
    }

    public void goSpawn(ConfigurationSection id, Player player) {
        String[] coordinates;
        String[] rotations;
        Location location;

        String name = id.getString("spawn.world", "");
        String path = id.getString("spawn.x-y-z", "");
        String rotation = id.getString("spawn.yaw-pitch", "");

        World world = Bukkit.getWorld(name);
        if (world == null) return;

        coordinates = path.split(",");
        rotations = rotation.split(",");

        if (!path.equals("") && coordinates.length == 3) {
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            int z = Integer.parseInt(coordinates[2]);

            if (rotation.equals("") || rotations.length != 2)
                location = new Location(world, x, y, z);
            else {
                int yaw = Integer.parseInt(rotations[0]);
                int pitch = Integer.parseInt(rotations[1]);
                location = new Location(world, x, y, z, yaw, pitch);
            }
        }
        else location = world.getSpawnLocation();

        player.teleport(location);
    }

    private void send(ConfigurationSection id, Player player, boolean isPublic) {
        for (String line : id.getStringList(isPublic ? "public" : "private")) {
            if (line == null || line.equals("")) continue;
            if (line.startsWith(" ")) line = line.substring(1);

            line = doFormat(line, player, false);
            sendToConsole(line);
            String message = text.parsePAPI(player, line);

            if (isPublic)
                main.everyPlayer().forEach(p -> typeMessage(p, message));
            else typeMessage(player, message);
        }
    }

    public void runCmds(ConfigurationSection id, Player player) {
        for (String message : id.getStringList("commands")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            if (player != null)
                message = doFormat(message, player, false);

            if (message.startsWith("[PLAYER]") && player != null) {
                Bukkit.dispatchCommand(player, initLine("[PLAYER]", message));
            }
            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    public void runEvent(ConfigurationSection id, Player player, boolean join, boolean spawn, boolean login) {
        Bukkit.getScheduler().runTaskLater(main, () -> {
            if (id == null) {
                records.doRecord(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            if (join) playSound(id, player);
            if (join) giveGod(id, player);
            if (join && spawn) goSpawn(id, player);

            send(id, player, true);
            if (join) send(id, player, false);
            runCmds(id, join ? player : null);
        }, login ? main.getConfig().getInt("login.ticks-after", 0) : 3);
    }
}
