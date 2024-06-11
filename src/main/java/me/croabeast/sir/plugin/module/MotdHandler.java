package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.beans.message.StringAligner;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.ResourceUtils;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
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
import java.util.function.UnaryOperator;

public final class MotdHandler extends SIRModule implements CustomListener {

    private static final char SP = File.separatorChar;

    @Getter @Setter
    private boolean registered = false;
    private int motdIndex = 0, iconIndex = 0;

    private final ConfigurableFile config;
    private final ConfigurableFile motds;

    MotdHandler() {
        super("motd");

        config = YAMLData.Module.MOTD.fromName("config");
        motds = YAMLData.Module.MOTD.fromName("motds");

        File folder = SIRPlugin.fileFrom("modules", "motd", "icons");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "server-icon.png");
        if (file.exists()) return;

        String icon = "modules" + SP + "motd" + SP + "icons" + SP + "server-icon.png";
        String s = ("resources" + SP + icon).replace(SP, '/');

        try {
            ResourceUtils.saveResource(
                    SIRPlugin.getInstance().getResource(s),
                    SIRPlugin.getFolder(), icon, false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initServerIcon(ServerListPingEvent event, CachedServerIcon icon) {
        try {
            event.setServerIcon(icon);
        } catch (Exception e) {
            BeansLogger.getLogger().log(
                    "&cError when trying to set the server icon.",
                    "&7Your server doesn't support this feature.",
                    "&cAvoid this error upgrading your server jar!"
            );
            e.printStackTrace();
        }
    }

    enum MaxPlayers {
        MAXIMUM, CUSTOM, DEFAULT
    }

    enum IconInput {
        DISABLED, SINGLE, RANDOM, LIST
    }

    private MaxPlayers getMaxPlayers() {
        String input = config.get("max-players.type", "DEFAULT");

        try {
            return MaxPlayers.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return MaxPlayers.DEFAULT;
        }
    }

    private IconInput getIconInput() {
        String input = config.get("server-icon.usage", "DISABLED");

        try {
            return IconInput.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return IconInput.DISABLED;
        }
    }

    @Override
    public boolean register() {
        try {
            registerOnSIR();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        if (!isEnabled()) return;

        ConfigurationSection motds = this.motds.getSection("motds");
        if (motds != null) {
            List<String> keys = new ArrayList<>(motds.getKeys(false));

            int count = keys.size() - 1;
            if (motdIndex > count) motdIndex = 0;

            Player player = null;

            for (Player tempPlayer : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = tempPlayer.getAddress();

                if (address == null) continue;
                if (address.getAddress() == event.getAddress())
                    player = tempPlayer;
            }

            ConfigurationSection id = motds.getConfigurationSection(keys.get(motdIndex));
            if (id != null) {
                StringBuilder builder = new StringBuilder();
                String two = id.getString("2");

                Player p = player;
                UnaryOperator<String> operator = s -> {
                    String temp = BeansLib.getLib().colorize(p, s);
                    return StringAligner.align(130, temp);
                };

                builder.append(operator.apply(id.getString("1", "")));

                if (StringUtils.isNotBlank(two))
                    builder.append("\n").append(operator.apply(two));

                event.setMotd(builder.toString());
            }
            else event.setMotd(ChatColor.RED + "SIR error: Incorrect MOTD");

            if (config.get("random-motds", false)) {
                motdIndex = new Random().nextInt(count + 1);
                return;
            }

            motdIndex = motdIndex < count ? (motdIndex + 1) : 0;
        }

        ((Consumer<MaxPlayers>) maxInput -> {
            if (maxInput == MaxPlayers.DEFAULT) return;

            int custom = config.get("max-players.count", 0);

            event.setMaxPlayers(
                    maxInput == MaxPlayers.CUSTOM ?
                            custom :
                            Bukkit.getOnlinePlayers().size() + 1
            );
        }).accept(getMaxPlayers());

        ((Consumer<IconInput>) input -> {
            if (input == IconInput.DISABLED) return;

            File folder = SIRPlugin.fileFrom("modules", "motd", "icons");
            File single = new File(folder, config.get("server-icon.image", ""));

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

                BeansLogger.getLogger().log("&7Error loading the icon: &c" + error);
            }

            if (icon == null) {
                initServerIcon(event, null);
                return;
            }

            initServerIcon(event, icon);
            if (isSingle) return;

            switch (input) {
                case LIST:
                    iconIndex = iconIndex < count ? (iconIndex + 1) : 0;
                    break;

                case RANDOM:
                    iconIndex = new Random().nextInt(count + 1);
                    break;

                default: break;
            }
        }).accept(getIconInput());
    }
}
