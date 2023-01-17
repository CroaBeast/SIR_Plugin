package me.croabeast.sirplugin.hook.discord;

import github.scarsz.discordsrv.dependencies.jda.api.*;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.util.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.object.file.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.time.*;
import java.util.List;
import java.util.Locale;

import static me.croabeast.beanslib.utility.TextUtils.*;
import static org.apache.commons.lang.StringUtils.*;

public class DiscordMsg {

    private final Player player;
    private final String channel, embedPath;

    private String[] keys, values;

    public DiscordMsg(Player player, String channel) {
        this.player = player;
        this.channel = channel;
        embedPath = "channels." + channel + ".embed";
    }

    public DiscordMsg(Player player, String channel, String[] keys, String[] values) {
        this(player, channel);
        this.keys = keys;
        this.values = new String[values.length];

        for (int i = 0; i < values.length; i++)
            this.values[i] = IridiumAPI.stripAll(stripJson(values[i]));
    }

    private String parseValues(String line) {
        if (StringUtils.isBlank(line)) return line;

        line = SIRPlugin.getUtils().parsePlayerKeys(player, line, false);

        if (this.keys != null && this.values != null) {
            String[] fKeys = new String[this.keys.length];

            for (int i = 0; i < this.keys.length; i++) {
                String oldKey = this.keys[i];
                if (!oldKey.startsWith("{")) oldKey = "{" + oldKey + "}";
                fKeys[i] = oldKey.toUpperCase(Locale.ENGLISH);
            }

            line = replaceInsensitiveEach(line, fKeys, this.values);
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

    @NotNull
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
            return Color.BLACK.asRGB();
        }
    }

    private MessageEmbed embedMessage() {
        ConfigurationSection id = disc().getConfigurationSection(embedPath);
        if (id == null) return null;

        EmbedBuilder embed = new EmbedBuilder();

        String author = id.getString("author.name"),
                url = id.getString("author.url"),
                icon = id.getString("author.iconURL");

        embed.setColor(embedColor());

        embed.setAuthor(
                parseValues(author), checkURL(url) ? parseValues(url) : null,
                checkURL(icon) ? parseValues(icon) : null
        );

        String title = id.getString("title");
        if (isNotBlank(title)) embed.setTitle(parseValues(title));

        String desc = id.getString("description");
        if (isNotBlank(desc)) embed.setDescription(parseValues(desc));

        String image = id.getString("thumbnail");
        if (checkURL(image)) embed.setThumbnail(parseValues(image));

        if (id.getBoolean("timeStamp")) embed.setTimestamp(Instant.now());

        return embed.build();
    }

    @SuppressWarnings("deprecation")
    public void send() {
        String p = file().getString("discord.server-id");
        if (p == null)
            p = file().getString("discord.default-server", "");

        List<String> list = toList(file(), "discord.channels." + channel);
        if (list.isEmpty()) return;

        String text = disc().getString("channels." + channel + ".text");

        for (String s : list) {
            String gName = p, cName;

            if (s.contains(":")) {
                String[] sp = s.split(":", 2);
                gName = sp[0];
                cName = sp[1];
            }
            else cName = s;

            Guild guild = null;
            try {
                guild = DiscordUtil.getJda().getGuildById(gName);
            } catch (Exception ignored) {}

            if (guild == null) continue;

            TextChannel channel = guild.getTextChannelById(cName);
            if (channel == null) continue;

            if (StringUtils.isNotBlank(text)) {
                channel.sendMessage(parseValues(text)).queue();
                continue;
            }

            MessageEmbed embed = embedMessage();
            if (embed != null) channel.sendMessage(embed).queue();
        }
    }
}
