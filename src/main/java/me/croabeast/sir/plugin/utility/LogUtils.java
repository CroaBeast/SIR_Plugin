package me.croabeast.sir.plugin.utility;

import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.Beans;
import me.croabeast.sir.plugin.module.object.EmojiParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@UtilityClass
public class LogUtils {

    private String[] parseEmojis(String... lines) {
        String[] results = new String[lines.length];

        for (int i = 0; i < lines.length; i++) {
            try {
                results[i] = EmojiParser.parse(null, lines[i]);
            } catch (Exception e) {
                results[i] = lines[i];
            }
        }
        return results;
    }

    public void playerLog(Player player, String... lines) {
        Beans.playerLog(player, parseEmojis(lines));
    }

    public void rawLog(String... lines) {
        Beans.rawLog(parseEmojis(lines));
    }

    public void doLog(CommandSender sender, String... lines) {
        Beans.doLog(sender, parseEmojis(lines));
    }

    public void doLog(String... lines) {
        doLog(null, lines);
    }

    public void mixLog(String... lines) {
        Beans.mixLog(parseEmojis(lines));
    }
}
