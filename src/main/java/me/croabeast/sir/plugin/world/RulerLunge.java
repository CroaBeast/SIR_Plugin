package me.croabeast.sir.plugin.world;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.sir.plugin.file.CacheManageable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"deprecation"})
@UtilityClass
public class RulerLunge implements CacheManageable {

    private final Map<String, Rule<?>> RULE_MAP = new LinkedHashMap<>();

    public final Rule<Boolean> DO_FIRE_TICK = new BoolRule("doFireTick", true, 4.2);

    public Rule<?>[] values() {
        return ArrayUtils.toArray(RULE_MAP.values());
    }

    public abstract class Rule<T> {

        final String rule;
        final Class<T> clazz;
        private final T def;
        final double minVersion;

        @SneakyThrows
        private Rule(String rule, Class<T> clazz, T def, double minVersion) {
            this.rule = rule;
            this.clazz = clazz;
            this.def = def;
            this.minVersion = minVersion;

            RULE_MAP.put(this.rule, this);
        }

        @Nullable
        public abstract T getValue(World world);

        public Class<T> getType() {
            return clazz;
        }

        public T getDefault() {
            return def;
        }

        public boolean isDefault(World world) {
            return Objects.equals(getValue(world), getDefault());
        }
    }

    private final class BoolRule extends Rule<Boolean> {

        private BoolRule(String rule, boolean defValue, double minVersion) {
            super(rule, Boolean.class, defValue, minVersion);
        }

        @Override
        public Boolean getValue(World world) {
            if (LibUtils.MAIN_VERSION < minVersion)
                return null;

            String value = world.getGameRuleValue(rule);

            return StringUtils.isBlank(value) ||
                    !value.matches("(?i)true|false") ?
                    null :
                    Boolean.parseBoolean(value);
        }
    }

    private final class IntRule extends Rule<Integer> {

        private IntRule(@NotNull String rule, int defValue, double minVersion) {
            super(rule, Integer.class, defValue, minVersion);
        }

        @Override
        public Integer getValue(World world) {
            if (LibUtils.MAIN_VERSION < minVersion)
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
    }
}
