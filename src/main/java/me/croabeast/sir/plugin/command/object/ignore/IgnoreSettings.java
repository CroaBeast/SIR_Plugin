package me.croabeast.sir.plugin.command.object.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@RequiredArgsConstructor
@Getter
public class IgnoreSettings implements ConfigurationSerializable {

    static {
        ConfigurationSerialization.registerClass(IgnoreSettings.class);
    }

    private final DoubleObject msgCache = new DoubleObject();
    private final DoubleObject chatCache = new DoubleObject();

    private final UUID uuid;

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

    @SuppressWarnings("unchecked")
    public static IgnoreSettings deserialize(Map<String, Object> args) {
        Object u = args.get("uuid");
        if (u == null) throw new NullPointerException();

        UUID uuid;
        try {
            uuid = UUID.fromString((String) u);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        IgnoreSettings setting = new IgnoreSettings(uuid);

        Object chatAll = args.get("chat.for-all");
        Object chatList = args.get("chat.list");

        Object msgAll = args.get("msg.for-all");
        Object msgList = args.get("msg.list");

        if (chatAll != null)
            setting.chatCache.setForAll((Boolean) chatAll);
        if (chatList != null)
            setting.chatCache.storedIds = (List<String>) chatList;

        if (msgAll != null)
            setting.msgCache.setForAll((Boolean) msgAll);
        if (msgList != null)
            setting.msgCache.storedIds = (List<String>) msgList;

        return setting;
    }

    public static class DoubleObject {

        private List<String> storedIds = new ArrayList<>();
        @Getter
        private boolean forAll = false;

        public DoubleObject setForAll(boolean b) {
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
