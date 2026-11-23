package net.ofatech.supernaturalbloodlines.core.boss;

import java.util.Locale;

/**
 * The form indicator of a boss bar: which row of chips it shows and which chip is lit. Klaus, Deucalion and Peter shift (HUMAN, SHIFTING, BEAST); the Beast of
 * Gevaudan goes from Host to Beast to Spirit; the other five bosses have one form and no chips. Pure: the server says it, the client draws it.
 */
public final class BossBarForms {
    /** The row of chips: three for a boss with forms, none for one without. */
    public enum Set {
        NONE, SHIFTER, HOST;

        public int chips() {
            return this == NONE ? 0 : 3;
        }

        /**
         * How many chips a bar draws. The Beast's row (HOST, BEAST, SPIRIT) stays hidden until its Host and its Spirit have bars of their own: today only its Beast stage has
         * one, and a row with one lit chip and two that can never light says nothing. Its portrait still follows the stage.
         */
        public int shownChips() {
            return this == SHIFTER ? chips() : 0;
        }

        /** The lang key of chip {@code i}: {@code supernaturalbloodlines.boss.bar.form.<human|shifting|beast|host|spirit>}. */
        public String chipKey(int i) {
            String form = switch (this) {
                case SHIFTER -> i == 0 ? BossForm.HUMAN.name() : i == 1 ? BossForm.SHIFTING.name() : BossForm.BEAST.name();
                case HOST -> i == 0 ? "HOST" : i == 1 ? "BEAST" : "SPIRIT";
                case NONE -> "NONE";
            };
            return "supernaturalbloodlines.boss.bar.form." + form.toLowerCase(Locale.ROOT);
        }
    }

    /** One bar's indicator. {@code active} is always a chip of the set (0 for no chips). */
    public record Look(Set set, int active) {
        public static final Look NONE = new Look(Set.NONE, 0);

        public Look {
            active = set.chips() == 0 ? 0 : Math.max(0, Math.min(set.chips() - 1, active));
        }

        /** Whether the boss is mid-shift: the gold look, the boss can be hit. */
        public boolean shifting() {
            return set == Set.SHIFTER && active == 1;
        }
    }

    private BossBarForms() {
    }

    /** The chips a boss shows. */
    public static Set setOf(BossId boss) {
        return switch (boss) {
            case KLAUS, DEUCALION, PETER_HALE -> Set.SHIFTER;
            case BEAST_OF_GEVAUDAN -> Set.HOST;
            default -> Set.NONE;
        };
    }

    /** The indicator of a shifting boss in a form (HUMAN, SHIFTING and BEAST are chips 0, 1 and 2). */
    public static Look shifter(BossForm form) {
        return new Look(Set.SHIFTER, form.ordinal());
    }

    /** The indicator of the Beast of Gevaudan in a stage: 0 the Host, 1 the Beast, 2 the Spirit. */
    public static Look host(int stage) {
        return new Look(Set.HOST, stage);
    }

    /** The look read back from the wire: a set that does not belong to the boss (a stale or forged value) is no chips at all. */
    public static Look checked(BossId boss, int set, int active) {
        Set[] all = Set.values();
        Set s = set >= 0 && set < all.length ? all[set] : Set.NONE;
        return s == setOf(boss) ? new Look(s, active) : Look.NONE;
    }
}
