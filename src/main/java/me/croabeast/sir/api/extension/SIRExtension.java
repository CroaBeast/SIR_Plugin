package me.croabeast.sir.api.extension;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.sir.plugin.SIRPlugin;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
public abstract class SIRExtension {

    /**
     * The folder that will storages any file of the extension.
     */
    @NotNull
    private final File dataFolder;

    /**
     * The defined name of the extension.
     */
    @NotNull
    private String name = getClass().getSimpleName();

    /**
     * The version of the extension.
     */
    private String version = "1.0";

    @Getter(AccessLevel.NONE)
    private boolean enabled = false, loaded = false;

    @SuppressWarnings("all")
    SIRExtension(String name, String folderPath) {
        if (StringUtils.isNotBlank(name)) this.name = name;

        final String f = folderPath;

        dataFolder = new File(
                SIRPlugin.getInstance().getDataFolder(),
                StringUtils.isBlank(f) ? "extensions" : f
        );
    }

    public final void setVersion(String version) {
        if (StringUtils.isNotBlank(version)) this.version = version;
    }

    public final boolean isEnabled() {
        return !loaded ? onLoading() : enabled;
    }

    public final boolean onLoading() {
        if (loaded) {
            String s = "This extension was already loaded.";
            throw new IllegalStateException(s);
        }

        if (!onEnabling()) {
            String.valueOf(onDisabling());
            return false;
        }

        return enabled = loaded = true;
    }

    protected abstract boolean onEnabling();

    protected abstract boolean onDisabling();

    @NotNull
    public String toString() {
        final String c = getClass().getSuperclass().getSimpleName(), n = getName();
        return c + "{" + (c.equals(n) ? "Unknown Extension" : (n + ":")) + getVersion() + "}";
    }
}
