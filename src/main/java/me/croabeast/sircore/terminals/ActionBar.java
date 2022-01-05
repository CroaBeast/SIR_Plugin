package me.croabeast.sircore.terminals;

import me.croabeast.sircore.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;

import java.lang.reflect.*;

public class ActionBar implements Reflection {

    private final GetActionBar actionBar;

    public ActionBar(Application main) {
        actionBar = main.MC_VERSION < 11 ? oldActionBar() : newActionBar();
    }

    public interface GetActionBar {
        void send(Player player, String message);
    }

    public GetActionBar getMethod() {
        return actionBar;
    }

    private GetActionBar oldActionBar() {
        return (player, message) -> {
            try {
                Constructor<?> constructor = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
                Object icbc = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
                Object packet = constructor.newInstance(icbc, (byte) 2);
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
                playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private GetActionBar newActionBar() {
        return (player, message) ->
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
