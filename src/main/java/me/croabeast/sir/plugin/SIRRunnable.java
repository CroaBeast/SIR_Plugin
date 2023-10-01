package me.croabeast.sir.plugin;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public final class SIRRunnable {

    private static SIRPlugin get() {
        return SIRPlugin.getInstance();
    }

    private final BukkitRunnable runnable;

    public SIRRunnable(Action action) {
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                action.act();
            }
        };
    }

    public boolean isCancelled() {
        return runnable.isCancelled();
    }

    public void cancel() {
        runnable.cancel();
    }

    public void runTask() {
        runnable.runTask(get());
    }

    public void runTaskAsynchronously() {
        runnable.runTaskAsynchronously(get());
    }

    public void runTaskLater(long delay) {
        runnable.runTaskLater(get(), delay);
    }

    public void runTaskLaterAsynchronously(long delay) {
        runnable.runTaskLaterAsynchronously(get(), delay);
    }

    public void runTaskTimer(long delay, long period) {
        runnable.runTaskTimer(get(), delay, period);
    }

    public void runTaskTimerAsynchronously(long delay, long period) {
        runnable.runTaskTimerAsynchronously(get(), delay, period);
    }

    public static void runFromSIR(Action action, Consumer<SIRRunnable> consumer) {
        consumer.accept(new SIRRunnable(action));
    }

    public interface Action {
        void act();
    }
}
