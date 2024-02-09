package me.croabeast.sir.plugin.command.object.mute;

import lombok.experimental.UtilityClass;
import lombok.var;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.file.CacheManageable;
import me.croabeast.sir.plugin.file.YAMLCache;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.UUID;

@UtilityClass
class TempMuteCache implements CacheManageable {

    void loadCache() {
        ConfigurationSection data = YAMLCache
                .fromData("mute")
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

    void saveCache() {
        YAMLFile data = YAMLCache.fromData("mute");
        int savesCount = 0;

        for (var entry : MuteCommand.MUTED_MAP.entrySet()) {
            final var task = entry.getValue();
            if (task == null) continue;

           final UUID uuid = entry.getKey();
            if (!task.isTemporary()) {
                data.set("data." + uuid, -1);
                savesCount++;
                continue;
            }

            data.set("data." + uuid, task.restTicksFromNow());
            savesCount++;
        }

        if (savesCount > 0) data.save();
    }
}
