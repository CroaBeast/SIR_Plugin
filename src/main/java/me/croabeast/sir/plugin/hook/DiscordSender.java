package me.croabeast.sir.plugin.hook;

import com.google.common.collect.Lists;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.key.ValueReplacer;
import me.croabeast.beanslib.misc.StringApplier;
import me.croabeast.beanslib.utility.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.file.FileCache;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DiscordSender {

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

    String formatString(String string) {
        if (StringUtils.isBlank(string)) return string;

        StringApplier applier = StringApplier.of(string).
                apply(s -> ValueReplacer.forEach(keys, values, s)).
                apply(s -> Beans.formatPlaceholders(player, s)).
                apply(NeoPrismaticAPI::stripAll).
                apply(DiscordUtil::translateEmotes);

        return applier.toString();
    }

    public DiscordSender setValues(String... values) {
        if (values == null || values.length <= 0) return this;

        List<String> list = new ArrayList<>();
        for (String s : values)
            list.add(formatString(TextUtils.STRIP_JSON.apply(s)));

        this.values = list.toArray(new String[0]);
        return this;
    }

    private static boolean checkURL(String string) {
        return StringUtils.isNotBlank(string) && string.startsWith("http");
    }

    FileConfiguration getModules() {
        return FileCache.DISCORD_HOOK_CACHE.getConfig().get();
    }

    FileConfiguration getChannels() {
        return FileCache.DISCORD_HOOK_CACHE.getCache("channels").get();
    }

    void generateEmbed() {
        String embedPath = "channels." + this.channel + ".embed";

        String rgb = getChannels().getString(embedPath + ".color", "BLACK");
        int colorInt = Color.BLACK.asRGB();

        try {
            try {
                colorInt = java.awt.Color.decode(rgb).getRGB();
            } catch (Exception e) {
                Field color = Class.forName("org.bukkit.Color").getField(rgb);
                colorInt = ((Color) color.get(null)).asRGB();
            }
        } catch (Exception ignored) {}

        org.bukkit.configuration.ConfigurationSection id = getChannels().getConfigurationSection(embedPath);
        if (id == null) return;

        EmbedBuilder embed = new EmbedBuilder();

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
        String path = getModules().getString("default-server", "");
        generateEmbed();

        List<String> list = TextUtils.toList(getModules(), "channels." + channel);
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

            DiscordSRV srv = DiscordSRV.getPlugin();

            Guild guild = srv.getMainGuild();
            try {
                guild = srv.getJda().getGuildById(guildId);
            } catch (Exception ignored) {}

            if (guild == null) continue;

            TextChannel channel = guild.getTextChannelById(id);
            if (channel == null) continue;

            if (StringUtils.isNotBlank(text)) {
                channel.sendMessage(formatString(text)).queue();

                if (!atLeastOneMessage)
                    atLeastOneMessage = true;
                continue;
            }

            if (embed == null) continue;

            channel.sendMessageEmbeds(Lists.newArrayList(embed)).queue();
            if (!atLeastOneMessage) atLeastOneMessage = true;
        }

        return atLeastOneMessage;
    }
}
