package me.croabeast.sircore.utilities;

import me.croabeast.sircore.*;
import me.croabeast.sircore.hooks.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

public class EventUtils {

    private final Application main;
    private final Initializer init;

    private final TextUtils text;
    private final PermUtils perms;

    public EventUtils(Application main) {
        this.main = main;
        this.init = main.getInitializer();

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
        return isColor ? text.colorize(player, message) : message;
    }

    private String isJoinString(Player p, boolean isJoin) {
        return isJoin ? (!p.hasPlayedBefore() ? "first-join" : "join") : "quit";
    }

    @Nullable
    public ConfigurationSection resultSection(FileConfiguration file, Player player, String path) {
        ConfigurationSection resultSection = null;

        String maxPerm = null;
        int highest = 0;

        ConfigurationSection section = file.getConfigurationSection(path);
        if (section == null || section.getKeys(false).isEmpty()) return null;

        for (String key : new ArrayList<>(section.getKeys(false))) {
            ConfigurationSection id = section.getConfigurationSection(key);
            assert id != null;

            String perm = id.getString("permission", "DEFAULT");
            int priority = id.getInt("priority", perm.matches("(?i)DEFAULT") ? 0 : 1);

            if (priority > highest) {
                maxPerm = perm;
                highest = priority;
            }
        }

        for (String key : new ArrayList<>(section.getKeys(false))) {
            ConfigurationSection id = section.getConfigurationSection(key);
            assert id != null;

            String perm = id.getString("permission", "DEFAULT");

            if (perm.matches("(?i)" + maxPerm) && perms.certainPerm(player, maxPerm)) return id;
            else if (perms.certainPerm(player, perm)) resultSection = id;
            else if (perm.matches("(?i)DEFAULT")) resultSection = id;
        }

        return resultSection;
    }

    @Nullable
    public ConfigurationSection resultSection(Player player, String path) {
        return resultSection(main.getMessages(), player, path);
    }

    @Nullable
    public ConfigurationSection resultSection(Player player, boolean isJoin) {
        return resultSection(player, isJoinString(player, isJoin));
    }

    private void sendToConsole(String message) {
        if (!text.getOption(1, "send-console")) return;
        String split = text.getSplit();
        message = message.replace(split, "&r" + split);
        main.getRecorder().doRecord("&7> &f" + message);
    }

    private String removeSpace(String line) {
        if (text.getOption(1, "hard-spacing")) {
            String startLine = line;
            try {
                while (line.charAt(0) == ' ') line = line.substring(1);
                return line;
            }
            catch (IndexOutOfBoundsException e) {
                return startLine;
            }
        }
        else return line.startsWith(" ") ? line.substring(1) : line;
    }

    public String parsePrefix(String type, String message) {
        message = message.substring(type.length());
        return removeSpace(message);
    }

    public void typeMessage(Player player, String line) {
        if (line.startsWith("[ACTION-BAR]"))
            text.actionBar(player, parsePrefix("[ACTION-BAR]", line));
        else if (line.startsWith("[TITLE]")) {
            String split = Pattern.quote(text.getSplit());
            text.title(player, parsePrefix("[TITLE]", line).split(split), null);
        }
        else if (line.startsWith("[JSON]") && line.contains("{\"text\":"))
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), "minecraft:tellraw " +
                    player.getName() + " " + parsePrefix("[JSON]", line)
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
        catch (Exception e) {
            main.getRecorder().doRecord(player, "<P> The sound you input is invalid.");
            return;
        }

        player.playSound(player.getLocation(), sound, 1, 1);
    }

    private void giveGod(ConfigurationSection id, Player player) {
        int godTime = id.getInt("invulnerable") ;
        if (main.MC_VERSION <= 8 | godTime <= 0) return;

        Runnable god = () -> player.setInvulnerable(false);
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(main, god, godTime);
    }

    public void goSpawn(ConfigurationSection id, Player player) {
        String[] coordinates, rotations;
        Location location;

        if (!id.isConfigurationSection("spawn")) return;

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

    private void runMsgs(ConfigurationSection id, Player player, boolean isPublic) {
        List<String> messages = id.getStringList(isPublic ? "public" : "private");
        if (messages.isEmpty()) return;

        for (String line : messages) {
            if (line == null || line.equals("")) continue;
            line = removeSpace(line);

            line = doFormat(line, player, false);
            sendToConsole(text.parsePAPI(player, line));
            String message = text.colorize(player, line);

            if (!isPublic) typeMessage(player, message);
            else main.everyPlayer().forEach(p -> typeMessage(p, message));
        }
    }

    public void runCmds(ConfigurationSection id, Player player) {
        List<String> commands = id.getStringList("commands");
        if (commands.isEmpty()) return;

        for (String line : commands) {
            if (line == null || line.equals("")) continue;
            line = removeSpace(line);
            if (player != null)
                line = doFormat(line, player, false);

            boolean isPlayer = line.startsWith("[PLAYER]") && player != null;
            CommandSender sender = isPlayer ? player : Bukkit.getConsoleSender();
            String cmd = isPlayer ? parsePrefix("[PLAYER]", line) : line;
            new BukkitRunnable() {
                @Override public void run() { Bukkit.dispatchCommand(sender, cmd); }
            }.runTask(main);
        }
    }

    private void initEventTasks(ConfigurationSection id, Player p, boolean isJoin, boolean doTP) {
        runMsgs(id, p, true);
        if (isJoin) {
            runMsgs(id, p, false);
            playSound(id, p);
            giveGod(id, p);
            if (doTP) goSpawn(id, p);
        }
        runCmds(id, isJoin ? p : null);

        if (init.DISCORD && init.discordServer() != null)
            new DiscordMsg(main, p, isJoinString(p, isJoin)).sendMessage();
    }

    public void runEvent(ConfigurationSection id, Player p, boolean isJoin, boolean doTP, boolean isLogin) {
        int ticks = main.getConfig().getInt("login.ticks-after");
        if (!isLogin || ticks <= 0) initEventTasks(id, p, doTP, isJoin);
        else new BukkitRunnable() {
            @Override public void run() { initEventTasks(id, p, isJoin, doTP); }
        }.runTaskLaterAsynchronously(main, ticks);
    }
}
