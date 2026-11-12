package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A player's species layer. Pure data: the rules live in {@link BloodlineRules}.
 *
 * @param turnedAt        game time the current species began; 0 = unset
 * @param thirst          0 to 20
 * @param previousRace    unused since the race is kept (R100); always null, kept for save compatibility
 * @param hybridCurseBound werewolf side sealed until the Rite of the Moon (R24a)
 */
public record BloodlineState(Species species, boolean witchLayer, @Nullable UUID sire, boolean inTransition,
                             boolean unstable, long transitionTicksLeft, @Nullable UUID pendingSire,
                             int siphonedPower, long turnedAt, int thirst, @Nullable String previousRace,
                             int markKills, boolean hunterCurse, long lastVampireKill, boolean hybridCurseBound,
                             WolfState wolf) {

    public static final BloodlineState NONE = new BloodlineState(Species.NONE, false, null, false, false, 0, null,
            0, 0, 20, null, 0, false, 0, false, WolfState.NONE);

    public static final Codec<BloodlineState> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.enumByName(Species.class, Species.NONE).optionalFieldOf("species", Species.NONE).forGetter(BloodlineState::species),
            Codec.BOOL.optionalFieldOf("witch_layer", false).forGetter(BloodlineState::witchLayer),
            BloodlineCodecs.UUID_CODEC.optionalFieldOf("sire").forGetter(s -> Optional.ofNullable(s.sire())),
            Codec.BOOL.optionalFieldOf("in_transition", false).forGetter(BloodlineState::inTransition),
            Codec.BOOL.optionalFieldOf("unstable", false).forGetter(BloodlineState::unstable),
            Codec.LONG.optionalFieldOf("transition_ticks_left", 0L).forGetter(BloodlineState::transitionTicksLeft),
            BloodlineCodecs.UUID_CODEC.optionalFieldOf("pending_sire").forGetter(s -> Optional.ofNullable(s.pendingSire())),
            Codec.INT.optionalFieldOf("siphoned_power", 0).forGetter(BloodlineState::siphonedPower),
            Codec.LONG.optionalFieldOf("turned_at", 0L).forGetter(BloodlineState::turnedAt),
            Codec.INT.optionalFieldOf("thirst", 20).forGetter(BloodlineState::thirst),
            Codec.STRING.optionalFieldOf("previous_race").forGetter(s -> Optional.ofNullable(s.previousRace())),
            Codec.INT.optionalFieldOf("mark_kills", 0).forGetter(BloodlineState::markKills),
            Codec.BOOL.optionalFieldOf("hunter_curse", false).forGetter(BloodlineState::hunterCurse),
            Codec.LONG.optionalFieldOf("last_vampire_kill", 0L).forGetter(BloodlineState::lastVampireKill),
            Codec.BOOL.optionalFieldOf("hybrid_curse_bound", false).forGetter(BloodlineState::hybridCurseBound),
            WolfState.CODEC.optionalFieldOf("wolf", WolfState.NONE).forGetter(BloodlineState::wolf)
    ).apply(i, (species, witch, sire, inTransition, unstable, ticks, pending, power, turnedAt, thirst, prev,
                markKills, curse, lastKill, bound, wolf) -> new BloodlineState(species, witch, sire.orElse(null),
            inTransition, unstable, ticks, pending.orElse(null), power, turnedAt, thirst, prev.orElse(null),
            markKills, curse, lastKill, bound, wolf)));

    public BloodlineState {
        species = species == null ? Species.NONE : species;
        wolf = wolf == null ? WolfState.NONE : wolf;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Species species;
        private boolean witchLayer;
        private UUID sire;
        private boolean inTransition;
        private boolean unstable;
        private long transitionTicksLeft;
        private UUID pendingSire;
        private int siphonedPower;
        private long turnedAt;
        private int thirst;
        private String previousRace;
        private int markKills;
        private boolean hunterCurse;
        private long lastVampireKill;
        private boolean hybridCurseBound;
        private WolfState wolf;

        private Builder(BloodlineState s) {
            species = s.species;
            witchLayer = s.witchLayer;
            sire = s.sire;
            inTransition = s.inTransition;
            unstable = s.unstable;
            transitionTicksLeft = s.transitionTicksLeft;
            pendingSire = s.pendingSire;
            siphonedPower = s.siphonedPower;
            turnedAt = s.turnedAt;
            thirst = s.thirst;
            previousRace = s.previousRace;
            markKills = s.markKills;
            hunterCurse = s.hunterCurse;
            lastVampireKill = s.lastVampireKill;
            hybridCurseBound = s.hybridCurseBound;
            wolf = s.wolf;
        }

        public Builder species(Species v) { species = v; return this; }
        public Builder witchLayer(boolean v) { witchLayer = v; return this; }
        public Builder sire(@Nullable UUID v) { sire = v; return this; }
        public Builder inTransition(boolean v) { inTransition = v; return this; }
        public Builder unstable(boolean v) { unstable = v; return this; }
        public Builder transitionTicksLeft(long v) { transitionTicksLeft = v; return this; }
        public Builder pendingSire(@Nullable UUID v) { pendingSire = v; return this; }
        public Builder siphonedPower(int v) { siphonedPower = v; return this; }
        public Builder turnedAt(long v) { turnedAt = v; return this; }
        public Builder thirst(int v) { thirst = v; return this; }
        public Builder previousRace(@Nullable String v) { previousRace = v; return this; }
        public Builder markKills(int v) { markKills = v; return this; }
        public Builder hunterCurse(boolean v) { hunterCurse = v; return this; }
        public Builder lastVampireKill(long v) { lastVampireKill = v; return this; }
        public Builder hybridCurseBound(boolean v) { hybridCurseBound = v; return this; }
        public Builder wolf(WolfState v) { wolf = v; return this; }

        public BloodlineState build() {
            return new BloodlineState(species, witchLayer, sire, inTransition, unstable, transitionTicksLeft,
                    pendingSire, siphonedPower, turnedAt, thirst, previousRace, markKills, hunterCurse,
                    lastVampireKill, hybridCurseBound, wolf);
        }
    }
}
