package me.croabeast.sircore.utilities;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sircore.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;

public class PermUtils {

    private final Application main;

    public PermUtils(Application main) {
        this.main = main;
    }

    public boolean hasPerm(CommandSender sender, String perm) {
        boolean isSet = main.getConfig().getBoolean("options.hard-perm-check");
        isSet = (!isSet || sender.isPermissionSet(perm)) && sender.hasPermission(perm);
        return (sender instanceof ConsoleCommandSender) || isSet;
    }

    public boolean certainPerm(Player player, String perm) {
        return perm != null && !perm.matches("(?i)DEFAULT") && hasPerm(player, perm);
    }

    private boolean essVanish(Player player, boolean isJoin) {
        Essentials ess = (Essentials) main.getPlugin("Essentials");
        if (!main.getInitializer().essentials || ess == null) return false;
        return ess.getUser(player).isVanished() ||
                (isJoin && hasPerm(player, "essentials.silentjoin.vanish"));
    }

    private boolean cmiVanish(Player player) {
        if (!main.getInitializer().hasCMI) return false;
        return CMIUser.getUser(player).isVanished();
    }

    private boolean normalVanish(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;
        return false;
    }

    public boolean isVanished(Player p, boolean isJoin) {
        return essVanish(p, isJoin) || cmiVanish(p) || normalVanish(p);
    }
}
