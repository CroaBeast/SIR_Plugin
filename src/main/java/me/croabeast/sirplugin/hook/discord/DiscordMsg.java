package me.croabeast.sirplugin.hook.discord;

import github.scarsz.discordsrv.dependencies.jda.api.*;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.util.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.object.file.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.lang.reflect.Field;
import java.time.*;

import static me.croabeast.beanslib.utility.TextUtils.*;
import static org.apache.commons.lang.StringUtils.*;

public class DiscordMsg {

    private final Player player;
    private final String channel, embedPath;

    private String[] keys, values;
    private final TextChannel textChannel;

    public DiscordMsg(Player player, String channel) {
        this.player = player;
        this.channel = channel;
        embedPath = "channels." + channel + ".embed";
        textChannel = getChannel();
    }

    public DiscordMsg(Player player, String channel, String[] keys, String[] values) {
        this(player, channel);
        this.keys = keys;
        this.values = new String[values.length];

        for (int i = 0; i < values.length; i++)
            this.values[i] = IridiumAPI.stripAll(stripJson(values[i]));
    }

    private String parseValues(String line) {
        String[] keys = {"{player}", "{uuid}"};
        String[] values = {player.getName(), player.getUniqueId().toString()};

        line = replaceInsensitiveEach(line, keys, values);

        if (this.keys != null && this.values != null) {
            String[] formatKeys = new String[this.keys.length];

            for (int i = 0; i < this.keys.length; i++) {
                String oldKey = this.keys[i];
                if (!oldKey.startsWith("{")) oldKey = "{" + oldKey + "}";
                formatKeys[i] = oldKey.toUpperCase();
            }

            line = replaceInsensitiveEach(line, formatKeys, this.values);
        }

        line = IridiumAPI.stripAll(parsePAPI(player, line));
        return DiscordUtil.translateEmotes(line);
    }

    private boolean checkURL(String string) {
        return isNotBlank(string) && string.startsWith("http");
    }

    private FileConfiguration file() {
        return FileCache.MODULES.get();
    }

    private FileConfiguration disc() {
        return FileCache.DISCORD.get();
    }

    private TextChannel getChannel() {
        String guildName = file().getString("discord.server-id", ""),
                id = file().getString("discord.channels." + channel, "");

        Guild guild;
        try {
            guild = DiscordUtil.getJda().getGuildById(guildName);
        } catch (Exception ignored) {
            return null;
        }

        if (guild == null) return null;

        try {
            return guild.getTextChannelById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer embedColor() {
        String rgb = disc().getString(embedPath + ".color", "BLACK");
        try {
            try {
                return java.awt.Color.decode(rgb).getRGB();
            } catch (Exception e) {
                Field color = Class.forName("org.bukkit.Color").getField(rgb);
                return ((Color) color.get(null)).asRGB();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private MessageEmbed embedMessage() {
        ConfigurationSection id = disc().getConfigurationSection(embedPath);
        if (id == null) return null;

        EmbedBuilder embed = new EmbedBuilder();

        String author = id.getString("author.name"),
                url = id.getString("author.url"),
                icon = id.getString("author.iconURL");

        Integer color = embedColor();
        if (color != null) embed.setColor(color);

        embed.setAuthor(
                parseValues(author), checkURL(url) ? parseValues(url) : null,
                checkURL(icon) ? parseValues(icon) : null
        );

        String title = id.getString("title");
        if (isNotBlank(title)) embed.setTitle(parseValues(title));

        String description = id.getString("description");
        if (isNotBlank(description)) embed.setDescription(parseValues(description));

        String image = id.getString("thumbnail");
        if (checkURL(image)) embed.setThumbnail(parseValues(image));

        if (id.getBoolean("timeStamp")) embed.setTimestamp(Instant.now());

        return embed.build();
    }

    @SuppressWarnings("deprecation")
    public void send() {
        String text = disc().getString("channels." + channel + ".text");
        if (textChannel == null) return;

        if (StringUtils.isNotBlank(text)) {
            textChannel.sendMessage(parseValues(text)).queue();
            return;
        }

        MessageEmbed embed = embedMessage();
        if (embed != null) textChannel.sendMessage(embed).queue();
    }
}
