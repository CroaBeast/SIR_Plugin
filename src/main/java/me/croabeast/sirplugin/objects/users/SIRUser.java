package me.croabeast.sirplugin.objects.users;

import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.LangUtils;
import me.croabeast.sirplugin.utilities.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class SIRUser {

    static Set<SIRUser> godUsers = new HashSet<>();
    static HashMap<Player, SIRUser> userMap = new HashMap<>();

    private final Player player;

    private SIRUser(Player player) {
        this.player = player;
    }

    public static boolean hasPerm(CommandSender sender, String perm) {
        boolean isSet = FileCache.CONFIG.get().getBoolean("options.hard-perm-check");
        isSet = (!isSet || sender.isPermissionSet(perm)) && sender.hasPermission(perm);
        return (sender instanceof ConsoleCommandSender) || isSet;
    }

    public boolean hasPerm(String perm) {
        return hasPerm(player, perm);
    }

    public void playSound(@Nullable String rawSound) {
        if (rawSound == null) return;
        Sound sound;

        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        player.playSound(player.getLocation(), sound, 1, 1);
    }

    public void giveInvulnerable(int godTime) {
        if (LangUtils.majorVersion() <= 8 | godTime <= 0) return;

        SIRUser user = this;
        player.setInvulnerable(true);
        getGodUsers().add(user);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
                getGodUsers().remove(user);
            }
        }.runTaskLater(SIRPlugin.getInstance(), godTime);
    }

    public void teleport(String worldName, String path, String rotation) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        String[] coordinates = path.split(","),
                rotations = rotation.split(",");
        Location location;

        if (!path.equals("") && coordinates.length == 3) {
            double x, y, z;
            try {
                x = Double.parseDouble(coordinates[0]);
                y = Double.parseDouble(coordinates[1]);
                z = Double.parseDouble(coordinates[2]);
            }
            catch (NumberFormatException e) {
                LogUtils.doLog(
                        "<P> &cThe coordinates are invalid, " +
                                "teleporting to the world's spawn."
                );
                player.teleport(world.getSpawnLocation());
                return;
            }

            if (rotation.equals("") || rotations.length != 2)
                location = new Location(world, x, y, z);
            else {
                float yaw, pitch;
                try {
                    yaw = Float.parseFloat(rotations[0]);
                    pitch = Float.parseFloat(rotations[1]);
                }
                catch (NumberFormatException e) {
                    LogUtils.doLog(
                            "<P> &cThe rotation numbers are invalid, " +
                                    "teleporting to the default location."
                    );
                    player.teleport(new Location(world, x, y, z));
                    return;
                }

                location = new Location(world, x, y, z, yaw, pitch);
            }
        }
        else location = world.getSpawnLocation();

        player.teleport(location);
    }

    private static boolean certainPerm(Player player, String perm) {
        return perm != null && !perm.matches("(?i)DEFAULT") && hasPerm(player, perm);
    }

    public static ConfigurationSection getSection(ConfigurationSection sect, Player player, String path) {
        ConfigurationSection resultSection = null;

        String maxPerm = null;
        int highest = 0;

        ConfigurationSection section = sect.getConfigurationSection(path);
        if (section == null || section.getKeys(false).isEmpty()) return null;

        List<String> keys = new ArrayList<>(section.getKeys(false));

        for (String key : keys) {
            ConfigurationSection id = section.getConfigurationSection(key);
            assert id != null;

            String perm = id.getString("permission", "DEFAULT");
            int priority = id.getInt("priority", perm.matches("(?i)DEFAULT") ? 0 : 1);

            if (priority > highest) {
                maxPerm = perm;
                highest = priority;
            }
        }

        for (String key : keys) {
            ConfigurationSection id = section.getConfigurationSection(key);
            assert id != null;

            String perm = id.getString("permission", "DEFAULT");

            if (perm.matches("(?i)" + maxPerm) && certainPerm(player, maxPerm)) return id;
            else if (certainPerm(player, perm)) resultSection = id;
            else if (perm.matches("(?i)DEFAULT")) resultSection = id;
        }

        return resultSection;
    }

    public ConfigurationSection getSection(ConfigurationSection sect, String path) {
        return getSection(sect, player, path);
    }

    public ConfigurationSection getFirstJoin() {
        return getSection(FileCache.JOIN_QUIT.get(), "first-join");
    }

    public ConfigurationSection getJoin() {
        return getSection(FileCache.JOIN_QUIT.get(), "join");
    }

    public ConfigurationSection getQuit() {
        return getSection(FileCache.JOIN_QUIT.get(), "quit");
    }

    public ConfigurationSection getChatFormat() {
        return getSection(FileCache.FORMATS.get(), "formats");
    }

    public ConfigurationSection getChatFiler() {
        return getSection(FileCache.FILTERS.get(), "filters");
    }

    public static Set<SIRUser> getGodUsers() {
        return godUsers;
    }

    @SafeVarargs
    public static void addPlayers(Collection<? extends Player>... collections) {
        for (Collection<? extends Player> c : collections)
            if (c != null) c.forEach(p -> userMap.put(p, new SIRUser(p)));
    }

    public static void addPlayer(@NotNull Player player) {
        addPlayers(Collections.singletonList(player));
    }

    @Nullable
    public static SIRUser getUser(Player player) {
        return player == null ? null : userMap.getOrDefault(player, null);
    }
}
