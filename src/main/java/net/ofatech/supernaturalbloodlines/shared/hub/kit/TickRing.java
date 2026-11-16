package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;

/** The dots of a progress ring: {@code count} ticks round a centre from the top, clockwise, the first {@code on} of them lit. Pure. */
public final class TickRing {
    public record Tick(int x, int y, boolean on) {
    }

    private TickRing() {
    }

    /** How many of {@code count} ticks a progress of {@code fraction} (0 to 1, clamped) lights. */
    public static int lit(int count, float fraction) {
        return Math.max(0, Math.min(count, Math.round(Math.max(0f, Math.min(1f, fraction)) * count)));
    }

    public static List<Tick> ticks(int cx, int cy, int radius, int count, float fraction) {
        int lit = lit(count, fraction);
        List<Tick> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / count;
            out.add(new Tick(cx + (int) Math.round(radius * Math.cos(a)), cy + (int) Math.round(radius * Math.sin(a)), i < lit));
        }
        return out;
    }
}
