package me.croabeast.sir.plugin.world;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.var;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"deprecation", "unchecked"})
@UtilityClass
public class WorldRule implements CacheHandler {
    
    private final Map<GameRule<?>, Rule<?>> RULE_MAP = new HashMap<>();

    public final Rule<Boolean> ANNOUNCE_ADVANCEMENTS = fromBool("announceAdvancements", true);
    public final Rule<Boolean> COMMAND_BLOCK_OUTPUT = fromBool("commandBlockOutput", true);
    public final Rule<Boolean> DISABLE_ELYTRA_MOVEMENT_CHECK = fromBool("disableElytraMovementCheck", false);
    public final Rule<Boolean> DO_DAYLIGHT_CYCLE = fromBool("doDaylightCycle", true);
    public final Rule<Boolean> DO_ENTITY_DROPS = fromBool("doEntityDrops", true);
    public final Rule<Boolean> DO_FIRE_TICK = fromBool("doFireTick", true);
    public final Rule<Boolean> DO_LIMITED_CRAFTING = fromBool("doLimitedCrafting", false);
    public final Rule<Boolean> DO_MOB_LOOT = fromBool("doMobLoot", true);
    public final Rule<Boolean> DO_MOB_SPAWNING = fromBool("doMobSpawning",true);
    public final Rule<Boolean> DO_TILE_DROPS = fromBool("doTileDrops", true);
    public final Rule<Boolean> DO_WEATHER_CYCLE = fromBool("doWeatherCycle", true);
    public final Rule<Boolean> KEEP_INVENTORY = fromBool("keepInventory", false);
    public final Rule<Boolean> LOG_ADMIN_COMMANDS = fromBool("logAdminCommands", true);
    public final Rule<Boolean> MOB_GRIEFING = fromBool("mobGriefing", true);
    public final Rule<Boolean> NATURAL_REGENERATION = fromBool("naturalRegeneration", true);
    public final Rule<Boolean> REDUCED_DEBUG_INFO = fromBool("reducedDebugInfo", true);
    public final Rule<Boolean> SEND_COMMAND_FEEDBACK = fromBool("sendCommandFeedback", true);
    public final Rule<Boolean> SHOW_DEATH_MESSAGES = fromBool("showDeathMessages", true);
    public final Rule<Boolean> SPECTATORS_GENERATE_CHUNKS = fromBool("spectatorsGenerateChunks", true);
    public final Rule<Boolean> DISABLE_RAIDS = fromBool("disableRaids", false);
    public final Rule<Boolean> DO_INSOMNIA = fromBool("doInsomnia", true);
    public final Rule<Boolean> DO_IMMEDIATE_RESPAWN = fromBool("doImmediateRespawn", false);
    public final Rule<Boolean> DROWNING_DAMAGE = fromBool("drowningDamage", true);
    public final Rule<Boolean> FALL_DAMAGE = fromBool("fallDamage", true);
    public final Rule<Boolean> FIRE_DAMAGE = fromBool("fireDamage", true);
    public final Rule<Boolean> FREEZE_DAMAGE = fromBool("freezeDamage", true);
    public final Rule<Boolean> DO_PATROL_SPAWNING = fromBool("doPatrolSpawning", true);
    public final Rule<Boolean> DO_TRADER_SPAWNING = fromBool("doTraderSpawning", true);
    public final Rule<Boolean> DO_WARDEN_SPAWNING = fromBool("doWardenSpawning", true);
    public final Rule<Boolean> FORGIVE_DEAD_PLAYERS = fromBool("forgiveDeadPlayers", true);
    public final Rule<Boolean> UNIVERSAL_ANGER = fromBool("universalAnger", false);
    public final Rule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = fromBool("blockExplosionDropDecay", true);
    public final Rule<Boolean> MOB_EXPLOSION_DROP_DECAY = fromBool("mobExplosionDropDecay", true);
    public final Rule<Boolean> TNT_EXPLOSION_DROP_DECAY = fromBool("tntExplosionDropDecay", false);
    public final Rule<Boolean> WATER_SOURCE_CONVERSION = fromBool("waterSourceConversion", true);
    public final Rule<Boolean> LAVA_SOURCE_CONVERSION = fromBool("lavaSourceConversion", false);
    public final Rule<Boolean> GLOBAL_SOUND_EVENTS = fromBool("globalSoundEvents", true);
    public final Rule<Boolean> DO_VINES_SPREAD = fromBool("doVinesSpread", true);
    public final Rule<Integer> RANDOM_TICK_SPEED = fromInt("randomTickSpeed", 3);
    public final Rule<Integer> SPAWN_RADIUS = fromInt("spawnRadius", 10);
    public final Rule<Integer> MAX_ENTITY_CRAMMING = fromInt("maxEntityCramming", 24);
    public final Rule<Integer> MAX_COMMAND_CHAIN_LENGTH = fromInt("maxCommandChainLength", 65536);
    public final Rule<Integer> COMMAND_MODIFICATION_BLOCK_LIMIT = fromInt("commandModificationBlockLimit", 32768);
    public final Rule<Integer> PLAYERS_SLEEPING_PERCENTAGE = fromInt("playersSleepingPercentage", 100);
    public final Rule<Integer> SNOW_ACCUMULATION_HEIGHT = fromInt("snowAccumulationHeight", 1);

