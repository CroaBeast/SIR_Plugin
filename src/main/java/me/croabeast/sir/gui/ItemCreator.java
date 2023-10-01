package me.croabeast.sir.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

public final class ItemCreator {

    private final ItemStack item;

    private ItemCreator(Material material) {
        item = new ItemStack(material);
    }

    public ItemCreator modify(Consumer<ItemStack> consumer) {
        consumer.accept(item);
        return this;
    }

    public ItemCreator modifyMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            item.setItemMeta(meta);
        }

        return this;
    }

    public GuiItem create() {
        return new GuiItem(item);
    }

    public static ItemCreator of(Material material) {
        return new ItemCreator(material);
    }
}
