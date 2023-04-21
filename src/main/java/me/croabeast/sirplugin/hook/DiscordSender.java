package me.croabeast.sirplugin.hook;

import com.google.common.collect.Lists;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import lombok.var;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.file.FileCache;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscordSender {

    private final Player player;
    private final String channel;

    private MessageEmbed embed = null;

    private String[] keys, values;

    public DiscordSender(Player player, String channel) {
        this.player = player;
        this.channel = channel;
    }

    public DiscordSender setKeys(String... keys) {
        if (keys == null || keys.length <= 0) return this;

        this.keys = keys;
        return this;
    }

    public DiscordSender setValues(String... values) {
        if (values == null || values.length <= 0) return this;

        List<String> list = new ArrayList<>();
        for (String s : values)
            list.add(IridiumAPI.stripAll(TextUtils.STRIP_JSON.apply(s)));

        this.values = list.toArray(new String[0]);
        return this;
    }

    private static boolean checkURL(String string) {
        return StringUtils.isNotBlank(string) && string.startsWith("http");
    }

    String formatString(String string) {
        if (StringUtils.isBlank(string)) return string;

        string = SIRPlugin.getUtils().
                parsePlayerKeys(player, string, false);

        return DiscordUtil.translateEmotes(
                IridiumAPI.stripAll(TextUtils.PARSE_PLACEHOLDERAPI.apply(
                        player, // player can be null
                        ValueReplacer.forEach(string, keys, values)
                )));
    }

    FileConfiguration getModules() {
        return FileCache.MODULES.get();
    }

    FileConfiguration getChannels() {
        return FileCache.DISCORD_CACHE.get();
    }

    void generateEmbed() {
        String embedPath = "channels." + this.channel + ".embed";

        String rgb = getChannels().getString(embedPath + ".color", "BLACK");
        int colorInt = Color.BLACK.asRGB();

        try {
            try {
                colorInt = java.awt.Color.decode(rgb).getRGB();
            } catch (Exception e) {
                var color = Class.forName("org.bukkit.Color").getField(rgb);
                colorInt = ((Color) color.get(null)).asRGB();
            }
        } catch (Exception ignored) {}

        var id = getChannels().getConfigurationSection(embedPath);
        if (id == null) return;

        var embed = new EmbedBuilder();

        String author = id.getString("author.name"),
                url = id.getString("author.url"),
                icon = id.getString("author.iconURL");

        embed.setColor(colorInt);

        embed.setAuthor(
                formatString(author), checkURL(url) ? formatString(url) : null,
                checkURL(icon) ? formatString(icon) : null
        );

        String title = id.getString("title");
        if (StringUtils.isNotBlank(title)) embed.setTitle(formatString(title));

        String desc = id.getString("description");
        if (StringUtils.isNotBlank(desc)) embed.setDescription(formatString(desc));

        String image = id.getString("thumbnail");
        if (checkURL(image)) embed.setThumbnail(formatString(image));

        if (id.getBoolean("timeStamp")) embed.setTimestamp(Instant.now());

        this.embed = embed.build();
    }

    public boolean send() {
        String path = getModules().getString("discord.server-id");
        if (StringUtils.isBlank(path))
            path = getModules().getString("discord.default-server", "");

        generateEmbed();

        var list = TextUtils.toList(getModules(), "discord.channels." + channel);
        if (list.isEmpty()) return false;

        String text = getChannels().getString("channels." + channel + ".text");
        boolean atLeastOneMessage = false;

        for (String s : list) {
            String guildId = path, id;

            if (s.contains(":")) {
                String[] sp = s.split(":", 2);

                id = sp[1];
                guildId = sp[0];
            }
            else id = s;

            var guild = DiscordSRV.getPlugin().getMainGuild();
            try {
                guild = DiscordSRV.getPlugin().getJda().getGuildById(guildId);
            } catch (Exception ignored) {}

            if (guild == null) continue;

            var channel = guild.getTextChannelById(id);
            if (channel == null) continue;

            var pre = new GameChatMessagePreProcessEvent(
                    id, MessageUtil.toComponent(text, true),
                    player, null
            );
            DiscordSRV.api.callEvent(pre);

            if (pre.isCancelled()) return false;

            channel = guild.getTextChannelById(pre.getChannel());
            if (channel == null) return false;

            text = MessageUtil.toLegacy(pre.getMessageComponent());

            var post = new GameChatMessagePostProcessEvent(
                    id, text, player, false, null);
            DiscordSRV.api.callEvent(post);

            channel = guild.getTextChannelById(post.getChannel());
            if (channel == null) return false;

            text = post.getProcessedMessage();

            if (StringUtils.isNotBlank(text)) {
                channel.sendMessage(formatString(text)).queue();
                if (!atLeastOneMessage) atLeastOneMessage = true;
                continue;
            }

            if (embed == null) continue;

            channel.sendMessageEmbeds(Lists.newArrayList(embed)).queue();
            if (!atLeastOneMessage) atLeastOneMessage = true;
        }

        return atLeastOneMessage;
    }
}
