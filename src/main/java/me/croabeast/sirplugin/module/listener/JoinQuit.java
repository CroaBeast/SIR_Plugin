package me.croabeast.sirplugin.module.listener;

import lombok.var;
import me.croabeast.beanslib.builder.BossbarBuilder;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.event.SIRLoginEvent;
import me.croabeast.sirplugin.event.SIRVanishEvent;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.object.analytic.Amender;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.object.instance.SIRListener;
import me.croabeast.sirplugin.object.instance.SIRViewer;
import me.croabeast.sirplugin.task.message.DirectTask;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static me.croabeast.sirplugin.utility.PlayerUtils.*;

public class JoinQuit extends SIRViewer {

    public static final Set<Player> LOGGED_PLAYERS = new HashSet<>();

    private static final HashMap<UUID, Long> JOIN_MAP = new HashMap<>(),
            QUIT_MAP = new HashMap<>(),
            PLAY_MAP = new HashMap<>();

    public JoinQuit() {
        super("join-quit");
    }

    @Override
    public void registerModule() {
        register();
        if (LoginHook.isEnabled()) new SIRLoginHook().register();
        if (VanishHook.isEnabled()) new SIRVanishHook().register();
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Amender.initUpdater(player);
        if (!isEnabled()) return;

        String path = !player.hasPlayedBefore() ? "first-join" : "join";
        var id = FileCache.JOIN_QUIT.permSection(player, path);

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (FileCache.MODULES.getValue("join-quit.default-messages.disable-join", true))
            event.setJoinMessage(null);

        int playTime = FileCache.MODULES.getValue("join-quit.cooldown.between", 0),
                joinCooldown = FileCache.MODULES.getValue("join-quit.cooldown.join", 0);

        if (joinCooldown > 0 && JOIN_MAP.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - JOIN_MAP.get(uuid);
            if (rest < joinCooldown * 1000L) return;
        }

        if (VanishHook.isVanished(player)) return;
        String p = "join-quit.login.";

        if (LoginHook.isEnabled() && FileCache.MODULES.getValue(p + "enabled", true)) {
            if (FileCache.MODULES.getValue(p + "spawn-before", false))
                PlayerUtils.teleport(id, player);
            return;
        }

        new Section(event, id, player).runTasks();
        final long data = System.currentTimeMillis();

        if (joinCooldown > 0) JOIN_MAP.put(uuid, data);
        if (playTime > 0) PLAY_MAP.put(uuid, data);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        var bar = BossbarBuilder.getBuilder(player);
        if (bar != null) bar.unregister();

        if (getGodPlayers().contains(player)) {
            player.setInvulnerable(false);
            getGodPlayers().remove(player);
        }

        DirectTask.RECEIVER_MAP.remove(Bukkit.getConsoleSender(), player);
        DirectTask.RECEIVER_MAP.remove(player);

        if (!isEnabled()) return;

        var id = FileCache.JOIN_QUIT.permSection(player, "quit");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (FileCache.MODULES.getValue("join-quit.default-messages.disable-quit", true))
            event.setQuitMessage(null);

        int playTime = FileCache.MODULES.getValue("join-quit.cooldown.between", 0),
                quitCooldown = FileCache.MODULES.getValue("join-quit.cooldown.quit", 0);

        final long now = System.currentTimeMillis();

        if (quitCooldown > 0 && QUIT_MAP.containsKey(uuid) &&
                now - QUIT_MAP.get(uuid) < quitCooldown * 1000L) return;
        if (playTime > 0 && PLAY_MAP.containsKey(uuid) &&
                now - PLAY_MAP.get(uuid) < playTime * 1000L) return;

        if (VanishHook.isVanished(player)) return;

        if (LoginHook.isEnabled()) {
            if (!LOGGED_PLAYERS.contains(player)) return;
            LOGGED_PLAYERS.remove(player);
        }

        new Section(event, id, player).runTasks();
        if (quitCooldown > 0) QUIT_MAP.put(uuid, System.currentTimeMillis());
    }

    class SIRLoginHook implements SIRListener {

        @EventHandler
        private void onLogin(SIRLoginEvent event) {
            Player player = event.getPlayer();

            UUID uuid = player.getUniqueId();
            LOGGED_PLAYERS.add(player);

            if (!isEnabled()) return;

            if (!FileCache.MODULES.getValue("join-quit.login.enabled", false)) return;
            if (VanishHook.isVanished(player)) return;

            var id = FileCache.JOIN_QUIT.permSection(
                    player, !player.hasPlayedBefore() ? "first-join" : "join");

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int playTime = FileCache.MODULES.getValue("join-quit.cooldown.between", 0),
                    joinCooldown = FileCache.MODULES.getValue("join-quit.cooldown.join", 0);

            if (joinCooldown > 0 && JOIN_MAP.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - JOIN_MAP.get(uuid);
                if (rest < joinCooldown * 1000L) return;
            }

            new Section(event, id, player).runTasks();

            final long data = System.currentTimeMillis();
            if (joinCooldown > 0) JOIN_MAP.put(uuid, data);
            if (playTime > 0) PLAY_MAP.put(uuid, data);
        }
    }

