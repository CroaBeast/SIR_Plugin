package me.croabeast.sirplugin.utility;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.var;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.task.ignore.IgnoreTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class PlayerUtils {

    @Getter
    private static final Set<Player> godPlayers = new HashSet<>();

    public <T extends CommandSender> boolean hasPerm(T sender, String perm) {
        final var b = sender.hasPermission(perm);
        final var s = "options.override-op";

        if (!FileCache.MAIN_CONFIG.getValue(s, false)) return b;
        return (!sender.isOp() ||
                sender.isPermissionSet(perm)) && b;
    }

    public Player getClosestPlayer(String input) {
        for (var p : Bukkit.getOnlinePlayers())
            if (p.getName().matches("(?i)" + input)) return p;

        return null;
    }

    public boolean isIgnoring(Player source, Player target, boolean isChat) {
        var s = IgnoreTask.getSettings(source);
        if (s == null) return false;

        var cache = isChat ? s.getChatCache() : s.getMsgCache();
        return cache.isForAll() ||
                (target != null && cache.contains(target));
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

    public void giveImmunity(Player player, int time) {
        if (LibUtils.getMainVersion() <= 8 | time <= 0) return;

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

    public <C extends CommandSender> void playSound(C sender, String rawSound) {
        if (!(sender instanceof Player)) return;

        if (rawSound == null) return;
        Sound sound;

        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        var p = (Player) sender;
        p.playSound(p.getLocation(), sound, 1, 1);
    }

    public void addChatCompletions(Player player, List<String> list) {
        if (!LibUtils.isPaper()) return;
        if (LibUtils.getMainVersion() < 19) return;

        if (player == null) return;
        if (list.isEmpty()) return;

        final String m = "addAdditionalChatCompletions";
        Method method = null;

        try {
            method = player.getClass().
                    getDeclaredMethod(m, Collection.class);
        } catch (Exception e) { e.printStackTrace(); }

        if (method == null) return;

        try {
            method.invoke(player, list);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
