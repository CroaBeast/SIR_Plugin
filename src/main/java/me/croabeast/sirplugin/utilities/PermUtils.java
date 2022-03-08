package me.croabeast.sirplugin.utilities;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;

public final class PermUtils {

    private final static SIRPlugin main = SIRPlugin.getInstance();

    public static boolean hasPerm(CommandSender sender, String perm) {
        boolean isSet = main.getConfig().getBoolean("options.hard-perm-check");
        isSet = (!isSet || sender.isPermissionSet(perm)) && sender.hasPermission(perm);
        return (sender instanceof ConsoleCommandSender) || isSet;
    }

    private static boolean essVanish(Player player, boolean isJoin) {
        Essentials ess = (Essentials)
                Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess == null) return false;
        return ess.getUser(player).isVanished() ||
                (isJoin && hasPerm(player, "essentials.silentjoin.vanish"));
    }

    private static boolean cmiVanish(Player player) {
        if (Bukkit.getPluginManager().getPlugin("CMI") == null) return false;
        return CMIUser.getUser(player).isVanished();
    }

    private static boolean normalVanish(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;
        return false;
    }

    public static boolean isVanished(Player p, boolean isJoin) {
        return essVanish(p, isJoin) || cmiVanish(p) || normalVanish(p);
    }
}
