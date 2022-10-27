package me.croabeast.sirplugin.object;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.object.file.FileCache;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatFormat {

    private final String prefix;
    private final String suffix;

    private final ColorChecker checker;
    private final Cooldown cooldown;

    private final int radius;
    private final String world;

    private final List<String> hover;
    private final String click;

    private final String format;

    public ChatFormat(ConfigurationSection section) {
        if (section == null) throw new NullPointerException();

        prefix = section.getString("prefix");
        suffix = section.getString("suffix");

        ColorChecker checker = new ColorChecker(false, false, false);
        Cooldown cooldown = new Cooldown(new ArrayList<>(), 0);

        try {
            checker = new ColorChecker(section);
            cooldown = new Cooldown(section);
        } catch (NullPointerException ignored) {}

        this.checker = checker;
        this.cooldown = cooldown;

        radius = section.getInt("radius");
        world = section.getString("world");

        this.hover =
                TextUtils.toList(section, "hover").
                stream().filter(Objects::nonNull).
                collect(Collectors.toList());

        click = section.getString("click");

        format = section.getString("format");
    }

    public ChatFormat(Player player) {
        this(FileCache.FORMATS.permSection(player, "formats"));
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

    public int getRadius() {
        if (getDefault() == null) return radius;
        return radius <= 0 ? getDefault().radius : radius;
    }

    public String getWorld() {
        if (getDefault() == null) return world;
        return world == null ? getDefault().world : world;
    }

    public List<String> getHover() {
        if (getDefault() == null) return hover;
        return hover.isEmpty() ? getDefault().hover : hover;
    }

    public String getClick() {
        if (getDefault() == null) return click;
        return click == null ? getDefault().click : click;
    }

    public String getFormat() {
        if (getDefault() == null) return format;
        return format == null ? getDefault().format : format;
    }

    @RequiredArgsConstructor
    @Getter
    static class ColorChecker {

        private final boolean normal;
        private final boolean special;
        private final boolean rgb;

        public ColorChecker(ConfigurationSection id) {
            if (id == null)
                throw new NullPointerException();

            id = id.getConfigurationSection("color");
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

        private final List<String> message;
        private final int time;

        public Cooldown(ConfigurationSection id) {
            if (id == null)
                throw new NullPointerException();

            id = id.getConfigurationSection("cooldown");
            if (id == null)
                throw new NullPointerException();

            message = TextUtils.toList(id, "message");
            time = id.getInt("time");
        }
    }

    public static ChatFormat getDefault() {
        if (!FileCache.MODULES.value("chat.default.enabled", true)) return null;
        return new ChatFormat(FileCache.MODULES.getSection("chat.default"));
    }
}
