package me.croabeast.sircore.objects;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class Reporter {

    private final Application main;
    private final TextUtils text;
    private final EventUtils utils;

    private int ORDER = 0;
    private boolean isRunning = false;

    private BukkitRunnable runnable;

    public Reporter(Application main) {
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

    private void announcerLogger(ConfigurationSection id) {
        if (!text.getOption(1, "send-console")) return;
        List<String> list = id.getStringList("lines");
        if (list.isEmpty()) return;

        list.forEach(s -> {
            s = s.replace(text.getSplit(), "&r" + text.getSplit());
            main.getRecorder().doRecord(s);
        });
    }

    public void runSection(ConfigurationSection id) {
        if (main.everyPlayer().isEmpty()) return;
        main.everyPlayer().forEach(p -> playMessage(id, p));
        announcerLogger(id);
        utils.runCmds(id, null);
    }

    public void startTask() {
        if (getDelay() <= 0) {
            cancelTask();
            return;
        }

        if (getSection() == null) return;
        isRunning = true;

        List<String> keys = new ArrayList<>(getSection().getKeys(false));
        Map<Integer, ConfigurationSection> sections = new HashMap<>();

        keys.forEach(s ->
                sections.put(keys.indexOf(s), getSection().getConfigurationSection(s))
        );

        int count = sections.size() - 1;
        if (ORDER > count) ORDER = 0;

        runSection(sections.get(ORDER));

        if (!main.getAnnounces().getBoolean("random")) {
            if (ORDER < count) ORDER++;
            else ORDER = 0;
        }
        else ORDER = new Random().nextInt(count + 1);

        runnable = new BukkitRunnable() {
            @Override public void run() { startTask(); }
        };
        runnable.runTaskLaterAsynchronously(main, getDelay());
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Nullable
    public ConfigurationSection getSection() {
        return main.getAnnounces().getConfigurationSection("messages");
    }

    public int getDelay() {
        return main.getAnnounces().getInt("interval", 1200);
    }

    public void cancelTask() {
        if (runnable == null) return;
        isRunning = false;
        runnable.cancel();
    }
}
