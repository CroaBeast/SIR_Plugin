package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.objects.handlers.TextParser;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class EventUtils {

    private final SIRPlugin main;

    public EventUtils(SIRPlugin main) {
        this.main = main;
    }

    protected Set<Player> loggedPlayers = new HashSet<>();
    public Set<Player> getLoggedPlayers() {
        return loggedPlayers;
    }

    private boolean certainPerm(Player player, String perm) {
        return perm != null && !perm.matches("(?i)DEFAULT") && PermUtils.hasPerm(player, perm);
    }

    @Nullable
    public ConfigurationSection getSection(FileConfiguration file, Player player, String path) {
        ConfigurationSection resultSection = null;

        String maxPerm = null;
        int highest = 0;

        ConfigurationSection section = file.getConfigurationSection(path);
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
    
    public void playSound(Player player, @Nullable String rawSound) {
        if (rawSound == null) return;
        Sound sound;

        try {
            sound = Enum.valueOf(Sound.class, rawSound);
        }
        catch (IllegalArgumentException | NullPointerException e) {
            LogUtils.doLog(player, "<P> The sound you input is invalid.");
            return;
        }

        player.playSound(player.getLocation(), sound, 1, 1);
    }

    public void giveInvulnerable(Player player, int godTime) {
        if (SIRPlugin.MAJOR_VERSION <= 8 | godTime <= 0) return;

        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
            }
        }.runTaskLater(main, godTime);
    }

    private void syncTeleport(Player player, Location location) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(location);
            }
        }.runTask(main);
    }
    
    public void teleportPlayer(Player player, String name, String path, String rotation) {
        World world = Bukkit.getWorld(name);
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
                LogUtils.doLog(player,
                        "<P> &cThe coordinates are invalid, " +
                                "teleporting to the world's spawn."
                );
                syncTeleport(player, world.getSpawnLocation());
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
                    LogUtils.doLog(player,
                            "<P> &cThe rotation numbers are invalid, " +
                                    "teleporting to the default location."
                    );
                    syncTeleport(player, new Location(world, x, y, z));
                    return;
                }

                location = new Location(world, x, y, z, yaw, pitch);
            }
        }
        else location = world.getSpawnLocation();

        syncTeleport(player, location);
    }

    public void teleportPlayer(ConfigurationSection id, Player player) {
        teleportPlayer(player, id.getString("spawn.world", ""),
                id.getString("spawn.x-y-z", ""),
                id.getString("spawn.yaw-pitch", ""));
    }

    public void sendMessages(Player sender, List<String> messages, boolean isPublic, boolean doLog) {
        if (messages.isEmpty()) return;

        for (String line : messages) {
            if (line == null || line.equals("")) continue;

            Matcher match = Pattern.compile("(?i)<ADD_EMPTY:(\\d+)>").matcher(line);
            if (match.find()) {
                for (int i = 0; i < Integer.parseInt(match.group(1)); i++) {
                    if (isPublic)
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(""));
                    else sender.sendMessage("");
                }
                continue;
            }

            line = parseInsensitiveEach(line, new String[] {"player", "world"},
                    new String[] {sender.getName(), sender.getWorld().getName()});
            line = removeSpace(getChatValues(sender, line));

            if (doLog && main.getConfig().getBoolean("options.send-console")) {
                String logLine = JsonMsg.centeredText(sender, TextParser.stripPrefix(line));
                LogUtils.doLog(logLine.replace(lineSplitter(), "&f" + lineSplitter()));
            }

            if (!isPublic) TextParser.send(null, sender, line);
            else for (Player p : Bukkit.getOnlinePlayers()) TextParser.send(p, sender, line);
        }
    }
    
    public void sendMessages(Player sender, List<String> messages, boolean isPublic) {
        sendMessages(sender, messages, isPublic, true);
    }

    public void runCommands(Player player, List<String> commands) {
        if (commands.isEmpty()) return;

        for (String line : commands) {
            if (line == null || line.equals("")) continue;
            line = removeSpace(line);
            boolean isPlayer = isStarting("[player]", line) && player != null;

            if (isPlayer) {
                line = parseInsensitiveEach(line, new String[] {"player", "world"},
                        new String[] {player.getName(), player.getWorld().getName()});
            }

            CommandSender sender = isPlayer ? player : Bukkit.getConsoleSender();
            String cmd = isPlayer ? parsePrefix("player", line) : line;

            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(sender, cmd);
                }
            }.runTask(main);
        }
    }
}
