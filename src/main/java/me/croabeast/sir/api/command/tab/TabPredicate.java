package me.croabeast.sir.api.command.tab;

import org.bukkit.command.CommandSender;

import java.util.function.BiPredicate;

public interface TabPredicate extends BiPredicate<CommandSender, String[]> {}
