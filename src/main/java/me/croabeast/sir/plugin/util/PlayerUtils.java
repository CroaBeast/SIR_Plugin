package me.croabeast.sir.plugin.util;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import net.milkbowl.vault.chat.Chat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class PlayerUtils {

    @Getter
    private static final Set<Player> GOD_PLAYERS = new HashSet<>();

    public String getPrefix(Player player) {
        final Chat chat = SIRInitializer.getChatMeta();
        return chat != null ? chat.getPlayerPrefix(player) : null;
    }

    public String getSuffix(Player player) {
        final Chat chat = SIRInitializer.getChatMeta();
        return chat != null ? chat.getPlayerSuffix(player) : null;
    }

    public boolean hasPerm(CommandSender sender, String perm) {
        if (sender instanceof ConsoleCommandSender)
            return true;

        if (StringUtils.isBlank(perm)) return false;

        if (perm.matches("(?i)DEFAULT")) return true;

        final boolean b = sender.hasPermission(perm);
        final String s = "options.override-op";

        if (!YAMLData.Main.CONFIG.from().get(s, false))
            return b;

        return (!sender.isOp() ||
                sender.isPermissionSet(perm)) && b;
    }

    public Player getClosest(String input) {
        for (Player p : Bukkit.getOnlinePlayers())
            if (p.getName().matches("(?i)" + Pattern.quote(input))) return p;

        return null;
    }

    public void teleport(ConfigurationSection id, Player player) {
        if (id == null) return;

        String w = id.getString("world");
        if (w == null) return;

        World world = Bukkit.getWorld(w);
        if (world == null) return;

        Location loc = world.getSpawnLocation();

        String coords = id.getString("coordinates");
        String rot = id.getString("rotation");

        double[] dC = {loc.getX(), loc.getY(), loc.getZ()},
                c = new double[3];

        float[] dD = {loc.getYaw(), loc.getPitch()},
                d = new float[2];

        if (coords != null) {
            String[] mC = coords.split(",", 3);

            c[0] = dC[0];
            c[1] = dC[1];
            c[2] = dC[2];

            try {
                c[0] = Double.parseDouble(mC[0]);
            } catch (Exception ignored) {}
            try {
                c[1] = Double.parseDouble(mC[1]);
            } catch (Exception ignored) {}
            try {
                c[2] = Double.parseDouble(mC[2]);
            } catch (Exception ignored) {}
        }

        if (rot != null) {
            String[] mD = rot.split(",", 2);

            d[0] = dD[0];
            d[1] = dD[1];

            try {
                d[0] = Float.parseFloat(mD[0]);
            } catch (Exception ignored) {}
            try {
                d[1] = Float.parseFloat(mD[1]);
            } catch (Exception ignored) {}
        }

        if (c != dC) {
            loc.setX(c[0]);
            loc.setY(c[1]);
            loc.setZ(c[2]);
        }

        if (d != dD) {
            loc.setYaw(d[0]);
            loc.setPitch(d[1]);
        }

        player.teleport(loc);
    }

    public boolean isImmune(Player player) {
        return GOD_PLAYERS.contains(player);
    }

    public boolean addToImmunePlayers(Player player) {
        return GOD_PLAYERS.add(player);
    }

    public boolean removeFromImmunePlayers(Player player) {
        return GOD_PLAYERS.remove(player);
    }

    public void giveImmunity(Player player, int time) {
        if (ServerInfoUtils.SERVER_VERSION <= 8 | time <= 0)
            return;

        addToImmunePlayers(player);
        player.setInvulnerable(true);

        Bukkit.getScheduler().runTaskLater(
                SIRPlugin.getInstance(),
                () -> {
                    player.setInvulnerable(false);
                    GOD_PLAYERS.remove(player);
                },
                time
        );
    }

    public void playSound(CommandSender sender, String rawSound) {
        if (!(sender instanceof Player)) return;
        if (rawSound == null) return;

        Sound sound;
        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        Player p = (Player) sender;
        p.playSound(p.getLocation(), sound, 1, 1);
    }

    public Set<Player> getNearbyPlayers(Location location, double range) {
        Set<Player> players = new HashSet<>();

        World world = location.getWorld();
        if (world == null) return players;

        world.getPlayers().forEach(p -> {
            if (p.getLocation().distance(location) <= range)
                players.add(p);
        });

        return players;
    }

    public Set<Player> getNearbyPlayers(Player player, double range) {
        return getNearbyPlayers(player.getLocation(), range);
    }
}
