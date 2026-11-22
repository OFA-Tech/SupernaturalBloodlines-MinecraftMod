package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * R214: Neck Snap. What a snapped neck does follows the show (the wiki's species pages): a vampire-type is "dead for a while" and wakes on its own, a mortal
 * dies (here: when weak) or staggers, a boss is not moved. The snapper gets the same line and the same cooldown for every result. Pure.
 */
public final class NeckSnapRules {
    public enum Kind { VAMPIRE, AUGUSTINE, HYBRID, ORIGINAL, BOSS, MORTAL }

    public enum Result { SNAP, KILL, STAGGER, NOTHING }

    private NeckSnapRules() {
    }

    /** BOSS: nothing. The four vampiric kinds lie down at any health. MORTAL: dies at or under {@code killHealth}, else staggers (no instant kill of a healthy player). */
    public static Result decide(Kind kind, float targetHealth, float killHealth) {
        return switch (kind) {
            case BOSS -> Result.NOTHING;
            case VAMPIRE, AUGUSTINE, HYBRID, ORIGINAL -> Result.SNAP;
            case MORTAL -> targetHealth <= killHealth ? Result.KILL : Result.STAGGER;
        };
    }

    /**
     * What kind a target is, in this order: a boss; an Original or Original hybrid; a hybrid or tribrid (the wiki: a hybrid's death is temporary; only a witch-werewolf
     * dies at once, and that has no vampire layer here); an Augustine; a heretic, a Ghoul, anyone in the vampire faction, anyone in the turning or the hybrid
     * transition (a vampire in the making lies down like one); everything else is mortal (humans, witches, siphoners, hunters, werewolves, creatures).
     */
    public static Kind kindOf(Species s, boolean vampireFaction, boolean ghoul, boolean inTurningOrTransition, boolean werewolfFaction, boolean boss) {
        if (boss) {
            return Kind.BOSS;
        }
        return switch (s) {
            case ORIGINAL, ORIGINAL_HYBRID -> Kind.ORIGINAL;
            case HYBRID, TRIBRID -> Kind.HYBRID;
            case AUGUSTINE -> Kind.AUGUSTINE;
            default -> s == Species.HERETIC || ghoul || vampireFaction || inTurningOrTransition ? Kind.VAMPIRE : Kind.MORTAL;
        };
    }

    /** How long a snap lasts, in ticks, for each vampiric kind (config, seconds); 0 for the others (nothing lies down). */
    public static int ticks(Kind k, int vampireSec, int augustineSec, int hybridSec, int originalSec) {
        return switch (k) {
            case VAMPIRE -> vampireSec * 20;
            case AUGUSTINE -> augustineSec * 20;
            case HYBRID -> hybridSec * 20;
            case ORIGINAL -> originalSec * 20;
            case BOSS, MORTAL -> 0;
        };
    }
}
