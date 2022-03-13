package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.hooks.*;
import me.croabeast.sirplugin.modules.*;
import me.croabeast.sirplugin.objects.handlers.TextParser;
import me.croabeast.sirplugin.objects.handlers.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class Advances extends BaseModule implements Listener {

    private final SIRPlugin main;

    public Advances(SIRPlugin main) {
        this.main = main;
    }

    @Override
    public Identifier getIdentifier() {
        return Identifier.ADVANCES;
    }

    @Override
    public void registerModule() {
        SIRPlugin.registerListener(this);
    }

    private void sendAdvSection(Player player, String path, String type, String name, String desc) {
        String[] keys = {"player", "adv", "description", "type", "low-type", "cap-type"};
        String[] values = {
                player.getName() + "&r", name + "&r", desc + "&r", type + "&r",
                type.toLowerCase() + "&r", WordUtils.capitalizeFully(type) + "&r"
        };

        List<String> messages = fileList(main.getAdvances(), path);
        if (messages.isEmpty()) return;

        for (String line : messages) {
            if (line == null || line.equals("")) continue;

            line = parseInsensitiveEach(line, keys, values);
            line = parseInsensitiveEach(line, "world", player.getWorld().getName());

            if (main.getConfig().getBoolean("options.send-console") &&
                    !isStarting("[cmd]", line)) LogUtils.doLog(line);

            if (isStarting("[cmd]", line)) {
                String cmd = parsePrefix("cmd", line.replace("&r", ""));
                boolean isLine = isStarting("[player]", cmd);

                cmd = isLine ? parsePrefix("player", cmd) : cmd;
                Bukkit.dispatchCommand(isLine ? player : Bukkit.getConsoleSender(), cmd);
            }
            else TextParser.send(null, player, line);
        }

        if (Initializer.hasDiscord())
            new Message(main, player, "advances", keys, values).sendMessage();
    }

    private List<String> advList(String path) {
        return main.getModules().getStringList("advancements.disabled-" + path);
    }

    @EventHandler
    private void onDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (!isEnabled()) return;

        if (!advList("worlds").isEmpty() &&
                advList("worlds").contains(player.getWorld().getName())) return;

        if (!advList("modes").isEmpty()) {
            for (String s : advList("modes")) {
                try {
                    if (player.getGameMode() == GameMode.valueOf(s.toUpperCase())) return;
                }
                catch (IllegalArgumentException ignored) {}
            }
        }

        Advancement adv = event.getAdvancement();
        if (!Initializer.getAdvancements().contains(adv)) return;

        AdvKeys handler = Initializer.getKeys().get(adv);
        String key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (!advList("advs").isEmpty() && advList("advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        Date date = player.getAdvancementProgress(adv).getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - 5 * 1000) return;

        String frameType = null, advName = null, description = null;

        String messageKey = main.getAdvances().getString(stringKey(key));
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
                LogUtils.doLog(
                        "&cError when getting the custom format of the advancement.",
                        "&7Localized error: &e" + e.getLocalizedMessage()
                );
                frameType = advName = description = null;
            }
        }

        if (advName == null) {
            if (handler == null) {
                String replacement = key.substring(key.lastIndexOf('/') + 1);
                replacement = StringUtils.replace(replacement, "_", " ");
                advName = WordUtils.capitalizeFully(replacement);
            }
            else advName = handler.getTitle();
        }
        if (frameType == null)
            frameType = handler == null ? "PROGRESS" : handler.getFrameType();
        if (description == null)
            description = handler == null ? null : handler.getDescription();

        sendAdvSection(player, messageKey, frameType, advName, description);
    }
}
