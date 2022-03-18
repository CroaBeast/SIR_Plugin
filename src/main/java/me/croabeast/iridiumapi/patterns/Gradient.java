package me.croabeast.iridiumapi.patterns;

import me.croabeast.iridiumapi.IridiumAPI;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gradient extends BasePattern {

    Pattern pattern = Pattern.compile("(?i)<G:([0-9A-F]{6})>(.*?)</G:([0-9A-F]{6})>");

    @Override
    public String process(String string) {
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            String start = matcher.group(1);
            String end = matcher.group(3);
            String content = matcher.group(2);
            string = string.replace(matcher.group(),
                    IridiumAPI.color(content,
                            new Color(Integer.parseInt(start, 16)),
                            new Color(Integer.parseInt(end, 16))
                    )
            );
        }
        return string;
    }
}