package me.croabeast.sircore.handlers;

import me.croabeast.sircore.interfaces.TitleMain;
import org.bukkit.entity.Player;

public class Title17 implements TitleMain {

    @Override
    public void send(Player player, String title, String subtitle, int in, int stay, int out) {
        player.sendTitle(title, subtitle, in, stay, out);
    }
}
