package me.croabeast.sir.plugin.channel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.module.instance.EmojiParser;
import me.croabeast.sir.plugin.module.instance.MentionParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

@Getter
abstract class AbstractChannel implements ChatChannel {

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

    AbstractChannel(ConfigurationSection section, @Nullable ChatChannel parent) {
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

    @SuppressWarnings("all")
    private boolean fromBoolean(String path) {
        return useParents(path) ? parent.getSection().getBoolean(path) : section.getBoolean(path);
    }

    @SuppressWarnings("unchecked")
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
        message = colorChecker.check(message);
        String format = isChat ? getChatFormat() : getLogFormat();

        format = SIRPlugin.getUtils().parsePlayerKeys(p, format);
        format = ValueReplacer.forEach(
                getChatKeys(),
                getChatValues(message), format
        );

        format = MentionParser.parse(p, EmojiParser.parse(p, format));

        if (isDefault() && !TextUtils.IS_JSON.test(format))
            return SIRPlugin.getUtils().createCenteredChatMessage(t, p, format);

        return !noChatEvents() ? TextUtils.STRIP_JSON.apply(format) : format;
    }

    @Override
    public String toString() {
        String sub = getSubChannel() == null ? "null" : getSubChannel().getName(),
                parentName = parent == null ? "null" : parent.getName();

        return "ChatChannel{" +
                "section=" + section.getName() + ", isGlobal=" + isGlobal +
                ", parent=" + parentName + ", subChannel=" + sub +
                ", permission='" + permission + '\'' + ", priority=" + priority +
                ", chatFormat='" + chatFormat + '\'' + '}';
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
            if (!normal) s = NeoPrismaticAPI.stripBukkit(s);
            if (!rgb) s = NeoPrismaticAPI.stripRGB(s);
            if (!special) s = NeoPrismaticAPI.stripSpecial(s);

            return s;
        }
    }
}
