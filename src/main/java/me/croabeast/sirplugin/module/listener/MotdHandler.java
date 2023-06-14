package me.croabeast.sirplugin.module.listener;

import lombok.var;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MotdHandler extends SIRViewer {

    private final SIRPlugin main = SIRPlugin.getInstance();

    private ServerListPingEvent event;
    private int MOTD = 0, ICON = 0;

    public MotdHandler() {
        super("motd");

        String path = "misc" + File.separator + "icons";

        var folder = new File(main.getDataFolder(), path);
        if (!folder.exists()) folder.mkdirs();

        var icon = new File(folder, "server-icon.png");
        if (!icon.exists()) main.saveResource(path
                + File.separator + "server-icon.png", false);
    }

    private ConfigurationSection getList() {
        return FileCache.MOTD_CACHE.getSection("motds");
    }

    private Player fromIP() {
        Player player = null;

        for (var p : Bukkit.getOnlinePlayers()) {
            var address = p.getAddress();

            if (address == null) return null;
            if (address.getAddress() == event.getAddress())
                player = p;
        }

        return player;
    }

    private void registerMOTD() {
        if (getList() == null) return;
        var keys = new ArrayList<>(getList().getKeys(false));

        int count = keys.size() - 1;
        if (MOTD > count) MOTD = 0;

        var id = getList().getConfigurationSection(keys.get(MOTD));
        var s = id != null ?
                id.getString("1", "") + "\n" + id.getString("2", "") :
                "&cError getting the correct motd from SIR.";

        event.setMotd(SIRPlugin.getUtils().colorize(null, fromIP(), s));


        if (!FileCache.MODULES.getValue("motd.random-motds", false)) {
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
        return FileCache.MODULES.getValue("motd.server-icon.usage", "DISABLED").toUpperCase(Locale.ENGLISH);
    }

    private void setServerIcon() {
        if (usageType().equals("DISABLED")) return;

        var folder = new File(main.getDataFolder() + File.separator + "misc" + File.separator + "icons");
        var single = new File(folder, FileCache.MODULES.getValue("motd.server-icon.image", ""));

        var icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
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

            event.setMotd(NeoPrismaticAPI.colorize("&cError loading your custom icon: \n&7" + error));
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

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        this.event = event;
        if (!isEnabled()) return;

        registerMOTD();
        setServerIcon();

        switch (
                FileCache.MODULES.getValue("motd.max-players.type",
                "DEFAULT").toUpperCase(Locale.ENGLISH)
        ) {
            case "MAXIMUM":
                event.setMaxPlayers(Bukkit.getOnlinePlayers().size() + 1);
                break;

            case "CUSTOM":
                event.setMaxPlayers(FileCache.MODULES.getValue("motd.max-players.count", 0));
                break;

            case "DEFAULT": default:
                break;
        }
    }
}
