package me.croabeast.sirplugin.utility;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.object.file.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;
import org.bukkit.scheduler.*;

import java.util.*;

public final class PlayerUtils {

    static Set<Player> godPlayers = new HashSet<>();
    public static Set<Player> getGodPlayers() {
        return godPlayers;
    }

    public static boolean hasPerm(CommandSender sender, String perm) {
        if (sender == null) return false;
        if (sender instanceof ConsoleCommandSender) return true;

        Player player = (Player) sender;
        if (player.isOp()) return true;
        if (StringUtils.isBlank(perm)) return false;

        boolean isSet = FileCache.CONFIG.get().getBoolean("options.hard-perm-check");
        return (!isSet || sender.isPermissionSet(perm)) && sender.hasPermission(perm);
    }

    private static boolean essVanish(Player player, boolean isJoin) {
        Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        return ess != null && (ess.getUser(player).isVanished() ||
                (isJoin && hasPerm(player, "essentials.silentjoin.vanish")));
    }

    private static boolean cmiVanish(Player player) {
        return Bukkit.getPluginManager().getPlugin("CMI") != null && CMIUser.getUser(player).isVanished();
    }

    private static boolean normalVanish(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;
        return false;
    }

    public static boolean isVanished(Player p, boolean isJoin) {
        return essVanish(p, isJoin) || cmiVanish(p) || normalVanish(p);
    }

    public static Player getClosestPlayer(String input) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getName().matches("(?i)" + input)) continue;
            return p;
        }
        return null;
    }

    public static boolean isIgnoredFrom(Player target, Player player, boolean isChat) {
        String data = "data." + target.getUniqueId() + ".",
                path = isChat ? "chat" : "msg";

        FileConfiguration file = FileCache.IGNORE.get();

        List<String> list = file.getStringList(data + path);
        boolean inList = !list.isEmpty() && list.contains(player.getUniqueId() + "");

        return file.getBoolean(data + "all-" + path) || inList;
    }

    public static void teleport(Player player, World world, double[] c, float[] d) {
        if (world == null) return;
        Location location = world.getSpawnLocation();

        if (c != null && c.length == 3) {
            location.setX(c[0]);
            location.setY(c[1]);
            location.setZ(c[2]);
        }

        if (d != null && d.length == 2) {
            location.setYaw(d[0]);
            location.setPitch(d[1]);
        }

        if (player != null) player.teleport(location);
    }

    public static void teleport(Player player, String worldName, String coords, String direction) {
        String[] coordinates = coords.split(","),
                rotations = direction.split(",");

        double[] c = new double[3];
        float[] d = new float[2];

        try {
            c[0] = Double.parseDouble(coordinates[0]);
            c[1] = Double.parseDouble(coordinates[1]);
            c[2] = Double.parseDouble(coordinates[2]);
        }
        catch (Exception e) {
            c = null;
        }

        try {
            d[0] = Float.parseFloat(rotations[0]);
            d[1] = Float.parseFloat(rotations[1]);
        }
        catch (NumberFormatException e) {
            d = null;
        }

        teleport(player, Bukkit.getWorld(worldName), c, d);
    }

    public static void teleportPlayer(ConfigurationSection id, Player player) {
        teleport(player, id.getString("spawn.world", ""),
                id.getString("spawn.x-y-z", ""),
                id.getString("spawn.yaw-pitch", ""));
    }

    public static void giveImmunity(Player player, int time) {
        if (LangUtils.majorVersion() <= 8 | time <= 0) return;

        player.setInvulnerable(true);
        getGodPlayers().add(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
                getGodPlayers().remove(player);
            }
        }.runTaskLater(SIRPlugin.getInstance(), time);
    }

    public static void playSound(Player player, String rawSound) {
        if (rawSound == null) return;
        Sound sound;

        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        player.playSound(player.getLocation(), sound, 1, 1);
    }
}
