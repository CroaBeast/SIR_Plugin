package me.croabeast.sircore.handlers;

import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.interfaces.TitleMain;
import me.croabeast.sircore.utils.LangUtils;
import org.bukkit.entity.Player;

public class TitleNew implements TitleMain {

    @Override
    public void send(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 20, 3 * 20, 20);
    }
}
