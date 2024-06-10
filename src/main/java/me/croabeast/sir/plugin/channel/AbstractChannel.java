package me.croabeast.sir.plugin.channel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.message.StringAligner;
import me.croabeast.lib.PlayerReplacer;
import me.croabeast.lib.applier.StringApplier;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.module.chat.EmojiParser;
import me.croabeast.sir.plugin.module.chat.MentionParser;
import me.croabeast.sir.plugin.module.chat.TagsParser;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Skeletal constructor of the {@link ChatChannel} interface.
 */
@SuppressWarnings("all")
@Getter
abstract class AbstractChannel implements ChatChannel {

    static final String DEF_FORMAT = " &7{player}: {message}";

    @NotNull
    private final ConfigurationSection section;
    private final boolean global;

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

    private final int radius;
    private final List<String> worldsNames;

    @Nullable
    private final String clickAction;
    @Nullable
    private final List<String> hoverList;

    @Setter
    @NotNull
    private String chatFormat;

    AbstractChannel(ConfigurationSection section, @Nullable ChatChannel parent) throws IllegalAccessException {
        this.section = section;
        global = section.getBoolean("global", true);

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

        radius = fromParent("radius", ChatChannel::getRadius, global ? -1 : 100);
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
                .apply(s -> PlayerReplacer.replaceKeys(p, s))
                .apply(s -> {
                    String[] values = getChatValues(colorChecker.check(p, message));
                    return ReplaceUtils.replaceEach(getChatKeys(), values, s);
                })
                .apply(s -> EmojiParser.parse(p, s))
                .apply(s -> TagsParser.parse(p, s))
                .apply(s -> MentionParser.parse(p, this, s));

        if (isChat) applier.apply(BeansLib.getLib()::convertToSmallCaps);

        final String format = applier.toString();

        if (isDefault() && !TextUtils.IS_JSON.test(format)) {
            return StringApplier.simplified(format)
                    .apply(s -> StringAligner.align(BeansLib.getLib().colorize(t, p, s)))
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

        return "ChatChannel{section='" + section.getName() + "', isGlobal='" + global +
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

        static final String PERM = "me.croabeast.sir.color.chat.";
        final boolean normal, special, rgb;

        boolean notColor(Player player, String perm) {
            boolean b;
            switch (perm) {
                case "normal": default:
                    b = normal;
                    break;
                case "rgb":
                    b = rgb;
                    break;
                case "special":
                    b = special;
                    break;
            }
            return !PlayerUtils.hasPerm(player, "me.croabeast.sir.color.chat." + perm) && !b;
        }

        String check(Player player, String string) {
            StringApplier applier = StringApplier.simplified(string);

            if (notColor(player, "normal"))
                applier.apply(NeoPrismaticAPI::stripBukkit);
            if (notColor(player, "rgb"))
                applier.apply(NeoPrismaticAPI::stripRGB);
            if (notColor(player, "special"))
                applier.apply(NeoPrismaticAPI::stripSpecial);

            return applier.toString();
        }
    }
}
