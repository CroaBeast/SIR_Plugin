package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

public final class AnnounceHandler extends SIRModule {

    private final ConfigurableFile config, announces;
    private static final Map<Integer, Announce> ANNOUNCE_MAP = new HashMap<>();

    private static int taskId = -1;
    private static int order = 0;

    @Getter
    private boolean running = false;

    AnnounceHandler() {
        super("announcements");

        this.config = YAMLData.Module.ANNOUNCEMENT.fromName("config");
        this.announces = YAMLData.Module.ANNOUNCEMENT.fromName("announces");
    }

    public boolean register() {
        if (running && !isEnabled()) {
            stop();
            return false;
        }

        ConfigurationSection section = announces.getSection("announces");
        if (section == null) return false;

        ANNOUNCE_MAP.clear();
        int index = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null) ANNOUNCE_MAP.put(index++, new Announce(s));
        }

        start();
        return true;
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

    public void start() {
        ConfigurationSection section = announces.getSection("announces");
        int delay = config.get("interval", 0);

        if (!isEnabled() || delay <= 0 || section == null || running)
            return;

        taskId = Bukkit.getScheduler().runTaskTimer(
                SIRPlugin.getInstance(),
                () -> {
                    int count = ANNOUNCE_MAP.size() - 1;
                    if (order > count) order = 0;

                    Announce a = ANNOUNCE_MAP.get(order);
                    a.display(PLAYERS.apply(a.id));

                    if (config.get("random", false)) {
                        order = new Random().nextInt(count + 1);
                        return;
                    }

                    order = order < count ? (order + 1) : 0;
                },
                0L, delay
        ).getTaskId();

        running = true;
    }

    public void stop() {
        if (!running) return;

        Bukkit.getScheduler().cancelTask(taskId);
        running = false;
    }

    public boolean displayAnnounce(String id) {
        for (Announce a : ANNOUNCE_MAP.values())
            if (a.id.getName().equals(id)) {
                a.display(PLAYERS.apply(a.id));
                return true;
            }

        return false;
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
            MessageSender.loaded()
                    .setTargets(players).send(lines);
        }
    }
}
