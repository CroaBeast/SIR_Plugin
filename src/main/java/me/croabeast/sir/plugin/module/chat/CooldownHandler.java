package me.croabeast.sir.plugin.module.chat;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.ConfigUnit;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.event.chat.SIRChatEvent;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.LangUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public final class CooldownHandler extends ChatModule implements CustomListener {

    private static final Map<Integer, Set<CooldownUnit>> UNIT_MAP = new TreeMap<>(Collections.reverseOrder());

    private static final Map<Player, Integer> CHECK_MAP = new HashMap<>();
    private static final Map<Player, Long> GLOBAL_TIMERS = new HashMap<>(), LOCAL_TIMERS = new HashMap<>();

    @Getter @Setter
    private boolean registered = false;

    CooldownHandler() {
        super(Name.COOLDOWNS, YAMLData.Module.Chat.COOLDOWNS);
    }

    @Override
    public boolean register() {
        if (!isEnabled()) return false;

        registerOnSIR();
        UNIT_MAP.clear();

        ConfigurationSection section = config.getSection("cooldowns");
        if (section == null) return false;

        for (String key : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(key);
            if (c == null) continue;

            final CooldownUnit unit = new CooldownUnit(c);
            int priority = unit.getPriority();

            Set<CooldownUnit> units = UNIT_MAP.get(priority);
            if (units == null) units = new HashSet<>();

            units.add(unit);
            UNIT_MAP.put(priority, units);
        }

        return true;
    }

    private static CooldownUnit get(Player player) {
        for (Set<CooldownUnit> set : UNIT_MAP.values())
            for (CooldownUnit unit : set)
                if (unit.isInGroupNonNull(player) && unit.hasPerm(player))
                    return unit;

        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onBukkit(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !isEnabled())
            return;

        Player player = event.getPlayer();
        Long data = GLOBAL_TIMERS.get(player);

        CooldownUnit unit = get(player);
        if (unit == null) return;

        final int time = unit.getTime();
        if (time <= 0 || data == null) return;

        long rest = System.currentTimeMillis() - data;
        if (rest >= time * 1000L) return;

        event.setCancelled(true);
        int result = Math.round(rest / 1000F);

        if (unit.canCheck()) {
            int tempCount = CHECK_MAP.getOrDefault(player, 0);

            if (rest >= unit.checks.timeLimit * 1000L &&
                    tempCount >= unit.checks.count)
            {
                List<String> commands = unit.checks.commands;
                LangUtils.executeCommands(player, commands);

                CHECK_MAP.put(player, ++tempCount);
            }
        }

        final int temp = time - result;

        MessageSender.loaded()
                .addKeyValue("{time}", temp)
                .setLogger(false)
                .setTargets(player)
                .send(unit.getMessages());

        GLOBAL_TIMERS.put(player, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onSIR(SIRChatEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();
        Long data = GLOBAL_TIMERS.get(player);

        Map<Player, Long> map =
                channel.isGlobal() ? GLOBAL_TIMERS : LOCAL_TIMERS;

        CooldownUnit unit = get(player);
        if (unit == null) return;

        final int time = unit.getTime();
        if (time <= 0 || data == null) return;

        long rest = System.currentTimeMillis() - data;
        if (rest >= time * 1000L) return;

        event.setCancelled(true);
        int result = Math.round(rest / 1000F);

        if (unit.canCheck()) {
            int tempCount = CHECK_MAP.getOrDefault(player, 0);

            if (rest >= unit.checks.timeLimit * 1000L &&
                    tempCount >= unit.checks.count)
            {
                List<String> commands = unit.checks.commands;
                LangUtils.executeCommands(player, commands);

                CHECK_MAP.put(player, ++tempCount);
            }
        }

        final int temp = time - result;

        MessageSender.loaded()
                .addKeyValue("{time}", temp)
                .setLogger(false)
                .setTargets(player)
                .send(unit.getMessages());

        map.put(player, System.currentTimeMillis());
    }

    private static class Checks {

        private final boolean enabled;
        private final int count, timeLimit;
        private final List<String> commands;

        private Checks(ConfigurationSection s) {
            this.enabled = s.getBoolean("enabled");
            this.count = s.getInt("count");
            this.timeLimit = s.getInt("time-limit");
            this.commands = TextUtils.toList(s, "commands");
        }
    }

    @Getter
    private static class CooldownUnit implements ConfigUnit {

        private final ConfigurationSection section;

        private Checks checks = null;
        private final int time;
        private final List<String> messages;

        private CooldownUnit(ConfigurationSection section) {
            this.section = section;

            this.messages = TextUtils.toList(section, "messages");
            this.time = section.getInt("time");

            ConfigurationSection s = section.getConfigurationSection("check");
            if (s != null) this.checks = new Checks(s);
        }

        public boolean canCheck() {
            return checks != null && checks.enabled;
        }
    }
}
