package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/**
 * The value check of the admin path: the type, the lossless conversion, the caps, then the spec's own {@code test} (range, choices and
 * custom validators). {@code ConfigValue.set} validates nothing, so nothing is written before this says yes. Pure.
 */
public final class SettingRules {
    public static final String KEY = "supernaturalbloodlines.admin.refused.";

    /** @param detail the offending line for {@code line}, else empty */
    public record Refusal(String key, String detail) {
        static Refusal of(String reason) {
            return new Refusal(KEY + reason, "");
        }
    }

    private SettingRules() {}

    /** @param lineCheck for a list: says whether one line is acceptable to the key's own parser; null accepts any line the spec accepts */
    public static Optional<Refusal> check(ValueSpec spec, SettingKind kind, SettingValue v, Predicate<String> lineCheck) {
        Object boxed;
        switch (kind) {
            case BOOL -> {
                if (!(v instanceof SettingValue.Bool b)) {
                    return type();
                }
                boxed = b.value();
            }
            case INT -> {
                if (!(v instanceof SettingValue.Int i)) {
                    return type();
                }
                if (i.value() < Integer.MIN_VALUE || i.value() > Integer.MAX_VALUE) {
                    return Optional.of(Refusal.of("range"));
                }
                boxed = (int) i.value();
            }
            case DOUBLE -> {
                if (!(v instanceof SettingValue.Dbl d)) {
                    return type();
                }
                if (!Double.isFinite(d.value())) {
                    return Optional.of(Refusal.of("range"));
                }
                boxed = d.value();
            }
            case CHOICE, TEXT -> {
                if (!(v instanceof SettingValue.Text t)) {
                    return type();
                }
                if (t.value().length() > SettingValue.MAX_LINE) {
                    return Optional.of(Refusal.of("too_long"));
                }
                boxed = t.value();
            }
            case LIST -> {
                if (!(v instanceof SettingValue.Lines l)) {
                    return type();
                }
                Optional<Refusal> bad = lines(l, lineCheck);
                if (bad.isPresent()) {
                    return bad;
                }
                boxed = new ArrayList<>(l.value());
            }
            default -> {
                return type();
            }
        }
        if (!spec.test(boxed)) {
            return Optional.of(Refusal.of(switch (kind) {
                case INT, DOUBLE -> "range";
                case CHOICE -> "choice";
                default -> "invalid";
            }));
        }
        return Optional.empty();
    }

    private static Optional<Refusal> lines(SettingValue.Lines l, Predicate<String> lineCheck) {
        int total = 0;
        if (l.value().size() > SettingValue.MAX_LINES) {
            return Optional.of(Refusal.of("too_long"));
        }
        for (String line : l.value()) {
            total += line.length();
            if (line.length() > SettingValue.MAX_LINE || total > SettingValue.MAX_TOTAL) {
                return Optional.of(Refusal.of("too_long"));
            }
        }
        if (lineCheck != null) {
            for (String line : l.value()) {
                if (!lineCheck.test(line)) {
                    return Optional.of(new Refusal(KEY + "line", line));
                }
            }
        }
        return Optional.empty();
    }

    /** A game rule has no spec: only the type and the int range are checked here, the rule itself refuses the rest. */
    public static Optional<Refusal> checkRule(SettingKind kind, SettingValue v) {
        return switch (kind) {
            case GAMERULE_BOOL -> v instanceof SettingValue.Bool ? Optional.empty() : type();
            case GAMERULE_INT -> !(v instanceof SettingValue.Int i) ? type()
                    : i.value() < Integer.MIN_VALUE || i.value() > Integer.MAX_VALUE ? Optional.of(Refusal.of("range")) : Optional.empty();
            default -> type();
        };
    }

    private static Optional<Refusal> type() {
        return Optional.of(Refusal.of("type"));
    }

    /** The value as the config stores it. Call only after {@link #check} or {@link #checkRule} said yes. */
    public static Object boxed(SettingValue v) {
        return switch (v) {
            case SettingValue.Bool b -> b.value();
            case SettingValue.Int i -> i.value() >= Integer.MIN_VALUE && i.value() <= Integer.MAX_VALUE ? (Object) (int) i.value() : (Object) i.value();
            case SettingValue.Dbl d -> d.value();
            case SettingValue.Text t -> t.value();
            case SettingValue.Lines l -> new ArrayList<>(l.value());
        };
    }
}
