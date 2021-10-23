package me.croabeast.iridiumapi.patterns;

import me.croabeast.iridiumapi.IridiumAPI;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a gradient color pattern which can be applied to a String.
 */
public class Gradient implements Patterns {

    public String process(String string) {
        Matcher matcher = Pattern.compile("<G:([0-9A-Fa-f]{6})>(.*?)</G:([0-9A-Fa-f]{6})>").matcher(string);
        while (matcher.find()) {
            String start = matcher.group(1);
            String end = matcher.group(3);
            String content = matcher.group(2);
            string = string.replace(matcher.group(), IridiumAPI.color(content, new Color(Integer.parseInt(start, 16)), new Color(Integer.parseInt(end, 16))));
        }
        return string;
    }

}