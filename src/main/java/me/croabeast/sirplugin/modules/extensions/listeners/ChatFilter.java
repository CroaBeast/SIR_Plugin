package me.croabeast.sirplugin.modules.extensions.listeners;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;
import java.util.regex.*;

public class ChatFilter extends BaseModule implements Listener {

    private final SIRPlugin main;

    public ChatFilter(SIRPlugin main) {
        this.main = main;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.FILTERS;
    }

    @Override
    public void registerModule() {
        SIRPlugin.registerListener(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFilter(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        ConfigurationSection id =
                main.getEventUtils().getSection(main.getFilters(), player, "filters");
        if (id == null) return;

        List<String> words = TextUtils.fileList(id, "words");
        if (words.isEmpty()) return;

        String replacer = id.getString("replace-char", "*");

        for (String word : words) {
            if (id.getBoolean("is-regex")) {
                Matcher match = Pattern.compile(word).matcher(message);
                if (!match.find()) continue;

                String matcher = match.group();
                String filter = StringUtils.repeat(replacer, matcher.length());
                message = message.replace(matcher, filter);
            }
            else {
                if (!message.contains(word)) continue;
                String filter = StringUtils.repeat(replacer, word.length());
                message = message.replace(word, filter);
            }
        }

        event.setMessage(message);
    }
}
