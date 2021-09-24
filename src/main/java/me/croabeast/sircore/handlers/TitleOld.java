package me.croabeast.sircore.handlers;

import me.croabeast.sircore.interfaces.Reflection;
import me.croabeast.sircore.interfaces.TitleMain;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public class TitleOld implements TitleMain, Reflection {

    private void title(Player player, String title) {
        try {
            Object e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
            Object chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, "{\"text\":\"" + title + "\"}");
            Constructor<?> subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            Object titlePacket = subtitleConstructor.newInstance(e, chatTitle, 20, 3 * 20, 20);

            sendPacket(player, titlePacket);

            e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
            chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, "{\"text\":\"" + title + "\"}");
            subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"));
            titlePacket = subtitleConstructor.newInstance(e, chatTitle);

            sendPacket(player, titlePacket);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void subtitle(Player player, String subtitle) {
        try {
            Object e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get(null);
            Object chatSubtitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, "{\"text\":\"" + subtitle + "\"}");
            Constructor<?> subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            Object subtitlePacket = subtitleConstructor.newInstance(e, chatSubtitle, 20, 3 * 20, 20);

            sendPacket(player, subtitlePacket);

            e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);
            chatSubtitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, "{\"text\":\"" + subtitle + "\"}");
            subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);
            subtitlePacket = subtitleConstructor.newInstance(e, chatSubtitle, 1, 3 ,1);

            sendPacket(player, subtitlePacket);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void send(Player player, String title, String subtitle) {
        title(player, title); subtitle(player, subtitle);
    }
}
