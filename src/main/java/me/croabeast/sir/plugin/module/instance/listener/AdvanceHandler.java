package me.croabeast.sir.plugin.module.instance.listener;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.misc.CustomListener;
import me.croabeast.sir.plugin.Initializer;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.utility.LangUtils;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvanceHandler extends SIRModule implements CustomListener {

    public AdvanceHandler() {
        super(ModuleName.ADVANCEMENTS);
    }

    @Override
    public void registerModule() {
        register();
    }

    private static List<String> advList(String path) {
        return FileCache.ADVANCE_CACHE.getConfig().toList("disabled-" + path);
    }

    @EventHandler
    private void onBukkit(PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        if (!isEnabled()) return;

        if (advList("worlds").contains(player.getWorld().getName()))
            return;

        for (String s : advList("modes")) {
            String g = s.toUpperCase(Locale.ENGLISH);

            try {
                if (player.getGameMode() == GameMode.valueOf(g))
                    return;
            } catch (Exception ignored) {}
        }

        Advancement adv = event.getAdvancement();
        if (!Initializer.ADV_LIST.contains(adv)) return;

        String key = adv.getKey().toString();

        if (key.contains("root") || key.contains("recipes")) return;
        if (advList("advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        AdvancementProgress p = player.getAdvancementProgress(adv);
        Date date = p.getDateAwarded(norms.get(norms.size() - 1));

        long now = System.currentTimeMillis();
        if (date != null && date.getTime() < now - 5 * 1000) return;

        String path = key.replaceAll("[/:]", ".");

        ConfigurationSection section = FileCache.ADVANCE_CACHE.getCache("lang").getSection(path);
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

        List<String> messages = FileCache.ADVANCE_CACHE.getCache("messages").toList(messagePath),
                mList = new ArrayList<>(),
                cList = new ArrayList<>();

        for (String s : messages) {
            Matcher m = Pattern.compile("(?i)^\\[cmd]").matcher(s);

            if (m.find()) {
                cList.add(TextUtils.STRIP_FIRST_SPACES.apply(s.substring(5)));
                continue;
            }
            mList.add(s);
        }

        MessageSender.fromLoaded().
                setTargets(Bukkit.getOnlinePlayers()).
                setParser(player).
                setKeys(keys).setValues(values).send(mList);

        LangUtils.executeCommands(player, cList);

        if (!Initializer.hasDiscord()) return;

        DiscordSender sender = new DiscordSender(player, "advances");
        sender.setKeys(keys).setValues(values).send();
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

            ItemStack item = section.getItemStack("item");
            this.item = item == null ? null : item.getType().toString();
        }
    }
}
