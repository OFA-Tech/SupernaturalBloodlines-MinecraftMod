package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What the Admin page does with typed text and with its search box, pure: text to a {@link SettingValue} of a kind (the server still checks
 * range and parser), a value to the text shown in a field, and the filter of rows by a query.
 */
public final class AdminEdit {
    /** How a draft stands: untouched, typed and not sent, sent and accepted, or refused by the server. */
    public enum State { DEFAULT, UNSAVED, SAVED, INVALID }

    private AdminEdit() {}

    public static Optional<SettingValue> parse(SettingKind kind, String text) {
        String t = text.strip();
        try {
            return switch (kind) {
                case BOOL, GAMERULE_BOOL -> t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false") ? Optional.of(new SettingValue.Bool(Boolean.parseBoolean(t))) : Optional.empty();
                case INT, GAMERULE_INT -> Optional.of(new SettingValue.Int(Long.parseLong(t)));
                case DOUBLE -> Optional.of(new SettingValue.Dbl(Double.parseDouble(t.replace(',', '.'))));
                case CHOICE, TEXT -> Optional.of(new SettingValue.Text(t));
                case LIST -> Optional.of(new SettingValue.Lines(text.lines().map(String::strip).filter(l -> !l.isEmpty()).toList()));
            };
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static String format(SettingValue v) {
        return switch (v) {
            case SettingValue.Bool b -> String.valueOf(b.value());
            case SettingValue.Int i -> String.valueOf(i.value());
            case SettingValue.Dbl d -> trim(d.value());
            case SettingValue.Text t -> t.value();
            case SettingValue.Lines l -> String.join("\n", l.value());
        };
    }

    private static String trim(double d) {
        String s = String.format(Locale.ROOT, "%.4f", d);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    public static State state(SettingValue current, SettingValue def, String draft, boolean refused, boolean justSaved) {
        if (refused) {
            return State.INVALID;
        }
        if (!draft.equals(format(current))) {
            return State.UNSAVED;
        }
        return justSaved ? State.SAVED : State.DEFAULT;
    }

    /** The rows whose id or label contains every word of {@code query}, case-insensitively; an empty query keeps all. */
    public static <T> List<T> filter(List<T> rows, String query, java.util.function.Function<T, String> haystack) {
        List<String> words = Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\s+")).filter(w -> !w.isEmpty()).collect(Collectors.toList());
        return rows.stream().filter(r -> {
            String h = haystack.apply(r).toLowerCase(Locale.ROOT);
            return words.stream().allMatch(h::contains);
        }).toList();
    }

    /** The label of a key's last path part when no lang entry exists: {@code full_form_level} reads "Full form level". */
    public static String humanize(String id) {
        String last = id.substring(Math.max(id.lastIndexOf('.'), id.lastIndexOf(':')) + 1).replace('_', ' ');
        return last.isEmpty() ? id : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }

    /** Clamps a stepped number into the spec's range (the arrows never leave it). */
    public static double step(double current, double delta, double min, double max) {
        return Math.max(min, Math.min(max, current + delta));
    }
}
