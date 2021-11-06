package me.croabeast.sircore.objects;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;

import java.util.*;

public class Announcer {

    private final Application main;
    private final TextUtils text;
    private final EventUtils utils;

    private int order = 0;
    private boolean isRunning = false;

    private ConfigurationSection id;
    private BukkitRunnable runnable;

    public Announcer(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
    }

    private void playMessage(ConfigurationSection id, Player player) {
        utils.playsound(id, player);
        for (String line : id.getStringList("lines")) {
            if (line == null || line.equals("")) continue;
            if (line.startsWith(" ")) line = line.substring(1);
            line = utils.doFormat(line, player, true);
            utils.typeMessage(player, line);
        }
    }

    private void logAnnounces(String message) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        String split = text.getValue("split");
        String prefix = "[SIR-ANNOUNCES] ";

        message = message.replace(split, "&r" + split);
        main.getRecords().rawRecord(prefix + message);
    }

    private void runCommands(ConfigurationSection id) {
        for (String message : id.getStringList("commands")) {
            if (message == null || message.equals("")) continue;
            if (message.startsWith(" ")) message = message.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
        }
    }

    public void runSection(ConfigurationSection id) {
        Bukkit.getOnlinePlayers().forEach(p -> playMessage(id, p));
        id.getStringList("lines").forEach(this::logAnnounces);
        runCommands(id);
    }

    public void startTask() {
        if (getDelay() <= 0) {
            cancelTask();
            return;
        }
        isRunning = true;

        id = main.getAnnounces().getConfigurationSection("messages");
        if (id == null) return;
        List<String> keys = new ArrayList<>(id.getKeys(false));
        Map<Integer, ConfigurationSection> sections = new HashMap<>();

        keys.forEach(s -> {
            ConfigurationSection i = id.getConfigurationSection(s);
            sections.put(keys.indexOf(s), i);
        });

        int count = sections.size() - 1;
        if (order > count) order = 0;
        runSection(sections.get(order));

        if (!main.getAnnounces().getBoolean("random")) {
            if (order < count) order++;
            else order = 0;
        }
        else order = new Random().nextInt(count + 1);

        runnable = new BukkitRunnable() {
            @Override
            public void run() { startTask(); }
        };
        runnable.runTaskLater(main, getDelay());
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getDelay() {
        return main.getAnnounces().getInt("interval", 20 * 60);
    }

    public void cancelTask() {
        if (runnable == null) return;
        isRunning = false;
        runnable.cancel();
    }
}
