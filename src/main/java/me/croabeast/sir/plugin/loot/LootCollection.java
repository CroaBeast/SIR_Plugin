package me.croabeast.sir.plugin.loot;

import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public interface LootCollection {

    @NotNull
    List<LootItem> getLoot();

    @NotNull
    default List<ItemStack> getDrops() {
        return getLoot().stream().map(LootItem::getItem).collect(Collectors.toList());
    }

    @Nullable
    default LootItem getItem(String name) {
        if (StringUtils.isBlank(name)) return null;

        for (LootItem item : getLoot())
            if (name.equals(item.getIdentifier())) return item;

        return null;
    }

    boolean addLoot(LootItem... items);

    boolean removeLoot(LootItem item);

    boolean removeAnyLoot(ItemStack item);
}
