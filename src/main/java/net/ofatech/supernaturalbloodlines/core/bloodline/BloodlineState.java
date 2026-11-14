package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A player's species layer. Pure data: the rules live in {@link BloodlineRules}.
 *
 * @param turnedAt        game time the current species began; 0 = unset (also what a species-less state holds,
 *                        and what a species change at game time 0 would stamp)
 * @param thirst          0 to 20, clamped by the constructor
 * @param previousRace    unused since the race is kept (R100); always null, kept for save compatibility
 * @param hybridCurseBound werewolf side sealed until the Rite of the Moon (R24a)
 * @param layerSkills     Tensura skills the species layer granted (species_tensura); only these are ever taken back
 * @param severedSire     the sire the bond was broken from by the Rite of Severance (R101b); kept only while it is still the {@code sire}.
 *                        The sire stays for the family tree and the sireline death (blood); only the bond (what the sire can do to or through
 *                        the sired) is gone. See {@link #bondBroken()}.
 * @param ghoul           a Sanguinare that finished made this player a Ghoul: stuck at Vampirism level 1 until the turning is done (plan 11)
 * @param vampireBloodUntil game time until which vampire blood is in the player's system; 0 = none (server only: not sent to clients)
 * @param turningTicksLeft online ticks left to drink human-like blood after dying with vampire blood; 0 = not in the turning
 */
public record BloodlineState(Species species, boolean witchLayer, @Nullable UUID sire, boolean inTransition,
                             boolean unstable, long transitionTicksLeft, @Nullable UUID pendingSire,
                             int siphonedPower, long turnedAt, int thirst, @Nullable String previousRace,
                             int markKills, boolean hunterCurse, long lastVampireKill, boolean hybridCurseBound,
                             WolfState wolf, Set<String> layerSkills, @Nullable UUID severedSire,
                             boolean ghoul, long vampireBloodUntil, int turningTicksLeft) {

    public static final BloodlineState NONE = new BloodlineState(Species.NONE, false, null, false, false, 0, null,
            0, 0, 20, null, 0, false, 0, false, WolfState.NONE, Set.of(), null, false, 0, 0);

    // RecordCodecBuilder groups at most 16 fields, so layer_skills is a second map codec merged in by the pair below
    private static final MapCodec<BloodlineState> BASE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BloodlineCodecs.enumByName(Species.class, Species.NONE).lenientOptionalFieldOf("species", Species.NONE).forGetter(BloodlineState::species),
            Codec.BOOL.lenientOptionalFieldOf("witch_layer", false).forGetter(BloodlineState::witchLayer),
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("sire").forGetter(s -> Optional.ofNullable(s.sire())),
            Codec.BOOL.lenientOptionalFieldOf("in_transition", false).forGetter(BloodlineState::inTransition),
            Codec.BOOL.lenientOptionalFieldOf("unstable", false).forGetter(BloodlineState::unstable),
            Codec.LONG.lenientOptionalFieldOf("transition_ticks_left", 0L).forGetter(BloodlineState::transitionTicksLeft),
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("pending_sire").forGetter(s -> Optional.ofNullable(s.pendingSire())),
            Codec.INT.lenientOptionalFieldOf("siphoned_power", 0).forGetter(BloodlineState::siphonedPower),
            Codec.LONG.lenientOptionalFieldOf("turned_at", 0L).forGetter(BloodlineState::turnedAt),
            Codec.INT.lenientOptionalFieldOf("thirst", 20).forGetter(BloodlineState::thirst),
            Codec.STRING.lenientOptionalFieldOf("previous_race").forGetter(s -> Optional.ofNullable(s.previousRace())),
            Codec.INT.lenientOptionalFieldOf("mark_kills", 0).forGetter(BloodlineState::markKills),
            Codec.BOOL.lenientOptionalFieldOf("hunter_curse", false).forGetter(BloodlineState::hunterCurse),
            Codec.LONG.lenientOptionalFieldOf("last_vampire_kill", 0L).forGetter(BloodlineState::lastVampireKill),
            Codec.BOOL.lenientOptionalFieldOf("hybrid_curse_bound", false).forGetter(BloodlineState::hybridCurseBound),
            WolfState.CODEC.lenientOptionalFieldOf("wolf", WolfState.NONE).forGetter(BloodlineState::wolf)
    ).apply(i, (species, witch, sire, inTransition, unstable, ticks, pending, power, turnedAt, thirst, prev,
                markKills, curse, lastKill, bound, wolf) -> new BloodlineState(species, witch, sire.orElse(null),
            inTransition, unstable, ticks, pending.orElse(null), power, turnedAt, thirst, prev.orElse(null),
            markKills, curse, lastKill, bound, wolf, Set.of(), null, false, 0L, 0)));

    /** The fields past the 16-field cap of a record codec group: the layer tracking, the severed sire and the turning (plan 11). */
    private record Extra(java.util.List<String> layerSkills, @Nullable UUID severedSire, boolean ghoul, long vampireBloodUntil,
                         int turningTicksLeft) {}

    private static final MapCodec<Extra> EXTRA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().lenientOptionalFieldOf("layer_skills", java.util.List.of()).forGetter(Extra::layerSkills),
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("severed_sire").forGetter(e -> Optional.ofNullable(e.severedSire())),
            Codec.BOOL.lenientOptionalFieldOf("ghoul", false).forGetter(Extra::ghoul),
            Codec.LONG.lenientOptionalFieldOf("vampire_blood_until", 0L).forGetter(Extra::vampireBloodUntil),
            Codec.INT.lenientOptionalFieldOf("turning_ticks_left", 0).forGetter(Extra::turningTicksLeft)
    ).apply(i, (skills, severed, ghoul, until, turning) -> new Extra(skills, severed.orElse(null), ghoul, until, turning)));

    public static final Codec<BloodlineState> CODEC = Codec.mapPair(BASE_CODEC, EXTRA_CODEC).codec().xmap(
            p -> p.getFirst().toBuilder().layerSkills(new HashSet<>(p.getSecond().layerSkills()))
                    .severedSire(p.getSecond().severedSire()).ghoul(p.getSecond().ghoul())
                    .vampireBloodUntil(p.getSecond().vampireBloodUntil()).turningTicksLeft(p.getSecond().turningTicksLeft()).build(),
            s -> {
                var sorted = new ArrayList<>(s.layerSkills());
                Collections.sort(sorted);
                return Pair.of(s, new Extra(sorted, s.severedSire(), s.ghoul(), s.vampireBloodUntil(), s.turningTicksLeft()));
            });

    public BloodlineState {
        species = species == null ? Species.NONE : species;
        thirst = Math.max(0, Math.min(20, thirst));
        wolf = wolf == null ? WolfState.NONE : wolf;
        vampireBloodUntil = Math.max(0, vampireBloodUntil);
        turningTicksLeft = Math.max(0, turningTicksLeft);
        layerSkills = layerSkills == null ? Set.of() : Set.copyOf(layerSkills);
        // a severance only means something against the sire it was done to: a new sire, or none, ends it
        severedSire = sire != null && sire.equals(severedSire) ? severedSire : null;
    }

    /** The Rite of Severance (R101b) broke the bond with the current sire. The sire is still recorded (the family tree, the sireline death). */
    public boolean bondBroken() {
        return severedSire != null;
    }

    /** The sire this player is still bound to: the sire unless the bond is broken. What a sire may do to or through the sired reads this, not {@link #sire()}. */
    @Nullable
    public UUID boundSire() {
        return bondBroken() ? null : sire;
    }

    /** In the turning (plan 11): died with vampire blood and now has until {@code turningTicksLeft} runs out to drink human-like blood. */
    public boolean inTurning() {
        return turningTicksLeft > 0;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * This state with the server-only skill tracking ({@code layerSkills} and {@code wolf.grantedSkills}: which Tensura skills this
     * mod granted) taken from {@code source}. The one place that lists those fields: a new tracking field goes here, and so into
     * both the rollback ({@code BloodlineAccess.restore}) and the client view.
     */
    public BloodlineState withTrackingOf(BloodlineState source) {
        return toBuilder().layerSkills(source.layerSkills())
                .wolf(wolf.toBuilder().grantedSkills(source.wolf().grantedSkills()).build()).build();
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
        private Set<String> layerSkills;
        private UUID severedSire;
        private boolean ghoul;
        private long vampireBloodUntil;
        private int turningTicksLeft;

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
            layerSkills = s.layerSkills;
            severedSire = s.severedSire;
            ghoul = s.ghoul;
            vampireBloodUntil = s.vampireBloodUntil;
            turningTicksLeft = s.turningTicksLeft;
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
        public Builder layerSkills(Set<String> v) { layerSkills = v; return this; }
        public Builder severedSire(@Nullable UUID v) { severedSire = v; return this; }
        public Builder ghoul(boolean v) { ghoul = v; return this; }
        public Builder vampireBloodUntil(long v) { vampireBloodUntil = v; return this; }
        public Builder turningTicksLeft(int v) { turningTicksLeft = v; return this; }

        public BloodlineState build() {
            return new BloodlineState(species, witchLayer, sire, inTransition, unstable, transitionTicksLeft,
                    pendingSire, siphonedPower, turnedAt, thirst, previousRace, markKills, hunterCurse,
                    lastVampireKill, hybridCurseBound, wolf, layerSkills, severedSire, ghoul, vampireBloodUntil, turningTicksLeft);
        }
    }
}
