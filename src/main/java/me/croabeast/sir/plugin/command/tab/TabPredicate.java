package me.croabeast.sir.plugin.command.tab;

import org.bukkit.command.CommandSender;

import java.util.function.BiPredicate;

public interface TabPredicate extends BiPredicate<CommandSender, String[]> {}
