package me.croabeast.sirplugin.event.chat;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.sirplugin.channel.ChatChannel;
import me.croabeast.sirplugin.event.SIRPlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter @Setter
public class SIRChatEvent extends SIRPlayerEvent implements Cancellable {

    private ChatChannel channel;
    private String message;

    private boolean cancelled = false;
    private boolean global = false;

    public SIRChatEvent(
            @NotNull Player player, @NotNull ChatChannel channel,
            @NotNull String message, boolean isAsync
    ) {
        super(player, isAsync);

        this.channel = channel;
        this.message = message;
    }

    public void setFormat(String format) {
        channel.setChatFormat(format);
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public Set<Player> getRecipients() {
        return channel.getRecipients(player);
    }
}
