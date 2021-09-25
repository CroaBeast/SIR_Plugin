package me.croabeast.sircore.utils;

import me.croabeast.sircore.MainClass;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EventUtils {

    private final MainClass main;
    private final LangUtils langUtils;

    public EventUtils(MainClass main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
    }

    private final String[] keys = {"{PLAYER}", "{WORLD}"};
    private final String prefix = "&e&lSIR &8> &f";

    private boolean isSound(String enumName) {
        if (enumName == null) return false;
        try {
            Enum.valueOf(Sound.class, enumName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void sound(Player player, String sound) {
        if (!isSound(sound)) return;
        player.playSound(player.getLocation(), Sound.valueOf(sound), 1, 1);
    }

    private String setUp(String type, String message) {
        message = message.substring(type.length());
        if (message.startsWith(" ")) message = message.substring(1);
        return message;
    }

    private boolean hasPerm(Player player, String perm) {
        boolean vault = main.getPerms().playerHas(null, player, perm);
        boolean perms = player.isPermissionSet(perm) && player.hasPermission(perm);
        return !perm.matches("(?i)DEFAULT") && (main.hasVault ? vault : perms);
    }

    public ConfigurationSection lastSection(Player player, boolean isJoin) {
        String maxPerm = ""; int highest = 0; ConfigurationSection finalId = null;
        String ID = isJoin ? (!player.hasPlayedBefore() ? "first-join" : "join") : "quit";

        ConfigurationSection section = main.getMessages().getConfigurationSection(ID);
        if (section == null) return null;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;

            String perm = id.getString("permission", "DEFAULT");
            int before = perm.matches("(?i)DEFAULT") ? 0 : 1;
            int priority = id.getInt("priority", before);

            if (priority > highest) { highest = priority; maxPerm = perm; }
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

    private void eventPublic(Player player, List<String> list) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        String[] values = {player.getName(), player.getWorld().getName()};
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            message = langUtils.parsePAPI(player, message);

            if (main.getConfig().getBoolean("options.send-console", true))
                main.logger(prefix + message);

            if (message.startsWith("[ACTION-BAR]"))
                for (Player p : players)
                    langUtils.actionBar(p, setUp("[ACTION-BAR]", message));

            else if (message.startsWith("[TITLE]")) {
                String sp = Pattern.quote(split);
                for (Player p : players) {
                    message = message.replace(split, "&r" + split);
                    langUtils.title(p, setUp("[TITLE]", message).split(sp));
                }
            }

            else for (Player p : players) langUtils.sendMixed(p, message);
        }
    }

    private void eventPrivate(Player player, List<String> list) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        String[] values = {player.getName(), player.getWorld().getName()};

        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            message = langUtils.parsePAPI(player, message);

            if (main.getConfig().getBoolean("options.send-console",true))
                main.logger(prefix + message);

            if (message.startsWith("[ACTION-BAR]"))
                langUtils.actionBar(player, setUp("[ACTION-BAR]", message));

            else if (message.startsWith("[TITLE]")) {
                String sp = Pattern.quote(split);
                message = message.replace(split, "&r" + split);
                langUtils.title(player, setUp("[TITLE]", message).split(sp));
            }

            else langUtils.sendMixed(player, message);
        }
    }

    private void eventCommand(Player player, List<String> list, boolean isJoin) {
        String[] values = {player.getName(), player.getWorld().getName()};

        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);

            if (message.startsWith("[PLAYER]") && isJoin)
                Bukkit.dispatchCommand(player, setUp("[PLAYER]", message));

            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    public void eventSpawn(ConfigurationSection id, Player player) {
        Location location; String[] coordinates;
        String path = id.getString("spawn.x-y-z", "");
        World world = Bukkit.getWorld(id.getString("spawn.world",""));
        if (world == null) return;

        if (path.trim().equals("")) location = world.getSpawnLocation();
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

    private void doLogger(ConfigurationSection id, Player player) {
        if (id != null) return;
        String prefix = "&7 &4&lSIR-DEBUG &8> ";
        langUtils.sendMixed(player, prefix + "&cA valid message group isn't found...");
        langUtils.sendMixed(player, prefix + "&7Please check your &messages.yml &7file.");
        main.logger(prefix + "&cA valid message group isn't found...");
        main.logger(prefix + "&7Please check your &messages.yml &7file.");
    }

    public void doAllEvent(ConfigurationSection id, Player player, boolean isJoin, boolean doSpawn) {
        String soundString = id.getString("sound"); doLogger(id, player);

        if (isJoin && soundString != null) sound(player, soundString);
        if (isJoin && doSpawn) eventSpawn(id, player);

        eventPublic(player, id.getStringList("public"));
        if (isJoin) eventPrivate(player, id.getStringList("private"));
        eventCommand(player, id.getStringList("commands"), isJoin);
    }
}
