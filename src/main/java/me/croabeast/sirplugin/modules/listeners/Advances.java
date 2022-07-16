package me.croabeast.sirplugin.modules.listeners;

import me.croabeast.advancementinfo.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.Transmitter;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.advancement.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static me.croabeast.sirplugin.utilities.LangUtils.*;

public class Advances extends SIRViewer {

    @Override
    public @NotNull Identifier getIdentifier() {
        return Identifier.ADVANCES;
    }

    private List<String> advList(String path) {
        return FileCache.MODULES.get().getStringList("advancements.disabled-" + path);
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

        AdvancementInfo info = Initializer.getKeys().get(adv);
        String key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (!advList("advs").isEmpty() && advList("advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        Date date = player.getAdvancementProgress(adv).getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - 5 * 1000) return;

        String frameType = null, advName = null, description = null;

        String messageKey = FileCache.ADVANCES.get().getString(stringKey(key));
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

        Transmitter.to(FileCache.ADVANCES.get(), messageKey)
                .setKeys("{player}", "{adv}", "{description}", "{type}", "{low-type}", "{cap-type}")
                .setValues(
                        player.getName() + "&r", advName + "&r", description + "&r", frameType + "&r",
                        frameType.toLowerCase() + "&r", WordUtils.capitalizeFully(frameType) + "&r"
                )
                .display(player);
    }
}
