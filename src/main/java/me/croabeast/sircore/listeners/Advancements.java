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

    public Advancements(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    private void sendAdvSection(Player player, String path, String type, String name, String desc) {
        String[] keys = {
                "{PLAYER}", "{ADV}", "{DESCRIPTION}", "{TYPE}", "{LOW-TYPE}", "{CAP-TYPE}"
        };
        String[] values = {
                player.getName() + "&r", name + "&r", desc + "&r", type + "&r",
                type.toLowerCase() + "&r", WordUtils.capitalizeFully(type) + "&r"
        };

        for (String line : text.fileList(main.getAdvances(), path)) {
            if (line == null || line.equals("")) continue;
            line = StringUtils.replaceEach(line, keys, values);

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
            new DiscordMsg(main, player, "advances", keys, values).sendMessage();
    }

    @EventHandler
    private void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (!main.getConfig().getBoolean("advances.enabled")) return;

        for (String s : main.getConfig().getStringList("advances.disabled.worlds"))
            if (s.equals(player.getWorld().getName())) return;

        for (String s : main.getConfig().getStringList("advances.disabled-modes")) {
            try {
                GameMode mode = GameMode.valueOf(s.toUpperCase());
                if (player.getGameMode() == mode) return;
            }
            catch (IllegalArgumentException ignored) {}
        }

        Advancement adv = event.getAdvancement();
        if (!main.getInitializer().getAdvancements().contains(adv)) return;

        ReflectKeys handler = main.getInitializer().getKeys().get(adv);
        String key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (main.getConfig().getStringList("advances.disabled-advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        Date date = player.getAdvancementProgress(adv).getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - 5 * 1000) return;

        String frameType = null, advName = null, description = null;

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
            }
            catch (IndexOutOfBoundsException e) {
                main.getRecorder().doRecord(
                        "&cError when getting the custom format of the advancement.",
                        "&7Localized error: &e" + e.getLocalizedMessage()
                );
                frameType = advName = description = null;
            }
        }

        if (advName == null) {
            if (handler.getTitle() == null) {
                String replacement = key.substring(key.lastIndexOf('/') + 1);
                replacement = StringUtils.replace(replacement, "_", " ");
                advName = WordUtils.capitalizeFully(replacement);
            }
            else advName = handler.getTitle();
        }
        if (frameType == null)
            frameType = handler.getFrameType() == null ? "PROGRESS" : handler.getFrameType();
        if (description == null)
            description = handler.getDescription() == null ? null : handler.getDescription();

        sendAdvSection(player, messageKey, frameType, advName, description);
    }
}
