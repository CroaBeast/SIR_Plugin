package me.croabeast.sirplugin.objects;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.hooks.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.*;

public class Section {

    private final SIRPlugin main;
    private final EventUtils utils;

    private final Event event;
    private final ConfigurationSection id;
    private final Player player;

    private boolean isJoin = true, doSpawn = true, isLogged = true;

    public Section(SIRPlugin main, Event event, ConfigurationSection id, Player player) {
        this.main = main;
        this.utils = main.getEventUtils();

        this.event = event;
        this.id = id;
        this.player = player;

        setUpOptions();
    }

    private void setUpOptions() {
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

                if (Initializer.hasDiscord()) new Message(main, player, isJoin ?
                        (player.hasPlayedBefore() ? "join" : "first-join") : "quit").sendMessage();
            }
        };

        int ticks = main.getModules().getInt("login.ticks-after");

        if (!isLogged || ticks <= 0) runnable.runTask(main);
        else runnable.runTaskLater(main, ticks);
    }
}
