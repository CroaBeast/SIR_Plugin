package me.croabeast.sir.plugin.module.object;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.misc.ConfigUnit;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.module.ModuleName;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatTagsParser extends ModuleCache {

    private static final Map<String, ChatTag> TAG_MAP = new HashMap<>();

    ChatTagsParser() {
        super(ModuleName.CHAT_TAGS);
    }

    @NotNull
    static List<ChatTag> fromGroup(Player player, String group) {
        if (player == null)
            return Collections.emptyList();

        List<ChatTag> tags = new ArrayList<>();

        for (ChatTag tag : TAG_MAP.values()) {
            if (!Objects.equals(tag.getGroup(), group)) continue;
            if (tag.isInGroupNonNull(player)) tags.add(tag);
        }

        return tags;
    }

    @Nullable
    static ChatTag fromPerm(Player player, String perm) {
        if (player == null) return null;

        Map<Integer, Set<ChatTag>> map = new TreeMap<>(Collections.reverseOrder());

        for (ChatTag tag : TAG_MAP.values()) {
            if (!Objects.equals(tag.getPermission(), perm)) continue;

            Set<ChatTag> tags = map.getOrDefault(tag.getPriority(), new HashSet<>());
            tags.add(tag);

            map.put(tag.getPriority(), tags);
        }
        if (map.isEmpty()) return null;

        Set<ChatTag> tags = map.values().iterator().next();
        return !tags.isEmpty() ? tags.iterator().next() : null;
    }

    static boolean expansionRegistered = false;

    @Priority(1)
    static void loadCache() {
        if (!ModuleName.CHAT_TAGS.isEnabled()) return;

        TAG_MAP.clear();
        ConfigurationSection c = YAMLCache.getTags().getSection("tags");

        if (c != null)
            for (String key : c.getKeys(false)) {
                ConfigurationSection s = c.getConfigurationSection(key);
                if (s != null) TAG_MAP.put(key, new ChatTag(s));
            }

        if (!Exceptions.isPluginEnabled("PlaceholderAPI") || expansionRegistered)
            return;

        expansionRegistered = new PlaceholderExpansion() {
            public @NotNull String getIdentifier() {
                return "sir_tag";
            }

            public @NotNull String getAuthor() {
                return "CroaBeast";
            }

            public @NotNull String getVersion() {
                return "1.0";
            }

            public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
                Player player = offlinePlayer.getPlayer();
                if (player == null) return null;

                if (params.matches("(?i)group:(.+)")) {
                    List<ChatTag> tags = fromGroup(player, params.split(":")[1]);
                    if (tags.isEmpty()) return null;

                    final String name = tags.get(0).getTag();
                    return StringUtils.isNotBlank(name) ? name : null;
                }

                if (params.matches("(?i)perm:(.+)")) {
                    ChatTag tag = fromPerm(player, params.split(":")[1]);
                    if (tag == null) return null;

                    final String name = tag.getTag();
                    return StringUtils.isNotBlank(name) ? name : null;
                }

                ChatTag tag = TAG_MAP.getOrDefault(params, null);
                if (tag == null) return null;

                final String name = tag.getTag();
                return StringUtils.isNotBlank(name) ? name : null;
            }
        }.register();
    }

    public static String parse(Player player, String string) {
        if (player == null || StringUtils.isBlank(string))
            return string;

        Pattern pattern = Pattern.compile("(?i)\\{tag_(.+)}");

        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            final String id = matcher.group(1);

            if (id.matches("(?i)group:(.+)")) {
                List<ChatTag> tags = fromGroup(player, id.split(":")[1]);
                if (tags.isEmpty()) continue;

                final String name = tags.get(0).getTag();
                if (StringUtils.isBlank(name)) continue;

                string = string.replace(matcher.group(), name);
                continue;
            }

            if (id.matches("(?i)perm:(.+)")) {
                ChatTag tag = fromPerm(player, id.split(":")[1]);
                if (tag == null) continue;

                final String name = tag.getTag();
                if (StringUtils.isBlank(name)) continue;

                string = string.replace(matcher.group(), name);
                continue;
            }

            ChatTag tag = TAG_MAP.getOrDefault(id, null);
            if (tag == null) continue;

            final String name = tag.getTag();
            if (StringUtils.isBlank(name)) continue;

            string = string.replace(matcher.group(), name);
        }

        return string;
    }

    private static class ChatTag implements ConfigUnit {

        private final ConfigurationSection section;

        private ChatTag(ConfigurationSection section) {
            this.section = section;
        }

        @Override
        public @NotNull ConfigurationSection getSection() {
            return section;
        }

        @Nullable
        public String getTag() {
            return getSection().getString("tag");
        }

        public List<String> getDescription() {
            return TextUtils.toList(getSection(), "description");
        }
    }
}
