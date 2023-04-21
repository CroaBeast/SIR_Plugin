package me.croabeast.sirplugin.utility;

import lombok.experimental.UtilityClass;
import lombok.var;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.EmojiParser;
import org.bukkit.command.CommandSender;

@UtilityClass
public class LogUtils {

    private String[] parseEmojis(String... lines) {
        var results = new String[lines.length];

        for (int i = 0; i < lines.length; i++) {
            try {
                results[i] = EmojiParser.parseEmojis(null, lines[i]);
            } catch (Exception e) {
                results[i] = lines[i];
            }
        }
        return results;
    }

    public void rawLog(String... lines) {
        SIRPlugin.getUtils().rawLog(parseEmojis(lines));
    }

    public void doLog(CommandSender sender, String... lines) {
        SIRPlugin.getUtils().doLog(sender, parseEmojis(lines));
    }

    public void doLog(String... lines) {
        doLog(null, lines);
    }

    public void mixLog(CommandSender sender, String... lines) {
        SIRPlugin.getUtils().mixLog(sender, parseEmojis(lines));
    }

    public void mixLog(String... lines) {
        SIRPlugin.getUtils().mixLog(parseEmojis(lines));
    }
}