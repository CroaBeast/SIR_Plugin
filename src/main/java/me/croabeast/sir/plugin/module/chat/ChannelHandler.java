package me.croabeast.sir.plugin.module.chat;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.builder.ChatBuilder;
import me.croabeast.beans.message.MessageChannel;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.event.chat.SIRChatEvent;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.command.mute.MuteCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.DiscordHook;
import me.croabeast.sir.plugin.module.hook.LoginHook;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
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

public final class ChannelHandler extends ChatModule implements CustomListener {

    private static final Map<Integer, Set<ChatChannel>> LOCAL_MAP, GLOBAL_MAP;

    static {
        LOCAL_MAP = new TreeMap<>(Collections.reverseOrder());
        GLOBAL_MAP = new TreeMap<>(Collections.reverseOrder());
    }

    private final ConfigurableFile main;

    @Getter @Setter
    private boolean registered = false;

    ChannelHandler() {
        super(Name.CHANNELS, YAMLData.Module.Chat.CHANNELS);
        main = YAMLData.Module.Chat.getMain();
    }

    @Override
    public boolean register() {
        if (!isEnabled()) return false;
        registerOnSIR();

        LOCAL_MAP.clear();
        GLOBAL_MAP.clear();

        ChatChannel def = ChatChannel.getDefaults();
        Set<ChatChannel> defs = def != null ?
                Sets.newHashSet(def) : new HashSet<>();

        Map<Integer, Set<ConfigUnit>> channels;
        try {
            channels = config.getUnitsByPriority("channels");
            if (channels.isEmpty()) {
                GLOBAL_MAP.put(0, defs);
                return true;
            }
        } catch (Exception e) {
            GLOBAL_MAP.put(0, defs);
            return true;
        }

        for (Map.Entry<Integer, Set<ConfigUnit>> entry : channels.entrySet()) {
            Set<ChatChannel> globals = new LinkedHashSet<>();
            Set<ChatChannel> locals = new LinkedHashSet<>();

            final int i = entry.getKey();

            for (ConfigUnit id : entry.getValue()) {
                ChatChannel c;
                try {
                    c = ChatChannel.of(id.getSection());
                } catch (Exception e) {
                    continue;
                }

                (c.isGlobal() ? globals : locals).add(c);

                ChatChannel local = c.getSubChannel();
                if (local != null) locals.add(local);
            }

            if (!globals.isEmpty()) GLOBAL_MAP.put(i, globals);
            if (!locals.isEmpty()) LOCAL_MAP.put(i, locals);
        }

        return true;
    }

    public static boolean isMuted(Player player) {
        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            String id = UUIDManager.get().getUUID(player.getName());
            return PunishmentManager.get().isMuted(id);
        }

        if (Exceptions.isPluginEnabled("Essentials")) {
            Essentials e = JavaPlugin.getPlugin(Essentials.class);
            return e.getUser(player).isVanished();
        }

        return Exceptions.isPluginEnabled("CMI") ?
                CMIUser.getUser(player).isMuted() :
                MuteCommand.isMuted(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        Player player = event.getPlayer();

        if (isMuted(player) || !LoginHook.isLogged(player)) {
            event.setCancelled(true);
            return;
        }

        String message = event.getMessage();

        if (!main.get("allow-empty", false) && StringUtils.isBlank(message))
        {
            MessageSender.loaded().setTargets(player)
                    .send(main.toStringList("chat.empty-message"));

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

        if (main.get("default-format", false)) {
            event.setFormat(TextUtils.STRIP_JSON.apply(output).replace("%", "%%"));
            return;
        }

        event.setCancelled(true);
        global.call();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        String[] args = event.getMessage().split(" ");
        Player player = event.getPlayer();

        ChatChannel local = getLocalFromCommand(player, args[0]);
        if (local == null) return;

        event.setCancelled(true);

        String message = LangUtils.stringFromArray(args, 1);
        if (StringUtils.isBlank(message)) return;

        boolean b = event.isAsynchronous();

        new SIRChatEvent(player, local, message, b).call();
    }

    @EventHandler
    private void onSIRChat(SIRChatEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();

        final String[] keys = channel.getChatKeys();
        String message = event.getMessage();

        if (DISCORD.isEnabled()) {
            String m = BeansLib.getLib().formatPlaceholders(player, message);
            String name = event.isGlobal() ? "global-chat" : channel.getName();

            DiscordHook.send(name, player, keys, channel.getChatValues(m));
        }

        BeansLib.logger().log(channel.formatOutput(player, message, false));
        String[] values = channel.getChatValues(message);

        List<String> hover = channel.getHoverList();
        if (hover != null)
            hover.replaceAll(s -> ReplaceUtils.replaceEach(keys, values, s));

        String click = channel.getClickAction();
        if (StringUtils.isNotBlank(click))
            click = ReplaceUtils.replaceEach(keys, values, click);

        for (Player p : event.getRecipients()) {
            String temp = channel.formatOutput(p, player, message, true);

            MessageChannel.CHAT.send(
                    p, player,
                    new ChatBuilder(temp)
                            .setHoverToAll(hover)
                            .setClickToAll(click).toPatternString()
            );
        }
    }

    @Nullable
    static ChatChannel getGlobalFormat(Player player) {
        for (Map.Entry<Integer, Set<ChatChannel>> entry : GLOBAL_MAP.entrySet())
            for (ChatChannel c : entry.getValue())
                if (PlayerUtils.hasPerm(player, c.getPermission()))
                    return c;

        return ChatChannel.getDefaults();
    }

    @Nullable
    static ChatChannel getLocalFromMessage(Player player, String message) {
        for (Map.Entry<Integer, Set<ChatChannel>> entry : LOCAL_MAP.entrySet())
            for (ChatChannel c : entry.getValue()) {
                if (!PlayerUtils.hasPerm(player, c.getPermission()))
                    continue;

                final String prefix = c.getAccessPrefix();
                if (StringUtils.isBlank(prefix)) continue;

                if (message.startsWith(prefix)) return c;
            }

        return null;
    }

    @Nullable
    static ChatChannel getLocalFromCommand(Player player, String command) {
        for (Set<ChatChannel> entry : LOCAL_MAP.values())
            for (ChatChannel c : entry) {
                if (!PlayerUtils.hasPerm(player, c.getPermission()))
                    continue;

                List<String> list = c.getAccessCommands();
                if (list == null || list.isEmpty()) continue;

                if (list.contains(command)) return c;
            }

        return null;
    }
}
