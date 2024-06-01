package me.croabeast.sir.plugin.command.ignore;

import lombok.Getter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.plugin.DataHandler;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unchecked")
public class IgnoreSettings implements ConfigurationSerializable, DataHandler {

    private final Entry msgCache = new Entry();
    private final Entry chatCache = new Entry();

    private final UUID uuid;

    @Priority(4)
    static void loadData() {
        ConfigurationSerialization.registerClass(IgnoreSettings.class);
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
