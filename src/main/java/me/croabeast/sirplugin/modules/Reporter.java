package me.croabeast.sirplugin.modules;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

import static me.croabeast.sirplugin.utilities.TextUtils.lineSplitter;

public class Reporter extends BaseModule {

    private final SIRPlugin main;
    private final EventUtils utils;

    private int ORDER = 0;
    private boolean isRunning = false;

    private BukkitRunnable runnable;
    Map<Integer, ConfigurationSection> sections = new HashMap<>();

    public Reporter(SIRPlugin main) {
        this.main = main;
        this.utils = main.getEventUtils();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.ANNOUNCES;
    }

    @Override
    public void registerModule() {
        if (getSection() == null) return;
        if (!sections.isEmpty()) sections.clear();

        List<String> keys = new ArrayList<>(getSection().getKeys(false));
        for (String key : keys)
            sections.put(keys.indexOf(key), getSection().getConfigurationSection(key));
    }

    private List<Player> getPlayers(String perm) {
        if (perm.matches("(?i)DEFAULT"))
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        else
            return Bukkit.getOnlinePlayers().stream().
                    filter(p -> PermUtils.hasPerm(p, perm)).
                    collect(Collectors.toList());
    }

    public void runSection(ConfigurationSection id) {
        String perm = id.getString("permission", "DEFAULT");
        if (getPlayers(perm).isEmpty()) return;

        List<String> msgs = TextUtils.fileList(id, "lines");
        List<String> cmds = TextUtils.fileList(id, "commands");

        if (!msgs.isEmpty()) {
            getPlayers(perm).forEach(p ->
                    utils.sendMessages(p, msgs, false, false));

            if (main.getConfig().getBoolean("options.send-console")) {
                for (String line : msgs) {
                    line = JsonMsg.centeredText(null, line);
                    LogUtils.doLog(line.replace(lineSplitter(), "&f" + lineSplitter()));
                }
            }
        }

        if (!cmds.isEmpty()) utils.runCommands(null, cmds);
    }

    public void startTask() {
        if (getDelay() <= 0) {
            cancelTask();
            return;
        }

        if (getSection() == null) return;
        isRunning = true;

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
        return isRunning;
    }

    @Nullable
    public ConfigurationSection getSection() {
        return main.getAnnounces().getConfigurationSection("announces");
    }

    public int getDelay() {
        return main.getModules().getInt("announces.interval", 1200);
    }

    public void cancelTask() {
        if (runnable == null) return;
        isRunning = false;
        runnable.cancel();
    }
}
