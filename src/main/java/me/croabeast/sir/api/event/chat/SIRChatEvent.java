package me.croabeast.sir.api.event.chat;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.api.event.SIRPlayerEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter @Setter
public class SIRChatEvent extends SIRPlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

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

    public Set<Player> getRecipients() {
        return channel.getRecipients(player);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
