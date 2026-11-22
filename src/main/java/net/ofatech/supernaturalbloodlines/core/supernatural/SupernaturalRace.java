package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.Locale;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.jetbrains.annotations.Nullable;

/**
 * The supernatural race of a player: computed from {@link BloodlineState} and the faction id, never stored.
 * A player has one creature race and at most one magic race (WITCH, SIPHONER). No game classes.
 */
public enum SupernaturalRace {
    HUMAN, GHOUL, VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, TRIBRID, HERETIC, WITCH, SIPHONER, HUNTER, WEREWOLF, AUGUSTINE;

    public static final String VAMPIRE_FACTION = "vampirism:vampire";
    public static final String HUNTER_FACTION = "vampirism:hunter";
    public static final String WEREWOLF_FACTION = "werewolves:werewolf";

    /** The key of this race in data files and in the state maps. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<SupernaturalRace> byId(String id) {
        for (SupernaturalRace r : values()) {
            if (r.id().equals(id)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** First match wins. {@code factionId} is the Vampirism faction id of the player, or null for none. */
    public static SupernaturalRace creature(BloodlineState s, @Nullable String factionId) {
        if (s.ghoul()) {
            return GHOUL;
        }
        switch (s.species()) {
            case ORIGINAL_HYBRID: return ORIGINAL_HYBRID;
            case ORIGINAL: return ORIGINAL;
            case TRIBRID: return TRIBRID;
            case HYBRID: return HYBRID;
            case HERETIC: return HERETIC;
            case AUGUSTINE: return AUGUSTINE;
            case POTENTIAL, HUNTER_OF_THE_FIVE: return HUNTER;
            default: break;
        }
        if (HUNTER_FACTION.equals(factionId)) {
            return HUNTER;
        }
        if (VAMPIRE_FACTION.equals(factionId)) {
            return VAMPIRE;
        }
        if (WEREWOLF_FACTION.equals(factionId)) {
            return WEREWOLF;
        }
        return HUMAN;
    }

    /** Tribrids and heretics are single races of their own, so only a witch or a siphoner has a magic race. */
    public static Optional<SupernaturalRace> magic(BloodlineState s) {
        if (s.species() == Species.WITCH) {
            return Optional.of(WITCH);
        }
        if (s.species() == Species.SIPHONER) {
            return Optional.of(SIPHONER);
        }
        return Optional.empty();
    }
}
