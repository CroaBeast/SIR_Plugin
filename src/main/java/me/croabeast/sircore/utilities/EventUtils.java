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
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

public class EventUtils {

    private final Application main;
    private final TextUtils text;

    public EventUtils(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
    }

    public List<Player> loggedPlayers = new ArrayList<>();

    public String doFormat(String msg, Player player, boolean isColor) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] v = {player.getName(), player.getWorld().getName()};

        String message = StringUtils.replaceEach(msg, keys, v);
        return isColor ? text.parsePAPI(player, message) : message;
    }

    public boolean hasPerm(CommandSender sender, String perm) {
        if (sender instanceof ConsoleCommandSender) return true;
        Player senderPlayer = (Player) sender;
        return  main.getInitializer().hasVault ?
                Initializer.Perms.playerHas(null, senderPlayer, perm) :
                senderPlayer.hasPermission(perm);
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
        if (main.getPlugin("CMI") == null) return false;
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

        String split = text.getValue("split");
        message = message.replace(split, "&r" + split);

        main.getRecords().doRecord("&7> &f" + message);
    }

    private String setUp(@NotNull String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    public void typeMessage(Player player, String message) {
        if (message.startsWith("[ACTION-BAR]")) {
            text.actionBar(player, setUp("[ACTION-BAR]", message));
        }
        else if (message.startsWith("[TITLE]")) {
            String split = text.getValue("split");
            String[] title = setUp("[TITLE]", message).split(Pattern.quote(split));
            text.title(player, title, new String[] {"20", "60", "20"});
        }
        else if (message.startsWith("[JSON]")) {
            String command = player.getName() + " " + setUp("[JSON]", message);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + command);
        }
        else text.sendMixed(player, message);
    }

    public void playsound(ConfigurationSection id, Player player) {
        String sound = id.getString("sound");
        if (sound == null) return;

        try {
            Enum.valueOf(Sound.class, sound);
        } catch (IllegalArgumentException ex) {
            return;
        }

        player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
    }

    private void invulnerable(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable", 0) ;
        if (text.getVersion <= 8 || godTime <= 0) return;

        Runnable god = () -> player.setInvulnerable(false);
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, god, godTime);
    }

    public void spawn(ConfigurationSection id, Player player) {
        String[] coordinates;
        Location location;
        String path = id.getString("spawn.x-y-z", "");
        String name = id.getString("spawn.world", "");

        World world = Bukkit.getWorld(name);
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

    private void send(ConfigurationSection id, Player player, boolean isPublic) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : id.getStringList(isPublic ? "public" : "private")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = doFormat(message, player, true);

            sendToConsole(message);

            if (isPublic) for (Player p : players) typeMessage(p, message);
            else typeMessage(player, message);
        }
    }

    public void command(ConfigurationSection id, Player player, boolean join) {
        for (String message : id.getStringList("commands")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            message = doFormat(message, player, false);

            if (message.startsWith("[PLAYER]") && join) {
                Bukkit.dispatchCommand(player, setUp("[PLAYER]", message));
            }
            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    public void runEvent(ConfigurationSection id, Player player, boolean join, boolean spawn, boolean login) {
        Runnable event = () -> {
            if (id == null) {
                main.getRecords().doRecord(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
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
