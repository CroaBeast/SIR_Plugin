package me.croabeast.sircore.utils;

import me.croabeast.sircore.MainClass;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
    private String prefix() { return langUtils.parseColor("&e&lSIR &8> &f"); }

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

    private void eventPublic(Player player, List<String> list) {
        String split = main.getConfig().getString("options.line-separator", "<n>");
        String[] values = {player.getName(), player.getWorld().getName()};
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            message = langUtils.parsePAPI(player, message);

            if (main.getConfig().getBoolean("options.send-console"))
                main.consoleMsg(prefix() + message);

            if (message.startsWith("[ACTION-BAR]")) {
                for (Player p : players)
                    langUtils.actionBar(p, setUp("[ACTION-BAR]", message));
            }

            else if (message.startsWith("[TITLE]")) {
                String sp = Pattern.quote(split);
                for (Player p : players)
                    langUtils.title(p, setUp("[TITLE]", message).split(sp));
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

            if (main.getConfig().getBoolean("options.send-console"))
                main.consoleMsg(prefix() + message);

            if (message.startsWith("[ACTION-BAR]")) {
                langUtils.actionBar(player, setUp("[ACTION-BAR]", message));
            }

            else if (message.startsWith("[TITLE]")) {
                String sp = Pattern.quote(split);
                langUtils.title(player, setUp("[TITLE]", message).split(sp));
            }

            else langUtils.sendMixed(player, message);
        }
    }

    public ConfigurationSection joinSection(Player player) {
        String select = !player.hasPlayedBefore() ? "first-join" : "join";
        return main.getMessages().getConfigurationSection(select);
    }

    private void doAllEvent(ConfigurationSection id, Player player, boolean isJoin) {
        String soundString = id.getString("sound");
        if (isJoin && soundString != null) sound(player, soundString);

        eventPublic(player, id.getStringList("public"));
        if (isJoin) eventPrivate(player, id.getStringList("private"));
    }

    private boolean hasPerm(Player player, String perm) {
        boolean vault = main.getPerms().playerHas(null, player, perm);
        boolean perms = player.isPermissionSet(perm) && player.hasPermission(perm);
        return !perm.matches("(?i)DEFAULT") && (main.hasVault ? vault : perms);
    }

    public void getSections(ConfigurationSection section, Player player, boolean isJoin) {
        String maxPerm = ""; int highest = 0; ConfigurationSection finalId = null;

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

        if (finalId != null) doAllEvent(finalId, player, isJoin);
    }
}
