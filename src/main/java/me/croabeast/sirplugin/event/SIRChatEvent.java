package me.croabeast.sirplugin.event;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.beanslib.builder.ChatMessageBuilder;
import me.croabeast.sirplugin.channel.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter @Setter
public class SIRChatEvent extends SIRPlayerEvent implements Cancellable {

    private ChatChannel channel;
    private String message;

    private boolean cancelled = false;

    public SIRChatEvent(
            @NotNull Player player, @NotNull ChatChannel channel,
            @NotNull String message, boolean isAsync
    ) {
        super(player, isAsync);

        this.channel = channel;
        this.message = message;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public void setFormat(String format) {
        channel.setChatFormat(format);
    }

    public Set<Player> getRecipients() {
        return channel.getRecipients(getPlayer());
    }

    public String getFormattedOutput(boolean isChat) {
        return channel.formatOutput(getPlayer(), message, isChat);
    }

    public ChatMessageBuilder getChatBuilder() {
        return new ChatMessageBuilder(getFormattedOutput(true)).
                setHover(channel.getHoverList()).
                setClick(channel.getClickAction());
    }
}
