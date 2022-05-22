package me.croabeast.sirplugin.hooks.discord;

import github.scarsz.discordsrv.*;
import github.scarsz.discordsrv.dependencies.jda.api.*;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.util.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.time.*;

import static me.croabeast.sirplugin.objects.FileCache.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;
import static org.apache.commons.lang.StringUtils.*;

public class Message {

    private final Player player;
    private final String channel, embedPath;

    private String[] keys, values;
    @Nullable private final TextChannel textChannel;

    public Message(Player player, String channel) {
        this.player = player;
        this.channel = channel;
        embedPath = "channels." + channel + ".embed";
        textChannel = getChannel();
    }

    public Message(Player player, String channel, String[] keys, String[] values) {
        this(player, channel);
        this.keys = keys;
        this.values = new String[values.length];

        for (int i = 0; i < values.length; i++)
            this.values[i] = IridiumAPI.stripAll(stripJson(values[i]));
    }

    private String parseValues(String line) {
        String[] keys = {"player", "uuid"};
        String[] values = {player.getName(), player.getUniqueId().toString()};

        line = parseInsensitiveEach(line, keys, values);
        if (this.keys != null && this.values != null)
            line = parseInsensitiveEach(line, this.keys, this.values);

        line = IridiumAPI.stripAll(parsePAPI(player, line));
        return DiscordUtil.translateEmotes(line);
    }

    @Nullable
    private TextChannel getChannel() {
        String guildName = MODULES.toFile().getString("discord.server-id", ""),
                id = MODULES.toFile().getString("discord.channels." + channel, "");

        Guild guild = null;
        try {
            guild = DiscordSRV.getPlugin().getJda().getGuildById(guildName);
        } catch (Exception ignored) {}

        try {
            return guild == null ? null : guild.getTextChannelById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private int embedColor() {
        int color = Color.BLACK.asRGB();
        String rgb = DISCORD.toFile().getString(embedPath + ".color", "BLACK");
        try {
            try {
                return java.awt.Color.decode(rgb).getRGB();
            } catch (Exception e) {
                return ((Color) Class.forName("org.bukkit.Color").getField(rgb).get(null)).asRGB();
            }
        } catch (Exception e) {
            LogUtils.doLog(
                    "<P> &cThe color " + rgb + " is not a valid color.",
                    "<P> &7Localized error: &e" + e.getLocalizedMessage()
            );
            return color;
        }
    }

    private MessageEmbed embedMessage() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(embedColor());

        String author = DISCORD.toFile().getString(embedPath + ".author.name");
        String url = DISCORD.toFile().getString(embedPath + ".author.url");
        String icon = DISCORD.toFile().getString(embedPath + ".author.iconURL");

        embed.setAuthor(
                parseValues(author), isNotBlank(url) && url.startsWith("http") ? parseValues(url) : null,
                isNotBlank(icon) && icon.startsWith("http") ? parseValues(icon) : null
        );

        String title = DISCORD.toFile().getString(embedPath + ".title");
        if (isNotBlank(title)) embed.setTitle(parseValues(title));

        String description = DISCORD.toFile().getString(embedPath + ".description");
        if (isNotBlank(description)) embed.setDescription(parseValues(description));

        String image = DISCORD.toFile().getString(embedPath + ".thumbnail");
        if (isNotBlank(image) && image.startsWith("http")) embed.setThumbnail(parseValues(image));

        if (DISCORD.toFile().getBoolean(embedPath + ".timeStamp"))
            embed.setTimestamp(Instant.now());

        return embed.build();
    }

    @SuppressWarnings("deprecation")
    public void sendMessage() {
        if (textChannel == null) return;
        String text = DISCORD.toFile().getString("channels." + channel + ".text");

        if ((text != null && !text.equals("")) || embedMessage().isEmpty())
            textChannel.sendMessage(parseValues(text)).queue();
        else textChannel.sendMessage(embedMessage()).queue();
    }
}
