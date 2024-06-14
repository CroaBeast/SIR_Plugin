package me.croabeast.sir.plugin.module.hook;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.croabeast.beans.BeansLib;
import me.croabeast.lib.applier.StringApplier;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.neoprismatic.NeoPrismaticAPI;
import me.croabeast.sir.plugin.file.YAMLData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class DiscordHook extends HookModule {

    private static final Map<String, List<String>> ID_MAP = new HashMap<>();
    private static final Map<String, EmbedObject> EMBED_MAP = new HashMap<>();

    DiscordHook() {
        super(Name.DISCORD, YAMLData.Module.Hook.DISCORD);
    }

    @Override
    public boolean isEnabled() {
        return Exceptions.isPluginEnabled("DiscordSRV") && super.isEnabled();
    }

    @Override
    public boolean register() {
        if (!isEnabled()) return false;

        ID_MAP.clear();
        EMBED_MAP.clear();

        ConfigurationSection s = config.getSection("channels");
        if (s == null) return false;

        ConfigurationSection ids = config.getSection("ids");
        if (ids != null)
            for (String key : ids.getKeys(false))
                ID_MAP.put(key, TextUtils.toList(ids, key));

        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c != null) EMBED_MAP.put(k, new EmbedObject(c));
        }

        return true;
    }

    private class EmbedObject {

        private final String text;
        private final String color;

        private final String authorName;
        private final String authorUrl;
        private final String authorIcon;

        private final String thumbnail;

        private final String titleText;
        private final String tUrl;

        private final String description;

        private final boolean timeStamp;

        EmbedObject(ConfigurationSection section) {
            this.text = section.getString("text");
            this.color = section.getString("color");

            this.authorName = section.getString("author.name");
            this.authorUrl = section.getString("author.url");
            this.authorIcon = section.getString("author.iconURL");

            this.thumbnail = section.getString("thumbnail");

            this.titleText = section.getString("title.text");
            this.tUrl = section.getString("title.url");

            this.description = section.getString("description");

            this.timeStamp = section.getBoolean("timeStamp");
        }

        private boolean isUrl(String string) {
            return StringUtils.isNotBlank(string) && string.startsWith("https");
        }

        DiscordSRV getDiscord() {
            return DiscordSRV.getPlugin();
        }

        private Guild getGuild(String guildName) {
            final DiscordSRV srv = getDiscord();

            Guild guild = srv.getMainGuild();
            String def = config.get("default-server", "");

            try {
                return srv.getJda().getGuildById(guildName);
            } catch (Exception e) {
                try {
                    return srv.getJda().getGuildById(def);
                } catch (Exception ignored) {}
            }

            return guild;
        }

        EmbedBuilder createEmbed(UnaryOperator<String> operator) {
            final EmbedBuilder embed = new EmbedBuilder();

            int colorInt = Color.BLACK.asRGB();
            String c = this.color;

            if (StringUtils.isNotBlank(c))
                try {
                    try {
                        colorInt = java.awt.Color.decode(c).getRGB();
                    } catch (Exception e) {
                        Field color = Class
                                .forName("org.bukkit.Color")
                                .getField(c);

                        colorInt = ((Color) color.get(null)).asRGB();
                    }
                } catch (Exception ignored) {}

            embed.setColor(colorInt);

            embed.setAuthor(operator.apply(authorName),
                    isUrl(authorUrl) ? operator.apply(authorUrl) : null,
                    isUrl(authorIcon) ? operator.apply(authorIcon) : null
            );

            if (StringUtils.isNotBlank(titleText)) {
                String url = isUrl(tUrl) ? operator.apply(tUrl) : null;
                embed.setTitle(operator.apply(titleText), url);
            }

            if (StringUtils.isNotBlank(description))
                embed.setDescription(description);

            if (isUrl(thumbnail))
                embed.setThumbnail(operator.apply(thumbnail));

            if (timeStamp) embed.setTimestamp(Instant.now());
            return embed;
        }

        boolean send(List<String> ids, UnaryOperator<String> operator) {
            TextChannel main = getDiscord().getMainTextChannel();
            if (main != null) {
                if (StringUtils.isNotBlank(text)) {
                    main.sendMessage(operator.apply(text)).queue();
                    return true;
                }

                MessageEmbed embed = createEmbed(operator).build();
                main.sendMessageEmbeds(embed).queue();
                return true;
            }

            if (ids.isEmpty()) return false;
            boolean atLeastOneMessage = false;

            for (String id : ids) {
                String guildId = null, channelId = id;

                if (id.contains(":")) {
                    String[] array = id.split(":", 2);

                    guildId = array[0];
                    channelId = array[1];
                }

                Guild guild = getGuild(guildId);

                TextChannel channel = null;
                try {
                    channel = guild.getTextChannelById(channelId);
                } catch (Exception ignored) {}

                if (channel == null) continue;

                if (StringUtils.isNotBlank(text)) {
                    channel.sendMessage(operator.apply(text)).queue();
                    if (!atLeastOneMessage) atLeastOneMessage = true;
                    continue;
                }

                MessageEmbed embed = createEmbed(operator).build();
                channel.sendMessageEmbeds(embed).queue();

                if (!atLeastOneMessage) atLeastOneMessage = true;
            }

            return atLeastOneMessage;
        }
    }

    private static class Sender {

        private String[] keys = null, values = null;

        private final List<String> ids;
        private final EmbedObject object;

        private final UnaryOperator<String> operator;

        static String replacePlaceholders(Player player, String s) {
            return BeansLib.getLib().formatPlaceholders(player, s);
        }

        private Sender(List<String> ids, EmbedObject object, Player player) {
            this.ids = ids;
            this.object = object;

            this.operator = string -> StringUtils.isBlank(string) ?
                    string :
                    StringApplier.simplified(string)
                            .apply(s -> ReplaceUtils.replaceEach(keys, values, s))
                            .apply(s -> replacePlaceholders(player, s))
                            .apply(NeoPrismaticAPI::stripAll)
                            .apply(DiscordUtil::translateEmotes).toString();
        }

        Sender setKeysValues(String[] keys, String[] values) {
            if (!ReplaceUtils.isApplicable(keys, values))
                return this;

            this.keys = keys;

            List<String> list = ArrayUtils.toList(values);

            list.replaceAll(TextUtils.STRIP_JSON);
            list.replaceAll(operator);

            this.values = list.toArray(new String[0]);
            return this;
        }

        public void send() {
            object.send(ids, operator);
        }
    }

    public static void send(String channel, Player player, String[] keys, String[] values) {
        if (!ID_MAP.containsKey(channel) || !EMBED_MAP.containsKey(channel))
            throw new NullPointerException();

        EmbedObject object = EMBED_MAP.get(channel);
        List<String> list = ID_MAP.get(channel);

        new Sender(list, object, player).setKeysValues(keys, values).send();
    }

    public static void send(String channel, Player player) {
        send(channel, player, null, null);
    }
}
