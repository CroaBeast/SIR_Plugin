package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.hooks.*;
import me.croabeast.sircore.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;

public class Advancements implements Listener {

    private final Application main;
    private final TextUtils text;
    private final EventUtils utils;

    private String frameType, advName, description;

    String[] keys = {
            "{PLAYER}", "{ADV}", "{DESCRIPTION}",
            "{TYPE}", "{LOW-TYPE}", "{CAP-TYPE}"
    };

    public Advancements(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    private String[] getValues(Player player) {
        return new String[] {
                player.getName() + "&r", advName + "&r", description + "&r",
                frameType + "&r", frameType.toLowerCase() + "&r",
                WordUtils.capitalizeFully(frameType) + "&r"
        };
    }

    private void sendAdvSection(Player player, String path) {
        for (String line : text.fileList(main.getAdvances(), path)) {
            if (line == null || line.equals("")) continue;
            line = StringUtils.replaceEach(line, keys, getValues(player));

            if (text.getOption(1, "send-console") &&
                    !line.startsWith("[CMD]")) main.getRecorder().doRecord(line);

            if (line.startsWith("[CMD]")) {
                String cmd = utils.parsePrefix("[CMD]", line.replace("&r", ""));
                boolean isLine = cmd.startsWith("[PLAYER]");
                cmd = isLine ? utils.parsePrefix("[PLAYER]", cmd) : cmd;
                Bukkit.dispatchCommand(isLine ? player : Bukkit.getConsoleSender(), cmd);
            }
            else {
                String result = text.colorize(player, line);
                if (line.startsWith("[PLAYER]"))
                    utils.typeMessage(player, utils.parsePrefix("[PLAYER]", result));
                else main.everyPlayer().forEach(p -> utils.typeMessage(p, result));
            }
        }

        if (main.getInitializer().DISCORD && main.getInitializer().discordServer() != null)
            new DiscordMsg(main, player, "advances", keys, getValues(player)).sendMessage();
    }

    @EventHandler
    private void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (!main.getConfig().getBoolean("advances.enabled")) return;

        for (String s : main.getConfig().getStringList("advances.disabled.worlds"))
            if (s.equals(player.getWorld().getName())) return;

        for (String s : main.getConfig().getStringList("advances.disabled-modes")) {
            try { if (player.getGameMode() == GameMode.valueOf(s.toUpperCase())) return; }
            catch (IllegalArgumentException ignored) {}
        }

        Advancement adv = event.getAdvancement();
        String key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (main.getConfig().getStringList("advances.disabled-advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        AdvancementProgress progress = player.getAdvancementProgress(adv);
        if (norms.isEmpty()) return;

        Date date = progress.getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - 5 * 1000) return;

        String messageKey = main.getAdvances().getString(text.stringKey(key));
        if (messageKey == null) return;

        if (messageKey.contains("-(")) {
            try {
                String split = messageKey.split("-\\(")[1];
                split = split.substring(0, split.lastIndexOf(')'));
                String[] format = split.split("::");
                if (format.length > 0 && format.length < 4) {
                    advName = format[0];
                    description = format.length == 2 ? format[1] : null;
                    frameType = format.length == 3 ? format[2] : null;
                }
                messageKey = messageKey.split("-\\(")[0];
            } catch (IndexOutOfBoundsException e) {
                main.getRecorder().doRecord(
                        "&cError when getting the custom format of the advancement.",
                        "&7Localized error: &e" + e.getLocalizedMessage()
                );
                frameType = advName = description = null;
            }
        }

        ReflectKeys handler = new ReflectKeys(adv);
        if (frameType == null) frameType = handler.getFrameType();
        if (advName == null) advName = handler.getTitle();
        if (description == null) description = handler.getDescription();

        sendAdvSection(player, messageKey);
    }

    public static class ReflectKeys {

        private final Advancement adv;
        private String title, description, frameType;

        public ReflectKeys(Advancement adv) {
            this.adv = adv;
            registerKeys();
        }

        private final int MAJOR_VERSION =
                Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);

        private Class<?> getNMSClass(String start, String name, boolean hasVersion) {
            String version = Bukkit.getServer().getClass().getPackage().
                    getName().split("\\.")[3];
            try {
                return Class.forName((start != null ? start : "net.minecraft.server" ) +
                        (hasVersion ? "." + version : "") + "." + name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private Object getObject(Class<?> clazz, Object initial, String method) {
            try {
                return (clazz != null ? clazz : initial.getClass()).
                        getDeclaredMethod(method).invoke(initial);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private Object getObject(Object initial, String method) {
            return getObject(null, initial, method);
        }

        private void registerKeys() {
            Class<?> craftClass = getNMSClass(
                    "org.bukkit.craftbukkit",
                    "advancement.CraftAdvancement",
                    true);
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

        public String getFrameType() {
            return frameType;
        }
        public String getTitle() {
            return title;
        }
        public String getDescription() {
            return description;
        }
    }
}
