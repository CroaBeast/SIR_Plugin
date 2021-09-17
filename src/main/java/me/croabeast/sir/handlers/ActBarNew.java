package me.croabeast.sir.handlers;

import me.croabeast.sir.SIR;
import me.croabeast.sir.interfaces.ActionBar;
import me.croabeast.sir.utils.LangUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ActBarNew implements ActionBar {

    private final LangUtils langUtils;

    public ActBarNew(SIR main) {
        this.langUtils = main.getLangUtils();
    }

    private void actionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }

    @Override
    public void send(Player player, String message) {
        actionBar(player, langUtils.parsePAPI(player, message));
    }
}
