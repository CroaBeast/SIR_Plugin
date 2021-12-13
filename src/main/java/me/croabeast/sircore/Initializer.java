package me.croabeast.sircore;

import me.croabeast.sircore.listeners.*;
import me.croabeast.sircore.objects.*;
import net.milkbowl.vault.permission.*;
import org.bukkit.plugin.*;

import java.util.*;

public class Initializer {

    private final Application main;
    private final Records records;
    public static Permission Perms;

    public YmlFile chat;
    public YmlFile announces;
    public YmlFile lang;
    public YmlFile messages;
    public YmlFile motd;

    public int LISTENERS = 0;
    public int FILES = 0;

    public boolean HAS_PAPI;
    public boolean HAS_VAULT;
    public boolean DISCORD;

    public boolean HAS_LOGIN;
    protected List<String> LOGIN_HOOKS = new ArrayList<>();

    public boolean HAS_VANISH;
    protected List<String> VANISH_HOOKS = new ArrayList<>();

    public boolean authMe;
    public boolean userLogin;

    public boolean hasCMI;
    public boolean essentials;
    public boolean srVanish;
    public boolean prVanish;

    public Initializer(Application main) {
        this.main = main;
        records = main.getRecords();

        HAS_PAPI = isPlugin("PlaceholderAPI");
        HAS_VAULT = isPlugin("Vault");
        DISCORD = isPlugin("DiscordSRV");

        authMe = isHooked("AuthMe", LOGIN_HOOKS);
        userLogin = isHooked("UserLogin", LOGIN_HOOKS);

        hasCMI = isHooked("CMI", VANISH_HOOKS);
        essentials = isHooked("Essentials", VANISH_HOOKS);
        srVanish = isHooked("SuperVanish", VANISH_HOOKS);
        prVanish = isHooked("PremiumVanish", VANISH_HOOKS);
    }

    private boolean isPlugin(String name) { return main.getPlugin(name) != null; }

    private boolean isHooked(String name, List<String> hookList) {
        if (!isPlugin(name)) return false;
        hookList.add(name);
        return true;
    }

    protected Set<YmlFile> filesList = new HashSet<>();
    public Set<YmlFile> getFilesList() { return filesList; }

    public void loadSavedFiles() {
        records.doRecord("&bLoading plugin's files...");

        new YmlFile(main, "config");
        lang = new YmlFile(main, "lang");
        messages = new YmlFile(main, "messages");
        chat = new YmlFile(main, "chat");
        announces = new YmlFile(main, "announces");
        motd = new YmlFile(main, "motd");

        filesList.forEach(YmlFile::updateInitFile);

        records.doRecord("&7Loaded &e" + FILES + "&7 files in the plugin's folder.");
    }

    public void startMetrics() {
        Metrics metrics = new Metrics(main, 12806);

        metrics.addCustomChart(new Metrics.SimplePie("hasPAPI", () -> HAS_PAPI + ""));
        metrics.addCustomChart(new Metrics.SimplePie("hasVault", () -> HAS_VAULT + ""));

        metrics.addCustomChart(new Metrics.DrilldownPie("loginPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Login Plugins", 1);

            if (HAS_LOGIN) {
                if (userLogin) map.put("UserLogin", entry);
                else if (authMe) map.put("AuthMe", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));

        metrics.addCustomChart(new Metrics.DrilldownPie("vanishPlugins", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();

            entry.put("Vanish Plugins", 1);

            if (HAS_VANISH) {
                if (hasCMI) map.put("CMI", entry);
                else if (essentials) map.put("EssentialsX", entry);
                else if (srVanish) map.put("SuperVanish", entry);
                else if (prVanish) map.put("PremiumVanish", entry);
            }
            else map.put("None / Other", entry);

            return map;
        }));
    }

    public void setPluginHooks() {
        // PlaceholderAPI
        records.doRecord("", "&bChecking all simple plugin hooks...");
        showPluginInfo("PlaceholderAPI");

        // Permissions
        if (!HAS_VAULT)
            records.doRecord("&7Vault&c isn't installed&7, using default system.");
        else {
            ServicesManager servMngr = main.getServer().getServicesManager();
            RegisteredServiceProvider<Permission> rsp = servMngr.getRegistration(Permission.class);
            if (rsp != null) {
                Perms = rsp.getProvider();
                records.doRecord("&7Vault&a installed&7, hooking in a perm plugin...");
            }
            else records.doRecord("&7Unknown perm provider&7, using default system.");
        }

        // Login hook
        records.doRecord("", "&bChecking if a login plugin is enabled...");

        if (LOGIN_HOOKS.size() == 1) {
            HAS_LOGIN = true;
            showPluginInfo(LOGIN_HOOKS.get(0));
        }
        else {
            HAS_LOGIN = false;
            if (LOGIN_HOOKS.size() > 1)
                records.doRecord(
                        "&cTwo or more compatible login plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            else records.doRecord("&cNo login plugin installed. &7Unhooking...");
        }

        records.doRecord("", "&bChecking if a vanish plugin is enabled...");

        if (VANISH_HOOKS.size() == 1) {
            HAS_VANISH = true;
            showPluginInfo(VANISH_HOOKS.get(0));
        }
        else {
            HAS_VANISH = false;
            if (VANISH_HOOKS.size() > 1)
                records.doRecord(
                        "&cTwo or more compatible vanish plugins are installed.",
                        "&cPlease leave one of them installed."
                );
            else records.doRecord("&cNo vanish plugin installed. &7Unhooking...");
        }
    }

    public void registerListeners() {
        records.doRecord("", "&bLoading all the listeners...");
        new PlayerListener(main);
        new MOTDListener(main);
        new FormatListener(main);
        new LoginListener(main);
        new VanishListener(main);
        records.doRecord("&7Registered &e" + LISTENERS + "&7 plugin's listeners.");
    }

    public void reloadFiles() { filesList.forEach(YmlFile::reloadFile); }

    private void showPluginInfo(String name) {
        String pluginVersion;
        String isHooked;

        if (isPlugin(name)) {
            pluginVersion = main.getPlugin(name).getDescription().getVersion();
            isHooked = " &aenabled&7. Hooking...";
        } else {
            pluginVersion = "";
            isHooked = "&cnot found&7. Unhooking...";
        }

        records.doRecord("&7" + name + " " + pluginVersion + isHooked);
    }
}
