package me.croabeast.sir.plugin.command.mute;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.file.YAMLData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
class TempMuteData implements DataHandler {

    void loadData() {
        ConfigurationSection data = YAMLData.Command.Multi
                .MUTE.from(false)
                .getSection("data");
        if (data == null) return;

        Set<String> keys = data.getKeys(false);
        if (keys.isEmpty()) return;

        for (String key : keys) {
            UUID uuid;

            try {
                uuid = UUID.fromString(key);
            } catch (Exception e) {
                continue;
            }

            int ticks = data.getInt(key);
            if (ticks == 0) continue;

            if (ticks == -1) {
                MuteCommand.invokeEmpty(uuid);
                continue;
            }

            MuteCommand.invoke(uuid, ticks / 20);
        }
    }

    void saveData() {
        ConfigurableFile data = YAMLData.Command.Multi.MUTE.from(false);
        AtomicInteger saves = new AtomicInteger();

        MuteCommand.MUTED_MAP.forEach((uuid, task) -> {
            if (task == null) return;
            task.cancel();

            if (!task.isTemporary()) {
                data.set("data." + uuid, -1);
                saves.getAndIncrement();
                return;
            }

            data.set("data." + uuid, task.restTicksFromNow());
            saves.getAndIncrement();
        });

        if (saves.get() > 0) data.save();
    }
}
