package me.croabeast.sir.plugin.module.object.listener;

import lombok.var;
import me.croabeast.beanslib.message.CenteredMessage;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.api.misc.JavaLoader;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MotdHandler extends SIRModule implements CustomListener {

    private static final String SP = File.separator;

    private int motdIndex = 0, ICON = 0;

    MotdHandler() {
        super(ModuleName.MOTD);

        String path = "modules" + SP + "motd" + SP + "icons";

        File folder = new File(SIRPlugin.getSIRFolder(), path);
        if (!folder.exists()) folder.mkdirs();

        File icon = new File(folder, "server-icon.png");
        if (icon.exists()) return;

        String s = "resources" + SP + path + SP + "server-icon.png";

        try {
            JavaLoader.saveResourceFrom(
                    SIRPlugin.getInstance().getResource(s.replace('\\', '/')),
                    SIRPlugin.getSIRFolder(),
                    path + SP + "server-icon.png", false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean registered = false;

    @Override
    public void register() {
        if (registered) return;

        CustomListener.super.register();
        registered = true;
    }

    private static ConfigurationSection motds() {
        return FileCache.MOTD_CACHE.getCache("motds").getSection("motds");
    }

    private void initServerIcon(ServerListPingEvent event, CachedServerIcon icon) {
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

    private static FileCache config() {
        return FileCache.MOTD_CACHE.getConfig();
    }

    private static String usageType() {
        return config()
                .getValue("server-icon.usage", "DISABLED")
                .toUpperCase(Locale.ENGLISH);
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        if (!isEnabled()) return;

        if (motds() != null) {
            List<String> keys = new ArrayList<>(motds().getKeys(false));

            int count = keys.size() - 1;
            if (motdIndex > count) motdIndex = 0;

            Player player = null;

            for (Player p : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = p.getAddress();

                if (address == null) continue;
                if (address.getAddress() == event.getAddress())
                    player = p;
            }

            var id = motds().getConfigurationSection(keys.get(motdIndex));
            if (id == null)
                event.setMotd(ChatColor.RED + "SIR error: Incorrect MOTD");
            else {
                StringBuilder builder = new StringBuilder();
                String two = id.getString("2");

                CenteredMessage center = new CenteredMessage(player).setLimit(130);

                builder.append(center.center(id.getString("1", "")));
                if (StringUtils.isNotBlank(two))
                    builder.append("\n").append(center.center(two));

                event.setMotd(builder.toString());
            }

            motdIndex = config().getValue("random-motds", false) ?
                    new Random().nextInt(count + 1) :
                    motdIndex < count ? motdIndex + 1 : 0;
        }

        if (!usageType().equals("DISABLED")) {
            File folder = new File(SIRPlugin.getSIRFolder(), "modules" + SP + "motd" + SP + "icons");
            File single = new File(folder, config().getValue("server-icon.image", ""));

            File[] icons = folder.listFiles((dir, name) -> name.endsWith(".png"));
            if (icons == null) {
                initServerIcon(event, null);
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
                initServerIcon(event, null);

                event.setMotd(NeoPrismaticAPI.colorize("&cError loading your custom icon: \n&7" + error));
                LogUtils.doLog("&7Error loading the icon: &c" + error);
            }

            if (icon == null) {
                initServerIcon(event, null);
                return;
            }

            initServerIcon(event, icon);
            if (usageType().equals("SINGLE")) return;

            if (usageType().equals("LIST")) {
                if (ICON < count) ICON++;
                else ICON = 0;
            }
            else if (usageType().equals("RANDOM"))
                ICON = new Random().nextInt(count + 1);
        }

        String type = config()
                .getValue("max-players.type", "DEFAULT")
                .toUpperCase(Locale.ENGLISH);

        if (!type.matches("(?i)MAXIMUM|CUSTOM")) return;

        event.setMaxPlayers(type.matches("(?i)CUSTOM") ?
                config().getValue("max-players.count", 0) :
                Bukkit.getOnlinePlayers().size() + 1
        );
    }
}
