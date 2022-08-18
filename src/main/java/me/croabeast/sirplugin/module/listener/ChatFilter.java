package me.croabeast.sirplugin.module.listener;

import me.croabeast.beanslib.utility.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import org.apache.commons.lang.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

public class ChatFilter extends SIRViewer {

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.FILTERS;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFilter(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        ConfigurationSection id = FileCache.FILTERS.permSection(player, "filters");
        if (id == null) return;

        List<String> words = TextUtils.toList(id, "words");
        if (words.isEmpty()) return;

        String replacer = id.getString("replace-char", "*");

        for (String word : words) {
            if (StringUtils.isBlank(word)) continue;

            if (id.getBoolean("is-regex")) {
                Matcher match = Pattern.compile(word).matcher(message);
                if (!match.find()) continue;

                String matcher = match.group();
                String filter = StringUtils.repeat(replacer, matcher.length());

                message = message.replace(matcher, filter);
                continue;
            }

            if (!message.contains(word)) continue;

            message = message.replace(word,
                    StringUtils.repeat(replacer, word.length()));
        }

        event.setMessage(message);
    }
}
