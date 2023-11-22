package me.croabeast.sir.plugin.command.tab;

import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class TabBuilder {

    private final Map<Integer, Map<TabFunction<?>, TabPredicate>> argPredicateMap;
    private int actualIndex = 0;

    private TabBuilder() {
        argPredicateMap = new LinkedHashMap<>();
    }

    private Map<TabFunction<?>, TabPredicate> mapFromIndex(int index) {
        return argPredicateMap.getOrDefault(index, new LinkedHashMap<>());
    }

    private TabBuilder addArg0(int index, TabPredicate predicate, TabFunction<?> arg) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(arg);

        Map<TabFunction<?>, TabPredicate> args = mapFromIndex(index);
        args.put(arg, predicate);

        argPredicateMap.put(index, args);
        return this;
    }

    public TabBuilder setIndex(int index) {
        actualIndex = index;
        return this;
    }

    public TabBuilder addArgument(int index, TabPredicate predicate, TabFunction<String> argument) {
        return addArg0(index, predicate, argument);
    }

    public TabBuilder addArgument(int index, TabPredicate predicate, String argument) {
        return addArg0(index, predicate, (s, a) -> argument);
    }

    public TabBuilder addArgument(TabPredicate predicate, TabFunction<String> argument) {
        return addArg0(actualIndex, predicate, argument);
    }

    public TabBuilder addArgument(TabPredicate predicate, String argument) {
        return addArg0(actualIndex, predicate, (s, a) -> argument);
    }

    public TabBuilder addArgument(int index, String permission, TabFunction<String> argument) {
        return addArg0(index, (s, a) -> PlayerUtils.hasPerm(s, permission), argument);
    }

    public TabBuilder addArgument(int index, String permission, String argument) {
        return addArgument(index, permission, (s, a) -> argument);
    }

    public TabBuilder addArgument(String permission, TabFunction<String> argument) {
        return addArgument(actualIndex, permission, argument);
    }

    public TabBuilder addArgument(String permission, String argument) {
        return addArgument(permission, (s, a) -> argument);
    }

    public TabBuilder addArgument(int index, TabFunction<String> argument) {
        return addArgument(index, (s, a) -> true, argument);
    }

    public TabBuilder addArgument(int index, String argument) {
        return addArgument(index, (s, a) -> true, (s, a) -> argument);
    }

    public TabBuilder addArgument(TabFunction<String> argument) {
        return addArgument(actualIndex, argument);
    }

    public TabBuilder addArgument(String argument) {
        return addArgument(actualIndex, (s, a) -> argument);
    }

    public TabBuilder addArguments(int index, TabPredicate predicate, TabFunction<Collection<String>> function) {
        return addArg0(index, Objects.requireNonNull(predicate), Objects.requireNonNull(function));
    }

    public TabBuilder addArguments(int index, TabPredicate predicate, Collection<String> arguments) {
        Objects.requireNonNull(arguments);
        return addArg0(index, Objects.requireNonNull(predicate), (s, a) -> arguments);
    }

    public TabBuilder addArguments(int index, TabPredicate predicate, String... arguments) {
        return addArguments(index, predicate, ArrayUtils.fromArray(arguments));
    }

    public TabBuilder addArguments(TabPredicate predicate, TabFunction<Collection<String>> function) {
        return addArguments(actualIndex, predicate, function);
    }

    public TabBuilder addArguments(TabPredicate predicate, Collection<String> arguments) {
        return addArguments(actualIndex, predicate, arguments);
    }

    public TabBuilder addArguments(TabPredicate predicate, String... arguments) {
        return addArguments(actualIndex, predicate, arguments);
    }

    public TabBuilder addArguments(int index, Supplier<String> permission, TabFunction<Collection<String>> function) {
        return addArguments(index, (s, a) -> PlayerUtils.hasPerm(s, permission.get()), function);
    }

    public TabBuilder addArguments(int index, Supplier<String> permission, Collection<String> arguments) {
        return addArguments(index, (s, a) -> PlayerUtils.hasPerm(s, permission.get()), arguments);
    }

    public TabBuilder addArguments(Supplier<String> permission, TabFunction<Collection<String>> function) {
        return addArguments(actualIndex, permission, function);
    }

    public TabBuilder addArguments(Supplier<String> permission, Collection<String> arguments) {
        return addArguments(actualIndex, permission, arguments);
    }

    public TabBuilder addArguments(int index, TabFunction<Collection<String>> function) {
        return addArguments(index, (s, a) -> true, function);
    }

    public TabBuilder addArguments(int index, Collection<String> arguments) {
        return addArguments(index, (s, a) -> true, arguments);
    }

    public TabBuilder addArguments(int index, String... arguments) {
        return addArguments(index, (s, a) -> true, arguments);
    }

    public TabBuilder addArguments(TabFunction<Collection<String>> function) {
        return addArguments(actualIndex, function);
    }

    public TabBuilder addArguments(Collection<String> arguments) {
        return addArguments(actualIndex, arguments);
    }

    public TabBuilder addArguments(String... arguments) {
        return addArguments(actualIndex, arguments);
    }

    public boolean isEmpty() {
        return argPredicateMap.isEmpty();
    }

    public List<String> build(CommandSender sender, String[] args) {
        List<Object> first = mapFromIndex(args.length - 1)
                .entrySet().stream()
                .filter(e -> {
                    final TabPredicate tab = e.getValue();
                    return tab != null && tab.test(sender, args);
                })
                .map(e -> {
                    TabFunction<?> function = e.getKey();
                    return function.apply(sender, args);
                })
                .collect(Collectors.toList());

        List<String> list = new LinkedList<>();
        Consumer<Object> consumer = o -> list.add(String.valueOf(o));

        first.forEach(o -> {
            if (o instanceof Collection) {
                ((Collection<?>) o).forEach(consumer);
                return;
            }
            consumer.accept(o);
        });
        first.removeIf(s -> s.equals("null"));

        final String t = args[args.length - 1];
        return list.stream()
                .filter(s -> s.regionMatches(true, 0, t, 0, t.length()))
                .collect(Collectors.toList());
    }

    public static TabBuilder of() {
        return new TabBuilder();
    }
}
