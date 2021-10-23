package me.croabeast.iridiumapi.patterns;

import me.croabeast.iridiumapi.IridiumAPI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rainbow implements Patterns {

    public String process(String string) {
        Matcher matcher = Pattern.compile("<R:([0-9]{1,3})>(.*?)</R>").matcher(string);
        while (matcher.find()) {
            String saturation = matcher.group(1);
            String content = matcher.group(2);
            string = string.replace(matcher.group(), IridiumAPI.rainbow(content, Float.parseFloat(saturation)));
        }
        return string;
    }

}