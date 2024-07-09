package me.croabeast.sir.plugin.command.ignore;

import lombok.Getter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.file.YAMLData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@ApiStatus.ScheduledForRemoval(inVersion = "4.0")
@Deprecated
@SuppressWarnings("all")
public class IgnoreSettings implements ConfigurationSerializable, DataHandler {

    private final Entry msgCache = new Entry();
    private final Entry chatCache = new Entry();

    private final UUID uuid;

    private static boolean alreadyUpdated = false;
    private static boolean registered = false;

    @Priority(4)
    static void loadData() {
        if (!registered) {
            ConfigurationSerialization.registerClass(IgnoreSettings.class);
            registered = true;
        }

        if (alreadyUpdated) return;

        ConfigurableFile file = YAMLData.Command.Multi.IGNORE.from(false);

        ConfigurationSection data = file.getSection("data");
        if (data == null) {
            alreadyUpdated = true;
            return;
        }

        Set<String> keys = data.getKeys(false);
        if (keys.isEmpty() || keys.stream()
                .noneMatch(s ->
                        data.get(s, null) instanceof IgnoreSettings)
        ) {
            alreadyUpdated = true;
            return;
        }

        int saveCount = 0;
        for (final String key : keys) {
            Object loaded = data.get(key, null);

            if (!(loaded instanceof IgnoreSettings))
                continue;

            data.set(key, null);
            IgnoreSettings settings = (IgnoreSettings) loaded;

            List<String> msg = settings.msgCache.serialize();
            List<String> chat = settings.chatCache.serialize();

            boolean msgAll = settings.msgCache.forAll;
            boolean chatAll = settings.chatCache.forAll;

            ConfigurationSection s = data.createSection(key);

            s.set("msg.list", msg);
            s.set("msg.for-all", msgAll);

            s.set("chat.list", chat);
            s.set("chat.for-all", chatAll);

            saveCount++;
        }

        if (saveCount > 0) {
            file.save();
            alreadyUpdated = true;
        }
    }

    public IgnoreSettings(Map<String, Object> args) {
        Object u = args.get("uuid");
        if (u == null) throw new NullPointerException();

        try {
            uuid = UUID.fromString((String) u);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        Object chatAll = args.get("chat.for-all");
        Object chatList = args.get("chat.list");

        Object msgAll = args.get("msg.for-all");
        Object msgList = args.get("msg.list");

        if (chatAll != null)
            setForAll(true, (Boolean) chatAll);
        if (chatList != null)
            chatCache.storedIds = CollectionBuilder.of((List<String>) chatList).map(UUID::fromString).toSet();

        if (msgAll != null)
            setForAll(false, (Boolean) msgAll);
        if (msgList != null)
            msgCache.storedIds = CollectionBuilder.of((List<String>) msgList).map(UUID::fromString).toSet();
    }

    IgnoreSettings(Player player) {
        this.uuid = player.getUniqueId();
    }

    public Set<UUID> getCache(boolean isChat) {
        return (!isChat ? msgCache : chatCache).storedIds;
    }

    public boolean isForAll(boolean isChat) {
        return (!isChat ? msgCache : chatCache).isForAll();
    }

    public void setForAll(boolean isChat, boolean value) {
        (!isChat ? msgCache : chatCache).forAll = value;
    }

    @NotNull
    public Map<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();

        data.put("uuid", uuid.toString());

        data.put("chat.for-all", chatCache.forAll);
        data.put("chat.list", chatCache.serialize());

        data.put("msg.for-all", msgCache.forAll);
        data.put("msg.list", msgCache.serialize());

        return data;
    }

    public static IgnoreSettings valueOf(Map<String, Object> args) {
        return new IgnoreSettings(args);
    }

    public static IgnoreSettings deserialize(Map<String, Object> args) {
        return valueOf(args);
    }

    static class Entry {

        private Set<UUID> storedIds = new HashSet<>();
        @Getter
        private boolean forAll = false;

        List<String> serialize() {
            return CollectionBuilder.of(storedIds).map(UUID::toString).toList();
        }
    }
}
