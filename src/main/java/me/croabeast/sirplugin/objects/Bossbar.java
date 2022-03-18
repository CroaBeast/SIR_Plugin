package me.croabeast.sirplugin.objects;

import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public class Bossbar {

    private final SIRPlugin main = SIRPlugin.getInstance();

    private final Player player;
    private String line;

    private BossBar bar = null;
    private BarColor color = null;
    private BarStyle style = null;
    private Integer time = null;
    private Boolean progress = null;

    protected static Map<Player, BossBar> bossbarMap = new HashMap<>();

    public Bossbar(@Nullable Player target, @NotNull Player player, String line) {
        if (target == null) target = player;
        this.player = target;
        this.line = colorize(player, removeSpace(line));

        Matcher matcher = Pattern.compile("(?i)\\[bossbar(:(.+?)(:(.+?)(:(\\d+)" +
                        "(:(true|false))?)?)?)?](.+)").matcher(line);

        if (matcher.find()) {
            try {
                color = BarColor.valueOf(matcher.group(2).toUpperCase());
            } catch (Exception e) {
                color = null;
            }

            try {
                style = BarStyle.valueOf(matcher.group(4).toUpperCase());
            } catch (Exception e) {
                style = null;
            }

            try {
                time = Integer.parseInt(matcher.group(6)) * 20;
            } catch (Exception e) {
                style = null;
            }

            try {
                progress = Boolean.valueOf(matcher.group(8));
            } catch (Exception e) {
                progress = null;
            }

            this.line = matcher.group(9);
        }

        if (color == null) color = BarColor.BLUE;
        if (style == null) style = BarStyle.SOLID;

        if (time == null) time = 3 * 20;
        if (progress == null) progress = true;
        if (this.line == null) this.line = "";
    }

    public Bossbar(@NotNull Player player, String line) {
        this(null, player, line);
    }

    private void unregister() {
        bar.removePlayer(player);
        bossbarMap.remove(player);
        bar = null;
    }

    public void display() {
        bar = Bukkit.createBossBar(line, color, style);
        bar.setProgress(1.0D);

        bar.addPlayer(player);
        bar.setVisible(true);
        bossbarMap.put(player, bar);

        if (progress && time > 0) {
            double time = 1.0D / Bossbar.this.time;
            double[] percentage = {1.0D};

            new BukkitRunnable() {
                @Override public void run() {
                    bar.setProgress(percentage[0]);

                    if (percentage[0] <= 0.0) {
                        unregister();
                        this.cancel();
                    }
                    else percentage[0] -= time;
                }
            }.runTaskTimer(main, 0, 0);
        }
        else new BukkitRunnable() {
            @Override public void run() {
                unregister();
            }
        }.runTaskLater(main, time);
    }

    @Nullable
    public static BossBar getBossbar(Player player) {
        return bossbarMap.getOrDefault(player, null);
    }

    public static Map<Player, BossBar> getBossbarMap() {
        return bossbarMap;
    }
}
