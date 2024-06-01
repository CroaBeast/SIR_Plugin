package me.croabeast.sir.plugin.world;

import me.croabeast.lib.util.ServerInfoUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({"deprecation"})
public abstract class WorldRule<T> {

    private static final Map<String, WorldRule<?>> RULE_MAP = new LinkedHashMap<>();

    public static final WorldRule<Boolean> COMMAND_BLOCK_OUTPUT;
    public static final WorldRule<Boolean> DO_FIRE_TICK;
    public static final WorldRule<Boolean> DO_MOB_LOOT;
    public static final WorldRule<Boolean> DO_MOB_SPAWNING;
    public static final WorldRule<Boolean> DO_TILE_DROPS;
    public static final WorldRule<Boolean> KEEP_INVENTORY;
    public static final WorldRule<Boolean> MOB_GRIEFING;
    public static final WorldRule<Boolean> DO_DAYLIGHT_CYCLE;
    public static final WorldRule<Boolean> NATURAL_REGENERATION;
    public static final WorldRule<Boolean> LOG_ADMIN_COMMANDS;
    public static final WorldRule<Integer> RANDOM_TICK_SPEED;
    public static final WorldRule<Boolean> REDUCED_DEBUG_INFO;
    public static final WorldRule<Boolean> SEND_COMMAND_FEEDBACK;
    public static final WorldRule<Boolean> SHOW_DEATH_MESSAGES;
    public static final WorldRule<Boolean> DO_ENTITY_DROPS;
    public static final WorldRule<Boolean> DISABLE_ELYTRA_MOVEMENT_CHECK;
    public static final WorldRule<Integer> SPAWN_RADIUS;
    public static final WorldRule<Boolean> SPECTATORS_GENERATE_CHUNKS;
    public static final WorldRule<Boolean> DO_WEATHER_CYCLE;
    public static final WorldRule<Integer> MAX_ENTITY_CRAMMING;
    public static final WorldRule<Boolean> ANNOUNCE_ADVANCEMENTS;
    public static final WorldRule<Boolean> DO_LIMITED_CRAFTING;
    public static final WorldRule<Integer> MAX_COMMAND_CHAIN_LENGTH;
    public static final WorldRule<Boolean> DISABLE_RAIDS;
    public static final WorldRule<Boolean> DO_IMMEDIATE_RESPAWN;
    public static final WorldRule<Boolean> DO_INSOMNIA;
    public static final WorldRule<Boolean> DROWNING_DAMAGE;
    public static final WorldRule<Boolean> FALL_DAMAGE;
    public static final WorldRule<Boolean> FIRE_DAMAGE;
    public static final WorldRule<Boolean> DO_PATROL_SPAWNING;
    public static final WorldRule<Boolean> DO_TRADER_SPAWNING;
    public static final WorldRule<Boolean> FORGIVE_DEAD_PLAYERS;
    public static final WorldRule<Boolean> UNIVERSAL_ANGER;
    public static final WorldRule<Boolean> FREEZE_DAMAGE;
    public static final WorldRule<Integer> PLAYERS_SLEEPING_PERCENTAGE;
    public static final WorldRule<Boolean> DO_WARDEN_SPAWNING;
    public static final WorldRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY;
    public static final WorldRule<Boolean> GLOBAL_SOUND_EVENTS;
    public static final WorldRule<Boolean> LAVA_SOURCE_CONVERSION;
    public static final WorldRule<Boolean> MOB_EXPLOSION_DROP_DECAY;
    public static final WorldRule<Integer> SNOW_ACCUMULATION_HEIGHT;
    public static final WorldRule<Boolean> TNT_EXPLOSION_DROP_DECAY;
    public static final WorldRule<Boolean> WATER_SOURCE_CONVERSION;
    public static final WorldRule<Integer> COMMAND_MODIFICATION_BLOCK_LIMIT;
    public static final WorldRule<Boolean> DO_VINES_SPREAD;
    public static final WorldRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH;
    public static final WorldRule<Integer> MAX_COMMAND_FORK_COUNT;
    public static final WorldRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY;
    public static final WorldRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY;
    public static final WorldRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS;
    public static final WorldRule<Integer> SPAWN_CHUNK_RADIUS;

