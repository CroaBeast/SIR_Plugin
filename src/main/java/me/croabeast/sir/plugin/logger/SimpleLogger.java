package me.croabeast.sir.plugin.logger;

import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.util.ArrayUtils;

import java.util.*;

final class SimpleLogger implements DelayLogger {

    private final List<LoggerLine> list = new ArrayList<>();

    public SimpleLogger add(String line, boolean usePrefix) {
        list.add(new LoggerLine(line, usePrefix));
        return this;
    }

    public SimpleLogger add(boolean usePrefix, String... lines) {
        ArrayUtils.toList(lines).forEach(s -> add(s, usePrefix));
        return this;
    }

    @Override
    public SimpleLogger addFirst(String line, boolean usePrefix) {
        return add(line, usePrefix);
    }

    @Override
    public SimpleLogger addFirst(boolean usePrefix, String... lines) {
        return add(usePrefix, lines);
    }

    @Override
    public SimpleLogger addLast(String line, boolean usePrefix) {
        return add(line, usePrefix);
    }

    @Override
    public SimpleLogger addLast(boolean usePrefix, String... lines) {
        return add(usePrefix, lines);
    }

    @Override
    public void sendLines() {
        if (list.isEmpty()) return;

        list.forEach(l -> {
            if (l.usePrefix) {
                BeansLogger.getLogger().log(l.line);
                return;
            }
            BeansLogger.doLog(l.line);
        });
    }
}
