package net.ofatech.supernaturalbloodlines.core.rite;

import java.util.EnumSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/** What a player is, as far as rites care: the kinds that follow from their species, their faction and a wolf lineage. Pure. */
public final class RiteKinds {
    private RiteKinds() {
    }

    /**
     * @param vampire     the Vampirism vampire faction
     * @param werewolf    the Werewolves faction, or a wolf lineage (bitten, born)
     * @param hunter      the Vampirism hunter faction
     */
    public static Set<RiteKind> of(Species species, boolean vampire, boolean werewolf, boolean hunter) {
        Set<RiteKind> out = EnumSet.noneOf(RiteKind.class);
        if (vampire) {
            out.add(RiteKind.VAMPIRE);
        }
        if (werewolf) {
            out.add(RiteKind.WEREWOLF);
        }
        if (hunter) {
            out.add(RiteKind.HUNTER);
        }
        switch (species) {
            case ORIGINAL -> {
                out.add(RiteKind.ORIGINAL);
                out.add(RiteKind.VAMPIRE);
            }
            case ORIGINAL_HYBRID -> {
                out.add(RiteKind.ORIGINAL);
                out.add(RiteKind.HYBRID);
                out.add(RiteKind.VAMPIRE);
                out.add(RiteKind.WEREWOLF);
            }
            case HYBRID -> {
                out.add(RiteKind.HYBRID);
                out.add(RiteKind.WEREWOLF);
            }
            case TRIBRID -> {
                out.add(RiteKind.TRIBRID);
                out.add(RiteKind.HYBRID);
                out.add(RiteKind.WEREWOLF);
            }
            case HERETIC -> out.add(RiteKind.VAMPIRE);
            case WITCH -> out.add(RiteKind.WITCH);
            case SIPHONER -> out.add(RiteKind.SIPHONER);
            case HUNTER_OF_THE_FIVE -> out.add(RiteKind.HUNTER);
            default -> {
            }
        }
        return out;
    }
}
