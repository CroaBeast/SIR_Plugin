package me.croabeast.sircore.hooks;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.*;
import org.bukkit.Color;
import org.jetbrains.annotations.*;

import java.time.Instant;
import java.util.*;

public class DiscordMsg {

    private final Application main;
    private final Records records;

    private final Player player;
    private final String type;
    private final String embedPath;

    private String message = "";

    private String[] keys = null;
    private String[] values = null;

    public DiscordMsg(Application main, Player player, String type) {
        this.main = main;
        this.records = main.getRecords();
        this.player = player;
        this.type = type;
        this.embedPath = "formats." + type + ".embed";
    }

    public DiscordMsg(Application main, Player player, String type, String[] keys, String[] values) {
        this.main = main;
        this.records = main.getRecords();
        this.player = player;
        this.type = type;
        this.embedPath = "formats." + type + ".embed";
        this.keys = keys;
        this.values = values;
    }

    @Nullable
    private Guild getServer() { return main.getInitializer().getDiscordServer(); }

    public DiscordMsg setMessage(@NotNull String message) {
        this.message = message;
        return this;
    }

    private String parseHolders(String line) {
        String[] keys = {"{PLAYER}", "{UUID}", "{MESSAGE}"};
        String[] values = {player.getName(), player.getUniqueId().toString(), message};

        line = StringUtils.replaceEach(line, keys, values);
        if (this.keys != null && this.values != null)
            line = StringUtils.replaceEach(line, this.keys, this.values);

        return main.getTextUtils().parsePAPI(player, line);
    }

    @Nullable private TextChannel getChannel() {
        try {
            String channel = main.getDiscord().getString("channels." + type, "");
            return Objects.requireNonNull(getServer()).getTextChannelById(channel);
        }
        catch (Exception e) { return null; }
    }

    private int embedColor() {
        int color = Color.BLACK.asRGB();
        String rgb = main.getDiscord().getString(embedPath + ".color", "BLACK");
        try {
            try { return java.awt.Color.decode(rgb).getRGB(); }
            catch (Exception e) {
                return ((Color) Class.forName("org.bukkit.Color").getField(rgb).get(null)).asRGB();
            }
        } catch (Exception e) {
            records.doRecord(player, "" +
                    "<P> &cThe color " + rgb + " is not a valid color.",
                    "<P> &7Localized error: &e" + e.getLocalizedMessage()
            );
            return color;
        }
    }

    private MessageEmbed embedMessage() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(embedColor());

        String authorName = main.getDiscord().getString(embedPath + ".author.name");
        String url = main.getDiscord().getString(embedPath + ".author.url");
        String icon = main.getDiscord().getString(embedPath + ".author.iconURL");

        embed.setAuthor(
                authorName != null ? parseHolders(authorName) : null,
                url != null && url.startsWith("http") ? parseHolders(url) : null,
                icon != null && icon.startsWith("http") ? parseHolders(icon) : null
        );

        String title = main.getDiscord().getString(embedPath + ".title");
        if (title != null) embed.setTitle(parseHolders(title));

        String description = main.getDiscord().getString(embedPath + ".description");
        if (description != null) embed.setDescription(parseHolders(description));

        String image = main.getDiscord().getString(embedPath + ".thumbnail");
        if (image != null && image.startsWith("http")) embed.setThumbnail(parseHolders(image));

        if (main.getDiscord().getBoolean(embedPath + ".timeStamp"))
            embed.setTimestamp(Instant.now());

        return embed.build();
    }

    @SuppressWarnings("deprecation")
    public void sendMessage() {
        if (getServer() == null || getChannel() == null) {
            String input = getServer() == null ?
                    "Discord Server" : "Text Channel";
            records.doRecord(player, "" +
                    "<P> &cThe " + input + " ID is invalid or doesn't exist.",
                    "<P> &7Please check your ID and change it ASAP."
            );
            return;
        }

        String text = main.getDiscord().getString("formats." + type + ".text");
        if ((text == null || text.equals("")) && !embedMessage().isEmpty())
            getChannel().sendMessage(embedMessage()).queue();
        else getChannel().sendMessage(parseHolders(text)).queue();
    }
}
