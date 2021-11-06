package me.croabeast.sircore.terminals;

import org.bukkit.entity.Player;

public interface TitleMain {
    void send(Player player, String title, String subtitle, int in, int stay, int out);
}
