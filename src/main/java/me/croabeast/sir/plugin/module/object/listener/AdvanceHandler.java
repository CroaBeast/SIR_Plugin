package me.croabeast.sir.plugin.module.object.listener;

import lombok.var;
import me.croabeast.advancementinfo.AdvancementInfo;
import me.croabeast.advancementinfo.FrameType;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.misc.CollectionBuilder;
import me.croabeast.beanslib.utility.LibUtils;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.CacheHandler;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.hook.DiscordSender;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.module.ModuleName;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.world.WorldRule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvanceHandler extends ModuleListener implements CacheHandler {

    private static final Map<FrameType, Set<AdvancementInfo>> ADV_INFO_MAP = new HashMap<>();

    private static final List<Advancement> ADV_LIST =
            CollectionBuilder.of(Bukkit.advancementIterator())
                    .filter(a -> {
                        String k = a.getKey().toString();
                        return !k.contains("recipes") && !k.contains("root");
                    })
                    .toList();

    private static boolean areAdvancementsLoaded = false;

    private static void forList(Set<AdvancementInfo> set, FrameType frame) {
        CollectionBuilder.of(set)
                .filter(info -> info.getFrame() == frame)
                .toSet()
                .forEach(info -> {
                    Set<AdvancementInfo> s = ADV_INFO_MAP.get(frame);
                    if (s == null) s = new LinkedHashSet<>();

                    s.add(info);
                    ADV_INFO_MAP.put(frame, s);
                });
    }

    static {
        Set<AdvancementInfo> infoSet = CollectionBuilder
                .of(ADV_LIST)
                .map(a -> {
                    AdvancementInfo info = null;
                    try {
                        info = new AdvancementInfo(a);
                    } catch (Exception ignored) {}
                    return info;
                })
                .filter(Objects::nonNull)
                .toSet();

        forList(infoSet, FrameType.TASK);
        forList(infoSet, FrameType.GOAL);
        forList(infoSet, FrameType.CHALLENGE);
        forList(infoSet, FrameType.UNKNOWN);
    }

    private static Set<AdvancementInfo> toTypeSet(FrameType frame) {
        return new LinkedHashSet<>(ADV_INFO_MAP.getOrDefault(frame, new LinkedHashSet<>()));
    }

    private static final Set<AdvancementInfo> TASKS = toTypeSet(FrameType.TASK);
    private static final Set<AdvancementInfo> GOALS = toTypeSet(FrameType.GOAL);
    private static final Set<AdvancementInfo> CHALLENGES = toTypeSet(FrameType.CHALLENGE);
    private static final Set<AdvancementInfo> UNKNOWNS = toTypeSet(FrameType.UNKNOWN);

    private static Consumer<AdvancementInfo> fromInfo(Set<Advancement> keys, String type) {
        return info -> {
            FileConfiguration advances = FileCache.ADVANCE_CACHE.getCache("lang").get();
            if (advances == null) return;

            Advancement adv = info.getBukkit();

            final String k = adv.getKey().toString();
            String key = k.replaceAll("[/:]", ".");

            if (advances.contains(key)) return;

            String title = info.getTitle();

            advances.set(key + ".path", "type." + type);

            advances.set(key + ".frame", info.getFrame().toString());
            advances.set(key + ".name", title);
            advances.set(key + ".description", info.getDescription());

            final ItemStack item = info.getItem();
            advances.set(key + ".item",
                    item == null ? null : item.getType().toString());

            keys.add(adv);
        };
    }

    static void checkAdvancements() {
        if (!ModuleName.ADVANCEMENTS.isEnabled()) return;

        for (World w : Bukkit.getWorlds()) {
            var announcesEnabled = WorldRule.ANNOUNCE_ADVANCEMENTS;
            if (advList("worlds").contains(w.getName())) continue;

            if (announcesEnabled.getValue(w))
                announcesEnabled.setValue(w, false);
        }
    }

    static void loadCache() {
        if (LibUtils.MAIN_VERSION < 12) return;

        if (WorldRule.areWorldsLoaded) checkAdvancements();
        if (areAdvancementsLoaded) return;

        try {
            SIRPlugin.runTaskWhenLoaded(() -> {
                checkAdvancements();

                LogUtils.mixLog("true::",
                        "&bRegistering all the advancement values...");

                long t = System.currentTimeMillis();
                final Set<Advancement> loadedKeys = new HashSet<>();

                TASKS.forEach(fromInfo(loadedKeys, "task"));
                GOALS.forEach(fromInfo(loadedKeys, "goal"));
                CHALLENGES.forEach(fromInfo(loadedKeys, "challenge"));
                UNKNOWNS.forEach(fromInfo(loadedKeys, "custom"));

                if (loadedKeys.size() > 0) {
                    YAMLFile f = FileCache
                            .ADVANCE_CACHE.getCache("lang").getFile();
                    if (f != null) f.save();
                }

                String advancements = "&7Tasks: &a" + TASKS.size() +
                        "&7 - Goals: &b" + GOALS.size() +
                        "&7 - &7Challenges: &d" + CHALLENGES.size();

                LogUtils.doLog(advancements);

                if (!UNKNOWNS.isEmpty())
                    LogUtils.doLog("&7Unknowns: &c" +
                            UNKNOWNS.size() +
                            "&7. Check your modules/advancements/lang.yml file!"
                    );

                t = System.currentTimeMillis() - t;
                LogUtils.mixLog(
                        "&7Loaded advancements in &e" + t + "&7 ms.",
                        "true::"
                );

                areAdvancementsLoaded = true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveCache() {
        if (LibUtils.MAIN_VERSION < 12) return;

        for (World w : Bukkit.getWorlds()) {
            var announces = WorldRule.ANNOUNCE_ADVANCEMENTS;
            if (advList("worlds").contains(w.getName()))
                return;

            String d = WorldRule.valueFromLoaded(w, announces);
            boolean v = announces.getValue(w);

            if (Boolean.parseBoolean(d) && !v)
                announces.setValue(w, true);
        }
    }

    AdvanceHandler() {
        super(ModuleName.ADVANCEMENTS);
    }

    private static List<String> advList(String path) {
        return FileCache.ADVANCE_CACHE.getConfig().toList("disabled-" + path);
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

        Advancement adv = event.getAdvancement();
        if (!ADV_LIST.contains(adv)) return;

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

        var section = FileCache.ADVANCE_CACHE.getCache("lang").getSection(path);
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
        List<String> messages = FileCache
                .ADVANCE_CACHE.getCache("messages").toList(messagePath);

        for (String s : messages) {
            Matcher m = Pattern.compile("(?i)^\\[cmd]").matcher(s);

            if (m.find()) {
                cList.add(TextUtils.STRIP_FIRST_SPACES.apply(s.substring(5)));
                continue;
            }
            mList.add(s);
        }

        MessageSender.fromLoaded()
                .setTargets(Bukkit.getOnlinePlayers())
                .setParser(player)
                .addKeysValues(keys, values).send(mList);

        LangUtils.executeCommands(player, cList);

        if (!SIRInitializer.hasDiscord()) return;

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
