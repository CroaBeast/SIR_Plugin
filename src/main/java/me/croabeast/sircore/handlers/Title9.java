package me.croabeast.sircore.handlers;

import me.croabeast.sircore.interfaces.Reflection;
import me.croabeast.sircore.interfaces.TitleMain;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public class Title9 implements TitleMain, Reflection {

    private void titleSubtitle(Player player, String message, boolean isTitle) {
        try {
            Object e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
            Object chatMessage = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
            Constructor<?> subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            Object titlePacket = subtitleConstructor.newInstance(e, chatMessage, 20, 3 * 20, 20);

            sendPacket(player, titlePacket);

            e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField((isTitle ? "" : "SUB") + "TITLE").get(null);
            chatMessage = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
            subtitleConstructor = isTitle ?
                    getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent")) :
                    getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            titlePacket = isTitle ? subtitleConstructor.newInstance(e, chatMessage) : subtitleConstructor.newInstance(e, chatMessage, 1, 3 ,1);

            sendPacket(player, titlePacket);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void send(Player player, String title, String subtitle) {
        titleSubtitle(player, title, true);
        titleSubtitle(player, subtitle, false);
    }
}
