package me.croabeast.sir.plugin.channel;

import lombok.SneakyThrows;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.api.file.Configurable;
import me.croabeast.sir.plugin.command.ChatViewCommand;
import me.croabeast.sir.plugin.command.ignore.IgnoreCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a chat channel that can be received from available and allowed players.
 */
public interface ChatChannel extends ConfigUnit {

    /**
     * If the channel represents the global channel or if it's a custom local channel.
     * @return if global or not
     */
    boolean isGlobal();

    /**
     * See {@link #isGlobal()}, this is the logical negation from that value.
     * @return if local or not
     */
    default boolean isLocal() {
        return !isGlobal();
    }

    /**
     * Returns the parent channel from this channel, can be null if there is no parent.
     * @return the parent channel, can be null
     */
    @Nullable ChatChannel getParent();

    /**
     * Returns the name of the channel defined from its {@link #getSection()} object.
     * @return the channel's name
     */
    @NotNull
    default String getName() {
        ChatChannel p = getParent(), def = ChatChannelImpl.getDefaults();
        return (p == null || p.equals(def) ? "" : (p.getName() + ":")) + getSection().getName();
    }

    /**
     * Returns the nested local channel in the {@link #getSection()} object.
     *
     * <p> Its section's name should be called "local" and can inherit the
     * same values from this channel if there is one or multiple values missing.
     *
     * @return the nested local channel
     */
    @Nullable ChatChannel getSubChannel();

    /**
     * Returns the player's prefix of this channel.
     * @return the prefix, can be null
     */
    @Nullable String getPrefix();

    /**
     * Returns the player's suffix of this channel.
     * @return the suffix, can be null
     */
    @Nullable String getSuffix();

    @Nullable String getColor();

    /**
     * Returns the radius (in blocks) where the receiver players should be located
     * around the sender player to receive the message of the channel.
     *
     * @return the radius
     */
    int getRadius();

    /**
     * Returns the name of the worlds that the channel should be showing.
     * @return the worlds' name, can be null
     */
    @Nullable List<String> getWorldsNames();

    /**
     * Returns the worlds as its Bukkit-related object, if a world with that name
     * exists.
     *
     * <p> If the {@link #getWorldsNames()} list is null, will return null.
     *
     * @return the bukkit worlds, can be null
     */
    @NotNull
    default List<World> getWorlds() {
        List<String> list = getWorldsNames();

        if (list == null || list.isEmpty())
            return Bukkit.getWorlds();

        return CollectionBuilder.of(getWorldsNames())
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull).toList();
    }

    /**
     * Returns the chat click component event of the channel if the format output
     * has as a whole a click event.
     *
     * @return the click action
     */
    @Nullable String getClickAction();

    @Nullable List<String> getHoverList();

    @Nullable
    default String getAccessPrefix() {
        return isGlobal() ? null : getSection().getString("access.prefix");
    }

    @Nullable
    default List<String> getAccessCommands() {
        return isGlobal() ? null : TextUtils.toList(getSection(), "access.commands", null);
    }

    @NotNull
    default Set<Player> getRecipients(Player player) {
        Set<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
        if (player == null) return set;

        CollectionBuilder<Player> builder = CollectionBuilder
                .of(set)
                .filter(p -> getWorlds().contains(p.getWorld()));

        final int r = getRadius();
        if (r > 0) {
            Set<Player> e = PlayerUtils.getNearbyPlayers(player, r);
            if (!e.isEmpty()) builder.filter(e::contains);
        }

        if (isLocal()) {
            if (StringUtils.isNotBlank(getGroup()))
                builder.filter(this::isInGroup);

            builder.filter(this::hasPerm);
            builder.filter(p -> ChatViewCommand.isToggled(p, getName()));
        }

        builder.filter(p -> !IgnoreCommand.isIgnoring(p, player, true));
        return builder.add(player).toSet();
    }

    @NotNull String getChatFormat();

    void setChatFormat(@NotNull String format);

    default String getLogFormat() {
        Configurable config = YAMLData.Module.Chat.getMain();
        String format = AbstractChannel.DEF_FORMAT;

        String log = !config.get("simple-logger.enabled", false) ?
                getChatFormat() :
                config.get("simple-logger.format", format);

        return TextUtils.STRIP_FIRST_SPACES.apply(log);
    }

    default boolean isChatEventless() {
        final List<String> h = getHoverList();
        return StringUtils.isBlank(getClickAction()) && (h == null || h.isEmpty());
    }

    default boolean isDefault() {
        return isChatEventless() && getRadius() <= 0;
    }

    @NotNull String formatOutput(Player target, Player parser, String message, boolean isChat);

    @NotNull
    default String formatOutput(Player player, String message, boolean isChat) {
        return formatOutput(player, player, message, isChat);
    }

    default String[] getChatKeys() {
        return new String[] {"{prefix}", "{me.croabeast.sir-prefix}", "{suffix}", "{me.croabeast.sir-suffix}", "{color}", "{message}"};
    }

    default String[] getChatValues(String message) {
        return new String[] {getPrefix(), getPrefix(), getSuffix(), getSuffix(), getColor(), message};
    }

    boolean equals(Object object);

    @SneakyThrows
    static ChatChannel of(ConfigurationSection section) {
        return new ChatChannelImpl(section);
    }

    static ChatChannel getDefaults() {
        return ChatChannelImpl.getDefaults();
    }
}
