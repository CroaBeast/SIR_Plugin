package me.croabeast.sir.plugin.logger;

public interface DelayLogger {

    DelayLogger add(String line, boolean usePrefix);

    DelayLogger add(boolean usePrefix, String... lines);

    DelayLogger addFirst(String line, boolean usePrefix);

    DelayLogger addFirst(boolean usePrefix, String... lines);

    DelayLogger addLast(String line, boolean usePrefix);

    DelayLogger addLast(boolean usePrefix, String... lines);

    void sendLines();

    static DelayLogger simplified() {
        return new SimpleLogger();
    }

    static DelayLogger queued() {
        return new DequeLogger();
    }
}
