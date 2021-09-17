package me.croabeast.sir;

import me.croabeast.sir.events.OnJoin;
import me.croabeast.sir.events.OnLogin;
import me.croabeast.sir.events.OnQuit;
import me.croabeast.sir.utils.CmdUtils;
import me.croabeast.sir.utils.FileUtils;
import me.croabeast.sir.utils.LangUtils;
import me.croabeast.sir.utils.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SIR extends JavaPlugin {

    private SIR main;
    private FileUtils fileUtils;
    private LangUtils langUtils;

    private String version;

    @Override
    public void onEnable() {
        main = this;
        version = main.getDescription().getVersion();

        fileUtils = new FileUtils(main);
        langUtils = new LangUtils(main);

        new Metrics(main, 12806); // The bStats class.

        new CmdUtils(main);

        logMsg(" &7-------- \u00BB Simple In-game Receptionist by CroaBeast \u00AB -------- ");

        fileUtils.registerFiles();
        new OldMessages(main);
        if (langUtils.hasUserLogin) new OnLogin(main);
        new OnJoin(main);
        new OnQuit(main);

        logMsg("&6[SIR] &7Checking &e"+ Bukkit.getVersion()+"&7...");
        logMsg("&6[SIR] &e" + langUtils.serverName + " &7detected.");

        logMsg("&6[SIR] &7PlaceholderAPI " + showPluginInfo("PlaceholderAPI"));
        logMsg("&6[SIR] &7UserLogin " + showPluginInfo("UserLogin"));
        logMsg("&6[SIR] &fSIR " + version + "&7 was&a loaded&7 successfully&7.");
    }

    @Override
    public void onDisable() {
        main = null;
        logMsg("&4[SIR] &7SIR &f" + version + "&7 was totally disabled &cu-u");
    }

    private void logMsg(String msg) { Bukkit.getConsoleSender().sendMessage(langUtils.parseColor(msg)); }

    private String showPluginInfo(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        String version = plugin != null ? plugin.getDescription().getVersion() + " " : "";
        String isPlugin = plugin != null ? "&aenabled. &7Hooking..." : "&cnot found. &7Hook disabled.";
        return version + isPlugin;
    }

    public LangUtils getLangUtils() { return langUtils; }

    public FileConfiguration getLang() { return fileUtils.getLang(); }
    public FileConfiguration getMessages() { return fileUtils.getMessages(); }

    public void reloadFiles() {
        main.reloadConfig();
        fileUtils.reloadLang();
        fileUtils.reloadMessages();
    }

    private static class OldMessages implements Listener {

        public OldMessages(SIR main) {
            main.getServer().getPluginManager().registerEvents(this, main);
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent e) { e.setJoinMessage(""); }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) { e.setQuitMessage(""); }
    }
}
