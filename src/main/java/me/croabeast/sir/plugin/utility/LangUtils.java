package me.croabeast.sir.plugin.utility;

import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.applier.StringApplier;
import me.croabeast.beanslib.key.PlayerKey;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLCache;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangUtils extends BeansLib {

    public LangUtils(SIRPlugin instance) {
        super(instance);

        PlayerKey.editKey("{playerDisplayName}", "{displayName}");
        PlayerKey.editKey("{playerUUID}", "{uuid}");
        PlayerKey.editKey("{playerWorld}", "{world}");
        PlayerKey.editKey("{playerGameMode}", "{gamemode}");
        PlayerKey.editKey("{playerX}", "{x}");
        PlayerKey.editKey("{playerY}", "{y}");
        PlayerKey.editKey("{playerZ}", "{z}");
        PlayerKey.editKey("{playerYaw}", "{yaw}");
        PlayerKey.editKey("{playerPitch}", "{pitch}");

        PlayerKey.loadKey("{prefix}", PlayerUtils::getPrefix);
        PlayerKey.loadKey("{suffix}", PlayerUtils::getSuffix);
    }

    static YAMLFile config() {
        return YAMLCache.getMainConfig();
    }

    public @NotNull String getLangPrefixKey() {
        return config().get("values.lang-prefix-key", "<P>");
    }

    public @NotNull String getLangPrefix() {
        return YAMLCache.getLang().get("main-prefix", " &e&lSIR &8>");
    }

    public @NotNull String getCenterPrefix() {
        return config().get("values.center-prefix", "<C>");
    }

    public @NotNull String getLineSeparator() {
        return Pattern.quote(config().get("values.line-separator", "<n>"));
    }

    public boolean isColoredConsole() {
        return !config().get("options.fix-logger", false);
    }

    public ConfigurationSection getWebhookSection() {
        return YAMLCache.getWebhooks().getSection("webhooks");
    }

    public ConfigurationSection getBossbarSection() {
        return YAMLCache.getBossbars().getSection("bossbars");
    }

    public boolean isStripPrefix() {
        return !config().get("options.show-prefix", false);
    }

    public static void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        Pattern cPattern = Pattern.compile("(?i)^\\[(global|console)]");
        Pattern pPattern = Pattern.compile("(?i)^\\[player]");

        for (String c : commands) {
            if (StringUtils.isBlank(c)) continue;

            Matcher pm = pPattern.matcher(c), cm = cPattern.matcher(c);

            StringApplier applier = StringApplier.simplified(c)
                    .apply(s -> PlayerKey.replaceKeys(player, s))
                    .apply(TextUtils.STRIP_FIRST_SPACES);

            if (pm.find() && player != null) {
                String text = applier.toString().replace(pm.group(), "");

                Bukkit.dispatchCommand(player, text);
                continue;
            }

            if (cm.find()) applier.apply(s -> s.replace(cm.group(), ""));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applier.toString());
        }
    }

    public static String messageFromArray(String[] args, int argumentIndex) {
        if (argumentIndex >= args.length) return null;
        StringBuilder b = new StringBuilder();

        for (int i = argumentIndex; i < args.length; i++)  {
            b.append(args[i]);
            if (i != args.length - 1) b.append(" ");
        }

        return b.toString();
    }
}
