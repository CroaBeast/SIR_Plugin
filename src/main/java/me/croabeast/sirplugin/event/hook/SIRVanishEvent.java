package me.croabeast.sirplugin.event.hook;

import lombok.Getter;
import me.croabeast.sirplugin.event.SIRPlayerEvent;
import org.bukkit.entity.Player;

public class SIRVanishEvent extends SIRPlayerEvent {

    @Getter
    private final boolean isVanished;

    public SIRVanishEvent(Player player, boolean isVanished) {
        super(player);
        this.isVanished = isVanished;
    }
}
