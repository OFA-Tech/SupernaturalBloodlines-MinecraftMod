package net.ofatech.supernaturalbloodlines.core.boss;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R33: which Ash Dagger stays in whom. The plain one lodges in an Original and nothing else; the Cursed one (R205) also in an Original hybrid. This
 * says what a hit does to the victim only: the wielder's backlash does not look at the victim, so a hit that lodges and one that does not hurt the
 * wielder the same. Pure.
 */
public final class DaggerRules {
    private DaggerRules() {
    }

    public static boolean lodges(boolean cursedDagger, Species victim) {
        return victim == Species.ORIGINAL || (cursedDagger && victim == Species.ORIGINAL_HYBRID);
    }
}
