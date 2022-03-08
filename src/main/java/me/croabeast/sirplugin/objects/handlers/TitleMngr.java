package me.croabeast.sirplugin.objects.handlers;

import me.croabeast.sirplugin.*;
import org.bukkit.entity.*;

import java.lang.reflect.*;

public class TitleMngr implements Reflection {

    private final Method title;
    private int in, stay, out;

    public TitleMngr() {
        title = SIRPlugin.MAJOR_VERSION < 10 ? oldTitle() : newTitle();
    }

    public interface Method {
        void send(Player player, String title, String subtitle, int in, int stay, int out);
    }

    public Method getMethod() {
        return title;
    }

    private void legacyMethod(Player player, String message, boolean isTitle) {
        try {
            Class<?> chat = getNMSClass("IChatBaseComponent"), pack = getNMSClass("PacketPlayOutTitle");

            Object e = pack.getDeclaredClasses()[0].getField("TIMES").get(null);
            Object chatMessage = chat.getDeclaredClasses()[0].getMethod("a", String.class).
                    invoke(null, "{\"text\":\"" + message + "\"}");

            Constructor<?> subtitleConstructor = pack.getConstructor(pack.getDeclaredClasses()[0],
                    chat, int.class, int.class, int.class);
            Object titlePacket = subtitleConstructor.newInstance(e, chatMessage, in, stay, out);

            sendPacket(player, titlePacket);

            e = pack.getDeclaredClasses()[0].getField((isTitle ? "" : "SUB") + "TITLE").get(null);
            chatMessage = chat.getDeclaredClasses()[0].getMethod("a", String.class).
                    invoke(null, "{\"text\":\"" + message + "\"}");

            int in = Math.round((float) this.in / 20),
                    stay = Math.round((float) this.stay / 20),
                    out = Math.round((float) this.out / 20);

            subtitleConstructor = isTitle ? pack.getConstructor(pack.getDeclaredClasses()[0], chat) :
                    pack.getConstructor(pack.getDeclaredClasses()[0], chat, int.class, int.class, int.class);
            titlePacket = isTitle ? subtitleConstructor.newInstance(e, chatMessage) :
                    subtitleConstructor.newInstance(e, chatMessage, in, stay, out);

            sendPacket(player, titlePacket);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Method oldTitle() {
        return (player, title, subtitle, in, stay, out) -> {
            this.in = in;
            this.stay = stay;
            this.out = out;
            legacyMethod(player, title, true);
            legacyMethod(player, subtitle, false);
        };
    }

    public Method newTitle() {
        return Player::sendTitle;
    }
}