    static {
        COMMAND_BLOCK_OUTPUT = boolRule("commandBlockOutput", true, 4.2);
        DO_FIRE_TICK = boolRule("doFireTick", true, 4.2);
        DO_MOB_LOOT = boolRule("doMobLoot", true, 4.2);
        DO_MOB_SPAWNING = boolRule("doMobSpawning", true, 4.2);
        DO_TILE_DROPS = boolRule("doTileDrops", true, 4.2);
        KEEP_INVENTORY = boolRule("keepInventory", false, 4.2);
        MOB_GRIEFING = boolRule("mobGriefing", true, 4.2);
        DO_DAYLIGHT_CYCLE = boolRule("doDaylightCycle", true, 6.1);
        NATURAL_REGENERATION = boolRule("naturalRegeneration", true, 6.1);
        LOG_ADMIN_COMMANDS = boolRule("logAdminCommands", true, 8);
        RANDOM_TICK_SPEED = intRule("randomTickSpeed", 3, 8);
        REDUCED_DEBUG_INFO = boolRule("reducedDebugInfo", false, 8);
        SEND_COMMAND_FEEDBACK = boolRule("sendCommandFeedback", true, 8);
        SHOW_DEATH_MESSAGES = boolRule("showDeathMessages", true, 8);
        DO_ENTITY_DROPS = boolRule("doEntityDrops", true, 8.1);
        DISABLE_ELYTRA_MOVEMENT_CHECK = boolRule("disableElytraMovementCheck", false, 9);
        SPAWN_RADIUS = intRule("spawnRadius", 10, 9);
        SPECTATORS_GENERATE_CHUNKS = boolRule("spectatorsGenerateChunks", true, 9);
        DO_WEATHER_CYCLE = boolRule("doWeatherCycle", true, 11);
        MAX_ENTITY_CRAMMING = intRule("maxEntityCramming", 24, 11);
        ANNOUNCE_ADVANCEMENTS = boolRule("announceAdvancements", true, 12);
        DO_LIMITED_CRAFTING = boolRule("doLimitedCrafting", false, 12);
        MAX_COMMAND_CHAIN_LENGTH = intRule("maxCommandChainLength", 65536, 12);
        DISABLE_RAIDS = boolRule("disableRaids", false, 14.3);
        DO_IMMEDIATE_RESPAWN = boolRule("doImmediateRespawn", false, 15);
        DO_INSOMNIA = boolRule("doInsomnia", true, 15);
        DROWNING_DAMAGE = boolRule("drowningDamage", true, 15);
        FALL_DAMAGE = boolRule("fallDamage", true, 15);
        FIRE_DAMAGE = boolRule("fireDamage", true, 15);
        DO_PATROL_SPAWNING = boolRule("doPatrolSpawning", true, 15.2);
        DO_TRADER_SPAWNING = boolRule("doTraderSpawning", true, 15.2);
        FORGIVE_DEAD_PLAYERS = boolRule("forgiveDeadPlayers", true, 16);
        UNIVERSAL_ANGER = boolRule("universalAnger", false, 16);
        FREEZE_DAMAGE = boolRule("freezeDamage", true, 17);
        PLAYERS_SLEEPING_PERCENTAGE = intRule("playersSleepingPercentage", 100, 17);
        DO_WARDEN_SPAWNING = boolRule("doWardenSpawning", true, 19);
        BLOCK_EXPLOSION_DROP_DECAY = boolRule("blockExplosionDropDecay", true, 19.3);
        GLOBAL_SOUND_EVENTS = boolRule("globalSoundEvents", true, 19.3);
        LAVA_SOURCE_CONVERSION = boolRule("lavaSourceConversion", false, 19.3);
        MOB_EXPLOSION_DROP_DECAY = boolRule("mobExplosionDropDecay", true, 19.3);
        SNOW_ACCUMULATION_HEIGHT = intRule("snowAccumulationHeight", 1, 19.3);
        TNT_EXPLOSION_DROP_DECAY = boolRule("tntExplosionDropDecay", false, 19.3);
        WATER_SOURCE_CONVERSION = boolRule("waterSourceConversion", true, 19.3);
        COMMAND_MODIFICATION_BLOCK_LIMIT = intRule("commandModificationBlockLimit", 32768, 19.4);
        DO_VINES_SPREAD = boolRule("doVinesSpread", true, 19.4);
        ENDER_PEARLS_VANISH_ON_DEATH = boolRule("enderPearlsVanishOnDeath", true, 20.2);
        MAX_COMMAND_FORK_COUNT = intRule("maxCommandForkCount", 65536, 20.3);
        PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = intRule("playersNetherPortalCreativeDelay", 1, 20.3);
        PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = intRule("playersNetherPortalDefaultDelay", 80, 20.3);
        PROJECTILES_CAN_BREAK_BLOCKS = boolRule("projectilesCanBreakBlocks", false, 20.3);
        SPAWN_CHUNK_RADIUS = intRule("spawnChunkRadius", 2, 20.5);
    }

    private final String rule;
    private final Class<T> clazz;
    private final T def;
    private final double minVersion;

    private WorldRule(String rule, Class<T> clazz, T def, double minVersion) {
        this.rule = rule;
        this.clazz = clazz;
        this.def = def;
        this.minVersion = minVersion;

        RULE_MAP.put(this.rule, this);
    }

    @NotNull
    public String getName() {
        return rule;
    }

    @NotNull
    public Class<T> getType() {
        return clazz;
    }

    @Nullable
    public abstract T getValue(World world);

    public T getDefault() {
        return def;
    }

    public boolean setValue(World world, T value) {
        return ServerInfoUtils.SERVER_VERSION >= minVersion && world.setGameRuleValue(rule, String.valueOf(value));
    }

    public boolean isDefault(World world) {
        return Objects.equals(getValue(world), getDefault());
    }

    private static WorldRule<Boolean> boolRule(String rule, boolean def, double min) {
        return new WorldRule<Boolean>(rule, Boolean.class, def, min) {
            @Override
            public Boolean getValue(World world) {
                if (ServerInfoUtils.SERVER_VERSION < min)
                    return null;

                String value = world.getGameRuleValue(rule);

                return StringUtils.isBlank(value) ||
                        !value.matches("(?i)true|false") ?
                        null :
                        Boolean.parseBoolean(value);
            }
        };
    }

    private static WorldRule<Integer> intRule(String rule, int def, double min) {
        return new WorldRule<Integer>(rule, Integer.class, def, min) {
            @Override
            public Integer getValue(World world) {
                if (ServerInfoUtils.SERVER_VERSION < min)
                    return null;

                String value = world.getGameRuleValue(rule);

                if (StringUtils.isBlank(value))
                    return null;

                try {
                    return Integer.parseInt(value);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    public static String valueFromLoaded(World world, WorldRule<?> rule) {
        try {
            return WorldData.LOADED_RULES_MAP.get(world).get(rule);
        } catch (Exception e) {
            return null;
        }
    }

    public static Set<WorldRule<?>> values() {
        return new LinkedHashSet<>(RULE_MAP.values());
    }
}
