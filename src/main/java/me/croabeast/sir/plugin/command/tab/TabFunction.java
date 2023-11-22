package me.croabeast.sir.plugin.command.tab;

import org.bukkit.command.CommandSender;

import java.util.function.BiFunction;

public interface TabFunction<T> extends BiFunction<CommandSender, String[], T> {}
