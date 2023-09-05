package me.croabeast.sir.plugin.module.instance.listener;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import me.croabeast.beanslib.builder.ChatMessageBuilder;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.event.chat.SIRChatEvent;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.Initializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.channel.GeneralChannel;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.hook.LoginHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.task.SIRTask;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChatFormatter extends SIRModule implements CustomListener, CacheHandler {

    public static final List<ChatChannel> CHANNEL_LIST = new ArrayList<>();

    private static boolean notRegistered = true;

    private static final HashMap<Player, Long>
            GLOBAL_PLAYERS = new HashMap<>(), LOCAL_PLAYERS = new HashMap<>();

    public ChatFormatter() {
        super(ModuleName.CHAT_CHANNELS);
    }

    @Override
    public void registerModule() {
        if (notRegistered) {
            register();
            notRegistered = false;
        }

        loadCache();
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
        if (!CHANNEL_LIST.isEmpty()) CHANNEL_LIST.clear();

        ChatChannel defs = GeneralChannel.getDefaults();

        ConfigurationSection section = channels().getSection("channels");
        if (section == null) {
            if (defs != null) CHANNEL_LIST.add(defs);
            return;
        }

        Set<String> keys = section.getKeys(false);
        if (keys.isEmpty()) {
            if (defs != null) CHANNEL_LIST.add(defs);
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(key);
            if (c == null) continue;

            GeneralChannel channel = new GeneralChannel(c);
            CHANNEL_LIST.add(channel);

            if (channel.getSubChannel() != null)
                CHANNEL_LIST.add(channel.getSubChannel());
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

        return LoginHook.isEnabled() &&
                !JoinQuitHandler.LOGGED_PLAYERS.contains(player);
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
            MessageSender.fromLoaded().setTargets(player).
                    send(FileCache.getLang().toList("chat.empty-message"));

            event.setCancelled(true);
            return;
        }

        final boolean isAsync = event.isAsynchronous();

        ChatChannel channel = getGlobalFormat(player);
        if (channel != null) {
            SIRChatEvent global = new SIRChatEvent(player, channel, message, isAsync);
            global.setGlobal(true);

            String output = channel.formatOutput(player, message, true);

            if (config().getValue("default-format", false) ||
                    (channel.isDefault() && !TextUtils.IS_JSON.test(output))
            ) {
                event.setFormat(TextUtils.STRIP_JSON.apply(output).replace("%", "%%"));
            } else {
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

        String[] args = event.getMessage().split(" ");

        ChatChannel local = getLocalFromCommand(args[0]);
        if (local == null) return;

        event.setCancelled(true);

        String message = SIRTask.getFromArray(args, 1);
        if (StringUtils.isBlank(message)) return;

        boolean b = event.isAsynchronous();
        Player player = event.getPlayer();

        new SIRChatEvent(player, local, message, b).call();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onGlobal(SIRChatEvent event) {
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

                MessageSender.fromLoaded().setTargets(player).
                        setKeys("{time}").setValues(t).
                        setLogger(false).
                        send(channel.getCdMessages());
                return;
            }
        }

        if (Initializer.hasDiscord()) {
            String m = SIRPlugin.getUtils().formatPlaceholders(player, message);
            String channelName = event.isGlobal() ? "global-chat" : channel.getName();

            new DiscordSender(player, channelName).
                    setKeys(channel.getChatKeys()).
                    setValues(channel.getChatValues(m)).
                    send();
        }

        LogUtils.doLog(channel.formatOutput(player, message, false));

        String[] keys = channel.getChatKeys();
        String[] values = channel.getChatValues(message);

        List<String> hover = channel.getHoverList();
        if (hover != null)
            hover.replaceAll(s -> ValueReplacer.forEach(keys, values, s));

        String click = channel.getClickAction();
        if (StringUtils.isNotBlank(click))
            click = ValueReplacer.forEach(keys, values, click);

        String fc = click;

        event.getRecipients().stream().
                map(p ->
                        new ChatMessageBuilder(
                                p, player,
                                channel.formatOutput(p, player, message, true)
                        ).
                        setHoverToAll(hover).setClickToAll(fc)
                ).
                forEach(ChatMessageBuilder::send);

        if (timer > 0) map.put(player, System.currentTimeMillis());
    }

    @Nullable
    public static ChatChannel getGlobalFormat(Player player) {
        ConfigurationSection c = channels().permSection(player, "channels");
        if (c == null) return null;

        for (ChatChannel channel : CHANNEL_LIST)
            if (channel.getSection().equals(c)) return channel;

        return null;
    }

    @Nullable
    public static ChatChannel getLocalFromMessage(String message) {
        HashMap<ChatChannel, String> map = new HashMap<>();
        for (ChatChannel c : CHANNEL_LIST) map.put(c, c.getAccessPrefix());

        for (Map.Entry<ChatChannel, String> entry : map.entrySet()) {
            String prefix = entry.getValue();

            if (StringUtils.isBlank(prefix)) continue;
            if (message.startsWith(prefix)) return entry.getKey();
        }

        return null;
    }

    @Nullable
    public static ChatChannel getLocalFromCommand(String command) {
        HashMap<ChatChannel, List<String>> map = new HashMap<>();
        for (ChatChannel c : CHANNEL_LIST) map.put(c, c.getAccessCommands());

        for (Map.Entry<ChatChannel, List<String>> entry : map.entrySet()) {
            List<String> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;

            if (list.contains(command)) return entry.getKey();
        }

        return null;
    }
}
