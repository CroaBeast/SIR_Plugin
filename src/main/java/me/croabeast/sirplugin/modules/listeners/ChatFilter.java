package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.sirplugin.objects.FileCache.*;

public class ChatFilter extends BaseViewer {

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.FILTERS;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFilter(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        ConfigurationSection id = EventUtils.getSection(FILTERS.toFile(), player, "filters");
        if (id == null) return;

        List<String> words = TextUtils.toList(id, "words");
        if (words.isEmpty()) return;

        String replacer = id.getString("replace-char", "*");

        for (String word : words) {
            if (id.getBoolean("is-regex")) {
                Matcher match = Pattern.compile(word).matcher(message);
                if (!match.find()) continue;

                String matcher = match.group();
                String filter = StringUtils.repeat(replacer, matcher.length());

                message = message.replace(matcher, filter);
                continue;
            }

            if (!message.contains(word)) continue;
            String filter = StringUtils.repeat(replacer, word.length());
            message = message.replace(word, filter);
        }

        event.setMessage(message);
    }
}
