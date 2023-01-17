package me.croabeast.sirplugin.module.listener;

import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.utility.LogUtils;
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

public class MOTD extends SIRViewer {

    private final SIRPlugin main = SIRPlugin.getInstance();

    private ServerListPingEvent event;
    private int MOTD = 0, ICON = 0;

    public MOTD() {
        String path = "misc" + File.separator + "icons";
        File folder = new File(main.getDataFolder(), path);
        if (!folder.exists()) folder.mkdirs();

        File icon = new File(folder, "server-icon.png");
        if (!icon.exists()) main.saveResource(path
                + File.separator + "server-icon.png", false);
    }

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.MOTD;
    }

    @Nullable
    private ConfigurationSection getList() {
        return FileCache.MOTD.getSection("motds");
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
                (IridiumAPI.process(TextUtils.parsePAPI(getPlayerFromIP(),
                id.getString("1", "") + "\n" + id.getString("2", "")))) :
                ("&cError getting the correct motd from SIR.")
        );


        if (!FileCache.MODULES.get().getBoolean("motd.random-motds")) {
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
        return FileCache.MODULES.get().getString("motd.server-icon.usage", "DISABLED").toUpperCase(Locale.ENGLISH);
    }

    private void setServerIcon() {
        if (usageType().equals("DISABLED")) return;

        File folder = new File(main.getDataFolder() + File.separator + "misc" + File.separator + "icons");
        File single = new File(folder, FileCache.MODULES.get().getString("motd.server-icon.image", ""));

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
        return FileCache.MODULES.get().getString("motd.max-players.type", "DEFAULT").toUpperCase(Locale.ENGLISH);
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
                event.setMaxPlayers(FileCache.MODULES.get().getInt("motd.max-players.count"));
                break;

            case "DEFAULT": default:
                break;
        }
    }
}
