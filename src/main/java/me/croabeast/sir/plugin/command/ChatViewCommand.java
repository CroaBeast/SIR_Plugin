package me.croabeast.sir.plugin.command;

import lombok.Getter;
import me.croabeast.beans.BeansLib;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.module.chat.ChannelHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChatViewCommand extends SIRCommand {

    @NotNull @Getter
    private final ConfigurableFile lang, data;

    ChatViewCommand() {
        super("chat-view");

        this.lang = YAMLData.Command.Multi.CHAT_VIEW.from(true);
        this.data = YAMLData.Command.Multi.CHAT_VIEW.from(false);
    }

    @NotNull
    protected SIRExtension getParent() {
        return SIRModule.CHANNELS.getData();
    }

    static List<String> getKeys(Player player) {
        final List<ChatChannel> channels = new ArrayList<>();

        Map<Integer, Set<ChatChannel>> localMap;
        try {
            localMap = Reflector.of(ChannelHandler.class).get("LOCAL_MAP");
        } catch (Exception e) {
            return new ArrayList<>();
        }

        localMap.values().forEach(channels::addAll);
        channels.removeIf(c -> !c.hasPerm(player));

        return CollectionBuilder.of(channels).map(ChatChannel::getName).toList();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            BeansLib.logger().log("&cYou can't toggle a local chat in console.");
            return true;
        }

        if (isProhibited(sender)) return true;

        if (args.length == 0) return fromSender(sender).send("help");

        if (args.length != 1)
            return isWrongArgument(sender, args[args.length - 1]);

        Player p = (Player) sender;

        String key = null;
        for (String k : getKeys(p)) if (k.matches("(?i)" + args[0])) key = k;

        if (key == null) return isWrongArgument(p, args[0]);

        String path = "data." + p.getUniqueId() + "." + key;
        boolean isToggled = getData().get(path, false);

        getData().set(path, !isToggled);
        getData().save();

        return fromSender(p).addKeyValue("{channel}", key).send(String.valueOf(!isToggled));
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of().addArguments((s, a) -> getKeys((Player) s));
    }

    public static boolean isToggled(Player player, String key) {
        return YAMLData.Command.Multi.CHAT_VIEW.from(false).get("data." + player.getUniqueId() + "." + key, true);
    }
}
