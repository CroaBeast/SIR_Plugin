package me.croabeast.sirplugin.module.listener;

import com.Zrips.CMI.Containers.CMIUser;
import lombok.var;
import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.channel.ChatChannel;
import me.croabeast.sirplugin.channel.GeneralChannel;
import me.croabeast.sirplugin.event.SIRChatEvent;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.hook.LoginHook;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ChatFormatter extends SIRViewer {

    public static final List<ChatChannel> CHANNEL_LIST = new ArrayList<>();
    private static final HashMap<Player, Long> TIMED_PLAYERS = new HashMap<>();

    private static boolean notRegistered = true;

    public ChatFormatter() {
        super("formats");
    }

    @Override
    public void registerModule() {
        if (notRegistered) {
            register();
            notRegistered = false;
        }

        if (!isEnabled()) return;
        if (!CHANNEL_LIST.isEmpty()) CHANNEL_LIST.clear();

        var section = FileCache.FORMATS.getSection("formats");
        if (section == null) return;

        for (var key : section.getKeys(false)) {
            var c = section.getConfigurationSection(key);
            if (c == null) continue;

            var channel = new GeneralChannel(c);
            CHANNEL_LIST.add(channel);

            if (channel.getSubChannel() != null)
                CHANNEL_LIST.add(channel.getSubChannel());
        }

        System.out.println(CHANNEL_LIST.stream().map(ChatChannel::getName).collect(Collectors.toList()));
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onChatting(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!isEnabled()) return;

        final var player = event.getPlayer();

        if (Exceptions.isPluginEnabled("AdvancedBan")) {
            var id = UUIDManager.get().getUUID(player.getName());

            if (PunishmentManager.get().isMuted(id)) {
                event.setCancelled(true);
                return;
            }
        }

        if (Exceptions.isPluginEnabled("CMI") && CMIUser.getUser(player).isMuted()) {
            event.setCancelled(true);
            return;
        }

        if (LoginHook.isEnabled() && !JoinQuitHandler.LOGGED_PLAYERS.contains(player)) {
            event.setCancelled(true);
            return;
        }

        var sender = LangUtils.getSender().setTargets(player);
        var message = event.getMessage();

        if (StringUtils.isBlank(message) &&
                !FileCache.MODULES.getValue("chat.allow-empty", false)) {
            event.setCancelled(true);

            sender.send(FileCache.LANG.toList("chat.empty-message"));
            return;
        }

        var invalid = FileCache.LANG.toList("chat.invalid-format");

        var id = FileCache.FORMATS.permSection(player, "formats");
        if (id == null) {
            sender.send(invalid);

            event.setCancelled(true);
            return;
        }

        ChatChannel channel = null;
        for (var m : CHANNEL_LIST)
            if (m.getSection() == id) channel = m;

        if (channel == null) {
            sender.send(invalid);

            event.setCancelled(true);
            return;
        }

        SIRChatEvent chatEvent = new SIRChatEvent(
                player, channel,
                event.getMessage(), event.isAsynchronous()
        );
        chatEvent.call();

        if (chatEvent.isCancelled()) {
            LogUtils.rawLog(
                    "&c[SIR-ERROR] The SIR Chat event was cancelled. " +
                    "No message was being sent."
            );

            event.setCancelled(true);
            return;
        }

        channel = chatEvent.getChannel();
        message = chatEvent.getFormattedOutput(true);

        if (FileCache.MODULES.getValue("chat.default-format", false) ||
                channel.isDefault() && !TextUtils.IS_JSON.apply(message)) {
            event.setFormat(message);
            return;
        }

        event.setCancelled(true); // cancels the bukkit chat event

        int timer = channel.getCooldown();

        if (timer > 0 && TIMED_PLAYERS.containsKey(player)) {
            long rest = System.currentTimeMillis() - TIMED_PLAYERS.get(player);

            if (rest < timer * 1000L) {
                event.setCancelled(true);
                int time = timer - ((int) (Math.round(rest / 100D) / 10));

                sender.setKeys("{time}").setValues(time).send(channel.getCdMessages());
                return;
            }
        }

        if (Initializer.hasDiscord())
            new DiscordSender(player, "chat").setKeys("{prefix}", "{suffix}", "{message}").
                    setValues(
                            channel.getPrefix(), channel.getSuffix(),
                            IridiumAPI.stripAll(BeansLib.getLoadedInstance().
                                    formatPlaceholders(player, chatEvent.getMessage()))
                    ).
                    send();

        var players = chatEvent.getRecipients();
        players.add(player);
        players.forEach(p -> chatEvent.getChatBuilder().clone().setPlayer(p).send());

        LogUtils.doLog(chatEvent.getFormattedOutput(false));

        if (timer > 0) TIMED_PLAYERS.put(player, System.currentTimeMillis());
    }
}
