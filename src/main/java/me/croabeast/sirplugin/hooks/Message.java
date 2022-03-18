package me.croabeast.sirplugin.hooks;

import github.scarsz.discordsrv.dependencies.jda.api.*;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.util.*;
import me.croabeast.iridiumapi.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.time.*;
import java.util.*;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class Message {

    private final SIRPlugin main = SIRPlugin.getInstance();

    private final Player player;
    private final String channel, embedPath;

    private String[] keys, values;

    public Message(Player player, String channel) {
        this.player = player;
        this.channel = channel;
        this.embedPath = "channels." + channel + ".embed";
    }

    public Message(Player player, String channel, String[] keys, String[] values) {
        this(player, channel);
        this.keys = keys;
        this.values = new String[values.length];

        for (int i = 0; i < values.length; i++)
            this.values[i] = IridiumAPI.stripAll(JsonMsg.stripJson(values[i]));
    }

    private String parse(String line) {
        String[] keys = {"player", "uuid"};
        String[] values = {player.getName(), player.getUniqueId().toString()};

        line = parseInsensitiveEach(line, keys, values);
        if (this.keys != null && this.values != null)
            line = parseInsensitiveEach(line, this.keys, this.values);

        line = IridiumAPI.stripAll(parsePAPI(player, line));
        return DiscordUtil.translateEmotes(line);
    }

    private boolean check(@Nullable String line) {
        return line != null && StringUtils.isBlank(line);
    }

    @Nullable
    private TextChannel getChannel() {
        try {
            String channel = main.getModules().getString("discord.channels." + this.channel, "");
            return Objects.requireNonNull(main.getInitializer().getGuild()).getTextChannelById(channel);
        }
        catch (Exception e) {
            return null;
        }
    }

    private int embedColor() {
        int color = Color.BLACK.asRGB();
        String rgb = main.getDiscord().getString(embedPath + ".color", "BLACK");
        try {
            try {
                return java.awt.Color.decode(rgb).getRGB();
            }
            catch (Exception e) {
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

        String authorName = main.getDiscord().getString(embedPath + ".author.name");
        String url = main.getDiscord().getString(embedPath + ".author.url");
        String icon = main.getDiscord().getString(embedPath + ".author.iconURL");

        embed.setAuthor(
                parse(authorName), check(url) && url.startsWith("http") ? parse(url) : null,
                check(icon) && icon.startsWith("http") ? parse(icon) : null
        );

        String title = main.getDiscord().getString(embedPath + ".title");
        if (check(title)) embed.setTitle(parse(title));

        String description = main.getDiscord().getString(embedPath + ".description");
        if (check(description)) embed.setDescription(parse(description));

        String image = main.getDiscord().getString(embedPath + ".thumbnail");
        if (check(image) && image.startsWith("http")) embed.setThumbnail(parse(image));

        if (main.getDiscord().getBoolean(embedPath + ".timeStamp"))
            embed.setTimestamp(Instant.now());

        return embed.build();
    }

    @SuppressWarnings("deprecation")
    public void sendMessage() {
        if (getChannel() == null) return;
        String text = main.getDiscord().getString("channels." + channel + ".text");

        if ((text != null && !text.equals("")) || embedMessage().isEmpty())
            getChannel().sendMessage(parse(text)).queue();
        else getChannel().sendMessage(embedMessage()).queue();
    }
}
