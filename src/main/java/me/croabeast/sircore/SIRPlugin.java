package me.croabeast.sircore;

import me.croabeast.sircore.utils.*;
import org.bukkit.configuration.file.*;
import org.bukkit.plugin.java.*;

public final class SIRPlugin extends JavaPlugin {

    private SIRPlugin main;
    private MainCore mainCore;
    private TextUtils textUtils;
    private EventUtils eventUtils;

    @Override
    public void onEnable() {
        main = this; // The plugin instance initializing...

        mainCore = new MainCore(main);
        textUtils = new TextUtils(main);
        eventUtils = new EventUtils(main);
        textUtils.loadLangClasses(); // Loading Title and Action Bar

        new Metrics(main, 12806); // The bStats class.
        new CmdUtils(main); // Register the main command for the plugin

        mainCore.setBooleans();
        mainCore.enablingHeader(textUtils.serverName);
        mainCore.setSavedFiles();
        mainCore.setPAPI();
        mainCore.setPermissions();
        mainCore.setLoginHook();
        mainCore.setVanishHook();
        mainCore.registerEvents();
        mainCore.enablingFooter();
    }

    public void onDisable() {
        mainCore.disableMessage();
        main = null; // This will prevent any memory leaks.
    }

    public FileConfiguration getLang() { return mainCore.getLang().getFile(); }
    public FileConfiguration getMessages() { return mainCore.getMessages().getFile(); }

    public MainCore getMainCore() { return mainCore; }
    public TextUtils getLangUtils() { return textUtils; }
    public EventUtils getEventUtils() { return eventUtils; }

    public void reloadFiles() {
        mainCore.getConfig().reloadFile();
        mainCore.getLang().reloadFile();
        mainCore.getMessages().reloadFile();
    }
}
