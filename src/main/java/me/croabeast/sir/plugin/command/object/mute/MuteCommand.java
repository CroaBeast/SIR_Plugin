package me.croabeast.sir.plugin.command.object.mute;

import me.croabeast.beanslib.time.TimeKeys;
import me.croabeast.beanslib.time.TimeParser;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.file.YAMLCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class MuteCommand extends SIRCommand {

    static final Map<UUID, TempMuteTask> MUTED_MAP = new HashMap<>();

    protected static final String PATH = "commands.mute.";

    protected MuteCommand(String name) {
        super(name);
    }

    public static boolean isMuted(Player player) {
        return MUTED_MAP.containsKey(player.getUniqueId());
    }

    private static String getFormat(String path) {
        return YAMLCache.getLang().get(
                PATH + "time." + path,
                path.substring(0, path.length() - 1) + "(s)"
        );
    }

    protected static String muteParser(long seconds) {
        return new TimeParser(new TimeKeys() {
            @Override
            public String getSecondFormat() {
                return getFormat("seconds");
            }
            @Override
            public String getMinuteFormat() {
                return getFormat("minutes");
            }
            @Override
            public String getHourFormat() {
                return getFormat("hours");
            }
            @Override
            public String getDayFormat() {
                return getFormat("days");
            }
            @Override
            public String getWeekFormat() {
                return getFormat("weeks");
            }
            @Override
            public String getMonthFormat() {
                return getFormat("months");
            }
            @Override
            public String getYearFormat() {
                return getFormat("years");
            }
        }, seconds).formatTime();
    }

    static class TempMuteTask {

        final int id;
        final long millis;
        final int time;

        private TempMuteTask(UUID uuid, int id, long millis, int time) {
            this.id = id;
            this.millis = millis;
            this.time = time;

            MUTED_MAP.put(uuid, this);
        }

        private TempMuteTask(UUID uuid) {
            this(uuid, -1, -1L, -1);
        }

        private TempMuteTask(UUID uuid, int seconds) {
            this(uuid,
                    Bukkit.getScheduler()
                            .runTaskLater(
                                    SIRPlugin.getInstance(),
                                    () -> MUTED_MAP.remove(uuid),
                                    seconds * 20L
                            )
                            .getTaskId(),
                    System.currentTimeMillis(),
                    seconds
            );
        }

        int restTicksFromNow() {
            int temp = (int) (System.currentTimeMillis() - millis);
            return (time * 20) - (temp / 50);
        }

        void cancel() {
            try {
                Bukkit.getScheduler().cancelTask(id);
            } catch (Exception ignored) {}
        }

        boolean isTemporary() {
            return true;
        }
    }

    static void invoke(UUID uuid, int seconds) {
        new TempMuteTask(uuid, seconds);
    }

    static void invokeEmpty(UUID uuid) {
        new TempMuteTask(uuid) {
            @Override
            void cancel() {}
            @Override
            boolean isTemporary() {
                return false;
            }
        };
    }
}
