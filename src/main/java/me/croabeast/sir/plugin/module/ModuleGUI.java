package me.croabeast.sir.plugin.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ModuleGUI implements CacheHandler {

    static final Map<ModuleName<?>, Boolean> MODULE_STATUS_MAP = new HashMap<>();
    private static ChestGui modulesGUI = null;

    @Priority(level = 2)
    static void loadCache() {
        ConfigurationSection data = FileCache.MODULES_DATA.getSection("modules");
        if (data == null) return;

        for (ModuleName<?> name : ModuleName.values()) {
            boolean status = data.getBoolean(name + "");
            MODULE_STATUS_MAP.put(name, status);
        }

        getModulesGUI();
    }

    static void saveCache() {
        YAMLFile file = FileCache.MODULES_DATA.getFile();
        FileConfiguration data = FileCache.MODULES_DATA.get();

        if (data == null || file == null) return;

        MODULE_STATUS_MAP.forEach((k, v) -> data.set("modules." + k, v));
        MODULE_STATUS_MAP.clear();

        file.save();
    }
    
    public static ChestGui getModulesGUI() {
        if (modulesGUI != null) return modulesGUI;

        ChestGui gui = new ChestGui(5, "Loaded SIR Modules");

        gui.addPane(createButton(
                1, 1, ModuleName.JOIN_QUIT,
                "&fJoin & Quit:",
                "&7Handles if join and quit messages should",
                "&7be custom or not.",
                "&7Can be hooked into a login plugin to send",
                "&7login messages, or a vanish plugin to",
                "&7send fake join or quit messages."
        ));
        gui.addPane(createButton(
                2, 1, ModuleName.ANNOUNCEMENTS,
                "&fAnnouncements:",
                "&7Handles if custom scheduled and",
                "&7automated messages will be displayed",
                "&7in a defined time frame in ticks.",
                "&7Remember: &e20 server ticks = 1 second"
        ));
        gui.addPane(createButton(
                3, 1, ModuleName.MOTD,
                "&fMOTD:",
                "&7Handles the motd of the server, having",
                "&7multiple motds in order or random.",
                "&7Works with placeholders, RGB and more."
        ));
        gui.addPane(createButton(
                4, 1, ModuleName.CHAT_CHANNELS,
                "&fChat Channels:",
                "&7Handles how the chat will display and",
                "&7if will be local channels as well with",
                "&7the global channel.",
                "&7Local channels works with commands and",
                "&7with prefix to access to them."
        ));
        gui.addPane(createButton(
                5, 1, ModuleName.DISCORD_HOOK,
                "&fDiscord Hook:",
                "&7Handles if DiscordSRV will work with",
                "&7SIR to display join-quit messages,",
                "&7chat messages, and more."
        ));
        gui.addPane(createButton(
                6, 1, ModuleName.ADVANCEMENTS,
                "&fAdvancements:",
                "&7Handles if custom advancement messages",
                "&7and/or rewards should be enabled.",
                "&7Each advancement can be in a different",
                "&7category."
        ));
        gui.addPane(createButton(
                7, 1, ModuleName.EMOJIS,
                "&fEmojis:",
                "&7Handles if custom emojis should be",
                "&7added in chat and/or other SIR features",
                "&7and files."
        ));

        gui.addPane(createButton(
                3, 2, ModuleName.CHAT_FILTERS,
                "&fChat Filters:",
                "&7Handles if filters will be applied on",
                "&7the chat to avoid insults or spamming."
        ));
        gui.addPane(createButton(
                4, 2, ModuleName.MENTIONS,
                "&fMentions:", ""
        ));

        return modulesGUI = gui;
    }

    static GuiItem getButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(NeoPrismaticAPI.colorize(name));
            meta.setLore(ArrayUtils.fromArray(NeoPrismaticAPI::colorize, lore));
        }

        item.setItemMeta(meta);
        return new GuiItem(item);
    }

    static ToggleButton createButton(int x, int y, ModuleName<?> module, String name, String... lore) {
        boolean def = MODULE_STATUS_MAP.getOrDefault(module, false);
        ToggleButton button = new ToggleButton(x, y, 1, 1, def);

        Material lime = Material.LIME_STAINED_GLASS_PANE,
                red = Material.RED_STAINED_GLASS_PANE;

        button.setEnabledItem(getButton(lime, name + " &a&lENABLED", lore));
        button.setDisabledItem(getButton(red, name + " &c&lDISABLED", lore));

        button.setOnClick(e -> 
                MODULE_STATUS_MAP.put(module, button.isEnabled()));
        return button;
    }
}
