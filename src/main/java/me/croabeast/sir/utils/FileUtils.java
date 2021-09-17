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

    private FileConfiguration custom;
    private File customFile;

    public void reloadFile(String file) {
        if (customFile == null) customFile = new File(main.getDataFolder(), file + ".yml");
        custom = YamlConfiguration.loadConfiguration(customFile);

        Reader defConfigStream;
        InputStream resource = main.getResource(file + ".yml");
        if (resource != null) {
            defConfigStream = new InputStreamReader(resource, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            custom.setDefaults(defConfig);
        }
    }

    public FileConfiguration getFile(String file) {
        if (custom == null) reloadFile(file);
        return custom;
    }

    public void saveDefaultFile(String file) {
        if (customFile == null) customFile = file(file);
        if (!customFile.exists()) main.saveResource(file + ".yml", false);
    }

    private void updateFile(String file) {
        String name = file + ".yml";
        try {
            ConfigUpdater.update(main, name, file(name), Collections.emptyList());
        } catch (IOException e) {
            consoleMsg("&7The " + file + ".yml file could not be updated...");
            e.printStackTrace();
        }
        main.reloadConfig();
    }

    private File file(String file) {
        return new File(main.getDataFolder(), file + ".yml");
    }

    public void registerFiles() {

        // For config.yml file
        if (!file("config").exists()) {
            consoleMsg("&cFile config.yml missing... &fGenerating!");
            main.getConfig().options().copyDefaults(true); main.saveDefaultConfig();
        }
        if (main.getConfig().contains("update.config")) {
            updateConfig = main.getConfig().getBoolean("update.config");
        }
        if (updateConfig) updateFile("config");

        // For lang.yml file
        if (!file("lang").exists()) {
            consoleMsg("&cFile lang.yml missing... &fGenerating!");
            main.getMessages().options().copyDefaults(true); saveDefaultFile("lang");
        }
        if (main.getConfig().contains("update.lang")) {
            updateLang = main.getConfig().getBoolean("update.lang");
        }
        if (updateLang) updateFile("messages");

        // For messages.yml file
        if (!file("messages").exists()) {
            consoleMsg("&cFile messages.yml missing... &fGenerating!");
            main.getMessages().options().copyDefaults(true); saveDefaultFile("messages");
        }
    }
}
