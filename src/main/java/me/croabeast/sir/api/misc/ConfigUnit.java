package me.croabeast.sir.api.misc;

import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface ConfigUnit {

    /**
     * Returns the bukkit section object from this config unit.
     *
     * @return the section
     * @throws NullPointerException if section is null
     */
    @NotNull ConfigurationSection getSection() throws NullPointerException;

    /**
     * Returns the name of the config unit defined from its {@link #getSection()} object.
     * @return the channel's name
     */
    @NotNull
    default String getName() {
        return getSection().getName();
    }

    /**
     * Returns the permission needed to use/view this config unit.
     * @return the permission
     */
    @NotNull
    default String getPermission() {
        return getSection().getString("permission", "DEFAULT");
    }

    /**
     * Checks if the sender has the respective permission of this config unit.
     *
     * @param sender a sender
     * @return true if the sender has the permission, false otherwise
     */
    default boolean hasPerm(CommandSender sender) {
        return PlayerUtils.hasPerm(sender, getPermission());
    }

    /**
     * Returns the priority defined of this config unit. This allows a config unit
     * to be in front or below of other loaded config unit.
     *
     * @return the priority
     */
    default int getPriority() {
        int def = getPermission().matches("(?i)DEFAULT") ? 0 : 1;
        return getSection().getInt("priority", def);
    }

    static ConfigUnit of(ConfigurationSection section) {
        return () -> Objects.requireNonNull(section);
    }

    static ConfigUnit of(ConfigUnit unit) {
        return of(unit.getSection());
    }
}
