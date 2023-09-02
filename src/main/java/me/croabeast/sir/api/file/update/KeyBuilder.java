package me.croabeast.sir.api.file.update;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

class KeyBuilder implements Cloneable {

    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    private KeyBuilder(KeyBuilder keyBuilder) {
        this.config = keyBuilder.config;
        this.separator = keyBuilder.separator;
        this.builder = new StringBuilder(keyBuilder.toString());
    }

    public void parseLine(String line, boolean checkIfExists) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");

        if (currentSplitLine.length > 2)
            currentSplitLine = line.split(": ");

        String key = currentSplitLine[0].replace("'", "").replace("\"", "");

        if (checkIfExists)
            while (builder.length() > 0 && !config.contains(builder.toString() + separator + key))
                removeLastKey();

        if (builder.length() > 0) builder.append(separator);

        builder.append(key);
    }

    public String getLastKey() {
        if (builder.length() == 0) return "";
        return builder.toString().split("[" + separator + "]")[0];
    }

    public boolean isEmpty() {
        return builder.length() == 0;
    }

    public void clear() {
        builder.setLength(0);
    }

    public boolean isSubKeyOf(String parentKey) {
        return KeyUtils.isSubKeyOf(parentKey, builder.toString(), separator);
    }

    public boolean isSubKey(String subKey) {
        return KeyUtils.isSubKeyOf(builder.toString(), subKey, separator);
    }

    public boolean isConfigSection() {
        String key = builder.toString();
        return config.isConfigurationSection(key);
    }

    public boolean isConfigSectionWithKeys() {
        String key = builder.toString();
        ConfigurationSection cs = config.getConfigurationSection(key);

        return config.isConfigurationSection(key) &&
                cs != null &&
                !cs.getKeys(false).isEmpty();
    }

    public void removeLastKey() {
        if (builder.length() == 0) return;

        String key = builder.toString();
        String[] split = key.split("[" + separator + "]");

        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
        builder.replace(minIndex, builder.length(), "");
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    public KeyBuilder clone() {
        try {
            super.clone();
            return new KeyBuilder(this);
        } catch (Exception e) {
            return this;
        }
    }
}
