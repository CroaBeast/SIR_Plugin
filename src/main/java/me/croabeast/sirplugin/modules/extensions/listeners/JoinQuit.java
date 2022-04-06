package me.croabeast.sirplugin.modules.extensions.listeners;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.hooks.*;
import me.croabeast.sirplugin.hooks.login.*;
import me.croabeast.sirplugin.hooks.vanish.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.boss.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.*;

import java.util.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

public class JoinQuit extends BaseModule implements Listener {

    private final SIRPlugin main;
    private final EventUtils utils;

    Map<UUID, Long> joinMap = new HashMap<>(), quitMap = new HashMap<>(), playMap = new HashMap<>();


    public JoinQuit(SIRPlugin main) {
        this.main = main;
        this.utils = main.getEventUtils();
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.JOIN_QUIT;
    }

    @Override
    public void registerModule() {
        registerListener(this);
        if (Initializer.hasLogin()) registerListener(new LoginHook(main));
        if (Initializer.hasVanish()) registerListener(new VanishHook(main));
    }

    private boolean isSilent(Player player, boolean isJoin) {
        return Initializer.hasVanish() && PermUtils.isVanished(player, isJoin) &&
                main.getModules().getBoolean("join-quit.vanish.silent");
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        main.getAmender().initUpdater(player);
        if (!isEnabled()) return;

        String path = !player.hasPlayedBefore() ? "first-join" : "join";
        ConfigurationSection id = utils.getSection(main.getJoinQuit(), player, path);

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        event.setJoinMessage(null);

        int playTime = main.getModules().getInt("join-quit.cooldown.between"),
                joinCooldown = main.getModules().getInt("join-quit.cooldown.join");

        if (joinCooldown > 0 && joinMap.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - joinMap.get(uuid);
            if (rest < joinCooldown * 1000L) return;
        }

        if (isSilent(player, true)) return;

        if (Initializer.hasLogin() &&
                main.getModules().getBoolean("join-quit.login.enabled")) {
            if (main.getModules().getBoolean("join-quit.login.spawn-before"))
                utils.teleportPlayer(id, player);
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

        if (!isEnabled()) return;

        ConfigurationSection id = utils.getSection(main.getJoinQuit(), player, "quit");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        event.setQuitMessage(null);

        int playTime = main.getModules().getInt("join-quit.cooldown.between"),
                quitCooldown = main.getModules().getInt("join-quit.cooldown.quit");

        final long now = System.currentTimeMillis();

        if (quitCooldown > 0 && quitMap.containsKey(uuid) &&
                now - quitMap.get(uuid) < quitCooldown * 1000L) return;
        if (playTime > 0 && playMap.containsKey(uuid) &&
                now - playMap.get(uuid) < playTime * 1000L) return;

        if (isSilent(player, false)) return;
        if (Initializer.hasLogin()) {
            if (!utils.getLoggedPlayers().contains(player)) return;
            utils.getLoggedPlayers().remove(player);
        }

        new Section(event, id, player).runTasks();

        if (quitCooldown > 0) quitMap.put(uuid, System.currentTimeMillis());
    }

    public class LoginHook implements Listener {

        private final SIRPlugin main;

        public LoginHook(SIRPlugin main) {
            this.main = main;
            new AuthMe();
            new UserLogin();
        }

        @EventHandler
        private void onLogin(LoginEvent event) {
            Player player = event.getPlayer();

            if (!main.getModules().getBoolean("join-quit.login.enabled")) return;
            if (JoinQuit.this.isSilent(player, true)) return;

            ConfigurationSection id = utils.getSection(
                    main.getJoinQuit(), player, !player.hasPlayedBefore() ? "first-join" : "join");
            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            main.getEventUtils().getLoggedPlayers().add(player);
            new Section(event, id, player).runTasks();
        }
    }

    public class VanishHook implements Listener {

        private final SIRPlugin main;

        public VanishHook(SIRPlugin main) {
            this.main = main;
            new CMI();
            new Essentials();
            new Vanish();
        }

        @EventHandler
        private void onVanish(VanishEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!Initializer.hasVanish() ||
                    !main.getModules().getBoolean("join-quit.vanish.enabled")) return;

            if (Initializer.hasLogin()) {
                if (event.isVanished() & !main.getEventUtils().getLoggedPlayers().contains(player))
                    main.getEventUtils().getLoggedPlayers().add(player);
                else main.getEventUtils().getLoggedPlayers().remove(player);
            }

            String path = event.isVanished() ? "join" : "quit";
            ConfigurationSection id = utils.getSection(main.getJoinQuit(), player, path);

            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            int timer = main.getConfig().getInt("options.cooldown." + path);
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
        private final EventUtils utils = main.getEventUtils();

        private final ConfigurationSection id;
        private final Player player;

        private boolean isJoin = true, doSpawn = true, isLogged = true;

        public Section(Event event, ConfigurationSection id, Player player) {
            this.id = id;
            this.player = player;

            if (event instanceof PlayerJoinEvent) {
                isLogged = false;
            }
            else if (event instanceof PlayerQuitEvent) {
                isJoin = doSpawn = isLogged = false;
            }
            else if (event instanceof LoginEvent) {
                doSpawn = !main.getModules().getBoolean("join-quit.login.enabled");
            }
            else if (event instanceof VanishEvent) {
                isJoin = ((VanishEvent) event).isVanished();
                doSpawn = main.getModules().getBoolean("join-quit.vanish.use-spawn");
                isLogged = false;
            }
        }

        public void runTasks() {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    utils.sendMessages(player, TextUtils.fileList(id, "public"), true);
                    if (isJoin) {
                        utils.sendMessages(player, TextUtils.fileList(id, "private"), false);
                        utils.playSound(player, id.getString("sound"));
                        utils.giveInvulnerable(player, id.getInt("invulnerable"));
                        if (doSpawn) utils.teleportPlayer(id, player);
                    }
                    utils.runCommands(isJoin ? player : null, TextUtils.fileList(id, "commands"));

                    if (!Initializer.hasDiscord()) return;
                    String path = isJoin ? (player.hasPlayedBefore() ? "" : "first-") + "join" : "quit";
                    new Message(player, path).sendMessage();
                }
            };

            int ticks = main.getModules().getInt("login.ticks-after");

            if (!isLogged || ticks <= 0) runnable.runTask(main);
            else runnable.runTaskLater(main, ticks);
        }
    }
}
