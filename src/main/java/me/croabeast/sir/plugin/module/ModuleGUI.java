package me.croabeast.sir.plugin.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.character.SmallCaps;
import me.croabeast.beanslib.map.MapBuilder;
import me.croabeast.beanslib.misc.CollectionBuilder;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.api.gui.*;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.file.YAMLCache;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

@UtilityClass
public class ModuleGUI implements CacheManageable {

    final Map<ModuleName, Boolean> MODULE_STATUS_MAP = new LinkedHashMap<>();
    private MenuCreator modulesMenu;

    Map<ModuleName, ButtonCreator> moduleEntries;

    GuiItem getButton(Material material, String name, String... lore) {
        return ItemCreator.of(material).modifyName(name)
                .modifyMeta(m -> {
                    final List<String> list = new LinkedList<>();

                    list.add("");
                    list.addAll(ArrayUtils.toList(lore));
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
                .setAction(b -> e -> {
                    YAMLCache.fromData("modules").set(
                            "modules." + module,
                            MODULE_STATUS_MAP.put(module, b.isEnabled())
                    );
                    YAMLCache.fromData("modules").save();
                })
                .modifyPane(b -> b.setPriority(Pane.Priority.HIGHEST));
    }

    enum Result {
        ALL_ENABLED,
        ALL_DISABLED,
        COMBINED
    }

    Result check() {
        int trueCount = 0, falseCount = 0;
        int total = 0;

        for (Boolean b : MODULE_STATUS_MAP.values()) {
            total++;

            if (b) {
                trueCount++;
                continue;
            }
            falseCount++;
        }

        if (trueCount == total)
            return Result.ALL_ENABLED;

        if (falseCount == total)
            return Result.ALL_DISABLED;

        return Result.COMBINED;
    }

    @Priority(2)
    void loadCache() {
        ConfigurationSection data = YAMLCache.fromData("modules").getSection("modules");

        for (ModuleName name : ModuleName.values()) {
            boolean status = data == null || data.getBoolean(name + "");
            MODULE_STATUS_MAP.put(name, status);
        }

        if (LibUtils.MAIN_VERSION < 14.0) return;
        if (modulesMenu != null) return;

        if (moduleEntries == null) {
            moduleEntries = new MapBuilder<ModuleName, ButtonCreator>()
                    .put(ModuleName.JOIN_QUIT, createButton(
                            3, 1, ModuleName.JOIN_QUIT,
                            "&fJoin & Quit:",
                            "Handles if join and quit messages should",
                            "be custom or not. Can be hooked into a login",
                            "plugin to send login messages, or a vanish",
                            "plugin to send fake join or quit messages."
                    ))
                    .put(ModuleName.ANNOUNCEMENTS, createButton(
                            4, 1, ModuleName.ANNOUNCEMENTS,
                            "&fAnnouncements:",
                            "Handles if custom scheduled and",
                            "automated messages will be displayed",
                            "in a defined time frame in ticks."
                    ))
                    .put(ModuleName.MOTD, createButton(
                            5, 1, ModuleName.MOTD,
                            "&fMOTD:",
                            "Handles the motd of the server, having",
                            "multiple motds in order or random.",
                            "Works with placeholders, RGB and more."
                    ))
                    .put(ModuleName.CHAT_CHANNELS, createButton(
                            6, 1, ModuleName.CHAT_CHANNELS,
                            "&fChat Channels:",
                            "Handles how the chat will display and if will",
                            "be local channels as well with the global",
                            "channel. Local ones only can be accessed",
                            "using a prefix and/or commands."
                    ))
                    .put(ModuleName.DISCORD_HOOK, createButton(
                            7, 1, ModuleName.DISCORD_HOOK,
                            "&fDiscord Hook:",
                            "Handles if DiscordSRV will work with",
                            "SIR to display join-quit messages,",
                            "chat messages, and more."
                    ))
                    .put(ModuleName.ADVANCEMENTS, createButton(
                            3, 2, ModuleName.ADVANCEMENTS,
                            "&fAdvancements:",
                            "Handles if custom advancement messages",
                            "and/or rewards should be enabled.",
                            "Each advancement can be in a different",
                            "category."
                    ))
                    .put(ModuleName.EMOJIS, createButton(
                            4, 2, ModuleName.EMOJIS,
                            "&fEmojis:",
                            "Handles if custom emojis should be",
                            "added in chat and/or other SIR features",
                            "and files."
                    ))
                    .put(ModuleName.MENTIONS, createButton(
                            5, 2, ModuleName.MENTIONS,
                            "&fMentions:",
                            "Handles if players can be mentioned or",
                            "tagged in the chat, similar how Discord",
                            "use their mentions."
                    ))
                    .put(ModuleName.CHAT_FILTERS, createButton(
                            6, 2, ModuleName.CHAT_FILTERS,
                            "&fChat Filters:",
                            "Handles if filters will be applied on",
                            "the chat to avoid insults or spamming."
                    ))
                    .put(ModuleName.CHAT_TAGS, createButton(
                            7, 2, ModuleName.CHAT_TAGS,
                            "&fChat tags:",
                            "Handles if SIR can create custom tags for",
                            "chat. Also, those tags can be parsed through",
                            "PlaceholderAPI in any plugin or any message",
                            "that can be parsed by PlaceholderAPI."
                    ))
                    .toMap();
        }

        MenuCreator menu = MenuCreator.of(4,
                "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:"));

        for (ButtonCreator button : moduleEntries.values())
            menu.addPane(0, button.create());

        String caps = SmallCaps.toSmallCaps("[Paid Version]");

        menu.addSingleItem(0, 1, 1, ItemCreator.of(Material.BARREL)
                .modifyLore(
                        "&7Opens a new menu with all the available",
                        "&7options from each module.",
                        "&eComing soon in SIR+. &8" + caps
                )
                .modifyName("&f&lModules Options:")
                .setAction(e -> e.setCancelled(true))
        );

        final String message = "able all modules available.";

        menu.addPane(0, ButtonCreator.of(1, 2, check() != Result.ALL_ENABLED)
                .setItem(
                        ItemCreator.of(Material.LIME_DYE)
                                .modifyName("&a&lENABLE ALL:")
                                .modifyLore("&f➤ &7En" + message),
                        true
                )
                .setItem(ItemCreator.of(Material.RED_DYE)
                                .modifyName("&c&lDISABLE ALL:")
                                .modifyLore("&f➤ &7Dis" + message),
                        false
                )
                .modifyPane(b -> b.setPriority(Pane.Priority.LOW))
                .setAction(b -> e -> {
                    final boolean is = b.isEnabled();

                    for (ToggleButton button : CollectionBuilder
                            .of(menu.getPanes(0))
                            .filter(p -> p.getPriority() == Pane.Priority.HIGHEST)
                            .map(p -> (ToggleButton) p).toList())
                    {
                        if (is != button.isEnabled()) continue;

                        button.toggle();

                        for (Map.Entry<ModuleName, ButtonCreator> en : moduleEntries.entrySet()) {
                            if (!en.getValue().compare(button))
                                continue;

                            ModuleName name = en.getKey();
                            MODULE_STATUS_MAP.put(name, !is);

                            String n = "modules." + name;

                            YAMLCache.fromData("modules").set(n, !is);
                            YAMLCache.fromData("modules").save();
                            break;
                        }
                    }
                }));

        modulesMenu = menu;
    }

    @Priority(2)
    void saveCache() {
        YAMLFile file = YAMLCache.fromData("modules");

        MODULE_STATUS_MAP.forEach((k, v) -> file.set("modules." + k, v));
        MODULE_STATUS_MAP.clear();

        YAMLCache.fromData("modules").save();
    }

    public void showGUI(Player player) {
        modulesMenu.showGUI(player);
    }
}
