package me.croabeast.sir.plugin.utility;

import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.misc.StringApplier;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import net.milkbowl.vault.chat.Chat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangUtils extends BeansLib {

    public LangUtils(SIRPlugin instance) {
        super(instance);
        getKeyManager().setKey(2, "{uuid}").setKey(3, "{world}")
                .setKey(1, "{displayName}")
                .setKey(4, "{gameMode}").setKey(5, "{x}")
                .setKey(6, "{y}").setKey(7, "{z}")
                .setKey(8, "{yaw}").setKey(9, "{pitch}")
                .createKey("{prefix}", PlayerUtils::getPrefix)
                .createKey("{suffix}", PlayerUtils::getSuffix);
    }

    public @NotNull String getLangPrefixKey() {
        return FileCache.MAIN_CONFIG.getValue("values.lang-prefix-key", "<P>");
    }

    public @NotNull String getLangPrefix() {
        return FileCache.getLang().getValue("main-prefix", " &e&lSIR &8>");
    }

    public @NotNull String getCenterPrefix() {
        return FileCache.MAIN_CONFIG.getValue("values.center-prefix", "<C>");
    }

    public @NotNull String getLineSeparator() {
        return Pattern.quote(FileCache.MAIN_CONFIG.getValue("values.line-separator", "<n>"));
    }

    public boolean isColoredConsole() {
        return !FileCache.MAIN_CONFIG.getValue("options.fix-logger", false);
    }

    public ConfigurationSection getWebhookSection() {
        return FileCache.WEBHOOKS.getSection("webhooks");
    }

    public ConfigurationSection getBossbarSection() {
        return FileCache.BOSSBARS.getSection("bossbars");
    }

    public boolean isStripPrefix() {
        return !FileCache.MAIN_CONFIG.getValue("options.show-prefix", false);
    }

    public static void executeCommands(Player player, List<String> commands) {
        if (commands.isEmpty()) return;

        Pattern cPattern = Pattern.compile("(?i)^\\[(global|console)]");
        Pattern pPattern = Pattern.compile("(?i)^\\[player]");

        for (String c : commands) {
            if (StringUtils.isBlank(c)) continue;

            Matcher pm = pPattern.matcher(c), cm = cPattern.matcher(c);

            StringApplier applier = StringApplier.of(c)
                    .apply(s -> Beans.parsePlayerKeys(player, s))
                    .apply(TextUtils.STRIP_FIRST_SPACES);

            if (pm.find() && player != null) {
                String text = applier.toString().replace(pm.group(), "");

                Bukkit.dispatchCommand(player, text);
                continue;
            }

            String text = applier.toString();
            if (cm.find())
                text = text.replace(cm.group(), "");

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), text);
        }
    }
}
