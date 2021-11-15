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

    private int ORDER = 0;
    private boolean IS_RUNNING = false;

    private ConfigurationSection id;
    private BukkitRunnable runnable;

    public Announcer(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
    }

    private void playMessage(ConfigurationSection id, Player player) {
        utils.playSound(id, player);

        for (String line : id.getStringList("lines")) {
            if (line == null || line.equals("")) continue;
            if (line.startsWith(" ")) line = line.substring(1);

            line = utils.doFormat(line, player, true);
            utils.typeMessage(player, line);
        }
    }

    private void lineLogger(String line) {
        main.getRecords().rawRecord("[SIR-ANNOUNCES] " +
                line.replace(
                        text.getSplit(),
                        "&r" + text.getSplit()
                )
        );
    }

    public void runSection(ConfigurationSection id) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Bukkit.getOnlinePlayers().forEach(p -> playMessage(id, p));
        id.getStringList("lines").forEach(this::lineLogger);
        utils.runCmds(id, null);
    }

    public void startTask() {
        if (getDelay() <= 0) {
            cancelTask();
            return;
        }
        IS_RUNNING = true;

        id = main.getAnnounces().getConfigurationSection("messages");
        if (id == null) return;

        List<String> keys = new ArrayList<>(id.getKeys(false));
        Map<Integer, ConfigurationSection> sections = new HashMap<>();

        keys.forEach(s -> sections.put(keys.indexOf(s), id.getConfigurationSection(s)));

        int count = sections.size() - 1;
        if (ORDER > count) ORDER = 0;
        runSection(sections.get(ORDER));

        if (!main.getAnnounces().getBoolean("random")) {
            if (ORDER < count) ORDER++;
            else ORDER = 0;
        }
        else ORDER = new Random().nextInt(count + 1);

        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                startTask();
            }
        };
        runnable.runTaskLater(main, getDelay());
    }

    public boolean isRunning() {
        return IS_RUNNING;
    }

    public int getDelay() {
        return main.getAnnounces().getInt("interval", 20 * 60);
    }

    public void cancelTask() {
        if (runnable == null) return;
        IS_RUNNING = false;
        runnable.cancel();
    }
}
