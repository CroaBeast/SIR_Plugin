package me.croabeast.sir.plugin.command.message;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
public final class Conversation {

    private static final Set<Conversation> CONVERSATIONS = new HashSet<>();

    private final UUID uuid = UUID.randomUUID();

    @NotNull
    private final CommandSender initiator, receiver;

    Conversation(CommandSender initiator, CommandSender receiver) {
        this.initiator = Objects.requireNonNull(initiator);
        this.receiver = Objects.requireNonNull(receiver);

        Conversation loaded = asInitiator(initiator);
        if (loaded != null) CONVERSATIONS.remove(loaded);

        CONVERSATIONS.add(this);
    }

    public boolean isInitiator(CommandSender sender) {
        return Objects.equals(initiator, sender);
    }

    public boolean isReceiver(CommandSender sender) {
        return Objects.equals(receiver, sender);
    }

    public boolean contains(CommandSender sender) {
        return isInitiator(sender) || isReceiver(sender);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation)) return false;

        Conversation that = (Conversation) o;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }

    public static Conversation find(CommandSender sender) {
        if (sender == null) return null;

        for (Conversation c : CONVERSATIONS)
            if (c.contains(sender)) return c;

        return null;
    }

    public static Conversation asInitiator(CommandSender initiator, CommandSender receiver) {
        Conversation conversation = null;

        for (Conversation c : CONVERSATIONS)
            if (c.isInitiator(initiator)) {
                conversation = c;
                break;
            }

        return conversation != null &&
                conversation.isReceiver(receiver) ?
                conversation : null;
    }

    public static Conversation asInitiator(CommandSender sender) {
        Conversation conversation = null;

        for (Conversation c : CONVERSATIONS)
            if (c.isInitiator(sender)) {
                conversation = c;
                break;
            }

        return conversation;
    }

    public static Conversation asReceiver(CommandSender receiver, CommandSender initiator) {
        Conversation conversation = null;

        for (Conversation c : CONVERSATIONS)
            if (c.isReceiver(receiver)) {
                conversation = c;
                break;
            }

        return conversation != null &&
                conversation.isInitiator(initiator) ?
                conversation : null;
    }

    public static Conversation asReceiver(CommandSender sender) {
        Conversation conversation = null;

        for (Conversation c : CONVERSATIONS)
            if (c.isReceiver(sender)) {
                conversation = c;
                break;
            }

        return conversation;
    }

    public static boolean hasConversation(CommandSender sender) {
        return find(sender) != null;
    }

    public static void remove(CommandSender initiator, CommandSender receiver) {
        Conversation conversation = asInitiator(initiator, receiver);
        if (conversation != null) CONVERSATIONS.remove(conversation);
    }

    public static void remove(CommandSender sender) {
        Conversation conversation;

        while ((conversation = find(sender)) != null)
            CONVERSATIONS.remove(conversation);
    }
}
