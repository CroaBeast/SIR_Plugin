package me.croabeast.sir.plugin.module;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.DiscordHook;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.LangUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdvanceHandler extends ListenerModule {

    private static final YAMLData.Module DATA = YAMLData.Module.ADVANCEMENT;

    AdvanceHandler() {
        super("advancements");
    }

    private static List<String> advList(String path) {
        return DATA.fromName("config").toStringList("disabled-" + path);
    }

    @EventHandler
    private void onBukkit(PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        if (!isEnabled() || VanishHook.isVanished(player)) return;

        if (advList("worlds").contains(player.getWorld().getName()))
            return;

        for (String s : advList("modes")) {
            String g = s.toUpperCase(Locale.ENGLISH);

            try {
                if (player.getGameMode() == GameMode.valueOf(g))
                    return;
            } catch (Exception ignored) {}
        }

        final Advancement adv = event.getAdvancement();

        String key = adv.getKey().toString();
        if (!AdvanceLoader.ADVANCEMENT_KEYS.contains(key) ||
                advList("advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        AdvancementProgress p = player.getAdvancementProgress(adv);
        Date date = p.getDateAwarded(norms.get(norms.size() - 1));

        long now = System.currentTimeMillis();
        if (date != null && date.getTime() < now - 5 * 1000) return;

        String path = key.replaceAll("[/:]", ".");

        ConfigurationSection section = DATA.fromName("lang").getSection(path);
        if (section == null) return;

        String messagePath = section.getString("path");
        if (StringUtils.isBlank(messagePath)) return;

        BaseInfo info = new BaseInfo(section, adv);

        String[] keys = {
                "{adv}", "{description}", "{type}", "{low-type}",
                "{cap-type}", "{item}"
        },
                values = {
                        info.title, info.description, info.frame,
                        info.frame.toLowerCase(Locale.ENGLISH),
                        WordUtils.capitalizeFully(info.frame), info.item
                };

        List<String> mList = new ArrayList<>(), cList = new ArrayList<>();
        List<String> messages = DATA.fromName("messages").toStringList(messagePath);

        for (String s : messages) {
            Matcher m = Pattern.compile("(?i)^\\[cmd]").matcher(s);

            if (m.find()) {
                cList.add(TextUtils.STRIP_FIRST_SPACES.apply(s.substring(5)));
                continue;
            }
            mList.add(s);
        }

        Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
        players.add(player);

        MessageSender.loaded()
                .setTargets(players)
                .setParser(player)
                .addKeysValues(keys, values).send(mList);

        LangUtils.executeCommands(player, cList);

        if (DISCORD.isEnabled())
            DiscordHook.send("advances", player, keys, values);
    }

    static class BaseInfo {

        @NotNull
        private final String title, description, frame;
        private final String item;

        BaseInfo(ConfigurationSection section, Advancement adv) {
            final String key = adv.getKey().toString();

            String temp = key.substring(key.lastIndexOf('/') + 1);
            temp = temp.replace('_', ' ');

            char f = temp.toCharArray()[0];
            String first = (f + "").toUpperCase(Locale.ENGLISH);

            title = section.getString("name", first + temp.substring(1));

            description = section.getString("description", "No description.");
            frame = section.getString("frame", "TASK");

            ItemStack item = section.getItemStack("item");
            this.item = item == null ? null : item.getType().toString();
        }
    }
}
