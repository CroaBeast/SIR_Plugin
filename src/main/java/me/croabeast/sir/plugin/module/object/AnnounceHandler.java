package me.croabeast.sir.plugin.module.object;

import lombok.Getter;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

public class AnnounceHandler extends SIRModule implements CacheHandler {

    private static final Map<Integer, Announce> ANNOUNCE_MAP = new HashMap<>();

    private static int taskId = -1;
    private static int order = 0;

    @Getter
    private static boolean running = false;

    AnnounceHandler() {
        super(ModuleName.ANNOUNCEMENTS);
    }

    @Priority(level = 1)
    static void loadCache() {
        ConfigurationSection section = announceSection();
        if (section == null) return;

        if (!ANNOUNCE_MAP.isEmpty()) ANNOUNCE_MAP.clear();

        List<String> keys = new ArrayList<>(section.getKeys(false));

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            ANNOUNCE_MAP.put(keys.indexOf(key), new Announce(s));
        }
    }

    @Override
    public void register() {}

    private static ConfigurationSection announceSection() {
        return FileCache.ANNOUNCE_CACHE.getCache("announces").getSection("announces");
    }

    private static FileCache config() {
        return FileCache.ANNOUNCE_CACHE.getConfig();
    }

    private static final Function<ConfigurationSection, List<Player>> PLAYERS = (c) -> {
        String perm = c.getString("permission", "DEFAULT");

        List<String> worlds = TextUtils.toList(c, "worlds");
        List<Player> players = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName();

            if (!worlds.isEmpty() && !worlds.contains(world))
                continue;

            if (VanishHook.isVanished(p)) continue;
            if (!PlayerUtils.hasPerm(p, perm)) continue;

            players.add(p);
        }

        return players;
    };

    public static void startTask() {
        final int delay = config().getValue("interval", 0);
        ConfigurationSection section = announceSection();

        if (!ModuleName.ANNOUNCEMENTS.isEnabled() ||
                delay <= 0 ||
                section == null)
            return;

        if (running) return;

        taskId = Bukkit.getScheduler().runTaskTimer(
                SIRPlugin.getInstance(),
                () -> {
                    int count = ANNOUNCE_MAP.size() - 1;
                    if (order > count) order = 0;

                    Announce announce = ANNOUNCE_MAP.get(order);
                    announce.display(PLAYERS.apply(announce.id));

                    order = config().getValue("random", false) ?
                            new Random().nextInt(count + 1) :
                            (order < count ? (order + 1) : 0);
                },
                0L, delay
        ).getTaskId();

        running = true;
    }

    public static void cancelTask() {
        if (!running) return;

        Bukkit.getScheduler().cancelTask(taskId);
        running = false;
    }

    public static void displayAnnounce(ConfigurationSection id) {
        for (Announce a : ANNOUNCE_MAP.values())
            if (a.id == id) a.display(PLAYERS.apply(a.id));
    }

    private static class Announce {

        private final List<String> lines, commands;

        private final ConfigurationSection id;

        private Announce(ConfigurationSection id) {
            this.id = id;

            this.commands = TextUtils.toList(id, "commands");
            this.lines = TextUtils.toList(id, "lines");
        }

        void display(Collection<Player> players) {
            if (players.isEmpty()) return;

            LangUtils.executeCommands(null, commands);
            MessageSender.fromLoaded()
                    .setTargets(players).send(lines);
        }
    }
}