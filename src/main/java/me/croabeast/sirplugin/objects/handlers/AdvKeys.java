package me.croabeast.sirplugin.objects.handlers;

import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class AdvKeys {

    private final Advancement adv;
    @Nullable private String title, description, frameType;

    public AdvKeys(Advancement adv) {
        this.adv = adv;
        registerKeys();
    }

    private final int MAJOR_VERSION =
            Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);

    @Nullable
    private Class<?> getNMSClass(String start, String name, boolean hasVersion) {
        String version = Bukkit.getServer().getClass().getPackage().
                getName().split("\\.")[3];
        try {
            return Class.forName((start != null ? start : "net.minecraft.server" ) +
                    (hasVersion ? "." + version : "") + "." + name);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private Object getObject(Class<?> clazz, Object initial, String method) {
        try {
            return (clazz != null ? clazz : initial.getClass()).
                    getDeclaredMethod(method).invoke(initial);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private Object getObject(Object initial, String method) {
        return getObject(null, initial, method);
    }

    private void registerKeys() {
        Class<?> craftClass = getNMSClass("org.bukkit.craftbukkit",
                "advancement.CraftAdvancement", true);
        if (craftClass == null) return;

        Object craftAdv = craftClass.cast(adv);
        Object advHandle = getObject(craftClass, craftAdv, "getHandle");
        if (advHandle == null) return;

        Object craftDisplay = getObject(advHandle, "c");
        if (craftDisplay == null) return;

        Object frameType = getObject(craftDisplay, "e");
        Object chatComponentTitle = getObject(craftDisplay, "a");
        Object chatComponentDesc = getObject(craftDisplay, "b");
        if (frameType == null || chatComponentTitle == null ||
                chatComponentDesc == null) return;

        Class<?> chatClass = MAJOR_VERSION >= 17 ?
                getNMSClass("net.minecraft.network.chat",
                        "IChatBaseComponent", false) :
                getNMSClass(null, "IChatBaseComponent", true);
        if (chatClass == null) return;

        String method = MAJOR_VERSION < 13 ? "toPlainText" : "getString";
        Object title = getObject(chatClass, chatComponentTitle, method);
        Object description = getObject(chatClass, chatComponentDesc, method);
        if (title == null || description == null) return;

        this.frameType = frameType.toString();
        this.title = title.toString();
        this.description = description.toString();
    }

    @NotNull
    public String getFrameType() {
        return frameType == null ? "PROGRESS" : frameType;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    private String removeLiteralChars(String input) {
        return input.replaceAll("\\\\Q", "").
                replaceAll("\\\\E", "");
    }

    @NotNull
    public String getDescription(boolean append) {
        return getDescription(append, 24);
    }

    @NotNull
    public String getDescription(boolean append, int charsLength) {
        if (description == null) return "No description.";

        String desc = description.replaceAll("[\\n]", " ");
        if (!append) return desc;

        StringTokenizer tok = new StringTokenizer(desc, " ");
        StringBuilder output = new StringBuilder(desc.length());

        int lineLen = 0;
        String delimiter = TextUtils.lineSplitter();

        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();
            int i = charsLength - lineLen;

            while (word.length() > charsLength) {
                output.append(word, 0, i).append(delimiter);
                word = word.substring(i);
                lineLen = 0;
            }

            if (lineLen + word.length() > charsLength) {
                output.append(delimiter);
                lineLen = 0;
            }

            output.append(word).append(" ");
            lineLen += word.length() + 1;
        }

        String format = removeLiteralChars(output.toString());
        StringBuilder build = new StringBuilder();

        for (String word : format.split(delimiter))
            build.append(word, 0, word.length() - 1).append(delimiter);

        format = build.substring(0, build.length() - delimiter.length());
        return removeLiteralChars(format);
    }
}
