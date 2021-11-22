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

public class MOTDListener implements Listener {

    private final Application main;
    private ServerListPingEvent event;

    private int MOTD = 0;
    private int ICON = 0;

    public MOTDListener(Application main) {
        this.main = main;
        main.registerListener(this);
        registerIconsFolder();
        main.getInitializer().LISTENERS++;
    }

    private void registerIconsFolder() {
        File folder = new File(main.getDataFolder(), "icons");
        if (!folder.exists() && folder.mkdirs())
            main.getRecords().doRecord("&eGenerating the 'icons' folder...");

        File icon = new File(folder, "server-icon.png");
        if (!icon.exists()) {
            String path = "icons" + File.separator + "server-icon.png";
            main.getRecords().doRecord(
                    "&eGenerating the default server icon...",
                    "&7If you don't want to generate this file,",
                    "&7just name a file/icon:&e 'server-icon.png'"
            );
            main.saveResource(path, false);
        }
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

    private enum UsageType {
        DISABLED,
        LIST,
        SINGLE,
        RANDOM
    }

    private String getUsageType() {
        return main.getMOTD().getString("server-icon.usage", "DISABLED");
    }

    private UsageType usageType() {
        UsageType type;
        switch (getUsageType().toUpperCase()) {
            case "SINGLE":
                type = UsageType.SINGLE;
                break;
            case "LIST":
                type = UsageType.LIST;
                break;
            case "RANDOM":
                type = UsageType.RANDOM;
                break;
            case "DISABLED": default:
                type = UsageType.DISABLED;
                break;
        }
        return type;
    }

    private void setServerIcon() {
        if (usageType() == UsageType.DISABLED) {
            event.setServerIcon(null);
            return;
        }

        File folder = new File(main.getDataFolder() + File.separator + "icons");
        File single = new File(folder, main.getMOTD().getString("server-icon.image", ""));

        File[] icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (icons == null) {
            event.setServerIcon(null);
            return;
        }

        int count = icons.length - 1;
        if (usageType() != UsageType.SINGLE && ICON > count) ICON = 0;

        CachedServerIcon icon = null;

        try {
            icon = Bukkit.loadServerIcon(
                    usageType() == UsageType.SINGLE ? single : icons[ICON]
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

        if (usageType() == UsageType.SINGLE) return;

        if (usageType() == UsageType.LIST) {
            if (ICON < count) ICON++;
            else ICON = 0;
        }
        else if (usageType() == UsageType.RANDOM)
            ICON = new Random().nextInt(count + 1);
    }

    private enum MaxType {
        DEFAULT,
        CUSTOM,
        MAXIMUM
    }

    private String getMaxType() {
        return main.getMOTD().getString("max-players.type", "DEFAULT");
    }

    private MaxType maxType() {
        MaxType type;
        switch (getMaxType().toUpperCase()) {
            case "CUSTOM":
                type = MaxType.CUSTOM;
                break;
            case "MAXIMUM":
                type = MaxType.MAXIMUM;
                break;
            case "DEFAULT": default:
                type = MaxType.DEFAULT;
                break;
        }
        return type;
    }

    private void setMaxPlayers() {
        if (maxType() == MaxType.DEFAULT) return;

        if (maxType() == MaxType.CUSTOM)
            event.setMaxPlayers(main.getMOTD().getInt("max-players.count"));

        else if (maxType() == MaxType.MAXIMUM)
            event.setMaxPlayers(main.everyPlayer().size() + 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onServerPing(ServerListPingEvent event) {
        this.event = event;
        if (!main.getMOTD().getBoolean("enabled")) return;

        registerMOTD();
        setMaxPlayers();
        setServerIcon();
    }
}
