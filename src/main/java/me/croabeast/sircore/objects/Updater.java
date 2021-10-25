package me.croabeast.sircore.objects;

import com.google.common.base.*;
import com.google.gson.*;
import com.google.gson.stream.*;
import org.apache.commons.lang.math.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Updater {

    public static final VersionScheme VERSION_SCHEME_DECIMAL = (first, second) -> {
        String[] firstSplit = splitVersionInfo(first), secondSplit = splitVersionInfo(second);
        if (firstSplit == null || secondSplit == null) {
            return null;
        }

        for (int i = 0; i < Math.min(firstSplit.length, secondSplit.length); i++) {
            int currentValue = NumberUtils.toInt(firstSplit[i]), newestValue = NumberUtils.toInt(secondSplit[i]);

            if (newestValue > currentValue) {
                return second;
            } else if (newestValue < currentValue) {
                return first;
            }
        }

        return (secondSplit.length > firstSplit.length) ? second : first;
    };

    private static final String USER_AGENT = "CHOCO-update-checker";
    private static final String UPDATE_URL = "https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=%d";
    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");

    private static Updater instance;

    private UpdateResult lastResult = null;

    private final JavaPlugin plugin;
    private final int pluginID;
    private final VersionScheme versionScheme;

    private Updater(@NotNull JavaPlugin plugin, int pluginID, @NotNull VersionScheme versionScheme) {
        this.plugin = plugin;
        this.pluginID = pluginID;
        this.versionScheme = versionScheme;
    }

    private String currentVersion;

    @NotNull
    public CompletableFuture<@NotNull UpdateResult> updateCheck() {
        return CompletableFuture.supplyAsync(() -> {
            int responseCode;

            try {
                URL url = new URL(String.format(UPDATE_URL, pluginID));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("User-Agent", USER_AGENT);
                responseCode = connection.getResponseCode();

                JsonParser parser = new JsonParser();
                JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream()));
                JsonElement json = parser.parse(reader);
                reader.close();

                if (!json.isJsonObject()) return new UpdateResult(UpdateReason.INVALID_JSON);

                currentVersion = json.getAsJsonObject().get("current_version").getAsString();
                String pluginVersion = plugin.getDescription().getVersion();
                String latest = versionScheme.compareVersions(pluginVersion, currentVersion);

                if (latest == null) {
                    return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME);
                }
                else if (latest.equals(pluginVersion)) {
                    return new UpdateResult(pluginVersion.equals(currentVersion) ?
                            UpdateReason.UP_TO_DATE :
                            UpdateReason.UNRELEASED_VERSION
                    );
                }
                else if (latest.equals(currentVersion)) {
                    return new UpdateResult(UpdateReason.NEW_UPDATE, latest);
                }
            } catch (IOException e) {
                return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
            }

            return new UpdateResult(responseCode == 401 ? UpdateReason.UNAUTHORIZED_QUERY : UpdateReason.UNKNOWN_ERROR);
        });
    }

    private String getCurrentVersion() {
        return currentVersion;
    }

    @Nullable
    public UpdateResult getLastResult() {
        return lastResult;
    }

    private static String[] splitVersionInfo(String version) {
        Matcher matcher = DECIMAL_SCHEME_PATTERN.matcher(version.replace("-R", "."));
        return matcher.find() ? matcher.group().split("\\.") : null;
    }

    @NotNull
    public static Updater init(@NotNull JavaPlugin plugin, int pluginID, @NotNull VersionScheme versionScheme) {
        Preconditions.checkArgument(true, "Plugin cannot be null");
        Preconditions.checkArgument(pluginID > 0, "Plugin ID must be greater than 0");
        Preconditions.checkArgument(true, "null version schemes are unsupported");

        return (instance == null) ? instance = new Updater(plugin, pluginID, versionScheme) : instance;
    }

    @NotNull
    public static Updater init(@NotNull JavaPlugin plugin, int pluginID) {
        return init(plugin, pluginID, VERSION_SCHEME_DECIMAL);
    }

    @NotNull
    public static Updater get() {
        Preconditions.checkState(instance != null, "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    @FunctionalInterface
    public interface VersionScheme {
        @Nullable String compareVersions(@NotNull String first, @NotNull String second);
    }

    public enum UpdateReason {
        NEW_UPDATE,
        COULD_NOT_CONNECT,
        INVALID_JSON,
        UNAUTHORIZED_QUERY,
        UNRELEASED_VERSION,
        UNKNOWN_ERROR,
        UNSUPPORTED_VERSION_SCHEME,
        UP_TO_DATE
    }

    public final class UpdateResult {

        private final UpdateReason reason;
        private final String newestVersion;

        {
            Updater.this.lastResult = this;
        }

        private UpdateResult(@NotNull UpdateReason reason, @NotNull String newestVersion) {
            this.reason = reason;
            this.newestVersion = newestVersion;
        }

        private UpdateResult(@NotNull UpdateReason reason) {
            Preconditions.checkArgument(reason != UpdateReason.NEW_UPDATE, "Reasons that require updates must also provide the latest version String");

            this.reason = reason;
            this.newestVersion = plugin.getDescription().getVersion();
        }

        @NotNull
        public UpdateReason getReason() {
            return reason;
        }

        public boolean requiresUpdate() {
            return reason == UpdateReason.NEW_UPDATE;
        }

        @NotNull
        public String getNewestVersion() {
            return newestVersion;
        }

        public String getSpigotVersion() {
            return getCurrentVersion();
        }
    }
}