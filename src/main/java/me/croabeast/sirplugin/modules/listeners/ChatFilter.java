package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.modules.BaseModule;
import me.croabeast.sirplugin.utilities.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

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
            if (!message.contains(word)) continue;
            String filter = StringUtils.repeat(replacer, word.length());
            message = message.replace(word, filter);
        }

        event.setMessage(message);
    }
}
