package me.croabeast.sir.plugin.loot;

import org.bukkit.inventory.ItemStack;

public interface LootItem extends Cloneable {

    String getIdentifier();

    ItemStack getItem();

    boolean isMultiplicative();

    default double getChance() {
        return 1.00;
    }

    LootItem clone();
}