    private final Map<World, Map<Rule<?>, String>> LOADED_RULES_MAP = new HashMap<>();
    public boolean areWorldsLoaded = false;

    void loadCache() {
        if (areWorldsLoaded) return;

        SIRPlugin.runTaskWhenLoaded(() -> {
            Bukkit.getWorlds().forEach(world -> {
                var v = LOADED_RULES_MAP.get(world);
                if (v == null) v = new HashMap<>();

                for (var rule : WorldRule.values()) {
                    Object value = rule.getValue(world);
                    if (value != null)
                        v.put(rule, value.toString());
                }

                LOADED_RULES_MAP.put(world, v);
            });

            areWorldsLoaded = true;
        });
    }

    private BooleanRule fromBool(String rule, boolean value) {
        return new BooleanRule(rule, value);
    }

    private IntRule fromInt(String rule, int value) {
        return new IntRule(rule, value);
    }

    public Rule<?>[] values() {
        return RULE_MAP.values().toArray(new Rule<?>[0]);
    }

    public <T> Rule<T> fromBukkit(GameRule<T> rule) {
        return (Rule<T>) RULE_MAP.get(rule);
    }

    public String valueFromLoaded(World world, Rule<?> rule) {
        try {
            return LOADED_RULES_MAP.get(world).get(rule);
        } catch (Exception e) {
            return null;
        }
    }

    public abstract class Rule<T> {

        @NotNull @Getter
        final String rule;

        private final Class<T> c;
        private final T defValue;

        private Rule(@NotNull String rule, Class<T> clazz, T def) {
            this.rule = rule;
            this.defValue = def;
            this.c = clazz;

            if (RULE_MAP.containsValue(this))
                throw new UnsupportedOperationException();

            RULE_MAP.put(GameRule.getByName(rule), this);
        }

        public abstract T getValue(World world);

        public T getDefault() {
            return defValue;
        }

        public boolean setValue(World world, T value) {
            return world.setGameRuleValue(rule, String.valueOf(value));
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Rule)) return false;

            Rule<?> o = (Rule<?>) obj;
            return c == o.c && rule.equals(o.rule);
        }

        @Override
        public String toString() {
            return "WorldRule{rule='" + rule + "', clazz=" + c.getSimpleName() + '}';
        }
    }

    class BooleanRule extends Rule<Boolean> {

        private BooleanRule(@NotNull String rule, boolean def) {
            super(rule, Boolean.class, def);
        }

        @Override
        public Boolean getValue(World world) {
            return Boolean.parseBoolean(world.getGameRuleValue(rule));
        }
    }

    class IntRule extends Rule<Integer> {

        private IntRule(@NotNull String rule, int def) {
            super(rule, Integer.class, def);
        }

        @Override
        public Integer getValue(World world) {
            return Integer.parseInt(world.getGameRuleValue(rule));
        }
    }
}
