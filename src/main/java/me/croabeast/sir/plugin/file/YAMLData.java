package me.croabeast.sir.plugin.file;

import lombok.experimental.UtilityClass;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.logger.BeansLogger;
import me.croabeast.lib.reflect.Reflector;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.DataHandler;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.logger.DelayLogger;
import me.croabeast.sir.plugin.util.LangUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@UtilityClass
public class YAMLData implements DataHandler {

    private final Map<String, ConfigurableFile> FILE_MAP = new LinkedHashMap<>();
    private boolean areFilesLoaded = false;

    private static class SIRFile extends ConfigurableFile {

        SIRFile(String folder, String name) throws IOException {
            super(SIRPlugin.getInstance(), folder, name);
            setResourcePath("resources" + File.separator + getLocation());
        }

        @Override
        public boolean isUpdatable() {
            return get("update", false);
        }
    }

    private List<String> filePaths() {
        try {
            return Reflector.of("me.croabeast.sir.plugin.SIRLoader").get("JAR_FILE_PATHS");
        } catch (Exception e) {
            return null;
        }
    }

    private static class Counter {

        private int loaded = 0;
        private int updated = 0;
        private int failed = 0;

        boolean isModified() {
            return loaded > 0 || updated > 0 || failed > 0;
        }

        void clear() {
            updated = 0;
            loaded = 0;
            failed = 0;
        }
    }

    private final Counter FILES_COUNTER = new Counter();

    @Priority(5)
    void loadData() {
        DelayLogger logger = DelayLogger.simplified();
        logger.add(true, "Loading files...");

        if (areFilesLoaded) {
            if (FILES_COUNTER.isModified())
                FILES_COUNTER.clear();

            for (YAMLFile file : FILE_MAP.values()) {
                file.reload();
                FILES_COUNTER.loaded++;

                if (file.update())
                    FILES_COUNTER.updated++;
            }

            logger.add(true,
                    "Reloaded: " + FILES_COUNTER.loaded +
                            ", Updated: " + FILES_COUNTER.updated +
                            ", Failed: " + FILES_COUNTER.failed
            );

            if (!FILES_COUNTER.isModified() || FILES_COUNTER.failed > 0)
                logger.add(true,
                        "Files not loaded correctly! Report to CroaBeast."
                );

            logger.sendLines();
            return;
        }

        List<String> paths = filePaths();
        if (paths == null) return;

        if (FILES_COUNTER.isModified())
            FILES_COUNTER.clear();

        for (String path : paths) {
            String separator = Pattern.quote(File.separator);
            String[] parts = path.split(separator);

            final String folder, name;
            int length = parts.length;

            if (length > 1) {
                StringBuilder builder = new StringBuilder();
                int last = length - 1;

                for (int i = 0; i < last; i++) {
                    builder.append(parts[i]);

                    if (i == last - 1) continue;
                    builder.append(File.separator);
                }

                folder = builder.toString();
                name = parts[last];
            } else {
                folder = null;
                name = path;
            }

            try {
                ConfigurableFile file = new SIRFile(folder, name);
                if (file.saveDefaults()) {
                    FILE_MAP.put(path, file);
                    FILES_COUNTER.loaded++;

                    if (file.update())
                        FILES_COUNTER.updated++;
                }
                else FILES_COUNTER.failed++;
            } catch (Exception e) {
                FILES_COUNTER.failed++;
                e.printStackTrace();
            }
        }

        logger.add(true,
                "Loaded: " + FILES_COUNTER.loaded +
                        ", Updated: " + FILES_COUNTER.updated +
                        ", Failed: " + FILES_COUNTER.failed
        );

        if (!FILES_COUNTER.isModified())
            logger.add(true,
                    "Files not loaded correctly! Report to CroaBeast."
            );

        try {
            Reflector.of(LangUtils.class).create(SIRPlugin.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }

        BeansLib.getLib().setLogger(new BeansLogger(BeansLib.getLib()) {
            public boolean isColored() {
                return !Main.CONFIG.from().get("options.fix-logger", false);
            }

            public boolean isStripPrefix() {
                return !Main.CONFIG.from().get("options.show-prefix", false);
            }
        });

        logger.sendLines();
        areFilesLoaded = true;
    }

    private ConfigurableFile folderPath(String... args) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            String temp = args[i];
            if (temp == null) continue;

            builder.append(temp);
            if (i != args.length - 1)
                builder.append(File.separatorChar);
        }

        return FILE_MAP.get(builder.toString());
    }

    public enum Main {
        CONFIG,
        BOSSBARS,
        WEBHOOKS;

        @NotNull
        public ConfigurableFile from() {
            return FILE_MAP.get(name().toLowerCase(Locale.ENGLISH));
        }
    }

    public enum Module {
        ADVANCEMENT("advancements"),
        ANNOUNCEMENT("announcements"),
        JOIN_QUIT, MOTD;

        @NotNull
        public static ConfigurableFile getMain() {
            return folderPath("modules", "modules");
        }

        private final String folder;

        Module(String folder) {
            this.folder = folder == null ? name().toLowerCase(Locale.ENGLISH) : folder;
        }

        Module() {
            this(null);
        }

        @NotNull
        public ConfigurableFile fromName(String name) {
            return folderPath("modules", folder, Exceptions.validate(StringUtils::isNotBlank, name));
        }

        public enum Chat {
             CHANNELS,
             COOLDOWNS,
             EMOJIS,
             MENTIONS,
             TAGS,
             FILTERS;

            @NotNull
            public static ConfigurableFile getMain() {
                return folderPath("modules", "chat", "config");
            }

            @NotNull
            public ConfigurableFile from() {
                return folderPath("modules", "chat", name().toLowerCase(Locale.ENGLISH));
            }
        }

        public enum Hook {
            DISCORD,
            LOGIN,
            VANISH;

            @NotNull
            public ConfigurableFile from() {
                return folderPath("modules", "hook", name().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    public static class Command {

        @NotNull
        public static ConfigurableFile getMain() {
            return folderPath("commands", "commands");
        }

        public enum Multi {
            CHAT_VIEW,
            MUTE,
            IGNORE;

            @NotNull
            public ConfigurableFile from(boolean isLang) {
                return folderPath(
                        "commands", name().toLowerCase(Locale.ENGLISH),
                        isLang ? "lang" : "data"
                );
            }
        }

        public enum Single {
            ANNOUNCER,
            SIR,
            MSG_REPLY("msg-reply"),
            PRINT;

            private final String name;

            Single(String name) {
                this.name = name;
            }

            Single() {
                this.name = name().toLowerCase(Locale.ENGLISH);
            }

            @NotNull
            public ConfigurableFile from() {
                return folderPath("commands", name);
            }
        }
    }
}
