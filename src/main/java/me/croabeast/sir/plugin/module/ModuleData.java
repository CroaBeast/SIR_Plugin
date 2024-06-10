package me.croabeast.sir.plugin.module;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import lombok.experimental.UtilityClass;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.character.SmallCaps;
import me.croabeast.lib.map.Entry;
import me.croabeast.lib.map.MapBuilder;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.api.gui.ButtonCreator;
import me.croabeast.sir.api.gui.ItemCreator;
import me.croabeast.sir.api.gui.MenuCreator;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.file.YAMLData;
import org.bukkit.Material;

import java.lang.reflect.Modifier;
import java.util.*;

@UtilityClass
class ModuleData implements DataHandler {

    final Map<String, SIRModule> MODULE_MAP = new LinkedHashMap<>();
    final Map<String, Boolean> STATUS_MAP = new LinkedHashMap<>();

    final MapBuilder<String, ButtonCreator> ENTRIES = new MapBuilder<>();

    private boolean modulesLoaded = false, entriesLoaded = false;
    final MenuCreator MODULES_MENU = MenuCreator.of(5,
            "&8" + SmallCaps.toSmallCaps("Loaded SIR Modules:"));

    GuiItem createMainButton(Material material, String name, String... lore) {
        return ItemCreator.of(material).modifyName(name)
                .modifyMeta(m -> {
                    final List<String> list = new LinkedList<>();

                    list.add("");
                    list.addAll(ArrayUtils.toList(lore));
                    list.add("");

                    list.replaceAll(s -> {
                        s = "&7" + SmallCaps.toSmallCaps(s);
                        return NeoPrismaticAPI.colorize(s);
                    });

                    m.setLore(list);
                })
                .create();
    }

    void putModuleButton(String name, int x, int y, String title, String... lore) {
        title = "&f" + SmallCaps.toSmallCaps(title);

        ENTRIES.put(name,
                ButtonCreator.of(x, y, STATUS_MAP.getOrDefault(name, false))
                        .setItem(createMainButton(
                                Material.LIME_STAINED_GLASS_PANE,
                                "&7• " + title + " &a&l✔", lore), true
                        )
                        .setItem(createMainButton(
                                Material.RED_STAINED_GLASS_PANE,
                                "&7• " + title + " &c&l❌", lore), false
                        )
                        .setAction(b -> e -> {
                            YAMLData.Module.getMain().set(
                                    "modules." + name,
                                    STATUS_MAP.put(name, b.isEnabled())
                            );
                            YAMLData.Module.getMain().save();
                        })
                        .modifyPane(b -> b.setPriority(Pane.Priority.HIGHEST))
        );
    }

    boolean menuLoaded = false;

