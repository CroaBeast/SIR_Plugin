package me.croabeast.sircore.objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Consumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class Updater {

    private final JavaPlugin plugin;
    private final int id;

    public Updater(JavaPlugin plugin, int id) {
        this.plugin = plugin;
        this.id = id;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String url = "https://api.spigotmc.org/legacy/update.php?resource=";
            try (InputStream inputStream = new URL(url + this.id).openStream();
                 Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) consumer.accept(scanner.next());
            } catch (IOException exception) {
                plugin.getLogger().info("Unable to check for updates: " + exception.getMessage());
            }
        });
    }
}
