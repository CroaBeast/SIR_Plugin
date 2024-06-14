package me.croabeast.sir.plugin.module.hook;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.JoinQuitHandler;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unchecked")
abstract class JoinQuitRelated extends HookModule implements CustomListener {

    private static final Class<JoinQuitHandler> MAIN_CLASS = JoinQuitHandler.class;

    @SneakyThrows
    static Class<?> from(String name) {
        return Class.forName(JoinQuitHandler.class.getName() + "$" + name);
    }

    private static final Class<?> UNIT_CLASS = from("ConnectionUnit");
    private static final Class<?> TYPE_CLASS = from("Type");

    @Getter @Setter
    private boolean registered = false;
    protected final ConfigurableFile jqConfig;

    protected JoinQuitRelated(boolean isLogin) {
        super(isLogin ? Name.LOGIN : Name.VANISH,
                isLogin ? YAMLData.Module.Hook.LOGIN : YAMLData.Module.Hook.VANISH);

        jqConfig = YAMLData.Module.JOIN_QUIT.fromName("config");
    }

    static JoinQuitHandler getInstance() {
        return JOIN_QUIT.getData();
    }

    Object getUnit(String joined, Player player) {
        try {
            Method typeM = TYPE_CLASS.getMethod("valueOf", String.class);
            Object type = typeM.invoke(null, joined);

            Method method = MAIN_CLASS.getDeclaredMethod("get", TYPE_CLASS, Player.class);
            method.setAccessible(true);

            return method.invoke(getInstance(), type, player);
        } catch (Exception e) {
            return null;
        }
    }

    void performActions(Object unit, Player player) {
        try {
            Method method = UNIT_CLASS.getDeclaredMethod("performAllActions", Player.class);
            method.setAccessible(true);

            method.invoke(unit, player);
        } catch (Exception ignored) {}
    }

    Map<UUID, Long> getJoinMap() {
        try {
            Field field = MAIN_CLASS.getDeclaredField("JOIN_MAP");
            field.setAccessible(true);

            return (Map<UUID, Long>) field.get(getInstance());
        } catch (Exception e) {
            return null;
        }
    }

    Map<UUID, Long> getPlayMap() {
        try {
            Field field = MAIN_CLASS.getDeclaredField("PLAY_MAP");
            field.setAccessible(true);

            return (Map<UUID, Long>) field.get(getInstance());
        } catch (Exception e) {
            return null;
        }
    }

    Map<UUID, Long> getQuitMap() {
        try {
            Field field = MAIN_CLASS.getDeclaredField("QUIT_MAP");
            field.setAccessible(true);

            return (Map<UUID, Long>) field.get(getInstance());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean register() {
        try {
            register(SIRPlugin.getInstance());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
