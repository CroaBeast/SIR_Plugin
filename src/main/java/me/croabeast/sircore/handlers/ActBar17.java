package me.croabeast.sircore.handlers;

import me.croabeast.sircore.terminals.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;

public class ActBar17 implements ActionBar {

    @Override
    public void send(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
