package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.builder.BossbarBuilder;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.command.message.PrivateMessageCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.DiscordHook;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public final class JoinQuitHandler extends SIRModule implements CustomListener {

    private static final Map<UUID, Long> JOIN_MAP, QUIT_MAP, PLAY_MAP;
    private static final Map<Type, Map<Integer, Set<ConnectionUnit>>> UNIT_MAP;

    static {
        JOIN_MAP = new LinkedHashMap<>();
        QUIT_MAP = new LinkedHashMap<>();
        PLAY_MAP = new LinkedHashMap<>();
        UNIT_MAP = new LinkedHashMap<>();
    }

    private final ConfigurableFile config;
    private final ConfigurableFile messages;

    @Getter @Setter
    private boolean registered = false;

    enum Type {
        FIRST("first-join"),
        JOIN("join"),
        QUIT("quit");

        private final String name;

        Type(String name) {
            this.name = name;
        }
    }

    JoinQuitHandler() {
        super("join-quit");

        config = YAMLData.Module.JOIN_QUIT.fromName("config");
        messages = YAMLData.Module.JOIN_QUIT.fromName("messages");
    }

    public boolean register() {
        register(SIRPlugin.getInstance());
        UNIT_MAP.clear();

        for (Type type : Type.values()) {
            Map<Integer, Set<ConfigUnit>> first = messages.getUnitsByPriority(type.name);
            if (first.isEmpty()) return false;

            Map<Integer, Set<ConnectionUnit>> loaded = UNIT_MAP.getOrDefault(type, new LinkedHashMap<>());

            for (Map.Entry<Integer, Set<ConfigUnit>> entry : first.entrySet()) {
                final int i = entry.getKey();

                Set<ConnectionUnit> before = loaded.getOrDefault(i, new LinkedHashSet<>());
                entry.getValue().forEach(c ->
                        before.add(new ConnectionUnit(c.getSection(), type)));

                loaded.put(i, before);
            }

            UNIT_MAP.put(type, loaded);
        }

        return true;
    }

    ConnectionUnit get(Type type, Player player) {
        Map<Integer, Set<ConnectionUnit>> loaded = UNIT_MAP.getOrDefault(type, new LinkedHashMap<>());
        if (loaded.isEmpty()) return null;

        for (Map.Entry<Integer, Set<ConnectionUnit>> maps : loaded.entrySet())
            for (ConnectionUnit unit : maps.getValue())
                if (PlayerUtils.hasPerm(player, unit.getPermission())) return unit;

        return null;
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        Type type = player.hasPlayedBefore() ? Type.JOIN : Type.FIRST;

        ConnectionUnit unit = get(type, player);
        if (unit == null) return;

        if (config.get("default-messages.disable-join", true))
            event.setJoinMessage(null);

        int joinTime = config.get("cooldown.join", 0);
        UUID uuid = player.getUniqueId();

        if (joinTime > 0 && JOIN_MAP.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - JOIN_MAP.get(uuid);
            if (rest < joinTime * 1000L) return;
        }

        if (VanishHook.isVanished(player)) return;

        if (LOGIN.isEnabled() &&
                YAMLData.Module.Hook.LOGIN.from().get("spawn-before", false)) {
            unit.teleportToSpawn(player);
            return;
        }

        unit.performAllActions(player);

        long data = System.currentTimeMillis();
        if (joinTime > 0) JOIN_MAP.put(uuid, data);

        if (config.get("cooldown.between", 0) > 0)
            PLAY_MAP.put(uuid, data);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        BossbarBuilder.getBuilders(event.getPlayer()).forEach(BossbarBuilder::unregister);

        if (PlayerUtils.isImmune(player)) {
            player.setInvulnerable(false);
            PlayerUtils.removeFromImmunePlayers(player);
        }

        PrivateMessageCommand.removeFromData(player, Bukkit.getConsoleSender());
        if (!isEnabled()) return;

        ConnectionUnit unit = get(Type.QUIT, player);
        if (unit == null) return;

        if (config.get("default-messages.disable-quit", true))
            event.setQuitMessage(null);

        UUID uuid = player.getUniqueId();

        int playTime = config.get("cooldown.between", 0);
        int quitTime = config.get("cooldown.quit", 0);

        final long now = System.currentTimeMillis();

        if (quitTime > 0 && QUIT_MAP.containsKey(uuid) &&
                now - QUIT_MAP.get(uuid) < quitTime * 1000L) return;

        if (playTime > 0 && PLAY_MAP.containsKey(uuid) &&
                now - PLAY_MAP.get(uuid) < playTime * 1000L) return;

        if (VanishHook.isVanished(player)) return;

        if (!LoginHook.isLogged(player)) {
            LoginHook.removePlayer(player);
            return;
        }

        unit.performAllActions(player);
        if (quitTime > 0) QUIT_MAP.put(uuid, System.currentTimeMillis());
    }

    private static class ConnectionUnit implements ConfigUnit {

        @Getter
        private final ConfigurationSection section;
        private final Type type;

        private final List<String> publicList;
        private List<String> privateList;
        private List<String> commandList;

        private String sound;
        private int invulnerability;

        private ConfigurationSection spawn;

        ConnectionUnit(ConfigurationSection section, Type type) {
            this.section = section;
            this.type = type;

            publicList = TextUtils.toList(section, "public");
            if (type == Type.QUIT) return;

            privateList = TextUtils.toList(section, "private");
            commandList = TextUtils.toList(section, "commands");

            sound = section.getString("sound");
            invulnerability = section.getInt("invulnerable");
            spawn = section.getConfigurationSection("spawn-location");
        }

        static MessageSender send() {
            return MessageSender.loaded();
        }

        void performAllActions(Player player) {
            send().setTargets(Bukkit.getOnlinePlayers()).setParser(player).send(publicList);

            if (type != Type.QUIT) {
                send().setTargets(player).send(privateList);

                PlayerUtils.giveImmunity(player, invulnerability);
                PlayerUtils.playSound(player, sound);

                teleportToSpawn(player);
            }

            LangUtils.executeCommands(type != Type.QUIT ? player : null, commandList);
            if (DISCORD.isEnabled()) DiscordHook.send(type.name, player);
        }

        void teleportToSpawn(Player player) {
            PlayerUtils.teleport(spawn, player);
        }
    }
}
