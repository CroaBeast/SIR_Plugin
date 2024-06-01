package me.croabeast.sir.plugin.world;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
class WorldData implements DataHandler {

    final Map<World, Map<WorldRule<?>, String>> LOADED_RULES_MAP = new HashMap<>();
    boolean areWorldsLoaded = false;

    @Priority(4)
    void loadData() {
        if (areWorldsLoaded) return;

        SIRPlugin.runTaskWhenLoaded(() -> {
            for (World world : Bukkit.getWorlds()) {
                Map<WorldRule<?>, String> v =
                        LOADED_RULES_MAP.getOrDefault(
                                world,
                                new HashMap<>()
                        );

                for (WorldRule<?> rule : WorldRule.values()) {
                    Object value = rule.getValue(world);
                    if (value != null)
                        v.put(rule, value.toString());
                }

                LOADED_RULES_MAP.put(world, v);
            }

            areWorldsLoaded = true;
        });
    }
}
