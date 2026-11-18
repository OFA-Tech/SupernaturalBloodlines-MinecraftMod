package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The knowledge table (hub IA 3.7): what an observer in each relation may know about a subject. Pure. One row per relation, one letter per
 * fact in {@link Fact} order: {@code K} known, {@code D} known once discovered, {@code ?} not known to this relation. A fact the observer has
 * discovered anyway (their own record, their pack's pool, an ability) is shown whatever its letter says, so {@code ?} means "not by the relation
 * alone", not "never". The server reads and sends only what is shown (see {@link ProfileView#shown}), so a client cannot be made to show a secret
 * by anything it asks for.
 */
public final class Knowledge {
    //                                       NAME SPECIES RANK LEVEL PACK PACK_DAYS PRESENCE SIRE LINE GENES ANCHOR BONUS
    private static final Map<Relation, String> TABLE = new EnumMap<>(Relation.class);

    static {
        TABLE.put(Relation.SELF,             "KKKKKKKKDDKK");
        TABLE.put(Relation.SAME_PACK,        "KKKKKKKKDDKK");
        TABLE.put(Relation.ALLY_LEADER,      "KKK?KKK?????");
        TABLE.put(Relation.DIRECT_SIRE,      "KK?KDDKKK???");
        TABLE.put(Relation.DIRECT_SIRED,     "KK?K??KKK???");
        TABLE.put(Relation.OTHER_IN_LINE,    "KK??????????");
        TABLE.put(Relation.NPC_CREATURE,     "KK??????????");
        TABLE.put(Relation.NPC_ANCESTOR,     "D???????????");
        TABLE.put(Relation.STRANGER,         "????????????");
        TABLE.put(Relation.STRANGER_OFFLINE, "????????????");
        TABLE.put(Relation.OP_VIEW,          "KKKKKKKKKKKK");
    }

    /** Things that make an observer learn a fact about a subject, and for good. */
    public enum Ability { REVELATION, LINEAGE_REVEAL, DOPPELGANGER_TAGLOCK, MEMORY_CLAWS }

    private Knowledge() {}

    public static Visibility visible(Relation relation, Fact fact) {
        return switch (TABLE.get(relation).charAt(fact.ordinal())) {
            case 'K' -> Visibility.KNOWN;
            case 'D' -> Visibility.DISCOVERED;
            default -> Visibility.UNKNOWN;
        };
    }

    /** The facts an ability teaches (about the subject it was used on). */
    public static Set<Fact> learnedBy(Ability ability) {
        return switch (ability) {
            case REVELATION -> EnumSet.of(Fact.LINE, Fact.GENES);
            case LINEAGE_REVEAL, DOPPELGANGER_TAGLOCK -> EnumSet.of(Fact.GENES);
            case MEMORY_CLAWS -> EnumSet.of(Fact.SIRE, Fact.GENES);
        };
    }
}

