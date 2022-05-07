package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.util.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static me.croabeast.sirplugin.objects.FileCatcher.*;

public class ServerList extends Module implements Listener {

    private final SIRPlugin main;

    private ServerListPingEvent event;

    private int MOTD = 0, ICON = 0;

    public ServerList(SIRPlugin main) {
        this.main = main;

        String path = "misc" + File.separator + "icons";
        File folder = new File(main.getDataFolder(), path);
        if (!folder.exists()) folder.mkdirs();

        File icon = new File(folder, "server-icon.png");
        if (!icon.exists()) main.saveResource(path
                + File.separator + "server-icon.png", false);
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.MOTD;
    }

    @Override
    public void registerModule() {
        SIRPlugin.registerListener(this);
    }

    @Nullable
    private ConfigurationSection getList() {
        return FileCatcher.MOTD.toFile().getConfigurationSection("motds");
    }

    private Player getPlayerFromIP() {
        Player player = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            InetSocketAddress address = p.getAddress();
            if (address == null) return null;
            if (address.getAddress() == event.getAddress())
                player = p;
        }
        return player;
    }

    private void registerMOTD() {
        if (getList() == null) return;
        List<String> keys = new ArrayList<>(getList().getKeys(false));

        int count = keys.size() - 1;
        if (MOTD > count) MOTD = 0;

        ConfigurationSection id = getList().getConfigurationSection(keys.get(MOTD));

        event.setMotd(id != null ?
                (IridiumAPI.process(TextUtils.parsePAPI(getPlayerFromIP(), "" +
                id.getString("1", "") + "\n" + id.getString("2", "")))) :
                ("&cError getting the correct motd from SIR.")
        );


        if (!MODULES.toFile().getBoolean("motd.random-motds")) {
            if (MOTD < count) MOTD++;
            else MOTD = 0;
        }
        else MOTD = new Random().nextInt(count + 1);
    }

    private void initServerIcon(CachedServerIcon icon) {
        try {
            event.setServerIcon(icon);
        } catch (Exception e) {
            LogUtils.doLog("" +
                    "&cError when trying to set the server icon.",
                    "&7Your server doesn't support this feature.",
                    "&cAvoid this error upgrading to Spigot!"
            );
            e.printStackTrace();
        }
    }

    private String usageType() {
        return MODULES.toFile().getString("motd.server-icon.usage", "DISABLED").toUpperCase();
    }

    private void setServerIcon() {
        if (usageType().equals("DISABLED")) return;

        File folder = new File(main.getDataFolder() + File.separator + "misc" + File.separator + "icons");
        File single = new File(folder, MODULES.toFile().getString("motd.server-icon.image", ""));

        File[] icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
        if (icons == null) {
            initServerIcon(null);
            return;
        }

        int count = icons.length - 1;
        if (!usageType().equals("SINGLE") && ICON > count) ICON = 0;

        CachedServerIcon icon = null;

        try {
            icon = Bukkit.loadServerIcon(usageType().equals("SINGLE") ? single : icons[ICON]);
        }
        catch (Exception e) {
            String error = e.getLocalizedMessage();
            initServerIcon(null);
            event.setMotd(IridiumAPI.process("&cError loading your custom icon: \n&7" + error));
            LogUtils.doLog("&7Error loading the icon: &c" + error);
        }

        if (icon == null) {
            initServerIcon(null);
            return;
        }

        initServerIcon(icon);
        if (usageType().equals("SINGLE")) return;

        if (usageType().equals("LIST")) {
            if (ICON < count) ICON++;
            else ICON = 0;
        }
        else if (usageType().equals("RANDOM"))
            ICON = new Random().nextInt(count + 1);
    }

    private String maxType() {
        return MODULES.toFile().getString("motd.max-players.type", "DEFAULT").toUpperCase();
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        this.event = event;
        if (!isEnabled()) return;

        registerMOTD();
        setServerIcon();

        switch (maxType()) {
            case "MAXIMUM":
                event.setMaxPlayers(Bukkit.getOnlinePlayers().size() + 1);
                break;

            case "CUSTOM":
                event.setMaxPlayers(MODULES.toFile().getInt("motd.max-players.count"));
                break;

            case "DEFAULT": default:
                break;
        }
    }
}
