package me.croabeast.sir.plugin.module.object;

import lombok.Getter;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.SIRRunnable;
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

public class AnnounceHandler extends SIRModule implements CacheHandler {

    private static final Map<Integer, Announce> ANNOUNCE_MAP = new HashMap<>();
    private static int order = 0;

    @Getter
    private boolean running = false;

    private SIRRunnable runnable;

    AnnounceHandler() {
        super(ModuleName.ANNOUNCEMENTS);
    }

    @Override
    public void register() {
        loadCache();
    }

    private static ConfigurationSection announceSection() {
        return FileCache.ANNOUNCE_CACHE.getCache("announces").getSection("announces");
    }

    @Priority(level = 1)
    static void loadCache() {
        ConfigurationSection section = announceSection();
        if (section == null) return;

        if (!ANNOUNCE_MAP.isEmpty()) ANNOUNCE_MAP.clear();

        List<String> keys = new ArrayList<>(section.getKeys(false));

        for (String key : keys) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            ANNOUNCE_MAP.put(keys.indexOf(key), new Announce(s));
        }
    }

    public static void displayAnnounce(ConfigurationSection id) {
        for (Announce a : ANNOUNCE_MAP.values()) if (a.id == id) a.display();
    }

    public void startTask() {
        int delay = FileCache.ANNOUNCE_CACHE.getConfig().getValue("interval", 0);
        ConfigurationSection section = announceSection();

        if (!isEnabled() || delay <= 0 || section == null) {
            cancelTask();
            return;
        }

        running = true;

        int count = ANNOUNCE_MAP.size() - 1;
        if (order > count) order = 0;

        ANNOUNCE_MAP.get(order).display();

        if (!FileCache.ANNOUNCE_CACHE.getConfig().getValue("random", false)) {
            if (order < count) order++;
            else order = 0;
        }
        else order = new Random().nextInt(count + 1);

        runnable = new SIRRunnable(this::startTask);
        runnable.runTaskLater(delay);
    }

    public void cancelTask() {
        if (runnable == null) return;

        running = false;
        runnable.cancel();
    }

    private static class Announce {

        private final ConfigurationSection id;

        private final List<String> lines, commands;
        private final List<Player> players;

        private Announce(ConfigurationSection id) {
            this.id = id;

            this.commands = TextUtils.toList(id, "commands");
            this.lines = TextUtils.toList(id, "lines");

            String perm = id.getString("permission", "DEFAULT");

            List<String> worlds = TextUtils.toList(id, "worlds");
            List<Player> players = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                String world = p.getWorld().getName();

                if (!worlds.isEmpty() && !worlds.contains(world))
                    continue;

                if (VanishHook.isVanished(p)) continue;
                if (!PlayerUtils.hasPerm(p, perm)) continue;

                players.add(p);
            }

            this.players = players;
        }

        void display() {
            if (players.isEmpty()) return;

            MessageSender.fromLoaded().setTargets(players).send(lines);
            LangUtils.executeCommands(null, commands);
        }
    }
}
