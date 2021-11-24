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
        if (sender instanceof ConsoleCommandSender) return true;
        Player player = (Player) sender;
        return  main.getInitializer().HAS_VAULT ?
                Initializer.Perms.playerHas(null, player, perm) :
                player.hasPermission(perm);
    }

    boolean certainPerm(Player player, String perm) {
        return !perm.matches("(?i)DEFAULT") && hasPerm(player, perm);
    }

    private boolean essVanish(Player player, boolean join) {
        Essentials ess = (Essentials) main.getPlugin("Essentials");
        if (ess == null) return false;

        boolean isJoin = join && certainPerm(player, "essentials.silentjoin.vanish");
        return ess.getUser(player).isVanished() || isJoin;
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

    public boolean isVanished(Player p, boolean join) {
        return essVanish(p, join) || cmiVanish(p) || normalVanish(p);
    }
}
