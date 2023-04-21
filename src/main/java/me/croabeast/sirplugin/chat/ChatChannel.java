package me.croabeast.sirplugin.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.file.FileCache;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatChannel {

    protected final String prefix;
    protected final String suffix;

    protected final ColorChecker checker;
    protected final Cooldown cooldown;

    protected final String click;
    protected final List<String> hover;

    protected final String format;

    public ChatChannel(ConfigurationSection id) {
        if (id == null) throw new NullPointerException();

        prefix = id.getString("prefix");
        suffix = id.getString("suffix");

        ColorChecker checker = ColorChecker.DEF_CHECKER;
        Cooldown cooldown = Cooldown.DEF_COOLDOWN;

        try {
            checker = new ColorChecker(
                    id.getConfigurationSection("color"));
        }
        catch (NullPointerException ignored) {}
        try {
            cooldown = new Cooldown(
                    id.getConfigurationSection("cooldown"));
        }
        catch (NullPointerException ignored) {}

        this.checker = checker;
        this.cooldown = cooldown;

        click = id.getString("click");
        this.hover =
                TextUtils.toList(id, "hover").stream().
                        filter(Objects::nonNull).
                        collect(Collectors.toList());

        format = id.getString("format");
    }

    protected ChatChannel getDefault() {
        if (!FileCache.MODULES.getValue("chat.default.enabled", true)) return null;
        return new ChatChannel(FileCache.MODULES.getSection("chat.default"));
    }

    public String getPrefix() {
        if (getDefault() == null) return prefix;
        return prefix == null ? getDefault().prefix : prefix;
    }

    public String getSuffix() {
        if (getDefault() == null) return suffix;
        return suffix == null ? getDefault().suffix : suffix;
    }

    private ColorChecker getChecker() {
        if (getDefault() == null) return checker;
        return checker == null ? getDefault().checker : checker;
    }

    public boolean isNormalColored() {
        return getChecker().isNormal();
    }

    public boolean isSpecialColored() {
        return getChecker().isSpecial();
    }

    public boolean isRgbColored() {
        return getChecker().isRgb();
    }

    private Cooldown getCooldown() {
        if (getDefault() == null) return cooldown;
        return cooldown == null ? getDefault().cooldown : cooldown;
    }

    public int cooldownTime() {
        return getCooldown().getTime();
    }

    public List<String> cooldownMessage() {
        return getCooldown().getMessage();
    }

    public String getClick() {
        if (getDefault() == null) return click;
        return click == null ? getDefault().click : click;
    }

    public List<String> getHover() {
        if (getDefault() == null) return hover;
        return hover.isEmpty() ? getDefault().hover : hover;
    }

    public String getFormat() {
        if (getDefault() == null) return format;
        return format == null ? getDefault().format : format;
    }

    @RequiredArgsConstructor
    @Getter
    static class ColorChecker {

        static final ColorChecker DEF_CHECKER =
                new ColorChecker(false, false, false);

        private final boolean normal;
        private final boolean special;
        private final boolean rgb;

        public ColorChecker(ConfigurationSection id) {
            if (id == null)
                throw new NullPointerException();

            normal = id.getBoolean("normal");
            special = id.getBoolean("special");
            rgb = id.getBoolean("rgb");
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class Cooldown {

        static final Cooldown DEF_COOLDOWN =
                new Cooldown(new ArrayList<>(), 0);

        private final List<String> message;
        private final int time;

        public Cooldown(ConfigurationSection id) {
            if (id == null)
                throw new NullPointerException();

            message = TextUtils.toList(id, "message").stream().
                    filter(Objects::nonNull).
                    collect(Collectors.toList());
            time = id.getInt("time");
        }
    }
}
