package me.croabeast.sircore.objects;

import me.croabeast.cupdater.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.file.*;

import java.io.*;

public class YMLFile {

    private final Application main;
    private final Recorder recorder;

    private final String name, location;

    private FileConfiguration file;
    private File rawYmlFile;

    public YMLFile(Application main, String name) {
        this.main = main;
        this.recorder = main.getRecorder();

        this.name = name;
        this.location = name + ".yml";

        saveDefaultFile();
        reloadFile();
    }

    private File catchFile() { return new File(main.getDataFolder(), location); }
    public FileConfiguration getFile() { return file; }
    public void reloadFile() { file = YamlConfiguration.loadConfiguration(catchFile()); }

    private void saveFile() {
        if (file == null || rawYmlFile == null) return;
        try { this.getFile().save(this.rawYmlFile); }
        catch (Exception e) {
            recorder.doRecord("&7The &e" + location + "&7 file&c couldn't be saved&7.");
            e.printStackTrace();
        }
        recorder.doRecord("&7The &e" + location + "&7 file has been&a saved&7.");
    }

    private void updatingFile() {
        try { ConfigUpdater.update(main, location, catchFile(), null); }
        catch (Exception e) {
            recorder.doRecord("&7The &e" + location + "&7 file&c couldn't be updated&7.");
            e.printStackTrace();
        }
        recorder.doRecord("&7The &e" + location + "&7 file has been&a updated&7.");
    }

    private void saveDefaultFile() {
        if (rawYmlFile == null) rawYmlFile = catchFile();
        if (rawYmlFile.exists()) return;

        recorder.doRecord("&cFile " + location + " missing... &7Generating!");
        main.saveResource(location, false);
    }

    public void updateInitFile() {
        if (main.getConfig().getBoolean("updater.files." + name)) updatingFile();
        reloadFile();
    }
}
