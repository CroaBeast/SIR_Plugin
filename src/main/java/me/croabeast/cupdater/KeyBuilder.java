package me.croabeast.cupdater;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class KeyBuilder implements Cloneable {

    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    private KeyBuilder(KeyBuilder keyBuilder) {
        this.config = keyBuilder.config;
        this.separator = keyBuilder.separator;
        this.builder = new StringBuilder(keyBuilder.toString());
    }

    public void parseLine(String line) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");
        while (builder.length() > 0 && !config.contains(builder.toString() + separator + currentSplitLine[0])) removeLastKey();
        if (builder.length() > 0) builder.append(separator);
        builder.append(currentSplitLine[0]);
    }

    public String getLastKey() {
        if (builder.length() == 0) return "";
        return builder.toString().split("[" + separator + "]")[0];
    }

    public boolean isEmpty() {
        return builder.length() == 0;
    }

    public boolean isSubKeyOf(String parentKey) {
        return isSubKeyOf(parentKey, builder.toString(), separator);
    }

    public boolean isSubKey(String subKey) {
        return isSubKeyOf(builder.toString(), subKey, separator);
    }

    public static String getIndents(String key, char separator) {
        String[] splitKey = key.split("[" + separator + "]");
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < splitKey.length; i++) builder.append("  ");
        return builder.toString();
    }

    public boolean isConfigSection() {
        String key = builder.toString();
        return config.isConfigurationSection(key);
    }

    public boolean isConfigSectionWithKeys() {
        String key = builder.toString();
        ConfigurationSection id = config.getConfigurationSection(key);
        return id != null && !id.getKeys(false).isEmpty();
    }

    public void removeLastKey() {
        if (builder.length() == 0) return;
        String keyString = builder.toString();
        String[] split = keyString.split("[" + separator + "]");
        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
        builder.replace(minIndex, builder.length(), "");
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    protected KeyBuilder clone() throws CloneNotSupportedException {
        return new KeyBuilder((KeyBuilder) super.clone());
    }

    public static boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (parentKey.isEmpty()) return false;
        return subKey.startsWith(parentKey) && subKey.startsWith(String.valueOf(separator), parentKey.length());
    }
}
