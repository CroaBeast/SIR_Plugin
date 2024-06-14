package me.croabeast.sir.plugin.module.hook;

import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import com.earth2me.essentials.Essentials;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.event.hook.SIRVanishEvent;
import me.croabeast.sir.plugin.SIRPlugin;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VanishHook extends JoinQuitRelated {

    private static final String[] SUPPORTED_PLUGINS = {
            "Essentials", "CMI", "SuperVanish", "PremiumVanish"
    };

    private static final List<Plugin> ENABLED_HOOKS = new ArrayList<>();

    static {
        for (String s : SUPPORTED_PLUGINS) {
            Plugin p = Bukkit.getPluginManager().getPlugin(s);
            if (p == null || !p.isEnabled()) continue;

            if (ENABLED_HOOKS.isEmpty()) ENABLED_HOOKS.add(p);
        }
    }

    private static final Set<LoadedListener> LISTENER_SET = new LinkedHashSet<>();

    VanishHook() {
        super(false);
    }

    static void loadHook() {
        if (!isHookEnabled()) return;

        if (Exceptions.isPluginEnabled("Essentials"))
            new LoadedListener() {
                @EventHandler
                private void onVanish(VanishStatusChangeEvent event) {
                    IUser user = event.getAffected();
                    new SIRVanishEvent(user.getBase(), user.isVanished()).call();
                }
            }.register(SIRPlugin.getInstance());

        if (Exceptions.isPluginEnabled("CMI")) {
            new LoadedListener() {
                @EventHandler
                private void onVanish(CMIPlayerVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), false).call();
                }
                @EventHandler
                private void onUnVanish(CMIPlayerUnVanishEvent event) {
                    new SIRVanishEvent(event.getPlayer(), true).call();
                }
            }.register(SIRPlugin.getInstance());
        }

        if (Exceptions.arePluginsEnabled(false, "SuperVanish", "PremiumVanish"))
            new LoadedListener() {
                @EventHandler
                private void onVanish(PlayerVanishStateChangeEvent event) {
                    Player player = Bukkit.getPlayer(event.getUUID());
                    new SIRVanishEvent(player, !event.isVanishing()).call();
                }
            }.register(SIRPlugin.getInstance());
    }

    static void unloadHook() {
        if (isHookEnabled()) LISTENER_SET.forEach(LoadedListener::unregister);
    }

    @Override
    public boolean isEnabled() {
        return isHookEnabled() && super.isEnabled();
    }

    @EventHandler
    private void onVanish(SIRVanishEvent event) {
        if (!isEnabled()) return;

        final boolean isVanished = event.isVanished();

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!isEnabled()) return;

        if (LOGIN.isEnabled())
            if (isVanished) LoginHook.addPlayer(player);
            else LoginHook.removePlayer(player);

        String type = isVanished ? "JOIN" : "QUIT";

        Object unit = getUnit(type, player);
        if (unit == null) return;

        int timer = jqConfig.get("cooldown." + type.toLowerCase(Locale.ENGLISH), 0);
        Map<UUID, Long> players = isVanished ? getJoinMap() : getQuitMap();

        if (timer > 0 && players.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - players.get(uuid);
            if (rest < timer * 1000L) return;
        }

        performActions(unit, player);
        if (timer > 0) players.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    private void onChat(AsyncPlayerChatEvent event) {
        ConfigurationSection s = jqConfig.getSection("chat-key");
        if (s == null || event.isCancelled()) return;

        if (!isEnabled()) return;
        if (!s.getBoolean("enabled")) return;

        String key = s.getString("key");
        if (StringUtils.isBlank(key)) return;

        String message = event.getMessage();
        Player player = event.getPlayer();

        List<String> notAllow = TextUtils.toList(s, "not-allowed");
        MessageSender sender = MessageSender.loaded().setTargets(player);

        if (s.getBoolean("regex")) {
            Matcher match = Pattern.compile(key).matcher(message);

            if (!match.find()) {
                event.setCancelled(true);
                sender.send(notAllow);
                return;
            }

            event.setMessage(message.replace(match.group(), ""));
            return;
        }

        String place = s.getString("place", "");
        boolean isPrefix = !place.matches("(?i)suffix");

        String pattern = (isPrefix ? "^" : "") +
                Pattern.quote(key) +
                (isPrefix ? "" : "$");

        Matcher match = Pattern.compile(pattern).matcher(message);

        if (!match.find()) {
            event.setCancelled(true);
            sender.send(notAllow);
            return;
        }

        event.setMessage(message.replace(match.group(), ""));
    }

    public static boolean isVanished(Player player) {
        if (!VANISH.isEnabled() || player == null) return false;

        Plugin e = Bukkit.getPluginManager().getPlugin("Essentials"),
                c = Bukkit.getPluginManager().getPlugin("CMI");

        if (e != null)
            return ((Essentials) e).getUser(player).isVanished();

        if (c != null)
            return CMIUser.getUser(player).isVanished();

        for (MetadataValue meta : player.getMetadata("vanished"))
            if (meta.asBoolean()) return true;

        return false;
    }

    public static boolean isVisible(Player player) {
        return !isVanished(player);
    }

    @Nullable
    public static Plugin getHook() {
        return isHookEnabled() ? ENABLED_HOOKS.get(0) : null;
    }

    public static boolean isHookEnabled() {
        return ENABLED_HOOKS.size() == 1;
    }

    static class LoadedListener implements CustomListener {

        @Getter @Setter
        private boolean registered = false;

        LoadedListener() {
            LISTENER_SET.add(this);
        }

        public void unregister() {
            CustomListener.super.unregister();
            LISTENER_SET.remove(this);
        }
    }
}
