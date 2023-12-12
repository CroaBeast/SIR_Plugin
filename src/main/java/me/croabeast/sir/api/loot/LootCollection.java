package me.croabeast.sir.api.loot;

import me.croabeast.beanslib.misc.CollectionBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LootCollection {

    @NotNull
    List<LootItem> getLoot();

    @NotNull
    default List<ItemStack> getDrops() {
        return CollectionBuilder.of(getLoot()).map(LootItem::getItem).toList();
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
