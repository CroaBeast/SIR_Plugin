package me.croabeast.sirplugin.module.listener;

import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.Initializer;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.hook.DiscordSender;
import me.croabeast.sirplugin.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LangUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AdvanceHandler extends SIRViewer {

    public AdvanceHandler() {
        super("advances");
    }

    private static List<String> advList(String path) {
        return FileCache.MODULES.toList("advancements.disabled-" + path);
    }

    @EventHandler
    private void onBukkit(PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        if (!isEnabled()) return;

        if (advList("worlds").contains(player.getWorld().getName()))
            return;

        for (var s : advList("modes")) {
            String g = s.toUpperCase(Locale.ENGLISH);

            try {
                if (player.getGameMode() == GameMode.valueOf(g))
                    return;
            } catch (Exception ignored) {}
        }

        var adv = event.getAdvancement();
        if (!Initializer.ADV_LIST.contains(adv)) return;

        var key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (advList("advs").contains(key)) return;

        var norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        var p = player.getAdvancementProgress(adv);
        var date = p.getDateAwarded(norms.get(norms.size() - 1));

        var now = System.currentTimeMillis();
        if (date != null && date.getTime() < now - 5 * 1000) return;

        var path = key.replaceAll("[/:]", ".");

        var section = FileCache.ADVANCE_LANG.getSection(path);
        if (section == null) return;

        String messagePath = section.getString("path");
        if (StringUtils.isBlank(messagePath)) return;

        var info = new BaseInfo(section, adv);

        String[] keys = {
                        "{adv}", "{description}", "{type}", "{low-type}",
                        "{cap-type}", "{item}"
                },
                values = {
                        info.title, info.description, info.frame,
                        info.frame.toLowerCase(Locale.ENGLISH),
                        WordUtils.capitalizeFully(info.frame), info.item
                };

        List<String> messages = FileCache.ADVANCE_CONFIG.toList(messagePath),
                mList = new ArrayList<>(),
                cList = new ArrayList<>();

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

    static class BaseInfo {

        private final String title, description, frame, item;

        BaseInfo(ConfigurationSection section, Advancement adv) {
            final String key = adv.getKey().toString();

            String temp = key.substring(key.lastIndexOf('/') + 1);
            temp = temp.replace('_', ' ');

            char f = temp.toCharArray()[0];
            String first = (f + "").toUpperCase(Locale.ENGLISH);

            title = section.getString("name", first + temp.substring(1));

            description = section.getString("description", "No description.");
            frame = section.getString("frame", "TASK");

            var item = section.getItemStack("item");
            this.item = item == null ? null : item.getType().toString();
        }
    }
}
