package me.croabeast.sircore.utilities;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sircore.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;

import java.util.*;
import java.util.regex.*;

public class EventUtils {

    private final Application main;
    private final TextUtils text;

    public EventUtils(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
    }

    public List<Player> LOGGED_PLAYERS = new ArrayList<>();

    public String doFormat(String line, Player player, boolean isColor) {
        String message = StringUtils.replaceEach(line,
                new String[] {"{PLAYER}", "{WORLD}"},
                new String[] {player.getName(), player.getWorld().getName()}
        );
        return isColor ? text.parsePAPI(player, message) : message;
    }

    public boolean hasPerm(CommandSender sender, String perm) {
        if (sender instanceof ConsoleCommandSender) return true;
        Player player = (Player) sender;
        return  main.getInitializer().HAS_VAULT ?
                Initializer.Perms.playerHas(null, player, perm) :
                player.hasPermission(perm);
    }

    private boolean certainPerm(Player player, String perm) {
        return !perm.matches("(?i)DEFAULT") && hasPerm(player, perm);
    }

    private boolean essVanish(Player player, boolean join) {
        Essentials ess = (Essentials) main.getPlugin("Essentials");
        if (ess == null) return false;

        boolean isJoin = join && certainPerm(player, "essentials.silentjoin.vanish");
        return ess.getUser(player).isVanished() || isJoin;
    }

    private boolean cmiVanish(Player player) {
        if (!main.getInitializer().hasCMI) return false;
        return CMIUser.getUser(player).isVanished();
    }

    private boolean normalVanish(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    public boolean isVanished(Player p, boolean join) {
        return essVanish(p, join) || cmiVanish(p) || normalVanish(p);
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

            if (certainPerm(player, maxPerm)) finalId = id;
            else if (certainPerm(player, perm)) finalId = id;
            else if (perm.matches("(?i)DEFAULT")) finalId = id;
        }

        return finalId;
    }

    public ConfigurationSection lastSection(Player player, boolean join) {
        return lastSection(player, join ?
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

        main.getRecords().doRecord("&7> &f" + message);
    }

    private String initLine(String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    public void typeMessage(Player player, String line) {
        if (line.startsWith("[ACTION-BAR]")) {
            text.actionBar(player,
                    initLine("[ACTION-BAR]", line)
            );
        }
        else if (line.startsWith("[TITLE]")) {
            String split = Pattern.quote(text.getSplit());
            text.title(player,
                    initLine("[TITLE]", line).split(split),
                    new String[] {"10", "50", "10"}
            );
        }
        else if (line.startsWith("[JSON]")) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "tellraw " + player.getName() + " " +
                    initLine("[JSON]", line)
            );
        }
        else text.sendMixed(player, line);
    }

    public void playSound(ConfigurationSection id, Player player) {
        Sound sound;
        String rawSound = id.getString("sound");
        if (rawSound == null) return;

        try {
            Enum.valueOf(Sound.class, rawSound);
            sound = Sound.valueOf(rawSound);
        } catch (IllegalArgumentException ex) {
            return;
        }

        player.playSound(
                player.getLocation(), sound, 1, 1
        );
    }

    private void giveGod(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable", 0) ;
        if (main.GET_VERSION <= 8 || godTime <= 0) return;

        Runnable god = () -> player.setInvulnerable(false);
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, god, godTime);
    }

    public void goSpawn(ConfigurationSection id, Player player) {
        String[] coordinates;
        Location location;
        String path = id.getString("spawn.x-y-z", "");
        String name = id.getString("spawn.world", "");

        World world = Bukkit.getWorld(name);
        if (world == null) return;

        if (!path.equals("")) {
            coordinates = path.split(",");
            if (coordinates.length == 3) {
                int x = Integer.parseInt(coordinates[0]);
                int y = Integer.parseInt(coordinates[1]);
                int z = Integer.parseInt(coordinates[2]);
                location = new Location(world, x, y, z);
            }
            else location = world.getSpawnLocation();
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
                main.getRecords().doRecord(player,
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
