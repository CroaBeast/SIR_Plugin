package me.croabeast.sirplugin.module.listener;

import me.croabeast.beanslib.object.display.Bossbar;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.event.*;
import me.croabeast.sirplugin.hook.discord.*;
import me.croabeast.sirplugin.hook.login.*;
import me.croabeast.sirplugin.hook.vanish.*;
import me.croabeast.sirplugin.object.Sender;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.task.message.*;
import me.croabeast.sirplugin.utility.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.sirplugin.utility.PlayerUtils.*;

public class JoinQuit extends SIRViewer {

    public static final Set<Player> LOGGED_PLAYERS = new HashSet<>();

    private final SIRPlugin main = SIRPlugin.getInstance();

    Map<UUID, Long> joinMap = new HashMap<>(),
            quitMap = new HashMap<>(),
            playMap = new HashMap<>();

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.JOIN_QUIT;
    }

    @Override
    public void registerModule() {
        registerListener();
        if (Initializer.hasLogin()) new LoginHook().registerListener();
        if (Initializer.hasVanish()) new VanishHook().registerListener();
    }

    private boolean isVanished(Player player, boolean isJoin) {
        return Initializer.hasVanish() && PlayerUtils.isVanished(player, isJoin);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        main.getAmender().initUpdater(player);
        if (!isEnabled()) return;

        String path = !player.hasPlayedBefore() ? "first-join" : "join";
        ConfigurationSection id = FileCache.JOIN_QUIT.permSection(player, path);

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (FileCache.MODULES.get().getBoolean("join-quit.default-messages.disable-join", true))
            event.setJoinMessage(null);

        int playTime = FileCache.MODULES.get().getInt("join-quit.cooldown.between"),
                joinCooldown = FileCache.MODULES.get().getInt("join-quit.cooldown.join");

        if (joinCooldown > 0 && joinMap.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - joinMap.get(uuid);
            if (rest < joinCooldown * 1000L) return;
        }

        if (isVanished(player, true)) return;
        String p = "join-quit.login.";

        if (Initializer.hasLogin() && FileCache.MODULES.get().getBoolean(p + "enabled")) {
            if (FileCache.MODULES.get().getBoolean(p + "spawn-before")) teleportPlayer(id, player);
            return;
        }

        new Section(event, id, player).runTasks();
        final long data = System.currentTimeMillis();

        if (joinCooldown > 0) joinMap.put(uuid, data);
        if (playTime > 0) playMap.put(uuid, data);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Bossbar bar = Bossbar.getBossbar(player);
        if (bar != null) bar.unregister();

        if (getGodPlayers().contains(player)) {
            player.setInvulnerable(false);
            getGodPlayers().remove(player);
        }

        DirectTask.getReceivers().remove(Bukkit.getConsoleSender(), player);
        DirectTask.getReceivers().remove(player);

        if (!isEnabled()) return;

        ConfigurationSection id = FileCache.JOIN_QUIT.permSection(player, "quit");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        if (FileCache.MODULES.get().getBoolean("join-quit.default-messages.disable-quit", true))
            event.setQuitMessage(null);

        int playTime = FileCache.MODULES.get().getInt("join-quit.cooldown.between"),
                quitCooldown = FileCache.MODULES.get().getInt("join-quit.cooldown.quit");

        final long now = System.currentTimeMillis();

        if (quitCooldown > 0 && quitMap.containsKey(uuid) &&
                now - quitMap.get(uuid) < quitCooldown * 1000L) return;
        if (playTime > 0 && playMap.containsKey(uuid) &&
                now - playMap.get(uuid) < playTime * 1000L) return;

        if (isVanished(player, false)) return;

        if (Initializer.hasLogin()) {
            if (!LOGGED_PLAYERS.contains(player)) return;
            LOGGED_PLAYERS.remove(player);
        }

        new Section(event, id, player).runTasks();
        if (quitCooldown > 0) quitMap.put(uuid, System.currentTimeMillis());
    }

    public class LoginHook implements RawViewer {

        public LoginHook() {
            new AuthMe();
            new UserLogin();
        }

        @EventHandler
        private void onLogin(LoginEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!isEnabled()) return;

            if (!FileCache.MODULES.get().getBoolean("join-quit.login.enabled")) return;
            if (JoinQuit.this.isVanished(player, true)) return;

            ConfigurationSection id = FileCache.JOIN_QUIT.permSection(
                    player, !player.hasPlayedBefore() ? "first-join" : "join");

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int playTime = FileCache.MODULES.get().getInt("join-quit.cooldown.between"),
                    joinCooldown = FileCache.MODULES.get().getInt("join-quit.cooldown.join");

            if (joinCooldown > 0 && joinMap.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - joinMap.get(uuid);
                if (rest < joinCooldown * 1000L) return;
            }

            LOGGED_PLAYERS.add(player);
            new Section(event, id, player).runTasks();

            final long data = System.currentTimeMillis();
            if (joinCooldown > 0) joinMap.put(uuid, data);
            if (playTime > 0) playMap.put(uuid, data);
        }
    }

    public class VanishHook implements RawViewer {

        public VanishHook() {
            new CMI();
            new Essentials();
            new Vanish();
        }

        @EventHandler
        private void onVanish(VanishEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!isEnabled()) return;

            if (!Initializer.hasVanish() ||
                    !FileCache.MODULES.get().getBoolean("join-quit.vanish.enabled")) return;

            if (Initializer.hasLogin()) {
                if (event.isVanished() & !LOGGED_PLAYERS.contains(player))
                    LOGGED_PLAYERS.add(player);
                else LOGGED_PLAYERS.remove(player);
            }

            String path = event.isVanished() ? "join" : "quit";
            ConfigurationSection id = FileCache.JOIN_QUIT.permSection(player, path);

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int timer = FileCache.MODULES.get().getInt("join-quit.cooldown." + path);
            Map<UUID, Long> players = event.isVanished() ? joinMap : quitMap;

            if (timer > 0 && players.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - players.get(uuid);
                if (rest < timer * 1000L) return;
            }

            new Section(event, id, player).runTasks();
            if (timer > 0) players.put(uuid, System.currentTimeMillis());
        }

        @EventHandler(priority = EventPriority.LOW)
        private void onChat(AsyncPlayerChatEvent event) {
            String path = "join-quit.vanish.chat-key";
            if (event.isCancelled()) return;

            String key = FileCache.MODULES.get().getString(path + "key");
            if (StringUtils.isBlank(key)) return;

            String message = event.getMessage();
            Player player = event.getPlayer();

            if (FileCache.MODULES.get().getBoolean(path + "regex")) {
                Matcher match = Pattern.compile(key).matcher(message);

                if (!match.find()) {
                    LangUtils.create(player, FileCache.MODULES.toList(path + "not-allowed")).display();
                    event.setCancelled(true);
                }
                else event.setMessage(message.replace(match.group(), ""));
                return;
            }

            String place = FileCache.MODULES.get().getString(path + "place", "");
            boolean isPrefix = !place.matches("(?i)suffix");

            String pattern = (isPrefix ? "^" : "") + Pattern.quote(key) + (isPrefix ? "" : "$");
            Matcher match = Pattern.compile(pattern).matcher(message);

            if (!match.find()) {
                LangUtils.create(player, FileCache.MODULES.toList(path + "not-allowed")).display();
                event.setCancelled(true);
            }
            else event.setMessage(message.replace(match.group(), ""));
        }
    }

    public static class Section {

        private final SIRPlugin main = SIRPlugin.getInstance();

        private final ConfigurationSection id;
        private final Player player;

        private boolean isJoin = true, doSpawn = true, isLogged = false;

        public Section(Event event, ConfigurationSection id, Player player) {
            this.id = id;
            this.player = player;

            if (event instanceof PlayerQuitEvent) isJoin = doSpawn = false;
            else if (event instanceof LoginEvent) {
                doSpawn = !FileCache.MODULES.get().getBoolean("join-quit.login.enabled");
                isLogged = true;
            }
            else if (event instanceof VanishEvent) {
                isJoin = ((VanishEvent) event).isVanished();
                doSpawn = FileCache.MODULES.get().getBoolean("join-quit.vanish.use-spawn");
            }
        }

        public void runTasks() {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    LangUtils.create(Bukkit.getOnlinePlayers(), player, TextUtils.toList(id, "public")).display();

                    if (isJoin) {
                        LangUtils.create(player, TextUtils.toList(id, "private")).display();

                        playSound(player, id.getString("sound"));
                        giveImmunity(player, id.getInt("invulnerable"));

                        if (doSpawn) teleportPlayer(id, player);
                    }

                    Sender.to(id, "commands").execute(isJoin ? player : null);
                    if (!Initializer.hasDiscord()) return;

                    String path = player.hasPlayedBefore() ? "" : "first-";
                    new DiscordMsg(player, isJoin ? path + "join" : "quit").send();
                }
            };

            int ticks = FileCache.MODULES.get().getInt("login.ticks-after");

            if (!isLogged || ticks <= 0) runnable.runTask(main);
            else runnable.runTaskLater(main, ticks);
        }
    }
}
