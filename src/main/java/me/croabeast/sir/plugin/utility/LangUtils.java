package me.croabeast.sir.plugin.utility;

import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangUtils extends BeansLib {

    public LangUtils(SIRPlugin instance) {
        super(instance);
        getKeyManager().setKey(2, "{uuid}").setKey(3, "{world}").
                setKey(1, "{displayName}").
                setKey(4, "{gameMode}").setKey(5, "{x}").
                setKey(6, "{y}").setKey(7, "{z}").
                setKey(8, "{yaw}").setKey(9, "{pitch}");
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

        UnaryOperator<String> operator = TextUtils.STRIP_FIRST_SPACES;

        for (String c : commands) {
            Matcher pMatch = pPattern.matcher(c);
            Matcher cMatch = cPattern.matcher(c);

            c = SIRPlugin.getUtils().
                    getKeyManager().parseKeys(player, c, false);

            if (pMatch.find() && player != null) {
                Bukkit.dispatchCommand(player,
                        operator.apply(c.replace(pMatch.group(), "")));
                continue;
            }

            if (cMatch.find())
                c = operator.apply(c.replace(cMatch.group(), ""));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
        }
    }
}
