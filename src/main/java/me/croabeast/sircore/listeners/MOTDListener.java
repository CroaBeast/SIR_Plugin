package me.croabeast.sircore.listeners;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.util.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import static me.croabeast.sircore.listeners.MOTDListener.UsageType.*;
import static me.croabeast.sircore.listeners.MOTDListener.MaxType.*;

public class MOTDListener implements Listener {

    private final Application main;
    private final Recorder recorder;

    private ServerListPingEvent event;

    private int MOTD = 0, ICON = 0;

    public MOTDListener(Application main) {
        this.main = main;
        this.recorder = main.getRecorder();
        main.registerListener(this);
        registerIconsFolder();
    }

    private void registerIconsFolder() {
        File folder = new File(main.getDataFolder(), "icons");
        if (!folder.exists() && folder.mkdirs())
            recorder.doRecord("&eGenerating the 'icons' folder...");

        File icon = new File(folder, "server-icon.png");
        if (icon.exists()) return;

        recorder.doRecord(
                "&eGenerating the default server icon...",
                "&7If you don't want to generate this file,",
                "&7just name a file/icon:&e 'server-icon.png'"
        );
        String path = "icons" + File.separator + "server-icon.png";
        main.saveResource(path, false);
    }

    private ConfigurationSection getList() {
        return main.getMOTD().getConfigurationSection("motd-list");
    }

    private Player getPlayerFromIP() {
        Player player = null;
        for (Player p : main.everyPlayer()) {
            InetSocketAddress address = p.getAddress();
            if (address == null) return null;
            if (address.getAddress() == event.getAddress()) player = p;
        }
        return player;
    }

    private void registerMOTD() {
        List<String> keys = new ArrayList<>(getList().getKeys(false));
        Map<Integer, ConfigurationSection> sections = new HashMap<>();

        keys.forEach(s ->
                sections.put(keys.indexOf(s), getList().getConfigurationSection(s))
        );

        int count = sections.size() - 1;
        if (MOTD > count) MOTD = 0;

        event.setMotd(IridiumAPI.process(
                main.getTextUtils().parsePAPI(getPlayerFromIP(), "" +
                sections.get(MOTD).getString("1", "") + "\n" +
                sections.get(MOTD).getString("2", "")
        )));

        if (!main.getMOTD().getBoolean("random-motds")) {
            if (MOTD < count) MOTD++;
            else MOTD = 0;
        }
        else MOTD = new Random().nextInt(count + 1);
    }

    enum UsageType {
        DISABLED,
        LIST,
        SINGLE,
        RANDOM
    }

    private UsageType usageType() {
        UsageType type;
        switch (main.getMOTD().getString("server-icon.usage", "DISABLED").toUpperCase()) {
            case "SINGLE":
                type = SINGLE;
                break;
            case "LIST":
                type = LIST;
                break;
            case "RANDOM":
                type = RANDOM;
                break;
            case "DISABLED": default:
                type = DISABLED;
                break;
        }
        return type;
    }

    private void defaultIcon() {
        try {
            event.setServerIcon(null);
        } catch (Exception e) {
            recorder.doRecord(
                    "&cError when trying to set to null the server icon.",
                    "&7Your server doesn't support setting the icon to none."
            );
        }
    }

    private void setServerIcon() {
        if (usageType() == DISABLED) return;

        File folder = new File(main.getDataFolder() + File.separator + "icons");
        File single = new File(folder, main.getMOTD().getString("server-icon.image", ""));

        File[] icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (icons == null) {
            defaultIcon();
            return;
        }

        int count = icons.length - 1;
        if (usageType() != SINGLE && ICON > count) ICON = 0;

        CachedServerIcon icon = null;

        try {
            icon = Bukkit.loadServerIcon(usageType() == SINGLE ? single : icons[ICON]);
        }
        catch (Exception e) {
            defaultIcon();
            event.setMotd(IridiumAPI.process(
                    "&cError loading your custom icon \n&7" +
                    e.getLocalizedMessage()
            ));
            recorder.doRecord("&7Error loading the icon: &c" + e.getLocalizedMessage());
        }

        if (icon == null) {
            defaultIcon();
            return;
        }

        event.setServerIcon(icon);

        if (usageType() == SINGLE) return;

        if (usageType() == LIST) {
            if (ICON < count) ICON++;
            else ICON = 0;
        }
        else if (usageType() == RANDOM)
            ICON = new Random().nextInt(count + 1);
    }

    enum MaxType {
        DEFAULT,
        CUSTOM,
        MAXIMUM
    }

    private MaxType maxType() {
        MaxType type;
        switch (main.getMOTD().getString("max-players.type", "DEFAULT").toUpperCase()) {
            case "CUSTOM":
                type = CUSTOM;
                break;
            case "MAXIMUM":
                type = MAXIMUM;
                break;
            case "DEFAULT": default:
                type = DEFAULT;
                break;
        }
        return type;
    }

    private void setMaxPlayers() {
        if (maxType() == DEFAULT) return;

        if (maxType() == CUSTOM)
            event.setMaxPlayers(main.getMOTD().getInt("max-players.count"));
        else if (maxType() == MAXIMUM)
            event.setMaxPlayers(main.everyPlayer().size() + 1);
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        this.event = event;
        if (!main.getMOTD().getBoolean("enabled")) return;

        registerMOTD();
        setServerIcon();
        setMaxPlayers();
    }
}
