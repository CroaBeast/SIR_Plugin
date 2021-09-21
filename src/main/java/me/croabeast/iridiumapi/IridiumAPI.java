package me.croabeast.iridiumapi;

import com.google.common.collect.ImmutableMap;
import me.croabeast.iridiumapi.patterns.Gradient;
import me.croabeast.iridiumapi.patterns.Patterns;
import me.croabeast.iridiumapi.patterns.Rainbow;
import me.croabeast.iridiumapi.patterns.SolidColor;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.*;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IridiumAPI {

    private static final int VERSION = Integer.parseInt(getMajorVersion(Bukkit.getVersion()).substring(2));

    private static final boolean SUPPORTS_RGB = VERSION >= 16;

    private static final List<String> SPECIAL_COLORS = Arrays.asList("&l", "&n", "&o", "&k", "&m");

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

    private static final List<Patterns> PATTERNS = Arrays.asList(new Gradient(), new SolidColor(), new Rainbow());

    @NotNull
    public static String process(@NotNull String string) {
        for (Patterns patterns : PATTERNS) string = patterns.process(string);
        string = ChatColor.translateAlternateColorCodes('&', string);
        return string;
    }

    @NotNull
    public static List<String> process(@NotNull List<String> strings) {
        return strings.stream().map(IridiumAPI::process).collect(Collectors.toList());
    }

    @NotNull
    public static String color(@NotNull String string, @NotNull Color color) {
        return (SUPPORTS_RGB ? ChatColor.of(color) : getClosestColor(color)) + string;
    }

    @NotNull
    public static String color(@NotNull String string, @NotNull Color start, @NotNull Color end) {
        StringBuilder specialColors = new StringBuilder();
        for (String color : SPECIAL_COLORS) {
            if (string.contains(color)) {
                specialColors.append(color);
                string = string.replace(color, "");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        ChatColor[] colors = createGradient(start, end, string.length());
        String[] characters = string.split("");
        for (int i = 0; i < string.length(); i++) stringBuilder.append(colors[i]).append(specialColors).append(characters[i]);
        return stringBuilder.toString();
    }

    @NotNull
    public static String rainbow(@NotNull String string, float saturation) {
        StringBuilder specialColors = new StringBuilder();
        for (String color : SPECIAL_COLORS) {
            if (string.contains(color)) {
                specialColors.append(color);
                string = string.replace(color, "");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        ChatColor[] colors = createRainbow(string.length(), saturation);
        String[] characters = string.split("");
        for (int i = 0; i < string.length(); i++) stringBuilder.append(colors[i]).append(specialColors).append(characters[i]);
        return stringBuilder.toString();
    }

    @NotNull
    public static ChatColor getColor(@NotNull String string) {
        return SUPPORTS_RGB ? ChatColor.of(new Color(Integer.parseInt(string, 16))) : getClosestColor(new Color(Integer.parseInt(string, 16)));
    }

    @NotNull
    public static String stripColorFormatting(@NotNull String string) {
        return string.replaceAll("[&§][a-f0-9lnokm]|<[/]?\\w{5,8}(:[0-9A-F]{6})?>", "");
    }

    @NotNull
    private static ChatColor[] createRainbow(int step, float saturation) {
        ChatColor[] colors = new ChatColor[step];
        double colorStep = (1.00 / step);
        for (int i = 0; i < step; i++) {
            Color color = Color.getHSBColor((float) (colorStep * i), saturation, saturation);
            if (SUPPORTS_RGB) colors[i] = ChatColor.of(color);
            else colors[i] = getClosestColor(color);
        }
        return colors;
    }

    @NotNull
    private static ChatColor[] createGradient(@NotNull Color start, @NotNull Color end, int step) {
        ChatColor[] colors = new ChatColor[step];
        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1);
        int stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1);
        int stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);
        int[] direction = new int[]{
                start.getRed() < end.getRed() ? +1 : -1,
                start.getGreen() < end.getGreen() ? +1 : -1,
                start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(start.getRed() + ((stepR * i) * direction[0]), start.getGreen() + ((stepG * i) * direction[1]), start.getBlue() + ((stepB * i) * direction[2]));
            if (SUPPORTS_RGB) colors[i] = ChatColor.of(color);
            else colors[i] = getClosestColor(color);
        }
        return colors;
    }

    @NotNull
    private static ChatColor getClosestColor(Color color) {
        Color nearestColor = null;
        double nearestDistance = Integer.MAX_VALUE;

        for (Color constantColor : COLORS.keySet()) {
            double distance = Math.pow(color.getRed() - constantColor.getRed(), 2) + Math.pow(color.getGreen() - constantColor.getGreen(), 2) + Math.pow(color.getBlue() - constantColor.getBlue(), 2);
            if (nearestDistance > distance) {
                nearestColor = constantColor;
                nearestDistance = distance;
            }
        }
        return COLORS.get(nearestColor);
    }

    @NotNull
    private static String getMajorVersion(@NotNull String version) {
        Validate.notEmpty(version, "Cannot get major Minecraft version from null or empty string");

        int index = version.lastIndexOf("MC:");
        if (index != -1) version = version.substring(index + 4, version.length() - 1);
        else if (version.endsWith("SNAPSHOT")) {
            index = version.indexOf('-');
            version = version.substring(0, index);
        }

        int lastDot = version.lastIndexOf('.');
        if (version.indexOf('.') != lastDot) version = version.substring(0, lastDot);

        return version;
    }

}