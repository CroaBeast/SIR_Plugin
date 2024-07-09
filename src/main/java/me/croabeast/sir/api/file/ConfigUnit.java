package me.croabeast.sir.api.file;

import me.croabeast.lib.file.ConfigurableUnit;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Represents a configuration unit used for handling permissions and groups in a configuration section.
 */
public interface ConfigUnit extends ConfigurableUnit {

    /**
     * Checks if the given command sender has the permission associated with this unit.
     * @param sender The command sender.
     * @return True if the sender has the permission, false otherwise.
     */
    default boolean hasPerm(CommandSender sender) {
        return PlayerUtils.hasPerm(sender, getPermission());
    }

    /**
     * Checks if the given command sender is in the group associated with this unit.
     *
     * @param sender The command sender.
     * @return True if the sender is in the group, false otherwise.
     */
    default boolean isInGroup(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return player != null &&
                SIRInitializer.getPermsMeta().playerInGroup(player, getGroup());
    }

    /**
     * Creates a new ConfigUnit instance based on the provided configuration section.
     *
     * @param section The configuration section.
     *
     * @return A new ConfigUnit instance.
     * @throws NullPointerException If the configuration section is null.
     */
    static ConfigUnit of(ConfigurationSection section) {
        return () -> Objects.requireNonNull(section);
    }

    /**
     * Creates a new ConfigUnit instance based on the provided ConfigUnit instance.
     *
     * @param unit The ConfigUnit instance.
     * @return A new ConfigUnit instance.
     */
    static ConfigUnit of(ConfigUnit unit) {
        return of(unit.getSection());
    }
}
