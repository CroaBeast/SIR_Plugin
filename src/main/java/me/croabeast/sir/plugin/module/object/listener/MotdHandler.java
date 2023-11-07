package me.croabeast.sir.plugin.module.object.listener;

import lombok.var;
import me.croabeast.beanslib.message.CenteredMessage;
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
import org.bukkit.util.Consumer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MotdHandler extends SIRModule implements CustomListener {

    private static final String SP = File.separator;

    private int motdIndex = 0, iconIndex = 0;

    MotdHandler() {
        super(ModuleName.MOTD);

        String path = "modules" + SP + "motd" + SP + "icons";

        File folder = new File(SIRPlugin.getSIRFolder(), path);
        if (!folder.exists()) folder.mkdirs();

        File icon = new File(folder, "server-icon.png");
        if (icon.exists()) return;

        String s = "resources" + SP + path + SP + "server-icon.png";
        s = s.replace('\\', '/');

        try {
            JavaLoader.saveResourceFrom(
                    SIRPlugin.getInstance().getResource(s),
                    SIRPlugin.getSIRFolder(),
                    path + SP + "server-icon.png", false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register() {
        registerOnSIR();
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

    private static MaxPlayers getMaxPlayers() {
        String input = config().getValue("max-players.type", "DEFAULT");

        try {
            return MaxPlayers.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return MaxPlayers.DEFAULT;
        }
    }

    private static IconInput getIconInput() {
        String input = config().getValue("server-icon.usage", "DISABLED");

        try {
            return IconInput.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return IconInput.DISABLED;
        }
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        if (!isEnabled()) return;

        if (motds() != null) {
            List<String> keys = new ArrayList<>(motds().getKeys(false));

            int count = keys.size() - 1;
            if (motdIndex > count) motdIndex = 0;

            Player player = null;

            for (Player tempPlayer : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = tempPlayer.getAddress();

                if (address == null) continue;
                if (address.getAddress() == event.getAddress())
                    player = tempPlayer;
            }

            var id = motds().getConfigurationSection(keys.get(motdIndex));
            if (id == null)
                event.setMotd(ChatColor.RED + "SIR error: Incorrect MOTD");
            else {
                StringBuilder builder = new StringBuilder();
                String two = id.getString("2");

                CenteredMessage center = new CenteredMessage(player);
                center.setLimit(130);

                builder.append(center.center(id.getString("1", "")));

                if (StringUtils.isNotBlank(two))
                    builder.append("\n").append(center.center(two));

                event.setMotd(builder.toString());
            }

            motdIndex = config().getValue("random-motds", false) ?
                    new Random().nextInt(count + 1) :
                    motdIndex < count ? motdIndex + 1 : 0;
        }

        ((Consumer<MaxPlayers>) maxInput -> {
            if (maxInput == MaxPlayers.DEFAULT) return;

            int custom = config().getValue("max-players.count", 0);

            event.setMaxPlayers(
                    maxInput == MaxPlayers.CUSTOM ?
                            custom :
                            Bukkit.getOnlinePlayers().size() + 1
            );
        }).accept(getMaxPlayers());

        ((Consumer<IconInput>) input -> {
            if (input == IconInput.DISABLED) return;

            final String folderName =  "modules" + SP + "motd" + SP + "icons";
            File folder = new File(SIRPlugin.getSIRFolder(), folderName);

            File single = new File(
                    folder,
                    config().getValue("server-icon.image", "")
            );

            File[] icons = folder.listFiles((dir, n) -> n.endsWith(".png"));
            if (icons == null) {
                initServerIcon(event, null);
                return;
            }

            boolean isSingle = input == IconInput.SINGLE;

            int count = icons.length - 1;
            if (!isSingle && iconIndex > count) iconIndex = 0;

            CachedServerIcon icon = null;

            try {
                icon = Bukkit.loadServerIcon(isSingle ? single : icons[iconIndex]);
            }
            catch (Exception e) {
                String error = e.getLocalizedMessage();
                initServerIcon(event, null);

                LogUtils.doLog("&7Error loading the icon: &c" + error);
            }

            if (icon == null) {
                initServerIcon(event, null);
                return;
            }

            initServerIcon(event, icon);
            if (isSingle) return;

            if (input == IconInput.LIST) {
                if (iconIndex < count) iconIndex++;
                else iconIndex = 0;
            }
            else if (input == IconInput.RANDOM)
                iconIndex = new Random().nextInt(count + 1);
        }).accept(getIconInput());
    }

    enum MaxPlayers {
        MAXIMUM, CUSTOM, DEFAULT
    }

    enum IconInput {
        DISABLED, SINGLE, RANDOM, LIST
    }
}
