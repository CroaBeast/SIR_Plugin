package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.hooks.login.*;
import me.croabeast.sirplugin.hooks.vanish.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class JoinQuit extends BaseModule implements Listener {

    private final SIRPlugin main;
    private final EventUtils utils;

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
        SIRPlugin.registerListener(this);
        if (Initializer.hasLogin())
            SIRPlugin.registerListener(new LoginHook(main));
        if (Initializer.hasVanish())
            SIRPlugin.registerListener(new VanishHook(main));
    }

    private boolean isSilent(Player player, boolean isJoin) {
        return Initializer.hasVanish() && PermUtils.isVanished(player, isJoin) &&
                main.getModules().getBoolean("join-quit.vanish.silent");
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        main.getAmender().initUpdater(player);

        if (!isEnabled()) return;

        ConfigurationSection id = utils.getSection(
                main.getJoinQuit(), player, !player.hasPlayedBefore() ? "first-join" : "join");

        if (id == null) {
            LogUtils.doLog(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e join-quit.yml &7file."
            );
            return;
        }

        event.setJoinMessage(null);

        if (isSilent(player, true)) return;

        if (Initializer.hasLogin() &&
                main.getModules().getBoolean("join-quit.login.enabled")) {
            if (main.getModules().getBoolean("join-quit.login.spawn-before"))
                utils.teleportPlayer(id, player);
            return;
        }

        new Section(main, event, id, player).runTasks();
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

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

        if (isSilent(player, false)) return;
        if (Initializer.hasLogin()) {
            if (!utils.getLoggedPlayers().contains(player)) return;
            utils.getLoggedPlayers().remove(player);
        }

        new Section(main, event, id, player).runTasks();
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
            new Section(main, event, id, player).runTasks();
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

            if (Initializer.hasVanish() &&
                    main.getModules().getBoolean("join-quit.vanish.enabled")) return;
            if (Initializer.hasLogin()) main.getEventUtils().getLoggedPlayers().add(player);

            ConfigurationSection id = utils.getSection(
                    main.getJoinQuit(), player, event.isVanished() ? "join" : "quit");
            if (id == null) {
                LogUtils.doLog(player,
                        "<P> &cA valid message group isn't found...",
                        "<P> &7Please check your&e messages.yml &7file."
                );
                return;
            }

            new Section(main, event, id, player).runTasks();
        }
    }
}
