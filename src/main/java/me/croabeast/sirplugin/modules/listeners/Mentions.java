package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.SIRPlugin.*;
import static me.croabeast.sirplugin.objects.FileCache.*;

public class Mentions extends BaseViewer {

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.MENTIONS;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMention(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        ConfigurationSection id = EventUtils.getSection(MENTIONS.toFile(), player, "mentions");
        if (id == null) return;

        String prefix = id.getString("prefix");
        if (StringUtils.isBlank(prefix)) return;

        Player target = null;
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (!message.matches("(?i)" + prefix + t.getName())) continue;
            target = t;
        }
        if (target == null || player == target) return;

        String[] keys = {"{sender}", "{receiver}", "{prefix}"},
                values = {player.getName(), target.getName(), prefix};

        textUtils().sendMessageList(player, id, "messages.sender", keys, values);
        textUtils().sendMessageList(target, id, "messages.receiver", keys, values);

        EventUtils.playSound(player, id.getString("sound.sender"));
        EventUtils.playSound(target, id.getString("sound.receiver"));

        String output = id.getString("value", "&b{prefix}{receiver}");

        List<String> hoverList = TextUtils.toList(id, "hover");
        if (!hoverList.isEmpty())
            hoverList.replaceAll(line -> TextUtils.replaceInsensitiveEach(line, keys, values));

        String click = id.getString("click");
        if (click != null) click = TextUtils.replaceInsensitiveEach(click, keys, values);

        if (!hoverList.isEmpty() || click != null) {
            String format = "";

            if (!hoverList.isEmpty()) {
                format += "<hover=[";
                format += String.join(textUtils().lineSeparator(), hoverList) + "]";
                format = format.replaceAll("\\\\Q", "").replaceAll("\\\\E", "");
            }

            if (click != null) {
                String[] array = click.split(":", 2);
                format += (!hoverList.isEmpty() ? "|" : "<") + array[0] + "=[" + array[1] + "]>";
            }
            else format += ">";

            if (StringUtils.isNotBlank(format)) output = format + output + "</text>";
        }

        output = TextUtils.replaceInsensitiveEach(output, keys, values);
        event.setMessage(message.replace(prefix + target.getName(), output));
    }
}
