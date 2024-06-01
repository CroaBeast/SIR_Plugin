package me.croabeast.sir.plugin.logger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.logger.LogLevel;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;

@AllArgsConstructor
final class SimpleLogger implements DelayLogger {

    private final List<LoggerLine> list = new ArrayList<>();
    @Getter @Setter
    private Plugin plugin;

    @Override
    public String[] getLoadedLines() {
        return !list.isEmpty() ? CollectionBuilder.of(list).map(l -> l.line).toArray() : new String[0];
    }

    public SimpleLogger add(boolean usePrefix, String line) {
        list.add(new LoggerLine(line, usePrefix));
        return this;
    }

    public SimpleLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> list.add(new LoggerLine(s, usePrefix)));
        return this;
    }

    @Override
    public SimpleLogger addFirst(boolean usePrefix, String line) {
        return add(usePrefix, line);
    }

    @Override
    public SimpleLogger addFirst(boolean usePrefix, String... lines) {
        return add(usePrefix, lines);
    }

    @Override
    public SimpleLogger addLast(boolean usePrefix, String line) {
        return add(usePrefix, line);
    }

    @Override
    public SimpleLogger addLast(boolean usePrefix, String... lines) {
        return add(usePrefix, lines);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void sendLines(boolean useScheduler) {
        Objects.requireNonNull(plugin);
        if (list.isEmpty()) return;

        Actionable actionable = () ->
                list.forEach(l -> BeansLib.logger().log(
                        l.line,
                        l.usePrefix, LogLevel.INFO
                ));

        if (!useScheduler) {
            actionable.act();
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Objects.requireNonNull(plugin),
                actionable.toRunnable()
        );
    }
}
