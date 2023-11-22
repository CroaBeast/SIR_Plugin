package me.croabeast.sir.plugin.module.object.listener;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.var;
import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.builder.ChatMessageBuilder;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.event.chat.SIRChatEvent;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChatFormatter extends ModuleListener implements CacheHandler {

    private static final Map<Integer, Set<ChatChannel>> CHANNELS_MAP = new LinkedHashMap<>();

    public static final Map<Integer, Set<ChatChannel>> LOCAL_MAP = new LinkedHashMap<>();
    private static final Map<Integer, Set<ChatChannel>> GLOBAL_MAP = new LinkedHashMap<>();

    private static final HashMap<Player, Long>
            GLOBAL_PLAYERS = new HashMap<>(), LOCAL_PLAYERS = new HashMap<>();

    ChatFormatter() {
        super(ModuleName.CHAT_CHANNELS);
    }

    private static FileCache config() {
        return FileCache.CHAT_CHANNELS_CACHE.getConfig();
    }

    private static FileCache channels() {
        return FileCache.CHAT_CHANNELS_CACHE.getCache("channels");
    }

    @Priority(level = 1)
    static void loadCache() {
        if (!ModuleName.CHAT_CHANNELS.isEnabled()) return;

        if (!CHANNELS_MAP.isEmpty()) CHANNELS_MAP.clear();
        if (!LOCAL_MAP.isEmpty()) LOCAL_MAP.clear();
        if (!GLOBAL_MAP.isEmpty()) GLOBAL_MAP.clear();

        final ChatChannel def = ChatChannel.getDefaults();

        Set<ChatChannel> defs = def != null ?
                Sets.newHashSet(def) : new HashSet<>();

        Map<Integer, Set<ConfigUnit>> channels;

        try {
            channels = channels().getUnitsByPermission("channels");
        } catch (Exception e) {
            CHANNELS_MAP.put(0, defs);
            return;
        }

        if (channels.isEmpty()) {
            CHANNELS_MAP.put(0, defs);
            return;
        }

        for (var entry : channels.entrySet()) {
            Set<ChatChannel> values = new LinkedHashSet<>();
            final int i = entry.getKey();

            for (var id : entry.getValue()) {
                ChatChannel channel;
                try {
                    channel = ChatChannel.of(id.getSection());
                } catch (Exception e) {
                    continue;
                }

                values.add(channel);

                ChatChannel local = channel.getSubChannel();
                if (local != null) values.add(local);
            }

            Set<ChatChannel> globals = new LinkedHashSet<>();
            Set<ChatChannel> locals = new LinkedHashSet<>();

            for (var c : values)
                (c.isGlobal() ? globals : locals).add(c);

            CHANNELS_MAP.put(i, values);

            if (!globals.isEmpty()) GLOBAL_MAP.put(i, globals);
            if (!locals.isEmpty()) LOCAL_MAP.put(i, locals);
        }
    }

    private static boolean canBeCancelled(Player player) {
        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            String id = UUIDManager.get().getUUID(player.getName());
            if (PunishmentManager.get().isMuted(id)) return true;
        }

        if (Exceptions.isPluginEnabled("Essentials")) {
            Essentials e = JavaPlugin.getPlugin(Essentials.class);
            return e.getUser(player).isVanished();
        }

        if (Exceptions.isPluginEnabled("CMI") &&
                CMIUser.getUser(player).isMuted()) return true;

        return !LoginHook.isLogged(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        Player player = event.getPlayer();

        if (canBeCancelled(player)) {
            event.setCancelled(true);
            return;
        }

        String message = event.getMessage();

        if (!config().getValue("allow-empty", false) && StringUtils.isBlank(message))
        {
            MessageSender.fromLoaded().setTargets(player)
                    .send(FileCache.getLang().toList("chat.empty-message"));

            event.setCancelled(true);
            return;
        }

        final boolean b = event.isAsynchronous();

        ChatChannel local = getLocalFromMessage(player, message);

        if (local != null) {
            String prefix = local.getAccessPrefix();
            if (StringUtils.isBlank(prefix)) return;

            String msg = message.substring(prefix.length());
            event.setCancelled(true);

            new SIRChatEvent(player, local, msg, b).call();
            return;
        }

        ChatChannel channel = getGlobalFormat(player);
        if (channel == null) return;

        SIRChatEvent global = new SIRChatEvent(player, channel, message, b);
        global.setGlobal(true);

        String output = channel.formatOutput(player, message, true);

        if (config().getValue("default-format", false) ||
                (channel.isDefault() && !TextUtils.IS_JSON.test(output))
        ) {
            event.setFormat(TextUtils.STRIP_JSON.apply(output).replace("%", "%%"));
            return;
        }

        event.setCancelled(true);
        global.call();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String[] args = event.getMessage().split(" ");

        ChatChannel local = getLocalFromCommand(event.getPlayer(), args[0]);
        if (local == null) return;

        event.setCancelled(true);

        String message = LangUtils.messageFromArray(args, 1);
        if (StringUtils.isBlank(message)) return;

        boolean b = event.isAsynchronous();
        Player player = event.getPlayer();

        new SIRChatEvent(player, local, message, b).call();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onSIRChat(SIRChatEvent event) {
        if (event.isCancelled()) return;

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();

        String message = event.getMessage();

        HashMap<Player, Long> map = event.isGlobal() ? GLOBAL_PLAYERS : LOCAL_PLAYERS;
        int timer = channel.getCooldown();

        if (timer > 0 && map.containsKey(player)) {
            long rest = System.currentTimeMillis() - map.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int t = timer - ((int) (Math.round(rest / 100D) / 10));

                MessageSender.fromLoaded().setTargets(player)
                        .addKeyValue("{time}", t)
                        .setLogger(false)
                        .send(channel.getCdMessages());
                return;
            }
        }

        String[] keys = channel.getChatKeys();

        if (SIRInitializer.hasDiscord()) {
            String m = Beans.formatPlaceholders(player, message);
            String name = event.isGlobal() ? "global-chat" : channel.getName();

            new DiscordSender(player, name)
                    .setKeys(keys)
                    .setValues(channel.getChatValues(m))
                    .send();
        }

        LogUtils.doLog(channel.formatOutput(player, message, false));

        String[] values = channel.getChatValues(message);

        List<String> hover = channel.getHoverList();
        if (hover != null)
            hover.replaceAll(s -> ValueReplacer.forEach(keys, values, s));

        String click = channel.getClickAction();
        if (StringUtils.isNotBlank(click))
            click = ValueReplacer.forEach(keys, values, click);

        for (Player p : event.getRecipients()) {
            String temp = channel.formatOutput(p, player, message, true);

            new ChatMessageBuilder(p, player, temp)
                    .setHoverToAll(hover)
                    .setClickToAll(click).send();
        }

        if (timer > 0) map.put(player, System.currentTimeMillis());
    }

    @SneakyThrows
    static void check() {
        SIRPlugin.checkAccess(ChatFormatter.class);
    }

    @Nullable
    public static ChatChannel getGlobalFormat(Player player) {
        check();

        for (var entry : GLOBAL_MAP.entrySet())
            for (var c : entry.getValue())
                if (PlayerUtils.hasPerm(player, c.getPermission()))
                    return c;

        return ChatChannel.getDefaults();
    }

    @Nullable
    public static ChatChannel getLocalFromMessage(Player player, String message) {
        check();

        for (var entry : LOCAL_MAP.entrySet())
            for (var c : entry.getValue()) {
                if (!PlayerUtils.hasPerm(player, c.getPermission()))
                    continue;

                final String prefix = c.getAccessPrefix();
                if (StringUtils.isBlank(prefix)) continue;

                if (message.startsWith(prefix)) return c;
            }

        return null;
    }

    @Nullable
    public static ChatChannel getLocalFromCommand(Player player, String command) {
        check();

        for (var entry : LOCAL_MAP.values())
            for (var c : entry) {
                if (!PlayerUtils.hasPerm(player, c.getPermission()))
                    continue;

                List<String> list = c.getAccessCommands();
                if (list == null || list.isEmpty()) continue;

                if (list.contains(command)) return c;
            }

        return null;
    }
}
