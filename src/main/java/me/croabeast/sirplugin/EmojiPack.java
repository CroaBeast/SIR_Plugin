package me.croabeast.sirplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("all")
public class EmojiPack extends JavaPlugin {

    private final Map<String, Integer> customEmojiMap = new HashMap<>();
    private final Map<UUID, Integer> playerVersionMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Load the custom emojis from the file
        File configFile = new File(getDataFolder(), "emojis.yml");
        loadCustomEmojis(configFile);

        // Schedule a task to generate and apply the resource pack to players
        BukkitRunnable packTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check if the player's version is compatible with the custom emojis
                    int playerVersion = playerVersionMap.getOrDefault(player.getUniqueId(), -1);
                    if (playerVersion < 0 || playerVersion >= 114) {
                        // Player version is not compatible, skip resource pack generation
                        continue;
                    }

                    // Generate the resource pack
                    String packName = "custom_emojis_" + player.getUniqueId();
                    File packFile = generateResourcePack(playerVersion, packName);

                    // Apply the resource pack to the player
                    player.setResourcePack(packFile.toURI().toString(), packName.getBytes());
                }
            }
        };
        packTask.runTaskTimer(this, 0L, 600L); // run every 30 seconds
    }

    @Override
    public void onDisable() {
        // Save the custom emojis to the file
        File configFile = new File(getDataFolder(), "emojis.yml");
        saveCustomEmojis(configFile);
    }

    private void loadCustomEmojis(File configFile) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String key : config.getConfigurationSection("emojis").getKeys(false)) {
            String codepointStr = config.getString("emojis." + key);
            int codepoint = Character.codePointAt(codepointStr, 0);
            customEmojiMap.put(key, codepoint);
        }
    }

    private void saveCustomEmojis(File configFile) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("emojis", null);

        for (Map.Entry<String, Integer> entry : customEmojiMap.entrySet()) {
            String key = entry.getKey();
            String codepointStr = new String(Character.toChars(entry.getValue()));
            config.set("emojis." + key, codepointStr);
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().warning("Failed to save custom emojis to " + configFile.getName() + ": " + e.getMessage());
        }
    }

    private File generateResourcePack(int playerVersion, String packName) {
        // Create a temporary directory for the resource pack files
        File tempDir = new File(getDataFolder(), "resource_packs");
        tempDir.mkdirs();

        // Generate the resource pack assets and pack.mcmeta file
        String packFormat = (playerVersion >= 565) ? "7" : "6";
        String packDescription = "Custom emojis for " + packName;
        String packAssets = generatePackAssets();
        String packMcmeta = generatePackMcmeta(packFormat, packDescription);

        // Write the assets and pack.mcmeta files to disk
        try {
            File packDir = new File(tempDir, packName);
            packDir.mkdirs();
            File assetsDir = new File(packDir, "assets");
            assetsDir.mkdirs();
            File minecraftDir = new File(assetsDir, "minecraft");
            minecraftDir.mkdirs();
            Files.write(new File(minecraftDir, "lang/en_us.lang").toPath(), packAssets.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(packDir, "pack.mcmeta").toPath(), packMcmeta.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            getLogger().warning("Failed to write resource pack files: " + e.getMessage());
        }

        // Create a .zip archive of the resource pack directory
        File zipFile = new File(tempDir, packName + ".zip");
        try {
            zipDirectory(zipFile, new File(tempDir, packName));
        } catch (IOException e) {
            getLogger().warning("Failed to create .zip archive of resource pack: " + e.getMessage());
        }

        // Return the .zip archive file
        return zipFile;
    }

    static void zipDirectory(File zipFile, File directory) throws IOException {
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()));
        zipDirectory(out, "", directory);
        out.close();
    }

    static void zipDirectory(ZipOutputStream out, String path, File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(out, path + file.getName() + "/", file);
                continue;
            }

            byte[] data = Files.readAllBytes(file.toPath());
            ZipEntry entry = new ZipEntry(path + file.getName());
            out.putNextEntry(entry);
            out.write(data, 0, data.length);
            out.closeEntry();
        }
    }

    private String generatePackAssets() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : customEmojiMap.entrySet()) {
            int codepoint = entry.getValue();

            builder.append("text.chat.").
                    append(entry.getKey()).append("=").
                    append(ChatColor.RESET).
                    append(new String(Character.toChars(codepoint))).
                    append("\n");
        }

        return builder.toString();
    }

    private String generatePackMcmeta(String packFormat, String packDescription) {
        return "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + packFormat + ",\n" +
                "    \"description\": \"" + packDescription + "\"\n" +
                "  }\n" +
                "}";
    }
}
