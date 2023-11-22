package me.croabeast.sir.api.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ItemCreator {

    private Consumer<InventoryClickEvent> consumer;
    private final ItemStack item;

    private ItemCreator(Material material) {
        item = new ItemStack(Objects.requireNonNull(material));
    }

    private ItemCreator(ItemStack stack) {
        item = Objects.requireNonNull(stack);
    }

    public ItemCreator modifyItem(Consumer<ItemStack> consumer) {
        Objects.requireNonNull(consumer).accept(item);
        return this;
    }

    public ItemCreator setAction(Consumer<InventoryClickEvent> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
        return this;
    }

    public ItemCreator modifyMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Objects.requireNonNull(consumer).accept(meta);
            item.setItemMeta(meta);
        }

        return this;
    }

    public ItemCreator modifyName(String name) {
        return modifyMeta(m -> m.setDisplayName(NeoPrismaticAPI.colorize(name)));
    }

    public ItemCreator modifyLore(List<String> lore) {
        lore.replaceAll(NeoPrismaticAPI::colorize);
        return modifyMeta(m -> m.setLore(lore));
    }

    public ItemCreator modifyLore(String... lore) {
        return modifyLore(ArrayUtils.fromArray(lore));
    }

    public GuiItem create() {
        GuiItem item = new GuiItem(this.item);

        if (consumer != null)
            item.setAction(consumer);

        return item;
    }

    public static ItemCreator of(Material material) {
        return new ItemCreator(material);
    }
}
