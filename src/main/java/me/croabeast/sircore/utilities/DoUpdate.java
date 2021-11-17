package me.croabeast.sircore.utilities;

import com.google.common.collect.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import org.bukkit.entity.*;

import java.util.*;

public class DoUpdate {

    private final Application main;
    private final Records records;
    private final TextUtils text;

    public DoUpdate(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.text = main.getTextUtils();
    }

    private void updateLogger(Player player, String... lines) {
        List<String> list = Lists.newArrayList(lines);

        if (player != null) {
            if (list.get(0).equals("RR")) list.remove(0);
            list.forEach(s ->
                    records.playerRecord(player, " " + s)
            );
        }
        else {
            if (list.get(0).equals("RR")) {
                list.remove(0);
                list.forEach(records::rawRecord);
            }
            else list.forEach(records::doRecord);
        }
    }

    private void runUpdater(Player player) {
        Updater.init(main, 96378).updateCheck().whenComplete((result, e) -> {
            String latest = result.getNewestVersion();

            updateLogger(player, "RR", "");
            switch (result.getReason()) {
                case NEW_UPDATE:
                    updateLogger(player,
                            "&4NEW UPDATE!",
                            "&cYou don't have the latest version of S.I.R. installed.",
                            "&cRemember, older versions won't receive any support.",
                            "&7New Version: &a" + latest +
                                    "&7 - Your Version: &e" + main.PLUGIN_VERSION,
                            "&7Link:&b https://www.spigotmc.org/resources/96378/"
                    );
                    break;

                case UP_TO_DATE:
                    updateLogger(player,
                            "&eYou have the latest version of S.I.R. &7(" + latest + ")",
                            "&7I would appreciate if you keep updating &c<3"
                    );
                    break;

                case UNRELEASED_VERSION:
                    updateLogger(player,
                            "&4DEVELOPMENT BUILD:",
                            "&cYou have a newer version of S.I.R. installed.",
                            "&cErrors might occur in this build.",
                            "Spigot Version: &a" + result.getSpigotVersion()
                                    + "&7 - Your Version: &e" + main.PLUGIN_VERSION
                    );
                    break;

                default:
                    updateLogger(player,
                            "&4WARNING!",
                            "&cCould not check for a new version of S.I.R.",
                            "&7Please check your connection and restart the server.",
                            "&7Possible reason: &e" + result.getReason()
                    );
                    break;
            }

            updateLogger(player, "RR", "");
        });
    }

    public void initUpdater(Player player) {
        if (player == null) {
            if (!text.getOption(4, "on-start"))
                return;
            runUpdater(null);
        }
        else {
            boolean perm = main.getEventUtils().
                    hasPerm(player, "sir.admin.updater");
            if (!text.getOption(4, "send-op") || !perm)
                return;
            runUpdater(player);
        }
    }
}
