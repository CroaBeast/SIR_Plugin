package me.croabeast.sirplugin.module;

import lombok.Getter;
import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.instance.SIRModule;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AnnounceViewer extends SIRModule {

    private static final Map<Integer, ConfigurationSection> SECTION_MAP = new HashMap<>();
    private static int order = 0;

    private final SIRPlugin main = SIRPlugin.getInstance();

    @Getter
    private boolean running = false;

    private BukkitRunnable runnable;

    public AnnounceViewer() {
        super("announces");
    }

    @Override
    public void registerModule() {
        if (getSection() == null) return;
        if (!SECTION_MAP.isEmpty()) SECTION_MAP.clear();

        List<String> keys = new ArrayList<>(getSection().getKeys(false));

        for (String key : keys)
            SECTION_MAP.put(
                    keys.indexOf(key),
                    getSection().
                    getConfigurationSection(key)
            );
    }

    private static List<Player> getPlayers(String perm) {
        return Bukkit.getOnlinePlayers().stream().
                filter(VanishHook::isVisible).
                filter(p -> PlayerUtils.hasPerm(p, perm)).
                collect(Collectors.toList());
    }

    public static void runSection(ConfigurationSection id) {
        var players = getPlayers(id.getString("permission", "DEFAULT"));
        if (players.isEmpty()) return;

        LangUtils.getSender().setTargets(players).send(TextUtils.toList(id, "lines"));
        LangUtils.executeCommands(null, TextUtils.toList(id, "commands"));
    }

    public void startTask() {
        if (!isEnabled() || getDelay() <= 0 || getSection() == null) {
            cancelTask();
            return;
        }

        running = true;

        int count = SECTION_MAP.size() - 1;
        if (order > count) order = 0;

        runSection(SECTION_MAP.get(order));

        if (!FileCache.ANNOUNCEMENTS.getValue("random", false)) {
            if (order < count) order++;
            else order = 0;
        }
        else order = new Random().nextInt(count + 1);

        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                startTask();
            }
        };
        runnable.runTaskLater(main, getDelay());
    }

    @Nullable
    public static ConfigurationSection getSection() {
        return FileCache.ANNOUNCEMENTS.getSection("announces");
    }

    static int getDelay() {
        return FileCache.MODULES.getValue("announces.interval", 0);
    }

    public void cancelTask() {
        if (runnable == null) return;
        running = false;
        runnable.cancel();
    }
}
