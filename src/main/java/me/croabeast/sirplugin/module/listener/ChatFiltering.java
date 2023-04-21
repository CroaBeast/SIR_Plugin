package me.croabeast.sirplugin.module.listener;

import lombok.var;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRViewer;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFiltering extends SIRViewer {

    public ChatFiltering() {
        super("filters");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onFilter(AsyncPlayerChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        var filters = FileCache.FILTERS.getSection("filters");
        if (filters == null) return;

        var sender = LangUtils.getSender().setKeys("{word}");

        for (var f : filters.getKeys(false)) {
            FilterSection section = null;
            try {
                section = new FilterSection(filters.getConfigurationSection(f));
            } catch (Exception ignored) {}
            if (section == null) continue;

            if (!section.hasPermission(player)) continue;

            String replacer = section.replaceKey;

            for (var line : section.lineList) {
                var isCancellable = section.cancellable || line.cancellable;

                if (section.regex || line.regex) {
                    var match = Pattern.compile(line.line).matcher(message);
                    if (!match.find()) continue;

                    if (isCancellable) {
                        sender.setValues(line.line).send(section.cancelList);
                        event.setCancelled(true);
                        return;
                    }

                    final String matcher = match.group();
                    String value = StringUtils.repeat(replacer, matcher.length());

                    message = message.replace(matcher, value);
                    continue;
                }

                if (!message.contains(line.line)) continue;

                if (isCancellable) {
                    sender.setValues(line.line).send(section.cancelList);
                    event.setCancelled(true);
                    return;
                }

                message = message.replace(line.line,
                        StringUtils.repeat(replacer, line.line.length()));
            }
        }

        event.setMessage(message);
    }

    static class FilterSection {

        private final List<Line> lineList = new ArrayList<>();

        private final String perm, replaceKey;
        private final boolean regex, cancellable;

        private final List<String> cancelList;

        FilterSection(ConfigurationSection section) {
            if (section == null) throw new NullPointerException();

            perm = section.getString("permission", "DEFAULT");
            replaceKey = section.getString("replace-key", "*");

            regex = section.getBoolean("is-regex");

            cancellable = section.getBoolean("cancel-event.enabled");
            cancelList = TextUtils.toList(section, "cancel-event.message");

            for (var s : TextUtils.toList(section, "words")) {
                try {
                    lineList.add(new Line(s));
                } catch (Exception ignored) {}
            }
        }

        public boolean hasPermission(Player player) {
            return perm.matches("(?i)default") || PlayerUtils.hasPerm(player, perm);
        }
    }

    static class Line {

        static final Pattern PREFIX_PATTERNS = Pattern.compile("(?i)^\\[(cancel|regex)]");

        private final String line;
        private boolean regex = false, cancellable = false;

        Line(String line) {
            if (StringUtils.isBlank(line)) throw new NullPointerException();

            var m1 = PREFIX_PATTERNS.matcher(line);
            if (m1.find()) {
                var first = m1.group(1);

                if (first.matches("(?i)cancel")) cancellable = true;
                if (first.matches("(?i)regex")) regex = true;

                line = line.substring(m1.group().length());

                var m2 = PREFIX_PATTERNS.matcher(line);
                if (m2.find()) {
                    var second = m2.group(1);

                    if (!cancellable && second.matches("(?i)cancel"))
                        cancellable = true;
                    if (!regex && second.matches("(?i)regex")) regex = true;

                    line = line.substring(m2.group().length());
                }
            }

            this.line = line;
        }
    }
}
