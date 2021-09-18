package me.croabeast.sir.utils;

import me.croabeast.sir.SIR;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EventUtils {

    private final SIR main;
    private final LangUtils langUtils;

    Map<String, Integer> perms = new HashMap<>();
    List<Player> deniedPlayers = new ArrayList<>();

    public EventUtils(SIR main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
    }


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

    private void eventSend(Player player, List<String> list) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] values = {player.getName(), player.getWorld() + ""};
        String split = main.getConfig().getString("options.line-splitter");
        String prefix = langUtils.parseColor("&e&lSIR &8> &f");
        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            if (main.getConfig().getBoolean("options.send-console")) {
                message = langUtils.parsePAPI(player, message);
                Bukkit.getConsoleSender().sendMessage(prefix + message);
            }
            if (message.startsWith("[ACTION-BAR]")) {
                message = message.substring(12);
                if (message.startsWith(" ")) message = message.substring(1);
                for (Player p : Bukkit.getOnlinePlayers()) langUtils.actionBar(p, message);
            }
            else if (message.startsWith("[TITLE]")) {
                message = message.substring(7);
                if (message.startsWith(" ")) message = message.substring(1);
                if (split == null) split = "";
                for (Player p : Bukkit.getOnlinePlayers())
                    langUtils.title(p, message.split(Pattern.quote(split)));
            }
            else for (Player p : Bukkit.getOnlinePlayers()) langUtils.sendMixed(p, message);
        }
    }

    private void eventMotd(Player player, List<String> list) {
        String[] keys = {"{PLAYER}", "{WORLD}"};
        String[] values = {player.getName(), player.getWorld() + ""};
        for (String message : list) {
            if (message == null || message.equals("")) continue;
            message = StringUtils.replaceEach(message, keys, values);
            if (message.startsWith(" ")) message = message.substring(1);
            langUtils.sendMixed(player, message);
        }
    }

    public ConfigurationSection joinSection(Player player) {
        String select = !player.hasPlayedBefore() ? "first-join" : "join";
        return main.getMessages().getConfigurationSection(select);
    }

    public void addPerms(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;
            String perm = id.getString("permission", "DEFAULT");
            int priority = id.getInt("priority", 1);
            perm = perm.toUpperCase(); if (perm.equals("DEFAULT")) priority = 0;
            if (perm.equals("OP")) priority = Integer.MAX_VALUE;
            perms.put(perm, priority);
        }
    }

    public void checkSections(ConfigurationSection section, Player player, boolean isJoin) {
        for (String key : section.getKeys(false)) {
            if (isJoin && deniedPlayers.contains(player)) return;
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;
            String perm = id.getString("permission", "DEFAULT");
            String sound = id.getString("sound");
            int priority = id.getInt("priority", 1);
            perm = perm.toUpperCase(); if (perm.equals("DEFAULT")) priority = 0;
            if (perm.equals("OP")) priority = Integer.MAX_VALUE;
            if (priority == perms.get(perm)) {
                if (isJoin && sound != null) sound(player, sound);
                eventSend(player, id.getStringList("messages"));
                if (isJoin) eventMotd(player, id.getStringList("motd"));
                if (isJoin) deniedPlayers.add(player);
                else deniedPlayers.remove(player);
            }
        }
        perms.clear();
    }
}
