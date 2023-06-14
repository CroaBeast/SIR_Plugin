package me.croabeast.sirplugin.channel;

import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.task.ChatViewTask;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface ChatChannel {

    String DEF_FORMAT = " &7{player}: {message}";

    @NotNull ConfigurationSection getSection();

    boolean isGlobal();

    @Nullable ChatChannel getParent();

    @NotNull
    default String getName() {
        return (
                getParent() == null ||
                        getParent().equals(GeneralChannel.getDefaults()) ?
                        "" : (getParent().getName() + ":")
                ) +
                getSection().getName();
    }

    @Nullable ChatChannel getSubChannel();

    int getRadius();

    @NotNull String getPermission();

    int getPriority();

    @Nullable String getPrefix();

    @Nullable String getSuffix();

    int getCooldown();

    @NotNull List<String> getCdMessages();

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
        final int r = getRadius();

        if (isGlobal() || player == null) return set;

        var players = r <= 0 ?
                player.getNearbyEntities(r, r, r).
                        stream().
                        filter(e -> e instanceof Player).
                        map(e -> (Player) e) :
                set.stream();

        set = players.
                filter(p ->
                        PlayerUtils.hasPerm(p, getPermission()) &&
                        ChatViewTask.isToggled(p, getName())
                ).
                collect(Collectors.toSet());

        set.add(player);
        return set;
    }

    @NotNull String getChatFormat();

    void setChatFormat(@NotNull String format);

    default String getLogFormat() {
        String log = !FileCache.MODULES.getValue("chat.simple-logger.enabled", false) ?
                getChatFormat() :
                FileCache.MODULES.getValue("chat.simple-logger.format", DEF_FORMAT);

        return TextUtils.STRIP_FIRST_SPACES.apply(log);
    }

    default boolean noChatEvents() {
        var h = getHoverList();
        return StringUtils.isBlank(getClickAction()) && (h == null || h.isEmpty());
    }

    default boolean isDefault() {
        return noChatEvents() && getRadius() <= 0;
    }

    @NotNull String formatOutput(Player target, Player parser, String message, boolean isChat);

    @NotNull
    default String formatOutput(Player player, String message, boolean isChat) {
        return formatOutput(player, player, message, isChat);
    }

    default String[] getChatKeys() {
        return new String[] {"{prefix}", "{suffix}", "{message}"};
    }

    default String[] getChatValues(String message) {
        return new String[] {getPrefix(), getSuffix(), message};
    }
}
