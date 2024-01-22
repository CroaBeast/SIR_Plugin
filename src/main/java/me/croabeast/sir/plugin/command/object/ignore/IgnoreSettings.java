package me.croabeast.sir.plugin.command.object.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@RequiredArgsConstructor
@Getter
@SuppressWarnings("unchecked")
public class IgnoreSettings implements ConfigurationSerializable {

    private final Entry msgCache = new Entry();
    private final Entry chatCache = new Entry();

    private final UUID uuid;

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
            chatCache.setForAll((Boolean) chatAll);
        if (chatList != null)
            chatCache.storedIds = new HashSet<>((List<String>) chatList);

        if (msgAll != null)
            msgCache.setForAll((Boolean) msgAll);
        if (msgList != null)
            msgCache.storedIds = new HashSet<>((List<String>) msgList);
    }

    IgnoreSettings(Player player) {
        this.uuid = player.getUniqueId();
    }

    @NotNull
    public Map<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<>();

        data.put("uuid", uuid.toString());

        data.put("chat.for-all", chatCache.forAll);
        data.put("chat.list", chatCache.storedIds);

        data.put("msg.for-all", msgCache.forAll);
        data.put("msg.list", msgCache.storedIds);

        return data;
    }

    public static IgnoreSettings valueOf(Map<String, Object> args) {
        return new IgnoreSettings(args);
    }

    public static IgnoreSettings deserialize(Map<String, Object> args) {
        return valueOf(args);
    }

    public static class Entry {

        private Set<String> storedIds = new HashSet<>();
        @Getter
        private boolean forAll = false;

        public Entry setForAll(boolean b) {
            forAll = b;
            return this;
        }

        public boolean remove(Player player) {
            return storedIds.remove(player.getUniqueId() + "");
        }

        public boolean add(Player player) {
            return storedIds.add(player.getUniqueId() + "");
        }

        public boolean contains(Player player) {
            return storedIds.contains(player.getUniqueId() + "");
        }
    }
}
