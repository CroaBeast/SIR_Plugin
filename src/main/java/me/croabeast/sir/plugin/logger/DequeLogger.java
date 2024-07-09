package me.croabeast.sir.plugin.logger;

import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.util.ArrayUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final class DequeLogger implements DelayLogger {

    private final LinkedList<LoggerLine> deque = new LinkedList<>();

    @Override
    public DequeLogger add(String line, boolean usePrefix) {
        deque.add(new LoggerLine(line, usePrefix));
        return this;
    }

    @Override
    public DequeLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> add(usePrefix, s));
        return this;
    }

    @Override
    public DequeLogger addFirst(String line, boolean usePrefix) {
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
    public DequeLogger addLast(String line, boolean usePrefix) {
        return add(line, usePrefix);
    }

    @Override
    public DequeLogger addLast(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> addLast(usePrefix, s));
        return this;
    }

    @Override
    public void sendLines() {
        if (deque.isEmpty()) return;

        deque.forEach(l -> {
            if (l.usePrefix) {
                BeansLogger.getLogger().log(l.line);
                return;
            }
            BeansLogger.doLog(l.line);
        });
    }
}
