package me.croabeast.sir.plugin.channel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.key.PlayerKey;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.applier.StringApplier;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.module.object.ChatTagsParser;
import me.croabeast.sir.plugin.module.object.EmojisParser;
import me.croabeast.sir.plugin.module.object.MentionsParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Skeletal constructor of the {@link ChatChannel} interface.
 */
@SuppressWarnings("all")
@Getter
abstract class AbstractChatChannel implements ChatChannel {

    static final String DEF_FORMAT = " &7{player}: {message}";

    @NotNull
    private final ConfigurationSection section;
    private final boolean isGlobal;

    @Nullable
    private final ChatChannel parent;

    @NotNull
    private final String permission;
    private final int priority;

    @Nullable
    private final String prefix, suffix;
    private final String color;

    @Getter(AccessLevel.NONE)
    @NotNull
    private final ColorChecker colorChecker;

    private final int cooldown;
    @NotNull
    private final List<String> cdMessages;

    private final int radius;
    private final List<String> worldsNames;

    @Nullable
    private final String clickAction;
    @Nullable
    private final List<String> hoverList;

    @Setter
    @NotNull
    private String chatFormat;

    AbstractChatChannel(ConfigurationSection section, @Nullable ChatChannel parent) throws IllegalAccessException {
        this.section = section;
        isGlobal = section.getBoolean("global", true);

        this.parent = parent;

        permission = fromParent("permission", ChatChannel::getPermission, "DEFAULT");
        priority = fromParent(
                "priority", ChatChannel::getPriority,
                permission.matches("(?i)DEFAULT") ? 0 : 1
        );

        prefix = fromParent("prefix", ChatChannel::getPrefix, null);
        suffix = fromParent("suffix", ChatChannel::getSuffix, null);

        color = fromParent("color-string", ChatChannel::getColor, null);

        colorChecker = new ColorChecker(
                fromBoolean("color.normal"), fromBoolean("color.special"),
                fromBoolean("color.rgb")
        );

        cooldown = fromParent("cooldown.time", ChatChannel::getCooldown, 0);
        cdMessages = fromList(
                "cooldown.message", ChatChannel::getCdMessages,
                new ArrayList<>()
        );

        radius = fromParent("radius", ChatChannel::getRadius, isGlobal ? -1 : 100);
        worldsNames = fromList("worlds", ChatChannel::getWorldsNames, null);

        clickAction = fromParent("click-action", ChatChannel::getClickAction, null);
        hoverList = fromList("hover", ChatChannel::getHoverList, null);

        chatFormat = fromParent("format", ChatChannel::getChatFormat, DEF_FORMAT);
    }

    private boolean useParents(String path) {
        return !section.isSet(path) && parent != null;
    }

    private boolean fromBoolean(String path) {
        return useParents(path) ? parent.getSection().getBoolean(path) : section.getBoolean(path);
    }

    private <T> T fromParent(String path, Function<ChatChannel, T> f, T def) {
        return useParents(path) ? f.apply(parent) : (T) section.get(path, def);
    }

    private List<String> fromList(
            String p, Function<ChatChannel, List<String>> list, List<String> def
    ) {
        return useParents(p) ? list.apply(parent) : TextUtils.toList(section, p, def);
    }

    @NotNull
    public String formatOutput(Player t, Player p, String message, boolean isChat) {
        String rawFormat = isChat ? getChatFormat() : getLogFormat();

        StringApplier applier = StringApplier.simplified(rawFormat)
                .apply(s -> PlayerKey.replaceKeys(p, s))
                .apply(s -> {
                    String[] values = getChatValues(colorChecker.check(message));
                    return ValueReplacer.forEach(getChatKeys(), values, s);
                })
                .apply(s -> ChatTagsParser.parse(p, s))
                .apply(s -> EmojisParser.parse(p, s))
                .apply(s -> MentionsParser.parse(p, this, s));

        if (isChat) applier.apply(Beans::convertToSmallCaps);

        final String format = applier.toString();

        if (isDefault() && !TextUtils.IS_JSON.test(format)) {
            return StringApplier.simplified(format)
                    .apply(s -> Beans.createCenteredChatMessage(t, p, s))
                    .toString();
        }

        return isChatEventless() ?
                format :
                StringApplier.simplified(format)
                        .apply(TextUtils.STRIP_JSON)
                        .toString();
    }

    @Override
    public String toString() {
        String sub = getSubChannel() == null ? "null" : getSubChannel().getName(),
                parentName = parent == null ? "null" : parent.getName();

        return "ChatChannel{section='" + section.getName() + "', isGlobal='" + isGlobal +
                "', parent='" + parentName + "', local='" + sub +
                "', permission='" + permission + "', priority='" + priority + "'}";
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;

        if (!(o instanceof ChatChannel)) return false;

        ChatChannel channel = (ChatChannel) o;
        return section.equals(channel.getSection());
    }

    @RequiredArgsConstructor
    static class ColorChecker {

        final boolean normal, special, rgb;

        String check(String s) {
            StringApplier applier = StringApplier.simplified(s);

            if (!normal) applier.apply(NeoPrismaticAPI::stripBukkit);
            if (!rgb) applier.apply(NeoPrismaticAPI::stripRGB);
            if (!special) applier.apply(NeoPrismaticAPI::stripSpecial);

            return applier.toString();
        }
    }
}
