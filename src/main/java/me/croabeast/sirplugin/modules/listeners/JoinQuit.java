package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.beanslib.terminals.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.hooks.discord.*;
import me.croabeast.sirplugin.hooks.login.*;
import me.croabeast.sirplugin.hooks.vanish.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.boss.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCache.*;
import static me.croabeast.sirplugin.utilities.EventUtils.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class JoinQuit extends BaseViewer {

    private final SIRPlugin main;
    Set<Player> loggedPlayers = new HashSet<>();

    Map<UUID, Long> joinMap = new HashMap<>(),
            quitMap = new HashMap<>(),
            playMap = new HashMap<>();

    public JoinQuit(SIRPlugin main) {
        this.main = main;
    }

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
        return Initializer.hasVanish() && PermUtils.isVanished(player, isJoin);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.isInvulnerable()) player.setInvulnerable(false);

        main.getAmender().initUpdater(player);
        if (!isEnabled()) return;

        String path = !player.hasPlayedBefore() ? "first-join" : "join";
        ConfigurationSection id = getSection(JOIN_QUIT.toFile(), player, path);

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        event.setJoinMessage(null);

        int playTime = MODULES.toFile().getInt("join-quit.cooldown.between"),
                joinCooldown = MODULES.toFile().getInt("join-quit.cooldown.join");

        if (joinCooldown > 0 && joinMap.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - joinMap.get(uuid);
            if (rest < joinCooldown * 1000L) return;
        }

        if (isVanished(player, true)) return;
        String p = "join-quit.login.";

        if (Initializer.hasLogin() && MODULES.toFile().getBoolean(p + "enabled")) {
            if (MODULES.toFile().getBoolean(p + "spawn-before")) teleportPlayer(id, player);
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

        BossBar bar = Bossbar.getBossbar(player);
        if (bar != null) {
            bar.removePlayer(player);
            Bossbar.getBossbarMap().remove(player);
        }

        if (getGodPlayers().contains(player)) {
            player.setInvulnerable(false);
            getGodPlayers().remove(player);
        }

        if (!isEnabled()) return;
        ConfigurationSection id = getSection(JOIN_QUIT.toFile(), player, "quit");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        event.setQuitMessage(null);

        int playTime = MODULES.toFile().getInt("join-quit.cooldown.between"),
                quitCooldown = MODULES.toFile().getInt("join-quit.cooldown.quit");

        final long now = System.currentTimeMillis();

        if (quitCooldown > 0 && quitMap.containsKey(uuid) &&
                now - quitMap.get(uuid) < quitCooldown * 1000L) return;
        if (playTime > 0 && playMap.containsKey(uuid) &&
                now - playMap.get(uuid) < playTime * 1000L) return;

        if (isVanished(player, false)) return;

        if (Initializer.hasLogin()) {
            if (!loggedPlayers.contains(player)) return;
            loggedPlayers.remove(player);
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

            if (!MODULES.toFile().getBoolean("join-quit.login.enabled")) return;
            if (JoinQuit.this.isVanished(player, true)) return;

            ConfigurationSection id = getSection(JOIN_QUIT.toFile(),
                    player, !player.hasPlayedBefore() ? "first-join" : "join");

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int playTime = MODULES.toFile().getInt("join-quit.cooldown.between"),
                    joinCooldown = MODULES.toFile().getInt("join-quit.cooldown.join");

            if (joinCooldown > 0 && joinMap.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - joinMap.get(uuid);
                if (rest < joinCooldown * 1000L) return;
            }

            loggedPlayers.add(player);
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
                    !MODULES.toFile().getBoolean("join-quit.vanish.enabled")) return;

            if (Initializer.hasLogin()) {
                if (event.isVanished() & !loggedPlayers.contains(player))
                    loggedPlayers.add(player);
                else loggedPlayers.remove(player);
            }

            String path = event.isVanished() ? "join" : "quit";
            ConfigurationSection id = getSection(JOIN_QUIT.toFile(), player, path);

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int timer = MODULES.toFile().getInt("join-quit.cooldown." + path);
            Map<UUID, Long> players = event.isVanished() ? joinMap : quitMap;

            if (timer > 0 && players.containsKey(uuid)) {
                long rest = System.currentTimeMillis() - players.get(uuid);
                if (rest < timer * 1000L) return;
            }

            new Section(event, id, player).runTasks();
            if (timer > 0) players.put(uuid, System.currentTimeMillis());
        }
    }

    public static class Section {

        private final SIRPlugin main = SIRPlugin.getInstance();

        private final ConfigurationSection id;
        private final Player player;

        private boolean isJoin = true, doSpawn = true, isLogged = true;

        public Section(Event event, ConfigurationSection id, Player player) {
            this.id = id;
            this.player = player;

            if (event instanceof PlayerJoinEvent) isLogged = false;
            else if (event instanceof PlayerQuitEvent) isJoin = doSpawn = isLogged = false;
            else if (event instanceof LoginEvent) {
                doSpawn = !MODULES.toFile().getBoolean("join-quit.login.enabled");
            }
            else if (event instanceof VanishEvent) {
                isJoin = ((VanishEvent) event).isVanished();
                doSpawn = MODULES.toFile().getBoolean("join-quit.vanish.use-spawn");
                isLogged = false;
            }
        }

        public void runTasks() {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    sendMessages(player, toList(id, "public"), true);
                    if (isJoin) {
                        sendMessages(player, toList(id, "private"), false);
                        playSound(player, id.getString("sound"));
                        giveInvulnerable(player, id.getInt("invulnerable"));
                        if (doSpawn) teleportPlayer(id, player);
                    }
                    runCommands(isJoin ? player : null, toList(id, "commands"));

                    if (!Initializer.hasDiscord()) return;

                    String path = player.hasPlayedBefore() ? "" : "first-";
                    new Message(player, isJoin ? path + "join" : "quit").sendMessage();
                }
            };

            int ticks = MODULES.toFile().getInt("login.ticks-after");

            if (!isLogged || ticks <= 0) runnable.runTask(main);
            else runnable.runTaskLater(main, ticks);
        }
    }
}
