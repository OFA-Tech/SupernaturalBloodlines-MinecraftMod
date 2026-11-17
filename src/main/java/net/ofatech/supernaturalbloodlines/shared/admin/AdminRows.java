package net.ofatech.supernaturalbloodlines.shared.admin;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.Range;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/** What the Admin page needs to draw a setting: the live value, the default, the range or choices from the spec, and the restart and client flags. */
public final class AdminRows {
    /** @param ranged whether {@code min} and {@code max} mean anything (a number key) */
    public record Row(String id, SettingKind kind, SettingValue current, SettingValue def, boolean ranged, double min, double max,
                      List<String> choices, boolean restart, boolean clientCopy) {
        public Row {
            choices = List.copyOf(choices);
        }
    }

    private AdminRows() {}

    /** The rows of the entries that are shown and whose config is loaded (a part that is off has none). */
    public static List<Row> of(List<SettingEntry> entries) {
        List<Row> out = new ArrayList<>();
        for (SettingEntry e : entries) {
            if (e.isHidden() || e.kind().isRule()) {
                continue;
            }
            AdminSpecs.value(e).ifPresent(v -> out.add(row(e, v)));
        }
        return out;
    }

    private static Row row(SettingEntry e, ConfigValue<?> v) {
        ValueSpec spec = v.getSpec();
        Range<?> range = spec.getRange();
        boolean ranged = range != null && range.getMin() instanceof Number && range.getMax() instanceof Number;
        return new Row(e.id(), e.kind(), toValue(v.get()), toValue(spec.getDefault()), ranged,
                ranged ? ((Number) range.getMin()).doubleValue() : 0, ranged ? ((Number) range.getMax()).doubleValue() : 0,
                e.kind() == SettingKind.CHOICE ? AdminSpecs.choices(e.id()) : List.of(),
                e.restart() == SettingEntry.Restart.SERVER_RESTART, e.clientCopy());
    }

    static SettingValue toValue(Object o) {
        return switch (o) {
            case Boolean b -> new SettingValue.Bool(b);
            case Integer i -> new SettingValue.Int(i);
            case Long l -> new SettingValue.Int(l);
            case Double d -> new SettingValue.Dbl(d);
            case String s -> new SettingValue.Text(s);
            case List<?> l -> new SettingValue.Lines(l.stream().map(String::valueOf).toList());
            default -> new SettingValue.Text(String.valueOf(o));
        };
    }
}
