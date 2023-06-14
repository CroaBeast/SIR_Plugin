package me.croabeast.sirplugin.module.listener;

import com.Zrips.CMI.Containers.CMIUser;
import lombok.var;
import me.croabeast.beanslib.builder.ChatMessageBuilder;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.channel.ChatChannel;
import me.croabeast.sirplugin.channel.GeneralChannel;
import me.croabeast.sirplugin.event.chat.SIRChatEvent;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.hook.LoginHook;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatFormatter extends SIRViewer {

    public static final List<ChatChannel> CHANNEL_LIST = new ArrayList<>();

    private static boolean notRegistered = true;

    private static final HashMap<Player, Long> GLOBAL_PLAYERS = new HashMap<>();
    private static final HashMap<Player, Long> LOCAL_PLAYERS = new HashMap<>();

    public ChatFormatter() {
        super("channels");
    }

    @Override
    public void registerModule() {
        if (notRegistered) {
            register();
            notRegistered = false;
        }

        if (!isEnabled()) return;
        if (!CHANNEL_LIST.isEmpty()) CHANNEL_LIST.clear();

        var defs = GeneralChannel.getDefaults();

        var section = FileCache.CHANNELS.getSection("channels");
        if (section == null) {
            if (defs != null) CHANNEL_LIST.add(defs);
            return;
        }

        var keys = section.getKeys(false);
        if (keys.isEmpty()) {
            if (defs != null) CHANNEL_LIST.add(defs);
            return;
        }

        for (var key : section.getKeys(false)) {
            var c = section.getConfigurationSection(key);
            if (c == null) continue;

            var channel = new GeneralChannel(c);
            CHANNEL_LIST.add(channel);

            if (channel.getSubChannel() != null)
                CHANNEL_LIST.add(channel.getSubChannel());
        }
    }

    private static boolean canBeCancelled(Player player) {
        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            var id = UUIDManager.get().getUUID(player.getName());
            if (PunishmentManager.get().isMuted(id)) return true;
        }

        if (Exceptions.isPluginEnabled("CMI") &&
                CMIUser.getUser(player).isMuted()) return true;

        return LoginHook.isEnabled() &&
                !JoinQuitHandler.LOGGED_PLAYERS.contains(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        final var player = event.getPlayer();

        if (canBeCancelled(player)) {
            event.setCancelled(true);
            return;
        }

        var message = event.getMessage();

        if (StringUtils.isBlank(message) &&
                !FileCache.MODULES.getValue("chat.allow-empty", false))
        {
            LangUtils.getSender().setTargets(player).
                    send(FileCache.LANG.toList("chat.empty-message"));

            event.setCancelled(true);
            return;
        }

        final boolean isAsync = event.isAsynchronous();

        ChatChannel channel = getGlobalFormat(player);
        if (channel != null) {
            var global = new SIRChatEvent(player, channel, message, isAsync);
            global.setGlobal(true);

            String output = channel.formatOutput(player, message, true);
            boolean notDefault = true;

            if (FileCache.MODULES.getValue("chat.default-format", false) ||
                    (channel.isDefault() && !TextUtils.IS_JSON.test(output))
            ) {
                event.setFormat(output);
                notDefault = false;
            }

            if (notDefault) {
                event.setCancelled(true);
                global.call();
            }
            return;
        }

        ChatChannel local = getLocalFromMessage(message);
        if (local == null) return;

        final String prefix = local.getAccessPrefix();

        if (StringUtils.isNotBlank(prefix))
            message = message.substring(prefix.length());

        new SIRChatEvent(player, local, message, isAsync).call();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        var args = event.getMessage().split(" ");

        var local = getLocalFromCommand(args[0]);
        if (local == null) return;

        event.setCancelled(true);

        String message = SIRTask.getFromArray(args, 1);
        if (StringUtils.isBlank(message)) return;

        boolean b = event.isAsynchronous();
        var player = event.getPlayer();

        new SIRChatEvent(player, local, message, b).call();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onGlobal(SIRChatEvent event) {
        if (event.isCancelled()) return;

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();

        String message = event.getMessage();

        var map = event.isGlobal() ? GLOBAL_PLAYERS : LOCAL_PLAYERS;
        int timer = channel.getCooldown();

        if (timer > 0 && map.containsKey(player)) {
            long rest = System.currentTimeMillis() - map.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));

                LangUtils.getSender().setTargets(player).
                        setKeys("{time}").
                        setValues(time).send(channel.getCdMessages());
                return;
            }
        }

        if (Initializer.hasDiscord()) {
            var m = SIRPlugin.getUtils().formatPlaceholders(player, message);
            var channelName = event.isGlobal() ? "global-chat" : channel.getName();

            new DiscordSender(player, channelName).
                    setKeys(channel.getChatKeys()).
                    setValues(channel.getChatValues(m)).
                    send();
        }

        LogUtils.doLog(channel.formatOutput(player, message, false));

        String[] values = channel.getChatValues(message);
        String[] keys = channel.getChatKeys();

        var hover = channel.getHoverList();
        if (hover != null)
            hover.replaceAll(s -> ValueReplacer.forEach(keys, values, s));

        var click = channel.getClickAction();
        if (StringUtils.isNotBlank(click))
            click = ValueReplacer.forEach(keys, values, click);

        String fc = click;

        event.getRecipients().stream().
                map(p ->
                        new ChatMessageBuilder(
                                p, player,
                                channel.formatOutput(p, player, message, true)
                        ).
                        setHover(hover).setClick(fc)
                ).
                forEach(ChatMessageBuilder::send);

        if (timer > 0) map.put(player, System.currentTimeMillis());
    }

    @Nullable
    public static ChatChannel getGlobalFormat(Player player) {
        var c = FileCache.CHANNELS.permSection(player, "channels");
        if (c == null) return null;

        for (var channel : CHANNEL_LIST)
            if (channel.getSection().equals(c)) return channel;

        return null;
    }

    @Nullable
    public static ChatChannel getLocalFromMessage(String message) {
        HashMap<ChatChannel, String> map = new HashMap<>();
        for (var c : CHANNEL_LIST) map.put(c, c.getAccessPrefix());

        for (var entry : map.entrySet()) {
            var prefix = entry.getValue();

            if (StringUtils.isBlank(prefix)) continue;
            if (message.startsWith(prefix)) return entry.getKey();
        }

        return null;
    }

    @Nullable
    public static ChatChannel getLocalFromCommand(String command) {
        HashMap<ChatChannel, List<String>> map = new HashMap<>();

        for (var c : CHANNEL_LIST)
            map.put(c, c.getAccessCommands());

        for (var entry : map.entrySet()) {
            var list = entry.getValue();
            if (list == null || list.isEmpty()) continue;

            if (list.contains(command)) return entry.getKey();
        }

        return null;
    }
}
