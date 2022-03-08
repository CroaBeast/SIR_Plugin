package me.croabeast.sirplugin.objects.handlers;

import me.croabeast.sirplugin.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;

import java.lang.reflect.*;

public class ActionBar implements Reflection {

    private final Method actionBar;

    public ActionBar() {
        actionBar = SIRPlugin.MAJOR_VERSION < 11 ? oldActionBar() : newActionBar();
    }

    public interface Method {
        void send(Player player, String message);
    }

    public Method getMethod() {
        return actionBar;
    }

    private Method oldActionBar() {
        return (player, message) -> {
            try {
                Class<?> chat = getNMSClass("IChatBaseComponent");
                Constructor<?> c = getNMSClass("PacketPlayOutChat").getConstructor(chat, byte.class);

                Object icbc = chat.getDeclaredClasses()[0].getMethod("a", String.class).
                        invoke(null, "{\"text\":\"" + message + "\"}");

                Object p = player.getClass().getMethod("getHandle").invoke(player);
                Object conn = p.getClass().getField("playerConnection").get(p);

                conn.getClass().getMethod("sendPacket", getNMSClass("Packet")).
                        invoke(conn, c.newInstance(icbc, (byte) 2));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Method newActionBar() {
        return (player, message) ->
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}