package me.croabeast.sir.api.command.tab;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class TabBuilder {

    private final Map<Integer, Set<TabObject<?>>> argPredicateMap;
    private int actualIndex = 0;

    private TabBuilder() {
        argPredicateMap = new LinkedHashMap<>();
    }

    private Set<TabObject<?>> fromIndex(int index) {
        return argPredicateMap.getOrDefault(index, new LinkedHashSet<>());
    }

    private TabBuilder addArg0(int index, TabPredicate predicate, TabFunction<String> arg) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(arg);

        Set<TabObject<?>> args = fromIndex(index);
        args.add(new StringObject(predicate, arg));

        argPredicateMap.put(index, args);
        return this;
    }

    private TabBuilder addCollectionArg0(int index, TabPredicate predicate, TabFunction<Collection<String>> arg) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(arg);

        Set<TabObject<?>> args = fromIndex(index);
        args.add(new CollectionObject(predicate, arg));

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
        return addCollectionArg0(index, Objects.requireNonNull(predicate), Objects.requireNonNull(function));
    }

    public TabBuilder addArguments(int index, TabPredicate predicate, Collection<String> arguments) {
        Objects.requireNonNull(arguments);
        return addCollectionArg0(index, Objects.requireNonNull(predicate), (s, a) -> arguments);
    }

    public TabBuilder addArguments(int index, TabPredicate predicate, String... arguments) {
        return addArguments(index, predicate, ArrayUtils.toList(arguments));
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

    @SuppressWarnings("unchecked")
    public List<String> build(CommandSender sender, String[] args) {
        List<String> list = new LinkedList<>();

        CollectionBuilder.of(fromIndex(args.length - 1))
                .filter(o -> o.predicate.test(sender, args))
                .collect(new LinkedList<>())
                .forEach(o -> {
                    if (o instanceof StringObject) {
                        list.add((String) o
                                .function
                                .apply(sender, args));
                        return;
                    }

                    list.addAll(((Collection<String>) o
                            .function
                            .apply(sender, args)));
                });

        final String t = args[args.length - 1];
        return CollectionBuilder.of(list)
                .filter(s -> s.regionMatches(true, 0, t, 0, t.length()))
                .toList();
    }

    public static TabBuilder of() {
        return new TabBuilder();
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static class TabObject<T> {
        protected final TabPredicate predicate;
        protected final TabFunction<T> function;
    }

    private static class StringObject extends TabObject<String> {
        StringObject(TabPredicate predicate, TabFunction<String> function) {
            super(predicate, function);
        }
    }

    private static class CollectionObject extends TabObject<Collection<String>> {
        CollectionObject(TabPredicate predicate, TabFunction<Collection<String>> function) {
            super(predicate, function);
        }
    }
}
