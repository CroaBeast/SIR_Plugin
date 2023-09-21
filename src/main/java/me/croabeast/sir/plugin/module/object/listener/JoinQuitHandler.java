package me.croabeast.sir.plugin.module.object.listener;

import me.croabeast.beanslib.builder.BossbarBuilder;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import me.croabeast.sir.api.event.hook.SIRVanishEvent;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.SIRRunnable;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.task.object.message.DirectTask;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.croabeast.sir.plugin.utility.PlayerUtils.*;

public class JoinQuitHandler extends SIRModule implements CustomListener {

    public static final Set<Player> LOGGED_PLAYERS = new HashSet<>();

    private static final HashMap<UUID, Long> JOIN_MAP = new HashMap<>(),
            QUIT_MAP = new HashMap<>(),
            PLAY_MAP = new HashMap<>();

    JoinQuitHandler() {
        super(ModuleName.JOIN_QUIT);
    }

    private boolean registered = false;

    @Override
    public void register() {
        if (registered) return;

        CustomListener.super.register();
        if (LoginHook.isEnabled()) new SIRLoginHook().register();
        if (VanishHook.isEnabled()) new SIRVanishHook().register();
        registered = true;
    }

    static FileCache config() {
        return FileCache.JOIN_QUIT_CACHE.getConfig();
    }

    static FileCache messages() {
        return FileCache.JOIN_QUIT_CACHE.getCache("messages");
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        SIRPlugin.checkUpdater(player);
        if (!isEnabled()) return;

        String path = !player.hasPlayedBefore() ? "first-join" : "join";
        ConfigurationSection id = messages().permSection(player, path);

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (config().getValue("default-messages.disable-join", true))
            event.setJoinMessage(null);

        int playTime = config().getValue("cooldown.between", 0),
                joinCooldown = config().getValue("cooldown.join", 0);

        if (joinCooldown > 0 && JOIN_MAP.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - JOIN_MAP.get(uuid);
            if (rest < joinCooldown * 1000L) return;
        }

        if (VanishHook.isVanished(player)) return;

        if (LoginHook.isEnabled() && config().getValue("login.enabled", true)) {
            if (config().getValue("login.spawn-before", false))
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

        Set<BossbarBuilder> bar = BossbarBuilder.getBuilder(player);
        if (bar != null) bar.forEach(BossbarBuilder::unregister);

        if (getGodPlayers().contains(player)) {
            player.setInvulnerable(false);
            getGodPlayers().remove(player);
        }

        DirectTask.getReceiverMap().remove(Bukkit.getConsoleSender(), player);
        DirectTask.getReceiverMap().remove(player);

        if (!isEnabled()) return;

        ConfigurationSection id = messages().permSection(player, "quit");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (config().getValue("default-messages.disable-quit", true))
            event.setQuitMessage(null);

        int playTime = config().getValue("cooldown.between", 0),
                quitCooldown = config().getValue("cooldown.quit", 0);

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

    class SIRLoginHook implements CustomListener {

        @EventHandler
        private void onLogin(SIRLoginEvent event) {
            Player player = event.getPlayer();

            UUID uuid = player.getUniqueId();
            LOGGED_PLAYERS.add(player);

            if (!isEnabled()) return;

            if (!config().getValue("login.enabled", false)) return;
            if (VanishHook.isVanished(player)) return;

            ConfigurationSection id = config().permSection(player,
                    !player.hasPlayedBefore() ? "first-join" : "join"
            );

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int playTime = config().getValue("cooldown.between", 0),
                    joinCooldown = config().getValue("cooldown.join", 0);

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

    class SIRVanishHook implements CustomListener {

        @EventHandler
        private void onVanish(SIRVanishEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!isEnabled()) return;

            if (!VanishHook.isEnabled() ||
                    !config().getValue("vanish.enabled", false)) return;

            if (LoginHook.isEnabled()) {
                if (event.isVanished()) LOGGED_PLAYERS.add(player);
                else LOGGED_PLAYERS.remove(player);
            }

            String path = event.isVanished() ? "join" : "quit";
            ConfigurationSection id = config().permSection(player, path);

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int timer = config().getValue("cooldown." + path, 0);
            HashMap<UUID, Long> players = event.isVanished() ? JOIN_MAP : QUIT_MAP;

            if (timer > 0 && players.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - players.get(uuid);
                if (rest < timer * 1000L) return;
            }

            new Section(event, id, player).runTasks();
            if (timer > 0) players.put(uuid, System.currentTimeMillis());
        }

        @EventHandler(priority = EventPriority.LOW)
        private void onChat(AsyncPlayerChatEvent event) {
            @org.jetbrains.annotations.Nullable ConfigurationSection s = config().getSection("vanish.chat-key");
            if (s == null || event.isCancelled()) return;

            if (!VanishHook.isEnabled()) return;
            if (!s.getBoolean("enabled")) return;

            String key = s.getString("key");
            if (StringUtils.isBlank(key)) return;

            String message = event.getMessage();
            Player player = event.getPlayer();

            List<String> notAllow = TextUtils.toList(s, "not-allowed");
            MessageSender sender = MessageSender.fromLoaded().setTargets(player);

            if (s.getBoolean("regex")) {
                Matcher match = Pattern.compile(key).matcher(message);

                if (!match.find()) {
                    event.setCancelled(true);
                    sender.send(notAllow);
                    return;
                }

                event.setMessage(message.replace(match.group(), ""));
                return;
            }

            String place = s.getString("place", "");
            boolean isPrefix = !place.matches("(?i)suffix");

            String pattern = (isPrefix ? "^" : "") +
                    Pattern.quote(key) +
                    (isPrefix ? "" : "$");

            Matcher match = Pattern.compile(pattern).matcher(message);

            if (!match.find()) {
                sender.send(notAllow);
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
                doSpawn = !config().getValue("login.enabled", false);
                isLogged = true;
            }
            else if (event instanceof SIRVanishEvent) {
                isJoin = ((SIRVanishEvent) event).isVanished();
                doSpawn = config().getValue("vanish.use-spawn", false);
            }
        }

        public void runTasks() {
            final int ticks = config().getValue("login.ticks-after", 0);

            SIRRunnable.runFromSIR(() -> {
                MessageSender.fromLoaded().setTargets(Bukkit.getOnlinePlayers()).
                        setParser(player).
                        send(TextUtils.toList(id, "public"));

                if (isJoin) {
                    playSound(player, id.getString("sound"));
                    giveImmunity(player, id.getInt("invulnerable"));

                    MessageSender.fromLoaded().
                            setTargets(player).send(TextUtils.toList(id, "private"));

                    if (doSpawn) PlayerUtils.teleport(id, player);
                }

                LangUtils.executeCommands(isJoin ? player : null,
                        TextUtils.toList(id, "commands"));

                if (!SIRInitializer.hasDiscord()) return;

                String path = player.hasPlayedBefore() ? "" : "first-";
                new DiscordSender(player, isJoin ? path + "join" : "quit").send();
            },
                    (!isLogged || ticks <= 0) ?
                            SIRRunnable::runTask : r -> r.runTaskLater(ticks)
            );
        }
    }
}