    @Priority(2)
    void loadData() {
        if (!modulesLoaded) {
            Counter loaded = new Counter(), failed = new Counter();
            final Counter total = new Counter();

            SIRCollector.from("me.croabeast.sir.plugin.module")
                    .filter(SIRModule.class::isAssignableFrom)
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect().forEach(c -> {
                        SIRModule module = null;
                        try {
                            module = Reflector.of(c).create();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        (module != null ? loaded : failed).add();
                        total.add();
                    });

            BeansLogger.getLogger().log("Loading modules...",
                    "Total: " + total.get() +
                            " [Loaded= " + loaded.get() +
                            ", Failed= " + failed.get() + "]"
            );

            if (loaded.get() < 1 || failed.get() > 0)
                BeansLogger.doLog(
                        "&cSome modules were not loaded correctly.",
                        "&cReport it to CreaBeast ASAP!"
                );

            modulesLoaded = true;
        }

        MODULE_MAP.values().forEach(SIRModule::register);

        if (ServerInfoUtils.SERVER_VERSION < 14.0 || menuLoaded)
            return;

        if (!entriesLoaded) {
            putModuleButton("join-quit", 3, 1,
                    "Join & Quit:",
                    "Handles if custom join and quit messages are",
                    "enabled. Works with multiple messages types",
                    "like chat, title, action bar, bossbar, json, etc."
            );
            putModuleButton("motd", 4, 1,
                    "MOTD:",
                    "Handles the motd of the server, having multiple",
                    "motds in order or random."
            );
            putModuleButton("announcements", 5, 1,
                    "Announcements:",
                    "Handles if custom scheduled and automated messages",
                    "will be displayed in a defined time frame in ticks."
            );
            putModuleButton("advancements", 6, 1,
                    "Advancements:",
                    "Handles if custom advancement messages and/or",
                    "rewards should be enabled.",
                    "Each advancement can be in a different category."
            );
            putModuleButton("chat.channels", 7, 1,
                    "Chat Channels:",
                    "Handles how the chat will display and if will be a",
                    "local channel as well with the global channel.",
                    "Local channels only can be accessed using a prefix",
                    "and/or command."
            );
            putModuleButton("chat.filters", 3, 2,
                    "Chat Filters:",
                    "Handles if filters will be applied on the chat",
                    "to avoid insults or spamming."
            );
            putModuleButton("chat.cooldowns", 4, 2,
                    "Cooldowns:",
                    "Handles if a cooldown will be applied in each chat",
                    "message to avoid spamming. Commands can be executed",
                    "if the player keeps spamming messages for a custom",
                    "time depending on its permission."
            );
            putModuleButton("chat.emojis", 5, 2,
                    "Emojis:",
                    "Handles if custom emojis should be added in chat",
                    "and/or other SIR features and files."
            );
            putModuleButton("chat.tags", 6, 2,
                    "Chat tags:",
                    "Handles if SIR can create custom tags for chat.",
                    "Tags can be parsed in any plugin or message managed",
                    "by PlaceholderAPI."
            );
            putModuleButton("chat.mentions", 7, 2,
                    "Mentions:",
                    "Handles if players can be mentioned or tagged in",
                    "the chat, similar how Discord use their mentions."
            );
            putModuleButton("hook.discord", 3, 3,
                    "Discord Hook:",
                    "Handles if DiscordSRV will work with SIR to display",
                    "join-quit messages, chat messages, and more."
            );
            putModuleButton("hook.login", 4, 3,
                    "Login Hook:",
                    "Handles if DiscordSRV will work with SIR to display",
                    "join-quit messages, chat messages, and more."
            );
            putModuleButton("hook.vanish", 5, 3,
                    "Vanish Hook:",
                    "Handles if DiscordSRV will work with SIR to display",
                    "join-quit messages, chat messages, and more."
            );

            entriesLoaded = true;
        }

        for (ButtonCreator button : ENTRIES.values())
            MODULES_MENU.addPane(0, button.create());

        String caps = SmallCaps.toSmallCaps("[Paid Version]");

        MODULES_MENU.addSingleItem(0, 1, 1, ItemCreator.of(Material.BARREL)
                .modifyLore(
                        "&7Opens a new menu with all the available",
                        "&7options from each module.",
                        "&eComing soon in SIR+. &8" + caps
                )
                .modifyName("&f&lModules Options:")
                .setAction(e -> e.setCancelled(true)),
                b -> b.setPriority(Pane.Priority.LOW)
        );

        MODULES_MENU.addSingleItem(0, 6, 3, ItemCreator.of(Material.BARRIER)
                .modifyName("&c&lCOMING SOON...")
                .modifyLore("&8More modules will be added soon.")
                .setAction(e -> e.setCancelled(true)),
                b -> b.setPriority(Pane.Priority.LOW)
        );

        final String message = "able all available modules.";

        MODULES_MENU.addPane(0, ButtonCreator.of(1, 2, false)
                .setItem(
                        ItemCreator.of(Material.LIME_DYE)
                                .modifyLore("&f➤ &7En" + message)
                                .modifyName("&a&lENABLE ALL:"),
                        true
                )
                .setItem(ItemCreator.of(Material.RED_DYE)
                                .modifyLore("&f➤ &7Dis" + message)
                                .modifyName("&c&lDISABLE ALL:"),
                        false
                )
                .modifyPane(b -> b.setPriority(Pane.Priority.LOW))
                .setAction(b -> e -> {
                    final boolean is = b.isEnabled();

                    for (ToggleButton button : CollectionBuilder
                            .of(MODULES_MENU.getPanes(0))
                            .filter(p -> p.getPriority() == Pane.Priority.HIGHEST)
                            .map(p -> (ToggleButton) p)
                    ) {
                        if (is != button.isEnabled()) continue;
                        button.toggle();

                        for (Entry<String, ButtonCreator> en : ENTRIES.entries()) {
                            if (!en.getValue().compare(button))
                                continue;

                            String name = en.getKey();
                            STATUS_MAP.put(name, !is);

                            String n = "modules." + name;

                            YAMLData.Module.getMain().set(n, !is);
                            YAMLData.Module.getMain().save();
                            break;
                        }
                    }
                }));

        menuLoaded = true;
    }

    @Priority(2)
    void saveData() {
        if (!modulesLoaded) return;

        ConfigurableFile file = YAMLData.Module.getMain();

        STATUS_MAP.forEach((k, v) -> file.set("modules." + k, v));
        file.save();
    }
}
