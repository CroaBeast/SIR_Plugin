package me.croabeast.sir.plugin.module;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.advancement.AdvancementInfo;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.logger.DelayLogger;
import me.croabeast.sir.plugin.world.WorldRule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@UtilityClass
class AdvanceLoader implements DataHandler {

    private final CollectionBuilder<Advancement> ADVANCEMENT_BUILDER =
            CollectionBuilder.of(Bukkit.advancementIterator())
                    .filter(a -> {
                        final String k = a.getKey().toString();
                        return !k.contains("recipes") && !k.contains("root");
                    });

    final Set<String> ADVANCEMENT_KEYS = ADVANCEMENT_BUILDER
            .map(a -> a.getKey().toString())
            .sort(Comparator.naturalOrder()).collect(new LinkedHashSet<>());

    private final Set<AdvancementInfo> ALL_INFO = ADVANCEMENT_BUILDER
            .copy()
            .map(a -> {
                    AdvancementInfo info = null;
                    try {
                        info = AdvancementInfo.from(a);
                    } catch (Exception ignored) {}
                    return info;
                })
            .filter(Objects::nonNull).toSet();

    private final Set<AdvancementInfo> TASKS, GOALS, CHALLENGES, UNKNOWNS;
    private static boolean areAdvancementsLoaded = false;

    static {
        TASKS = new LinkedHashSet<>();
        GOALS = new LinkedHashSet<>();
        CHALLENGES = new LinkedHashSet<>();
        UNKNOWNS = new LinkedHashSet<>();

        ALL_INFO.forEach(info -> {
            AdvancementInfo.Frame type = info.getFrame();

            switch (type) {
                case TASK:
                    TASKS.add(info);
                    break;

                case GOAL:
                    GOALS.add(info);
                    break;

                case CHALLENGE:
                    CHALLENGES.add(info);
                    break;

                default:
                    UNKNOWNS.add(info);
                    break;
            }
        });
    }

    static void checkAdvancements() {
        if (!SIRModule.ADVANCEMENTS.isEnabled()) return;

        List<String> list = YAMLData.Module.ADVANCEMENT
                .fromName("config")
                .toStringList("disabled-worlds");

        WorldRule<Boolean> rule = WorldRule.ANNOUNCE_ADVANCEMENTS;

        for (World w : Bukkit.getWorlds()) {
            if (list.contains(w.getName())) continue;

            if (Boolean.TRUE.equals(rule.getValue(w)))
                rule.setValue(w, false);
        }
    }

    void loadData() {
        try {
            if (ServerInfoUtils.SERVER_VERSION < 12) return;

            if (Reflector
                    .of("me.croabeast.sir.plugin.world.WorldData")
                    .get("areWorldsLoaded"))
                checkAdvancements();

            if (areAdvancementsLoaded) return;

            SIRPlugin.runWhenLoaded(() -> {
                final DelayLogger logger = DelayLogger.simplified()
                        .add("", false)
                        .add("&bRegistering advancements in SIR...", true);

                long time = System.currentTimeMillis();
                ConfigurableFile advances =
                        YAMLData.Module.ADVANCEMENT.fromName("lang");

                int count = 0;
                checkAdvancements();

                List<String> keys = advances.getKeys(null, false);
                for (final AdvancementInfo info : ALL_INFO) {
                    Advancement adv = info.getBukkit();

                    String k = adv.getKey().toString();
                    String key = k.replaceAll("[/:]", ".");

                    if (keys.contains(key)) continue;

                    String type = (info.getFrame() + "").toLowerCase(Locale.ENGLISH);

                    advances.set(key + ".path", "type." +
                            (type.equals("unknown") ? "custom" : type));

                    advances.set(key + ".frame", type);

                    advances.set(key + ".name", info.getTitle());
                    advances.set(key + ".description", info.getDescription());

                    final ItemStack item = info.getIcon();
                    advances.set(key + ".item",
                            item == null ? null : item.getType().toString());

                    count++;
                }

                if (count > 0) advances.save();
                areAdvancementsLoaded = true;

                String advancements = "&7Tasks: &a" + TASKS.size() +
                        "&7 - Goals: &b" + GOALS.size() +
                        "&7 - &7Challenges: &d" + CHALLENGES.size();

                logger.add(advancements, true);

                if (!UNKNOWNS.isEmpty())
                    logger.add(
                            "&7Unknowns: &c" + UNKNOWNS.size() +
                                    "&7. Check your modules" +
                                    "/advancements/lang.yml file!",
                            true
                    );

                time = System.currentTimeMillis() - time;

                logger.add("&7Loaded advancements in &e" + time + "&7 ms.", true)
                        .add("", false).sendLines();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveData() {
        if (ServerInfoUtils.SERVER_VERSION < 12) return;

        List<String> list = YAMLData.Module.ADVANCEMENT
                .fromName("config")
                .toStringList("disabled-worlds");

        WorldRule<Boolean> announces = WorldRule.ANNOUNCE_ADVANCEMENTS;

        for (World w : Bukkit.getWorlds()) {
            if (list.contains(w.getName())) continue;

            String d = WorldRule.valueFromLoaded(w, announces);
            boolean v = Boolean.TRUE.equals(announces.getValue(w));

            if (Boolean.parseBoolean(d) && !v)
                announces.setValue(w, true);
        }
    }
}
