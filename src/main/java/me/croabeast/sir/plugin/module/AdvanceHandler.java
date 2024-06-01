package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.advancementinfo.AdvancementInfo;
import me.croabeast.advancementinfo.FrameType;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.DiscordHook;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.world.WorldRule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdvanceHandler extends SIRModule implements CustomListener, DataHandler {

    private static final YAMLData.Module DATA = YAMLData.Module.ADVANCEMENT;

    private static final Map<FrameType, Set<AdvancementInfo>> ADV_INFO_MAP = new HashMap<>();

    private static final Set<Advancement> ADV_SET =
            CollectionBuilder.of(Bukkit.advancementIterator())
                    .filter(a -> {
                        String k = a.getKey().toString();
                        return !k.contains("recipes") && !k.contains("root");
                    })
                    .collect(new LinkedHashSet<>());

    private static final Set<String> ADV_KEYS = CollectionBuilder.of(ADV_SET)
            .map(a -> a.getKey().toString())
            .collect(new LinkedHashSet<>());

    private static boolean areAdvancementsLoaded = false;

    private static void forList(Set<AdvancementInfo> set, FrameType frame) {
        CollectionBuilder.of(set)
                .filter(info -> info.getType() == frame)
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
                .of(ADV_SET)
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
            ConfigurableFile advances = DATA.fromName("lang");
            Advancement adv = info.getBukkit();

            final String k = adv.getKey().toString();
            String key = k.replaceAll("[/:]", ".");

            if (advances.getKeys(null, false).contains(key)) return;

            String title = info.getTitle();

            advances.set(key + ".path", "type." + type);

            advances.set(key + ".frame", info.getType().toString());
            advances.set(key + ".name", title);
            advances.set(key + ".description", info.getDescription());

            final ItemStack item = info.getIcon();
            advances.set(key + ".item",
                    item == null ? null : item.getType().toString());

            keys.add(adv);
        };
    }

    static void checkAdvancements() {
        if (!ADVANCEMENTS.isEnabled()) return;

        for (World w : Bukkit.getWorlds()) {
            WorldRule<Boolean> announcesEnabled = WorldRule.ANNOUNCE_ADVANCEMENTS;
            if (advList("worlds").contains(w.getName())) continue;

            if (Boolean.TRUE.equals(announcesEnabled.getValue(w)))
                announcesEnabled.setValue(w, false);
        }
    }

    static void loadData() {
        if (ServerInfoUtils.SERVER_VERSION < 12) return;

        if (Reflector.of("me.croabeast.sir.plugin.world.WorldData").get("areWorldsLoaded"))
            checkAdvancements();

        if (areAdvancementsLoaded) return;

        try {
            SIRPlugin.runTaskWhenLoaded(() -> {
                checkAdvancements();

                BeansLib.logger().log(false, "");
                BeansLib.logger().log("&bRegistering all the advancement values in SIR...");

                long t = System.currentTimeMillis();
                final Set<Advancement> loadedKeys = new HashSet<>();

                TASKS.forEach(fromInfo(loadedKeys, "task"));
                GOALS.forEach(fromInfo(loadedKeys, "goal"));
                CHALLENGES.forEach(fromInfo(loadedKeys, "challenge"));
                UNKNOWNS.forEach(fromInfo(loadedKeys, "custom"));

                if (loadedKeys.size() > 0)
                    DATA.fromName("lang").save();

                String advancements = "&7Tasks: &a" + TASKS.size() +
                        "&7 - Goals: &b" + GOALS.size() +
                        "&7 - &7Challenges: &d" + CHALLENGES.size();

                BeansLib.logger().log(advancements);

                if (!UNKNOWNS.isEmpty())
                    BeansLib.logger().log("&7Unknowns: &c" +
                            UNKNOWNS.size() +
                            "&7. Check your modules/advancements/lang.yml file!"
                    );

                t = System.currentTimeMillis() - t;

                BeansLib.logger().log("&7Loaded advancements in &e" + t + "&7 ms.");
                BeansLib.logger().log(false, "");

                areAdvancementsLoaded = true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveData() {
        if (ServerInfoUtils.SERVER_VERSION < 12) return;

        for (World w : Bukkit.getWorlds()) {
            WorldRule<Boolean> announces = WorldRule.ANNOUNCE_ADVANCEMENTS;
            if (advList("worlds").contains(w.getName()))
                return;

            String d = WorldRule.valueFromLoaded(w, announces);
            boolean v = Boolean.TRUE.equals(announces.getValue(w));

            if (Boolean.parseBoolean(d) && !v)
                announces.setValue(w, true);
        }
    }

    @Getter @Setter
    private boolean registered = false;

    AdvanceHandler() {
        super("advancements");
    }

    @Override
    public boolean register() {
        try {
            registerOnSIR();
            return true;
        } catch (Exception e) {
            return false;
        }
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
        if (!ADV_KEYS.contains(key) || advList("advs").contains(key)) return;

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

        MessageSender.loaded()
                .setTargets(Bukkit.getOnlinePlayers())
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
