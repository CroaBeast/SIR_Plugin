package me.croabeast.sir.handlers;

import me.croabeast.sir.SIR;
import me.croabeast.sir.interfaces.TitleMain;
import me.croabeast.sir.utils.LangUtils;
import org.bukkit.entity.Player;

public class TitleNew implements TitleMain {

    private final LangUtils langUtils;

    public TitleNew(SIR main) {
        this.langUtils = main.getLangUtils();
    }

    @Override
    public void send(Player player, String title, String subtitle) {
        title = langUtils.parsePAPI(player, title); subtitle = langUtils.parsePAPI(player, subtitle);
        player.sendTitle(title, subtitle, 20, 3 * 20, 20);
    }
}
