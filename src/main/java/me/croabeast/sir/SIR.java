package me.croabeast.sir;

import me.croabeast.sir.events.OnJoin;
import me.croabeast.sir.events.login.AuthMe;
import me.croabeast.sir.events.login.UserLogin;
import me.croabeast.sir.events.OnQuit;
import me.croabeast.sir.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SIR extends JavaPlugin {

    private SIR main;
    private FileUtils fileUtils;
    private LangUtils langUtils;
    private EventUtils eventUtils;

    private String version;
    public boolean hasPAPI;
    public boolean hasLogin;
    public boolean afterLogin;
    public boolean userLogin;
    public boolean authMe;

    @Override
    public void onEnable() {
        main = this;
        version = main.getDescription().getVersion();
        setBooleans();

        fileUtils = new FileUtils(main);
        langUtils = new LangUtils(main);
        eventUtils = new EventUtils(main);
        langUtils.loadLangClasses();

        new Metrics(main, 12806); // The bStats class.
        new CmdUtils(main);

        logMsg(" &7-------- \u00BB Simple In-game Receptionist by CroaBeast \u00AB -------- ");

        logMsg("&6[SIR] &7Checking &e"+ Bukkit.getVersion()+"&7...");
        logMsg("&6[SIR] &e" + langUtils.serverName + " &7detected.");

        fileUtils.registerFiles();

        showPluginInfo("PlaceholderAPI");
        setLogin();

        new OldMessages(main);
        if (hasLogin) {
            if (userLogin) new UserLogin(main);
            if (authMe) new AuthMe(main);
        }
        new OnJoin(main);
        new OnQuit(main);

        logMsg("&6[SIR] &fSIR " + version + "&7 was&a loaded&7 successfully&7.");
    }

    @Override
    public void onDisable() {
        main = null;
        logMsg("&4[SIR] &7SIR &f" + version + "&7 was totally disabled &cu-u");
    }

    private void setLogin() {
        int i = 0; String loginPlugin = null;
        if (authMe) { i = 1 + i; loginPlugin = "AuthMe"; }
        if (userLogin) { i = 1 + i; if (i < 2) loginPlugin = "UserLogin"; }

        logMsg("&6[SIR] &7Checking if a compatible login plugin installed...");

        if (i > 1) { hasLogin = false;
            logMsg("&6[SIR] &cTwo or more compatible login plugins are installed.");
            logMsg("&6[SIR] &cPlease delete the extra ones and leave one of them.");
        } else if (i == 1) { hasLogin = true;
            showPluginInfo(loginPlugin);
        } else { hasLogin = false;
            logMsg("&6[SIR] &cThere is no login plugin installed. &7Unhooking...");
        }
    }

    private void logMsg(String msg) { Bukkit.getConsoleSender().sendMessage(langUtils.parseColor(msg)); }
    PluginManager pMngr = Bukkit.getPluginManager();

    private void setBooleans() {
        this.hasPAPI = pMngr.isPluginEnabled("PlaceholderAPI");
        this.authMe = pMngr.isPluginEnabled("AuthMe");
        this.userLogin = pMngr.isPluginEnabled("UserLogin");
        this.afterLogin = main.getConfig().getBoolean("options.after-login");
    }

    private void showPluginInfo(String pluginName) {
        Plugin plugin = pMngr.getPlugin(pluginName);
        String version = plugin != null ? plugin.getDescription().getVersion() + " " : "";
        String isPlugin = plugin != null ? "&aenabled. &7Hooking..." : "&cnot found. &7Unhooking...";
        logMsg("&6[SIR] &7" + pluginName + " " + version + isPlugin);
    }

    public LangUtils getLangUtils() { return langUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

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
