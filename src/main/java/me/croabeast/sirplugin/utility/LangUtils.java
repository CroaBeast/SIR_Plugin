package me.croabeast.sirplugin.utility;

import lombok.var;
import me.croabeast.beanslib.BeansLib;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.module.EmojiParser;
import me.croabeast.sirplugin.file.FileCache;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LangUtils extends BeansLib {

    @Nullable
    private static MessageSender sender = null;

    public LangUtils(SIRPlugin instance) {
        super(instance);
        getKeyManager().setKey(2, "{uuid}").setKey(3, "{world}").
                setKey(1, "{displayName}").setKey(5, "{x}").
                setKey(6, "{y}").setKey(7, "{z}").
                setKey(8, "{yaw}").setKey(9, "{pitch}");
    }

    public @NotNull String getLangPrefixKey() {
        return FileCache.MAIN_CONFIG.getValue("values.lang-prefix-key", "<P>");
    }

    public @NotNull String getLangPrefix() {
        return FileCache.LANG.getValue("main-prefix", " &e&lSIR &8>");
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
        return FileCache.WEBHOOKS_FILE.getSection("webhooks");
    }

    public ConfigurationSection getBossbarSection() {
        return FileCache.BOSSBARS_FILE.getSection("bossbars");
    }

    public boolean isStripPrefix() {
        return !FileCache.MAIN_CONFIG.getValue("options.show-prefix", false);
    }

    public static String stringKey(@Nullable String key) {
        return key == null ? "empty" : key.replace("/", ".").replace(":", ".");
    }

    @NotNull
    public static MessageSender setSender() {
        final var config = FileCache.MAIN_CONFIG;

        return sender = new MessageSender().
                setLogger(config.getValue("options.send-console", true)).
                setCaseSensitive(false).
                setNoFirstSpaces(config.getValue("options.strip-spaces", false)).
                addFunctions(EmojiParser::parseEmojis);
    }

    @NotNull
    public static MessageSender getSender() {
        return (sender == null ? setSender() : sender).clone();
    }

    public static List<String> toList(FileCache cache, String path) {
        var file = cache.getFile();
        return file == null ? new ArrayList<>() : TextUtils.toList(file.get(), path);
    }

    public static void executeCommands(Player player, List<String> commands) {
        if (commands.isEmpty()) return;

        var cPattern = Pattern.compile("(?i)^\\[(global|console)]");
        var pPattern = Pattern.compile("(?i)^\\[player]");

        var operator = TextUtils.STRIP_FIRST_SPACES;

        for (var c : commands) {
            var pMatch = pPattern.matcher(c);
            var cMatch = cPattern.matcher(c);

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
