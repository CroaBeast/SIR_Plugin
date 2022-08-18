package me.croabeast.sirplugin.module;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.object.Sender;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

public class Announcer extends SIRModule {

    private final SIRPlugin main = SIRPlugin.getInstance();
    private final Map<Integer, ConfigurationSection> sections = new HashMap<>();

    private int ORDER = 0;
    private boolean isRunning = false;

    private BukkitRunnable runnable;

    @Override
    public @NotNull Identifier getIdentifier() {
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
                    filter(p -> PlayerUtils.hasPerm(p, perm)).
                    collect(Collectors.toList());
    }

    public void runSection(ConfigurationSection id) {
        List<Player> players = getPlayers(id.getString("permission", "DEFAULT"));
        if (players.isEmpty()) return;

        Sender.to(id, "lines").send(players, null);
        Sender.to(id, "commands").execute(null);
    }

    public void startTask() {
        if (!isEnabled()) {
            cancelTask();
            return;
        }

        if (getDelay() <= 0) {
            cancelTask();
            return;
        }

        if (getSection() == null) return;
        isRunning = true;

        int count = sections.size() - 1;
        if (ORDER > count) ORDER = 0;

        runSection(sections.get(ORDER));

        if (!FileCache.ANNOUNCES.get().getBoolean("random")) {
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
        return FileCache.ANNOUNCES.getSection("announces");
    }

    public int getDelay() {
        return FileCache.MODULES.get().getInt("announces.interval");
    }

    public void cancelTask() {
        if (runnable == null) return;
        isRunning = false;
        runnable.cancel();
    }
}
