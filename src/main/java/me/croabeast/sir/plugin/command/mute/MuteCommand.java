package me.croabeast.sir.plugin.command.mute;

import lombok.Getter;
import me.croabeast.lib.time.TimeFormatter;
import me.croabeast.lib.time.TimeValues;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class MuteCommand extends SIRCommand {

    static final Map<UUID, TempMuteTask> MUTED_MAP = new HashMap<>();

    @NotNull @Getter
    private final ConfigurableFile lang, data;

    protected MuteCommand(String name) {
        super(name);

        this.lang = YAMLData.Command.Multi.MUTE.from(true);
        this.data = YAMLData.Command.Multi.MUTE.from(false);
    }

    public static boolean isMuted(Player player) {
        return MUTED_MAP.containsKey(player.getUniqueId());
    }

    protected static String getParsedTime(int time) {
        ConfigurationSection s = YAMLData.Command.Multi.MUTE.from(true).getSection("lang.time");
        return new TimeFormatter(s != null ? TimeValues.fromSection(s) : null, time).formatTime();
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

        String getParsedTime() {
            return MuteCommand.getParsedTime(restTicksFromNow() / 20);
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
