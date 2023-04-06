package me.croabeast.sirplugin.task.ignore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@RequiredArgsConstructor
@Getter
public class IgnoreSettings implements ConfigurationSerializable {

    private final DoubleObject msgCache = new DoubleObject();
    private final DoubleObject chatCache = new DoubleObject();

    private final UUID uuid;

    IgnoreSettings(Player player) {
        this.uuid = player.getUniqueId();
    }

    @NotNull
    public Map<String, Object> serialize() {
        var data = new HashMap<String, Object>();

        data.put("uuid", uuid.toString());

        data.put("chat.for-all", chatCache.forAll);
        data.put("chat.list", chatCache.storedIds);

        data.put("msg.for-all", msgCache.forAll);
        data.put("msg.list", msgCache.storedIds);

        return data;
    }

    @SuppressWarnings("unchecked")
    public static IgnoreSettings deserialize(Map<String, Object> args) {
        var u = args.get("uuid");
        if (u == null) throw new NullPointerException();

        UUID uuid;
        try {
            uuid = UUID.fromString((String) u);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        var setting = new IgnoreSettings(uuid);

        var chatAll = args.get("chat.for-all");
        var chatList = args.get("chat.list");

        var msgAll = args.get("msg.for-all");
        var msgList = args.get("msg.list");

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
