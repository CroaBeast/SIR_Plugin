package me.croabeast.sircore.listeners;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.*;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.util.*;

import java.io.*;
import java.util.*;

import static me.croabeast.sircore.listeners.MOTDListener.UsageType.*;
import static me.croabeast.sircore.listeners.MOTDListener.MaxType.*;

public class MOTDListener implements Listener {

    private final Application main;
    private ServerListPingEvent event;

    private int MOTD = 0;
    private int ICON = 0;

    public MOTDListener(Application main) {
        this.main = main;
        main.registerListener(this);
        registerIconsFolder();
    }

    private void registerIconsFolder() {
        File folder = new File(main.getDataFolder(), "icons");
        if (!folder.exists() && folder.mkdirs())
            main.getRecords().doRecord("&eGenerating the 'icons' folder...");

        File icon = new File(folder, "server-icon.png");
        if (icon.exists()) return;

        String path = "icons" + File.separator + "server-icon.png";
        main.getRecords().doRecord(
                "&eGenerating the default server icon...",
                "&7If you don't want to generate this file,",
                "&7just name a file/icon:&e 'server-icon.png'"
        );
        main.saveResource(path, false);
    }

    private ConfigurationSection getID() {
        return main.getMOTD().getConfigurationSection("motd-list");
    }

    private void registerMOTD() {
        List<String> keys = new ArrayList<>(getID().getKeys(false));
        Map<Integer, ConfigurationSection> sections = new HashMap<>();

        keys.forEach(s ->
                sections.put(keys.indexOf(s), getID().getConfigurationSection(s))
        );

        int count = sections.size() - 1;
        if (MOTD > count) MOTD = 0;

        event.setMotd(IridiumAPI.process(
                sections.get(MOTD).getString("1", "") + "\n" +
                sections.get(MOTD).getString("2", "")
        ));

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

    private void setServerIcon() {
        if (usageType() == DISABLED) return;

        File folder = new File(main.getDataFolder() + File.separator + "icons");
        File single = new File(folder, main.getMOTD().getString("server-icon.image", ""));

        File[] icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (icons == null) {
            event.setServerIcon(null);
            return;
        }

        int count = icons.length - 1;
        if (usageType() != SINGLE && ICON > count) ICON = 0;

        CachedServerIcon icon = null;

        try {
            icon = Bukkit.loadServerIcon(
                    usageType() == SINGLE ? single : icons[ICON]
            );
        } catch (Exception e) {
            event.setServerIcon(null);
            event.setMotd(IridiumAPI.process(
                    "&cError loading your custom icon \n&7" +
                    e.getLocalizedMessage()
            ));
            main.getRecords().doRecord(
                    "&7Error loading the icon: &c" + e.getLocalizedMessage()
            );
        }

        if (icon == null) {
            event.setServerIcon(null);
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

    @EventHandler(priority = EventPriority.MONITOR)
    private void onServerPing(ServerListPingEvent event) {
        this.event = event;
        if (!main.getMOTD().getBoolean("enabled")) return;

        registerMOTD();
        setServerIcon();
        setMaxPlayers();
    }
}
