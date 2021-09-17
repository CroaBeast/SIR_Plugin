package me.croabeast.iridiumapi.patterns;

import me.croabeast.iridiumapi.IridiumAPI;

import java.util.regex.Matcher;

public class Rainbow implements Patterns {

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<R:([0-9]{1,3})>(.*?)</R>");

    public String process(String string) {
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            String saturation = matcher.group(1);
            String content = matcher.group(2);
            string = string.replace(matcher.group(), IridiumAPI.rainbow(content, Float.parseFloat(saturation)));
        }
        return string;
    }

}