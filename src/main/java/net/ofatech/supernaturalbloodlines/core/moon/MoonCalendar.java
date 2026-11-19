package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.ArrayList;
import java.util.List;

/**
 * The lunar calendar of the hub and what tonight is (R140). Vanilla phase 0 is the full moon, one phase a day, eight in all. Pure: the window of
 * the night is {@link MoonRule}'s, so there is one definition of it.
 */
public final class MoonCalendar {
    /** How many days the calendar shows: two cycles. */
    public static final int DAYS = 16;

    /** What tonight is, strongest first: an eclipse, a full moon, an Enhanced Celestials Blood Moon, nothing. */
    public enum Night { NONE, BLOOD_MOON, FULL_MOON, ECLIPSE }

    /** One day of the calendar: {@code offset} days from today. */
    public record Day(int offset, int phase, boolean full) {
    }

    private MoonCalendar() {
    }

    /** Days until the next full moon: 0 when it is the full moon today. */
    public static int daysToFull(int phase) {
        return Math.floorMod(8 - phase, 8);
    }

    /** The {@code count} days from today, whose phase today is {@code phase}. */
    public static List<Day> days(int phase, int count) {
        List<Day> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int p = Math.floorMod(phase + i, 8);
            out.add(new Day(i, p, p == 0));
        }
        return out;
    }

    /** The offsets of the full moons in the next {@code count} days. */
    public static List<Integer> fullMoons(int phase, int count) {
        return days(phase, count).stream().filter(Day::full).map(Day::offset).toList();
    }

    /**
     * What tonight is. Over once the night window has ended; the day of the full moon is already "tonight" in the morning. The Nether and the End
     * have no night. A running eclipse beats a full moon, which beats a Blood Moon.
     */
    public static Night tonight(boolean hasFixedTime, int phase, long dayTime, LunarMood mood, boolean eclipseRunning) {
        if (hasFixedTime || Math.floorMod(dayTime, 24000L) >= MoonRule.NIGHT_END) {
            return Night.NONE;
        }
        if (eclipseRunning) {
            return Night.ECLIPSE;
        }
        if (phase == 0) {
            return Night.FULL_MOON;
        }
        return mood.kind() == LunarMood.Kind.BLOOD ? Night.BLOOD_MOON : Night.NONE;
    }
}
