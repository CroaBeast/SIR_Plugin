package me.croabeast.sirplugin.module.listener;

import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.LogUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static me.croabeast.sirplugin.utility.LangUtils.stringKey;

public class AdvanceHandler extends SIRViewer {

    public AdvanceHandler() {
        super("advances");
    }

    private List<String> advList(String path) {
        return FileCache.MODULES.toList("advancements.disabled-" + path);
    }

    @EventHandler
    private void onDone(PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        if (!isEnabled()) return;

        var worlds = advList("worlds");
        if (!worlds.isEmpty() && worlds.contains(player.getWorld().getName())) return;

        var modes = advList("modes");
        if (!modes.isEmpty()) {
            for (var s : modes) {
                try {
                    if (player.getGameMode() == GameMode.valueOf(
                            s.toUpperCase(Locale.ENGLISH))) return;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        var adv = event.getAdvancement();
        if (!Initializer.getAdvancements().contains(adv)) return;

        var info = Initializer.getKeys().getOrDefault(adv, null);
        var key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (!advList("advs").isEmpty() && advList("advs").contains(key)) return;

        var norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        var date = player.getAdvancementProgress(adv).getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - 5 * 1000) return;

        String frameType = null, advName = null, description = null;

        var messageKey = FileCache.ADVANCE_LANG.getValue(stringKey(key), String.class);
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
            String replacement = key.substring(key.lastIndexOf('/') + 1);
            replacement = StringUtils.replace(replacement, "_", " ");
            advName = info == null || info.getTitle() == null ?
                    WordUtils.capitalizeFully(replacement) : info.getTitle();
        }

        if (frameType == null)
            frameType = info == null ? "PROGRESS" : info.getFrameType();
        if (description == null)
            description = info == null ? "No description." : info.getDescription();

        String[] keys = {
                        "{player}", "{adv}", "{description}",
                        "{type}", "{low-type}", "{cap-type}"
                },
                values = {
                        player.getName(), advName,
                        description, frameType,
                        frameType.toLowerCase(Locale.ENGLISH),
                        WordUtils.capitalizeFully(frameType)
                };

        if (messageKey.matches("(?i)null")) return;

        System.out.println(Arrays.toString(keys));
        System.out.println(Arrays.toString(values));

        List<String> messages = FileCache.ADVANCE_CONFIG.toList(messageKey),
                mList = new ArrayList<>(), cList = new ArrayList<>();

        for (var s : messages) {
            var m = Pattern.compile("(?i)^\\[cmd]").matcher(s);

            if (m.find()) {
                cList.add(TextUtils.STRIP_FIRST_SPACES.apply(s.substring(5)));
                continue;
            }
            mList.add(s);
        }

        LangUtils.getSender().setTargets(Bukkit.getOnlinePlayers()).
                setParser(player).
                setKeys(keys).setValues(values).send(mList);

        LangUtils.executeCommands(player, cList);

        if (Initializer.hasDiscord())
            new DiscordSender(player, "advances").setKeys(keys).setValues(values).send();
    }
}
