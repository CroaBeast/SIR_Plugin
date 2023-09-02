package me.croabeast.sir.plugin.channel;

import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.task.ChatViewTask;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @NotNull String getPermission();

    int getPriority();

    @Nullable String getPrefix();

    @Nullable String getSuffix();

    int getCooldown();

    @NotNull List<String> getCdMessages();

    int getRadius();

    @Nullable List<String> getWorldsNames();

    @Nullable
    default List<World> getWorlds() {
        List<String> w = getWorldsNames();
        if (w == null) return null;

        return w.stream().map(Bukkit::getWorld).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

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

        if (player == null) return set;

        Stream<Player> stream = set.stream();

        if (!isGlobal())
            stream = stream.filter(p -> {
                boolean b = PlayerUtils.hasPerm(p, getPermission());
                return b && ChatViewTask.isToggled(p, getName());
            });

        stream = stream.filter(p -> {
                    if (r >= 0) {
                        List<Entity> e = player.getNearbyEntities(r, r, r);
                        return e.isEmpty() || e.contains(p);
                    }

                    List<World> worlds = getWorlds();
                    if (worlds == null || worlds.isEmpty()) return true;

                    for (World w : worlds)
                        if (w.getPlayers().contains(p)) return true;

                    return false;
        });

        set = stream.collect(Collectors.toSet());
        set.add(player);

        return new HashSet<>(set);
    }

    @NotNull String getChatFormat();

    void setChatFormat(@NotNull String format);

    default String getLogFormat() {
        FileCache config = FileCache.CHAT_CHANNELS_CACHE.getConfig();

        String log = !config.getValue("simple-logger.enabled", false) ?
                getChatFormat() :
                config.getValue("simple-logger.format", DEF_FORMAT);

        return TextUtils.STRIP_FIRST_SPACES.apply(log);
    }

    default boolean noChatEvents() {
        List<String> h = getHoverList();
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
