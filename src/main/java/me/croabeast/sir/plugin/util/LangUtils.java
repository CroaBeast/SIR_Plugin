package me.croabeast.sir.plugin.util;

import me.croabeast.beans.BeansLib;
import me.croabeast.lib.PlayerReplacer;
import me.croabeast.lib.applier.StringApplier;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.file.Configurable;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangUtils extends BeansLib {

    private LangUtils(SIRPlugin instance) {
        super(instance);

        PlayerReplacer.editKey("{playerDisplayName}", "{displayName}");
        PlayerReplacer.editKey("{playerUUID}", "{uuid}");
        PlayerReplacer.editKey("{playerWorld}", "{world}");
        PlayerReplacer.editKey("{playerGameMode}", "{gamemode}");
        PlayerReplacer.editKey("{playerX}", "{x}");
        PlayerReplacer.editKey("{playerY}", "{y}");
        PlayerReplacer.editKey("{playerZ}", "{z}");
        PlayerReplacer.editKey("{playerYaw}", "{yaw}");
        PlayerReplacer.editKey("{playerPitch}", "{pitch}");

        PlayerReplacer.loadKey("{prefix}", PlayerUtils::getPrefix);
        PlayerReplacer.loadKey("{suffix}", PlayerUtils::getSuffix);
    }

    static Configurable config() {
        return Objects.requireNonNull(YAMLData.Main.CONFIG.from());
    }

    public @NotNull String getLangPrefixKey() {
        return config().get("values.lang-prefix-key", "<P>");
    }

    public @NotNull String getLangPrefix() {
        return config().get("values.lang-prefix", " &e&lSIR &8>");
    }

    public @NotNull String getCenterPrefix() {
        return config().get("values.center-prefix", "<C>");
    }

    public @NotNull String getLineSeparator() {
        return Pattern.quote(config().get("values.line-separator", "<n>"));
    }

    public ConfigurationSection getWebhookSection() {
        return YAMLData.Main.WEBHOOKS.from().getSection("webhooks");
    }

    public ConfigurationSection getBossbarSection() {
        return YAMLData.Main.WEBHOOKS.from().getSection("bossbars");
    }

    public static void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        Pattern cPattern = Pattern.compile("(?i)^\\[(global|console)]");
        Pattern pPattern = Pattern.compile("(?i)^\\[player]");

        for (String c : commands) {
            if (StringUtils.isBlank(c)) continue;

            Matcher pm = pPattern.matcher(c), cm = cPattern.matcher(c);

            StringApplier applier = StringApplier.simplified(c)
                    .apply(s -> PlayerReplacer.replaceKeys(player, s))
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

    public static String stringFromArray(String[] args, int argumentIndex) {
        if (argumentIndex >= args.length) return null;
        StringBuilder b = new StringBuilder();

        for (int i = argumentIndex; i < args.length; i++)  {
            b.append(args[i]);
            if (i != args.length - 1) b.append(" ");
        }

        return b.toString();
    }
}
