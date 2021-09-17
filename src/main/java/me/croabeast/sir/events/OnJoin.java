package me.croabeast.sir.events;

import me.croabeast.sir.SIR;
import me.croabeast.sir.utils.LangUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoin implements Listener {

    private final SIR main;
    private final LangUtils langUtils;

    public OnJoin(SIR main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (langUtils.hasUserLogin && main.getConfig().getBoolean("options.after-login")) return;

        Player player = event.getPlayer();
        String select = !player.hasPlayedBefore() ? "first-join" : "join";
        ConfigurationSection section = main.getMessages().getConfigurationSection(select);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection id = section.getConfigurationSection(key);
            if (id == null) continue;
            String perm = id.getString("permission");
            String sound = id.getString("sound");

            if (perm != null && !player.hasPermission(perm)) continue;
            if (sound != null) langUtils.sound(player, sound);

            langUtils.eventSend(player, id + ".send");
            langUtils.eventSend(player, id + ".motd");
            if (id.isSet("commands")) langUtils.eventCommand(player, id + ".commands");
        }
    }
}
