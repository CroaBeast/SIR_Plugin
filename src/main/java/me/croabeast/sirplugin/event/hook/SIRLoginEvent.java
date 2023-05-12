package me.croabeast.sirplugin.event.hook;

import me.croabeast.sirplugin.event.SIRPlayerEvent;
import org.bukkit.entity.Player;

public class SIRLoginEvent extends SIRPlayerEvent {

    public SIRLoginEvent(Player player, boolean isAsync) {
        super(player, isAsync);
    }
}
