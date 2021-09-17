package me.croabeast.sir.utils;

import me.croabeast.cupdater.ConfigUpdater;
import me.croabeast.sir.SIR;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class FileUtils {

    private final SIR main;

    private boolean updateConfig;
    private boolean updateLang;

    public FileUtils(SIR main) {
        this.main = main;
        updateConfig = true;
        updateLang = true;
    }

    private void consoleMsg(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&',"&e[ERMA] "+msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    private FileConfiguration lang;
    private FileConfiguration messages;
    private File langFile;
    private File messagesFile;

    public void reloadLang() {
        if (langFile == null) langFile = get("lang");
        lang = YamlConfiguration.loadConfiguration(langFile);

        Reader defConfigStream;
        InputStream resource = main.getResource("lang.yml");
        if (resource != null) {
            defConfigStream = new InputStreamReader(resource, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            lang.setDefaults(defConfig);
        }
    }

    public FileConfiguration getLang() {
        if (lang == null) reloadLang();
        return lang;
    }

    public void saveDefaultLang() {
        if (langFile == null) langFile = get("lang");
        if (!langFile.exists()) main.saveResource("lang.yml", false);
    }

    public void reloadMessages() {
        if (messagesFile == null) messagesFile = get("lang");
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        Reader defConfigStream;
        InputStream resource = main.getResource("messages.yml");
        if (resource != null) {
            defConfigStream = new InputStreamReader(resource, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            messages.setDefaults(defConfig);
        }
    }

    public FileConfiguration getMessages() {
        if (messages == null) reloadMessages();
        return messages;
    }

    public void saveDefaultMessages() {
        if (messagesFile == null) messagesFile = get("messages");
        if (!messagesFile.exists()) main.saveResource("messages.yml", false);
    }

    private void updateFile(String file) {
        String name = file + ".yml";
        try {
            ConfigUpdater.update(main, name, get(file), Collections.emptyList());
        } catch (IOException e) {
            consoleMsg("&7The " + file + ".yml file could not be updated...");
            e.printStackTrace();
        }
        main.reloadConfig();
    }

    private File get(String fileName) {
        return new File(main.getDataFolder(), fileName + ".yml");
    }

    public void registerFiles() {

        // For config.yml file
        if (!get("config").exists()) {
            consoleMsg("&cFile config.yml missing... &fGenerating!");
            main.getConfig().options().copyDefaults(true); main.saveDefaultConfig();
        }
        if (main.getConfig().contains("update.config")) {
            updateConfig = main.getConfig().getBoolean("update.config");
        }
        if (updateConfig) updateFile("config");

        // For lang.yml file
        if (!get("lang").exists()) {
            consoleMsg("&cFile lang.yml missing... &fGenerating!");
            main.getLang().options().copyDefaults(true); saveDefaultLang();
        }
        if (main.getConfig().contains("update.lang")) {
            updateLang = main.getConfig().getBoolean("update.lang");
        }
        if (updateLang) updateFile("lang");

        // For messages.yml file
        if (!get("messages").exists()) {
            consoleMsg("&cFile messages.yml missing... &fGenerating!");
            saveDefaultMessages();
        }
    }
}
