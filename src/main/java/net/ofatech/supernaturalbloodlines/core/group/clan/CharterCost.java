package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What forging a Charter costs: lines of {@code namespace:item} or {@code namespace:item=count}, read the way {@code npc_line_weights} is. */
public final class CharterCost {
    public static final List<String> DEFAULT = List.of("minecraft:writable_book", "minecraft:gold_block");
    private static final Pattern LINE = Pattern.compile("\\s*([a-z0-9_.-]+:[a-z0-9_./-]+)\\s*(?:=\\s*(\\d{1,4}))?\\s*");

    public record Entry(String item, int count) {
    }

    private CharterCost() {
    }

    /** Empty when the line is not an item id with an optional positive count. */
    public static Optional<Entry> parseLine(String line) {
        Matcher m = LINE.matcher(line == null ? "" : line);
        if (!m.matches()) {
            return Optional.empty();
        }
        int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
        return count < 1 ? Optional.empty() : Optional.of(new Entry(m.group(1), count));
    }

    public static List<Entry> defaults() {
        return DEFAULT.stream().map(line -> parseLine(line).orElseThrow()).toList();
    }

    /** A Charter always costs something: an empty list is refused here, by the config spec and by the Admin row. */
    public static boolean acceptable(List<String> lines) {
        return !lines.isEmpty();
    }

    /** An empty list is the default (never free); every bad or unknown line is skipped with one warning; a list that leaves nothing is the default. */
    public static List<Entry> parse(List<String> lines, Predicate<String> known, Consumer<String> warn) {
        if (lines.isEmpty()) {
            warn.accept("charter_forge_cost: an empty list is not allowed, the default is used");
            return defaults();
        }
        List<Entry> out = new ArrayList<>();
        for (String line : lines) {
            Optional<Entry> entry = parseLine(line);
            if (entry.isEmpty()) {
                warn.accept("charter_forge_cost: '" + line + "' is not an item id with an optional =count");
            } else if (!known.test(entry.get().item())) {
                warn.accept("charter_forge_cost: no item '" + entry.get().item() + "'");
            } else {
                out.add(entry.get());
            }
        }
        return out.isEmpty() ? defaults() : List.copyOf(out);
    }
}
