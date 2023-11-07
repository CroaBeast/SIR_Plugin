package me.croabeast.sir.api.extension;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.sir.plugin.SIRPlugin;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
abstract class SIRExtension {

    /**
     * The folder that will storages any file of the extension.
     */
    @NotNull
    private final File dataFolder;

    /**
     * The defined name of the extension.
     */
    @NotNull
    protected String name = getClass().getSimpleName();

    /**
     * The version of the extension.
     */
    private String version = "1.0";

    @Getter(AccessLevel.NONE)
    private boolean enabled = false, loaded = false;

    protected SIRExtension(@NotNull String name, String folderPath) {
        if (StringUtils.isNotBlank(name)) this.name = name;

        String path = StringUtils.isBlank(folderPath) ? "extensions" : folderPath;
        dataFolder = new File(SIRPlugin.getSIRFolder(), path);
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

    @Override
    public String toString() {
        return "SIRExtension{name='" + getName() + "', version='" + getVersion() + "'}";
    }
}
