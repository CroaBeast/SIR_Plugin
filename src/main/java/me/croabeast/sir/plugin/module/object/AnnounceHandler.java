package me.croabeast.sir.plugin.module.object;

import lombok.Getter;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.file.YAMLCache;
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

public class AnnounceHandler extends SIRModule implements CacheManageable {

    private static final Map<Integer, Announce> ANNOUNCE_MAP = new HashMap<>();

    private static int taskId = -1;
    private static int order = 0;

    @Getter
    private static boolean running = false;

    AnnounceHandler() {
        super(ModuleName.ANNOUNCEMENTS);
    }

    private static ConfigurationSection announceSection() {
        return YAMLCache.fromAnnounces("announces").getSection("announces");
    }

    @Priority(1)
    static void loadCache() {
        ConfigurationSection section = announceSection();
        if (section == null) return;

        if (!ANNOUNCE_MAP.isEmpty()) ANNOUNCE_MAP.clear();

        List<String> keys = new ArrayList<>(section.getKeys(false));

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null)
                ANNOUNCE_MAP.put(keys.indexOf(key), new Announce(s));
        }
    }

    private static YAMLFile config() {
        return YAMLCache.fromAnnounces("config");
    }

    private static final Function<ConfigurationSection, Set<Player>> PLAYERS = (c) -> {
        String perm = c.getString("permission", "DEFAULT");

        List<String> worlds = TextUtils.toList(c, "worlds");
        Set<Player> players = new HashSet<>();

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
        ConfigurationSection section = announceSection();
        int delay = config().get("interval", 0);

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

                    Announce a = ANNOUNCE_MAP.get(order);
                    a.display(PLAYERS.apply(a.id));

                    if (config().get("random", false)) {
                        order = new Random().nextInt(count + 1);
                        return;
                    }

                    order = order < count ? (order + 1) : 0;
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

        void display(Set<Player> players) {
            if (players.isEmpty()) return;

            LangUtils.executeCommands(null, commands);
            MessageSender.fromLoaded()
                    .setTargets(players).send(lines);
        }
    }
}