    class SIRVanishHook implements SIRListener {

        @EventHandler
        private void onVanish(SIRVanishEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!isEnabled()) return;

            if (!VanishHook.isEnabled() ||
                    !FileCache.MODULES.getValue("join-quit.vanish.enabled", false)) return;

            if (LoginHook.isEnabled()) {
                if (event.isVanished()) LOGGED_PLAYERS.add(player);
                else LOGGED_PLAYERS.remove(player);
            }

            String path = event.isVanished() ? "join" : "quit";
            var id = FileCache.JOIN_QUIT.permSection(player, path);

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int timer = FileCache.MODULES.getValue("join-quit.cooldown." + path, 0);
            var players = event.isVanished() ? JOIN_MAP : QUIT_MAP;

            if (timer > 0 && players.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - players.get(uuid);
                if (rest < timer * 1000L) return;
            }

            new Section(event, id, player).runTasks();
            if (timer > 0) players.put(uuid, System.currentTimeMillis());
        }

        @EventHandler(priority = EventPriority.LOW)
        private void onChat(AsyncPlayerChatEvent event) {
            var s = FileCache.MODULES.getSection("join-quit.vanish.chat-key");
            if (s == null || event.isCancelled()) return;

            var key = s.getString("key");
            if (StringUtils.isBlank(key)) return;

            var message = event.getMessage();
            var player = event.getPlayer();

            var notAllow = TextUtils.toList(s, "not-allowed");

            if (s.getBoolean("regex")) {
                var match = Pattern.compile(key).matcher(message);

                if (!match.find()) {
                    LangUtils.getSender().setTargets(player).send(notAllow);
                    event.setCancelled(true);
                    return;
                }

                event.setMessage(message.replace(match.group(), ""));
                return;
            }

            var place = s.getString("place", "");
            var isPrefix = !place.matches("(?i)suffix");

            var pattern = (isPrefix ? "^" : "") +
                    Pattern.quote(key) +
                    (isPrefix ? "" : "$");

            var match = Pattern.compile(pattern).matcher(message);

            if (!match.find()) {
                LangUtils.getSender().setTargets(player).send(notAllow);
                event.setCancelled(true);
                return;
            }

            event.setMessage(message.replace(match.group(), ""));
        }
    }

    static class Section {

        private final SIRPlugin main = SIRPlugin.getInstance();

        private final ConfigurationSection id;
        private final Player player;

        private boolean isJoin = true, doSpawn = true, isLogged = false;

        public Section(Event event, ConfigurationSection id, Player player) {
            this.id = id;
            this.player = player;

            if (event instanceof PlayerQuitEvent) isJoin = doSpawn = false;
            else if (event instanceof SIRLoginEvent) {
                doSpawn = !FileCache.MODULES.getValue("join-quit.login.enabled", false);
                isLogged = true;
            }
            else if (event instanceof SIRVanishEvent) {
                isJoin = ((SIRVanishEvent) event).isVanished();
                doSpawn = FileCache.MODULES.getValue("join-quit.vanish.use-spawn", false);
            }
        }

        public void runTasks() {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    LangUtils.getSender().setTargets(Bukkit.getOnlinePlayers()).
                            setParser(player).
                            send(TextUtils.toList(id, "public"));

                    if (isJoin) {
                        LangUtils.getSender().setTargets(player).
                                send(TextUtils.toList(id, "private"));

                        playSound(player, id.getString("sound"));
                        giveImmunity(player, id.getInt("invulnerable"));

                        if (doSpawn) PlayerUtils.teleport(id, player);
                    }

                    LangUtils.executeCommands(isJoin ? player : null,
                            TextUtils.toList(id, "commands"));

                    if (!Initializer.hasDiscord()) return;

                    String path = player.hasPlayedBefore() ? "" : "first-";
                    new DiscordSender(player, isJoin ? path + "join" : "quit").send();
                }
            };

            int ticks = FileCache.MODULES.getValue("login.ticks-after", 0);

            if (!isLogged || ticks <= 0) runnable.runTask(main);
            else runnable.runTaskLater(main, ticks);
        }
    }
}
