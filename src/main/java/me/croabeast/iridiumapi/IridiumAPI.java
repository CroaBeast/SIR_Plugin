package me.croabeast.iridiumapi;

import com.google.common.collect.*;
import me.croabeast.iridiumapi.patterns.*;
import net.md_5.bungee.api.*;
import org.apache.commons.lang.*;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class IridiumAPI {

    private static final int VERSION = Integer.parseInt(Bukkit.getBukkitVersion()
            .split("-")[0].split("\\.")[1]);

    private static final boolean SUPPORTS_RGB = VERSION > 15;

    private static final List<String> SPECIAL_COLORS =
            Arrays.asList("&l", "&n", "&o", "&k", "&m", "§l", "§n", "§o", "§k", "§m");

    private static final Map<Color, ChatColor> COLORS = ImmutableMap.<Color, ChatColor>builder()
            .put(new Color(0), ChatColor.getByChar('0'))
            .put(new Color(170), ChatColor.getByChar('1'))
            .put(new Color(43520), ChatColor.getByChar('2'))
            .put(new Color(43690), ChatColor.getByChar('3'))
            .put(new Color(11141120), ChatColor.getByChar('4'))
            .put(new Color(11141290), ChatColor.getByChar('5'))
            .put(new Color(16755200), ChatColor.getByChar('6'))
            .put(new Color(11184810), ChatColor.getByChar('7'))
            .put(new Color(5592405), ChatColor.getByChar('8'))
            .put(new Color(5592575), ChatColor.getByChar('9'))
            .put(new Color(5635925), ChatColor.getByChar('a'))
            .put(new Color(5636095), ChatColor.getByChar('b'))
            .put(new Color(16733525), ChatColor.getByChar('c'))
            .put(new Color(16733695), ChatColor.getByChar('d'))
            .put(new Color(16777045), ChatColor.getByChar('e'))
            .put(new Color(16777215), ChatColor.getByChar('f')).build();

    private static final List<BasePattern> PATTERNS =
            Arrays.asList(new Gradient(), new SolidColor(), new Rainbow());

    @NotNull
    public static String process(@NotNull String string) {
        for (BasePattern pattern : PATTERNS) string = pattern.process(string);
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    @NotNull
    public static String color(@NotNull String string, @NotNull Color start, @NotNull Color end) {
        int step = withoutSpecialChar(string).length();
        if (step <= 1) return string;
        return apply(string, createGradient(start, end, step));
    }

    @NotNull
    public static String rainbow(@NotNull String string, float saturation) {
        int step = withoutSpecialChar(string).length();
        if (step <= 0) return string;
        return apply(string, createRainbow(step, saturation));
    }

    @NotNull
    public static ChatColor getColor(@NotNull String string) {
        return SUPPORTS_RGB ? ChatColor.of(new Color(Integer.parseInt(string, 16)))
                : getClosestColor(new Color(Integer.parseInt(string, 16)));
    }

    @NotNull
    public static String stripBukkit(@NotNull String string) {
        return string.replaceAll("(?i)[&§][a-f0-9]", "");
    }

    @NotNull
    public static String stripSpecial(@NotNull String string) {
        return string.replaceAll("(?i)[&§][k-o]", "");
    }

    @NotNull
    public static String stripRGB(@NotNull String string) {
        return string.replaceAll("(?i)<[/]?[gr](:[0-9]{3,6})?>|\\{#[0-9A-F]{6}}|" +
                "<#[0-9A-F]{6}>|&#[0-9A-F]{6}|#[0-9A-F]{6}", "");
    }

    @NotNull
    public static String stripAll(@NotNull String string) {
        return string.replaceAll("(?i)[&§][a-f0-9lnokmr]|<[/]?[gr](:[0-9]{3,6})?>|" +
                "\\{#[0-9A-F]{6}}|<#[0-9A-F]{6}>|&#[0-9A-F]{6}|#[0-9A-F]{6}/gm", "");
    }

    @NotNull
    private static String apply(@NotNull String source, @NotNull ChatColor[] colors) {
        StringBuilder specialColors = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        String[] characters = source.split("");

        if (StringUtils.isBlank(source)) return source;

        int outIndex = 0;
        try {
            for (int i = 0; i < characters.length; i++) {
                if (characters[i].matches("[&§]") && i + 1 < characters.length) {
                    if (!characters[i + 1].equals("r")) {
                        specialColors.append(characters[i]);
                        specialColors.append(characters[i + 1]);
                    }
                    else specialColors.setLength(0);
                    i++;
                }
                else stringBuilder.append(colors[outIndex++])
                        .append(specialColors).append(characters[i]);
            }
        }
        catch (IndexOutOfBoundsException e) {
            return source;
        }

        return stringBuilder.toString();
    }

    @NotNull
    private static String withoutSpecialChar(@NotNull String source) {
        String workingString = source;
        for (String color : SPECIAL_COLORS) {
            if (!workingString.contains(color)) continue;
            workingString = workingString.replace(color, "");
        }
        return workingString;
    }

    @NotNull
    private static ChatColor[] createRainbow(int step, float saturation) {
        ChatColor[] colors = new ChatColor[step];
        double colorStep = (1.00 / step);

        for (int i = 0; i < step; i++) {
            Color color = Color.getHSBColor((float) (colorStep * i), saturation, saturation);
            colors[i] = SUPPORTS_RGB ? ChatColor.of(color) : getClosestColor(color);
        }
        return colors;
    }

    @NotNull
    private static ChatColor[] createGradient(@NotNull Color start, @NotNull Color end, int step) {
        ChatColor[] colors = new ChatColor[step];

        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1),
                stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1),
                stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);

        int[] direction = new int[] {
                start.getRed() < end.getRed() ? +1 : -1,
                start.getGreen() < end.getGreen() ? +1 : -1,
                start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(start.getRed() + ((stepR * i) * direction[0]),
                    start.getGreen() + ((stepG * i) * direction[1]),
                    start.getBlue() + ((stepB * i) * direction[2]));
            colors[i] = SUPPORTS_RGB ? ChatColor.of(color) : getClosestColor(color);
        }

        return colors;
    }

    @NotNull
    private static ChatColor getClosestColor(Color color) {
        Color nearestColor = null;
        double nearestDistance = Integer.MAX_VALUE;

        for (Color color1 : COLORS.keySet()) {
            double distance = Math.pow(color.getRed() - color1.getRed(), 2)
                    + Math.pow(color.getGreen() - color1.getGreen(), 2)
                    + Math.pow(color.getBlue() - color1.getBlue(), 2);
            if (nearestDistance > distance) {
                nearestColor = color1;
                nearestDistance = distance;
            }
        }
        return COLORS.get(nearestColor);
    }
}