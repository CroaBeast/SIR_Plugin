package me.croabeast.sirplugin.utilities;

import com.Zrips.CMI.Containers.*;
import com.earth2me.essentials.*;
import me.croabeast.sirplugin.objects.files.FileCache;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.*;


public final class PlayerUtils {

    public static boolean hasPerm(CommandSender sender, String perm) {
        boolean isSet = FileCache.CONFIG.get().getBoolean("options.hard-perm-check");
        isSet = (!isSet || sender.isPermissionSet(perm)) && sender.hasPermission(perm);
        return (sender instanceof ConsoleCommandSender) || isSet;
    }

    private static boolean essVanish(Player player, boolean isJoin) {
        Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        return ess != null && (ess.getUser(player).isVanished() ||
                (isJoin && hasPerm(player, "essentials.silentjoin.vanish")));
    }

    private static boolean cmiVanish(Player player) {
        return Bukkit.getPluginManager().getPlugin("CMI") != null && CMIUser.getUser(player).isVanished();
    }

    private static boolean normalVanish(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;
        return false;
    }

    public static boolean isVanished(Player p, boolean isJoin) {
        return essVanish(p, isJoin) || cmiVanish(p) || normalVanish(p);
    }

    public static Player getClosestPlayer(String input) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getName().matches("(?i)" + input)) continue;
            return p;
        }
        return null;
    }
}
