package me.croabeast.sirplugin.channel;

import lombok.*;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.EmojiParser;
import me.croabeast.sirplugin.module.MentionParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
abstract class AbstractChannel implements ChatChannel {

    @NotNull
    private final ConfigurationSection section;
    private final boolean isGlobal;

    private final ChatChannel parent;

    private final int radius;

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

    @Nullable
    private final String clickAction;
    @Nullable
    private final List<String> hoverList;

    @Setter @NotNull
    private String chatFormat;

    AbstractChannel(ConfigurationSection section, ChatChannel parent) {
        this.section = section;
        isGlobal = section.getBoolean("global", true);

        this.parent = parent;

        radius = fromParent("radius", ChatChannel::getRadius, isGlobal ? -1 : 100);

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

        clickAction = fromParent("click-action", ChatChannel::getClickAction, null);
        hoverList = fromList("hover", ChatChannel::getHoverList, null);

        chatFormat = fromParent("format", ChatChannel::getChatFormat, DEF_FORMAT);
    }

    private boolean useParents(String path) {
        return !section.isSet(path) && !isParent();
    }

    private boolean fromBoolean(String path) {
        return useParents(path) ? parent.getSection().getBoolean(path) : section.getBoolean(path);
    }

    @SuppressWarnings("unchecked")
    private <T> T fromParent(String path, Function<ChatChannel, T> f, T def) {
        return useParents(path) ? f.apply(parent) : (T) section.get(path, def);
    }

    private List<String> fromList(String path, Function<ChatChannel, List<String>> list, List<String> def) {
        return useParents(path) ? list.apply(parent) : TextUtils.toList(section, path, def);
    }

    public String formatOutput(Player p, String message, boolean isChat) {
        message = colorChecker.check(message);
        String format = isChat ? getChatFormat() : getLogFormat();

        format = ValueReplacer.of("{message}", message, format);

        format = ValueReplacer.forEach(
                new String[] {"{prefix}", "{suffix}"},
                new String[] {getPrefix(), getSuffix()},
                format, false
        );

        format = SIRPlugin.getUtils().parsePlayerKeys(p, format);
        format = MentionParser.parse(p, EmojiParser.parse(p, format));

        if (isDefault() && !TextUtils.IS_JSON.apply(format)) {
            format = TextUtils.STRIP_JSON.apply(format.
                    replace("\\", "\\\\").
                    replace("$", "\\$"));

            return SIRPlugin.getUtils().
                    createCenteredChatMessage(null, p, format).
                    replace("%", "%%");
        }

        return !noChatEvents() ?
                TextUtils.STRIP_JSON.apply(format) : format;
    }

    @Override
    public String toString() {
        String sub = getSubChannel() == null ? "null" : getSubChannel().getName();

        return "ChatChannel{" +
                "section=" + section.getName() + ", isGlobal=" + isGlobal +
                ", parent=" + parent.getName() + ", subChannel=" + sub +
                ", permission='" + permission + '\'' + ", priority=" + priority +
                ", chatFormat='" + chatFormat + '\'' + '}';
    }

    @RequiredArgsConstructor
    static final class ColorChecker {

        final boolean normal, special, rgb;

        String check(String s) {
            if (!normal) s = IridiumAPI.stripBukkit(s);
            if (!rgb) s = IridiumAPI.stripRGB(s);
            if (!special) s = IridiumAPI.stripSpecial(s);

            return s;
        }
    }
}
