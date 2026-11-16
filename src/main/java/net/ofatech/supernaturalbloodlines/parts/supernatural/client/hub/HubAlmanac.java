package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;

/** The moon, as the almanac shows it: the next few phases and the days to the full moon. Vanilla phase 0 is the full moon, one phase a day, eight in all. Pure. */
public final class HubAlmanac {
    private HubAlmanac() {
    }

    /** Days until the next full moon: 0 when it is the full moon today. */
    public static int daysToFull(int phase) {
        return Math.floorMod(8 - phase, 8);
    }

    /** The vanilla phases of {@code count} days, starting today. */
    public static List<Integer> phases(int today, int count) {
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(Math.floorMod(today + i, 8));
        }
        return out;
    }
}
