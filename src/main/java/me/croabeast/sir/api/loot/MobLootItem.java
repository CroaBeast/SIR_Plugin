package me.croabeast.sir.api.loot;

import org.bukkit.entity.*;

public interface MobLootItem extends LootItem {

    Cause getCause();

    static boolean isDropException(Entity entity) {
        if (entity == null) return true;

        return entity instanceof Projectile ||
                entity instanceof Boat ||
                entity instanceof ArmorStand ||
                entity instanceof Hanging ||
                entity instanceof EnderCrystal ||
                entity instanceof Minecart;
    }

    enum Cause {
        GENERIC,
        PLAYER,
        CHARGE_CREEPER,
        FIRE,
        SKELETON
    }
}
