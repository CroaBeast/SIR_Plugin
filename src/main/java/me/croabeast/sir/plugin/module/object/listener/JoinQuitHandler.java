package me.croabeast.sir.plugin.module.object.listener;

import lombok.Getter;
import me.croabeast.beanslib.misc.BossbarBuilder;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.event.hook.SIRLoginEvent;
import me.croabeast.sir.api.event.hook.SIRVanishEvent;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.command.object.message.DirectCommand;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JoinQuitHandler extends ModuleListener implements CacheManageable {

    private static final Map<UUID, Long> JOIN_MAP, QUIT_MAP, PLAY_MAP;

    enum Type {
        FIRST("first-join"), JOIN("join"), QUIT("quit");

        private final String name;

        Type(String name) {
            this.name = name;
        }
    }

    private static final Map<Type, Map<Integer, Set<ConnectionUnit>>> UNIT_MAP;

    static {
        JOIN_MAP = new LinkedHashMap<>();
        QUIT_MAP = new LinkedHashMap<>();
        PLAY_MAP = new LinkedHashMap<>();
        UNIT_MAP = new LinkedHashMap<>();
    }

    static void loadCache() {
        UNIT_MAP.clear();

        for (Type type : Type.values()) {
            Map<Integer, Set<ConfigUnit>> first = messages().getUnitsByPriority(type.name);
            if (first.isEmpty()) return;

            Map<Integer, Set<ConnectionUnit>> loaded = UNIT_MAP.getOrDefault(type, new LinkedHashMap<>());

            for (Map.Entry<Integer, Set<ConfigUnit>> entry : first.entrySet()) {
                int i = entry.getKey();

                Set<ConnectionUnit> before = loaded.getOrDefault(i, new LinkedHashSet<>());
                entry.getValue().forEach(c ->
                        before.add(new ConnectionUnit(c.getSection(), type)));

                loaded.put(i, before);
            }

            UNIT_MAP.put(type, loaded);
        }
    }

    JoinQuitHandler() {
        super(ModuleName.JOIN_QUIT);
    }

    static YAMLFile config() {
        return YAMLCache.fromJoinQuit("config");
    }

    static YAMLFile messages() {
        return YAMLCache.fromJoinQuit("messages");
    }

    static ConnectionUnit get(Type type, Player player) {
        Map<Integer, Set<ConnectionUnit>> loaded = UNIT_MAP.getOrDefault(type, new LinkedHashMap<>());
        if (loaded.isEmpty()) return null;

        for (Map.Entry<Integer, Set<ConnectionUnit>> maps : loaded.entrySet())
            for (ConnectionUnit unit : maps.getValue()) {
                final String perm = unit.getPermission();

                if (PlayerUtils.hasPerm(player, perm))
                    return unit;
            }

        return null;
    }

    static long current() {
        return System.currentTimeMillis();
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        SIRPlugin.checkUpdater(player);
        if (!isEnabled()) return;

        Type type = player.hasPlayedBefore() ? Type.JOIN : Type.FIRST;

        ConnectionUnit unit = get(type, player);
        if (unit == null) return;

        if (config().get("default-messages.disable-join", true))
            event.setJoinMessage(null);

        int joinTime = config().get("cooldown.join", 0);
        UUID uuid = player.getUniqueId();

        if (joinTime > 0 && JOIN_MAP.containsKey(uuid)) {
            long rest = current() - JOIN_MAP.get(uuid);
            if (rest < joinTime * 1000L) return;
        }

        if (VanishHook.isVanished(player)) return;

        if (LoginHook.isEnabled()
                && config().get("login.enabled", true))
        {
            if (config().get("login.spawn-before", false))
                unit.teleportToSpawn(player);
            return;
        }

        unit.performAllActions(player);

        final long data = current();
        if (joinTime > 0) JOIN_MAP.put(uuid, data);

        if (config().get("cooldown.between", 0) > 0)
            PLAY_MAP.put(uuid, data);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Set<BossbarBuilder> bar = BossbarBuilder.getBuilders(player);
        if (!bar.isEmpty())
            bar.forEach(BossbarBuilder::unregister);

        if (PlayerUtils.isImmune(player)) {
            player.setInvulnerable(false);
            PlayerUtils.removeFromImmunePlayers(player);
        }

        DirectCommand.getReceiverMap().remove(Bukkit.getConsoleSender(), player);
        DirectCommand.getReceiverMap().remove(player);

        if (!isEnabled()) return;

        ConnectionUnit unit = get(Type.QUIT, player);
        if (unit == null) return;

        if (config().get("default-messages.disable-quit", true))
            event.setQuitMessage(null);

        UUID uuid = player.getUniqueId();

        int playTime = config().get("cooldown.between", 0);
        int quitTime = config().get("cooldown.quit", 0);

        final long now = current();

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

        if (quitTime > 0) QUIT_MAP.put(uuid, current());
    }

    @Override
    public void register() {
        registerOnSIR();

        if (LoginHook.isEnabled())
            new LoginListener().registerOnSIR();

        if (VanishHook.isEnabled())
            new VanishListener().registerOnSIR();
    }

    class LoginListener implements CustomListener {

        @EventHandler
        private void onLogin(SIRLoginEvent event) {
            if (!isEnabled()) return;

            Player player = event.getPlayer();

            UUID uuid = player.getUniqueId();
            LoginHook.addPlayer(player);

            if (!config().get("login.enabled", false)) return;
            if (VanishHook.isVanished(player)) return;

            Type type = player.hasPlayedBefore() ? Type.JOIN : Type.FIRST;

            ConnectionUnit unit = get(type, player);
            if (unit == null) return;

            int joinTime = config().get("cooldown.join", 0);

            if (joinTime > 0 && JOIN_MAP.containsKey(uuid)) {
                long rest = current() - JOIN_MAP.get(uuid);
                if (rest < joinTime * 1000L) return;
            }

            unit.performAllActions(player);

            final long data = current();
            if (joinTime > 0) JOIN_MAP.put(uuid, data);

            if (config().get("cooldown.between", 0) > 0)
                PLAY_MAP.put(uuid, data);
        }
    }

    class VanishListener implements CustomListener {

        @EventHandler
        private void onVanish(SIRVanishEvent event) {
            if (!isEnabled()) return;

            final boolean isVanished = event.isVanished();

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (!VanishHook.isEnabled() ||
                    !config().get("vanish.enabled", false)) return;

            if (LoginHook.isEnabled())
                if (isVanished) LoginHook.addPlayer(player);
                else LoginHook.removePlayer(player);

            Type type = isVanished ? Type.JOIN : Type.QUIT;

            ConnectionUnit unit = get(type, player);
            if (unit == null) return;

            int timer = config().get("cooldown." + type.name, 0);
            Map<UUID, Long> players = isVanished ? JOIN_MAP : QUIT_MAP;

            if (timer > 0 && players.containsKey(uuid)) {
                long rest = current() - players.get(uuid);
                if (rest < timer * 1000L) return;
            }

            unit.performAllActions(player);
            if (timer > 0) players.put(uuid, current());
        }

        @EventHandler
        private void onChat(AsyncPlayerChatEvent event) {
            ConfigurationSection s = config().getSection("vanish.chat-key");
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
                event.setCancelled(true);
                sender.send(notAllow);
                return;
            }

            event.setMessage(message.replace(match.group(), ""));
        }
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
            return MessageSender.fromLoaded();
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

            if (SIRInitializer.hasDiscord())
                new DiscordSender(player, type.name).send();
        }

        void teleportToSpawn(Player player) {
            PlayerUtils.teleport(spawn, player);
        }
    }
}
