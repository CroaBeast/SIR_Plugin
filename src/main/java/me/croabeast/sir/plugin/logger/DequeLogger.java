package me.croabeast.sir.plugin.logger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.logger.LogLevel;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
final class DequeLogger implements DelayLogger {

    private final LinkedList<LoggerLine> deque = new LinkedList<>();
    @Getter @Setter
    private Plugin plugin;

    @Override
    public String[] getLoadedLines() {
        return !deque.isEmpty() ?
                CollectionBuilder.of(deque).map(l -> l.line).toArray() :
                new String[0];
    }

    @Override
    public DequeLogger add(boolean usePrefix, String line) {
        deque.add(new LoggerLine(line, usePrefix));
        return this;
    }

    @Override
    public DequeLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> add(usePrefix, s));
        return this;
    }

    @Override
    public DequeLogger addFirst(boolean usePrefix, String line) {
        deque.addFirst(new LoggerLine(line, usePrefix));
        return this;
    }

    @Override
    public DequeLogger addFirst(boolean usePrefix, String... lines) {
        List<String> list = ArrayUtils.toList(lines);
        Collections.reverse(list);
        list.forEach(s -> addFirst(usePrefix, s));
        return this;
    }

    @Override
    public DequeLogger addLast(boolean usePrefix, String line) {
        deque.add(new LoggerLine(line, usePrefix));
        return this;
    }

    @Override
    public DequeLogger addLast(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> addLast(usePrefix, s));
        return this;
    }

    @Override
    public void clear() {
        deque.clear();
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    @Override
    public void sendLines(boolean useScheduler) {
        if (deque.isEmpty()) return;

        Actionable actionable = () ->
                deque.forEach(l -> BeansLib.logger().log(
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
