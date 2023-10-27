package me.croabeast.sir.plugin.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.gui.ButtonCreator;
import me.croabeast.sir.api.gui.ChestGUICreator;
import me.croabeast.sir.api.gui.ItemCreator;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ModuleGUI implements CacheHandler {

    final Map<ModuleName, Boolean> MODULE_STATUS_MAP = new HashMap<>();
    private ChestGui modulesGUI = null;

    @Priority(level = 2)
    void loadCache() {
        ConfigurationSection data = FileCache.MODULES_DATA.getSection("modules");
        if (data == null) return;

        for (ModuleName name : ModuleName.values()) {
            boolean status = data.getBoolean(name + "");
            MODULE_STATUS_MAP.put(name, status);
        }

        getModulesGUI();
    }

    @Priority(level = 2)
    void saveCache() {
        MODULE_STATUS_MAP.forEach((k, v) -> FileCache.MODULES_DATA.setValue("modules." + k, v));
        MODULE_STATUS_MAP.clear();
    }

    GuiItem getButton(Material material, String name, String... lore) {
        final String text = NeoPrismaticAPI.colorize(name);

        return ItemCreator.of(material)
                .modifyMeta(m -> m.setDisplayName(text))
                .modifyMeta(m -> {
                    List<String> list = new LinkedList<>();

                    list.add("");
                    list.addAll(ArrayUtils.fromArray(lore));
                    list.add("");

                    list.replaceAll(s -> "&7" + s);
                    list.replaceAll(NeoPrismaticAPI::colorize);

                    m.setLore(list);
                })
                .create();
    }

    ButtonCreator createButton(int x, int y, ModuleName module, String name, String... lore) {
        return ButtonCreator.of(x, y, MODULE_STATUS_MAP.getOrDefault(module, false))
                .setItem(getButton(
                        Material.LIME_STAINED_GLASS_PANE,
                        "&7• " + name + " &a&l✔", lore), true
                )
                .setItem(getButton(
                        Material.RED_STAINED_GLASS_PANE,
                        "&7• " + name + " &c&l❌", lore), false
                )
                .onClick(b -> e ->
                        FileCache.MODULES_DATA.setValue(
                                "modules." + module,
                                MODULE_STATUS_MAP.put(module, b.isEnabled())
                        )
                );
    }
    
    public ChestGui getModulesGUI() {
        return modulesGUI != null ?
                modulesGUI :
                (modulesGUI = ChestGUICreator.of(5, "&8Loaded SIR Modules:")
                        .addPane(createButton(
                                1, 1, ModuleName.JOIN_QUIT,
                                "&fJoin & Quit:",
                                "Handles if join and quit messages should",
                                "be custom or not.",
                                "Can be hooked into a login plugin to send",
                                "login messages, or a vanish plugin to",
                                "send fake join or quit messages."
                        ))
                        .addPane(createButton(
                                2, 1, ModuleName.ANNOUNCEMENTS,
                                "&fAnnouncements:",
                                "Handles if custom scheduled and",
                                "automated messages will be displayed",
                                "in a defined time frame in ticks.",
                                "Remember: &e20 server ticks = 1 second"
                        ))
                        .addPane(createButton(
                                3, 1, ModuleName.MOTD,
                                "&fMOTD:",
                                "Handles the motd of the server, having",
                                "multiple motds in order or random.",
                                "Works with placeholders, RGB and more."
                        ))
                        .addPane(createButton(
                                4, 1, ModuleName.CHAT_CHANNELS,
                                "&fChat Channels:",
                                "Handles how the chat will display and",
                                "if will be local channels as well with",
                                "the global channel.",
                                "Local channels works with commands and",
                                "with prefix to access to them."
                        ))
                        .addPane(createButton(
                                5, 1, ModuleName.DISCORD_HOOK,
                                "&fDiscord Hook:",
                                "Handles if DiscordSRV will work with",
                                "SIR to display join-quit messages,",
                                "chat messages, and more."
                        ))
                        .addPane(createButton(
                                6, 1, ModuleName.ADVANCEMENTS,
                                "&fAdvancements:",
                                "Handles if custom advancement messages",
                                "and/or rewards should be enabled.",
                                "Each advancement can be in a different",
                                "category."
                        ))
                        .addPane(createButton(
                                7, 1, ModuleName.EMOJIS,
                                "&fEmojis:",
                                "Handles if custom emojis should be",
                                "added in chat and/or other SIR features",
                                "and files."
                        ))
                        .addPane(createButton(
                                3, 2, ModuleName.CHAT_FILTERS,
                                "&fChat Filters:",
                                "Handles if filters will be applied on",
                                "the chat to avoid insults or spamming."
                        ))
                        .addPane(createButton(
                                4, 2, ModuleName.MENTIONS,
                                "&fMentions:",
                                "Handles if players can be mentioned or",
                                "tagged in the chat, similar how Discord",
                                "use their mentions."
                        ))
                        .create());
    }
}
