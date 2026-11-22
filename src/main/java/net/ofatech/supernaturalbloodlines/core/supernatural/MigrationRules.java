package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import org.jetbrains.annotations.Nullable;

/**
 * The one-time migration of a player who had a race before the ladders existed (schema 0 to 1), pure. It only adds: it proves tasks
 * from the current state, raises the rung to the highest of the proven rung, the layer rung (so every layer skill and bonus the
 * player holds stays), the rank floor and the apex floor, and pays the SP of every rung it gave (no EP: they earned it playing).
 * A task that is only a count (feeds, rites performed, kills) is never proven. No game classes.
 *
 * <p>Order of the rules (docs/superpowers/specs, "Migration"): race since, proven tasks, rung, rewards, backfill record.
 */
public final class MigrationRules {
    public static final int SCHEMA = 1;
    /** L-WOL-5 "Alpha": the rung a rank of ALPHA or TRUE_ALPHA proves. */
    private static final int ALPHA_RUNG = 5;

    /**
     * What the game knows about the player right now.
     *
     * @param factionId  the Vampirism faction id of the player, or null
     * @param apexPath   the path of the player's Tensura race when it is one of ours ({@code primordial_vampire}), else null
     */
    public record Facts(SupernaturalRace creature, Optional<SupernaturalRace> magic, BloodlineState bloodline, long now,
                        @Nullable String factionId, int factionLevel, boolean emissary, boolean moonControl, int sired,
                        @Nullable String apexPath) {
        public List<SupernaturalRace> races() {
            List<SupernaturalRace> out = new ArrayList<>();
            out.add(creature);
            magic.ifPresent(out::add);
            return out;
        }
    }

    /** @param rungsGiven the ids of the rungs the migration raised the player to (for the debug command), in order */
    public record Result(SupernaturalState state, List<String> rungsGiven, int spGiven) {
    }

    private MigrationRules() {
    }

    /** Unchanged for a player who was migrated already, so a second login changes nothing. */
    public static Result migrate(LadderTable table, SupernaturalState before, Facts f) {
        if (before.schema() >= SCHEMA) {
            return new Result(before, List.of(), 0);
        }
        List<SupernaturalRace> races = f.races();
        if (f.creature() == SupernaturalRace.GHOUL) {
            // frozen: no race is stamped and nothing is given; the turning starts the vampire ladder later (plan 11)
            return new Result(before.toBuilder().schema(SCHEMA).backfill(List.of("ghoul")).build(), List.of(), 0);
        }
        // 1. race since: when the species began, else now
        Map<String, Long> since = new HashMap<>(before.raceSince());
        long began = f.bloodline().turnedAt() > 0 ? f.bloodline().turnedAt() : f.now();
        for (SupernaturalRace race : races) {
            since.putIfAbsent(race.id(), began);
        }
        SupernaturalState s = before.toBuilder().schema(SCHEMA).raceSince(since).build();

        List<TaskEvent> readings = readings(f, since);
        List<String> given = new ArrayList<>();
        int sp = 0;
        for (SupernaturalRace race : races) {
            Ladder ladder = table.get(race);
            // 2 and 3. the last rung whose tasks are all proven, in order
            while (true) {
                Optional<Rung> active = LadderRules.activeRung(table, s, race);
                if (active.isEmpty()) {
                    break;
                }
                SupernaturalState proven = s;
                for (TaskEvent e : readings) {
                    proven = LadderRules.apply(table, proven, List.of(race), e);
                }
                final SupernaturalState tried = proven;
                Rung rung = active.get();
                boolean all = rung.tasks().stream().allMatch(t -> LadderRules.isDone(t, tried));
                if (!all) {
                    s = tried; // the tasks that are proven stay done, claimable later
                    break;
                }
                s = reach(tried, race, ladder, rung, true);
                given.add(rung.id());
                sp += rung.sp();
            }
            // floors: the layer rung, the rank floor, the apex floor
            Raised up = raise(s, race, ladder, floor(race, f), given);
            s = up.state();
            sp += up.sp();
            // a rung left with no tasks is reached on its own (its mod or part is missing)
            LadderRules.ClaimResult settled = LadderRules.settle(table, s, race);
            for (int r = s.rung(race) + 1; r <= settled.state().rung(race); r++) {
                given.add(ladder.rungs().get(r).id());
            }
            s = settled.state();
            sp += settled.sp();
        }
        // a True Alpha who is not a werewolf now (cured, or gone) keeps the werewolf rung their rank proves: it is dormant, not lost (R91)
        if (!races.contains(SupernaturalRace.WEREWOLF) && f.bloodline().wolf().rank() == WolfState.Rank.TRUE_ALPHA) {
            Raised up = raise(s, SupernaturalRace.WEREWOLF, table.get(SupernaturalRace.WEREWOLF), ALPHA_RUNG, given);
            s = up.state();
            sp += up.sp();
        }
        // the SP of every rung given goes into the pool at once (settle already put its own in, so the total is set, not added)
        s = s.toBuilder().sp(before.sp() + sp).spEarned(before.spEarned() + sp).backfill(List.copyOf(given)).build();
        return new Result(s, List.copyOf(given), sp);
    }

    private record Raised(SupernaturalState state, int sp) {
    }

    /** Raises the rung of {@code race} to {@code floor} (clipped to the ladder), no tasks marked; returns the SP of the rungs given. */
    private static Raised raise(SupernaturalState s, SupernaturalRace race, Ladder ladder, int floor, List<String> given) {
        int top = Math.min(floor, ladder.rungs().size() - 1);
        int sp = 0;
        while (s.rung(race) < top) {
            Rung rung = ladder.rungs().get(s.rung(race) + 1);
            s = reach(s, race, ladder, rung, false);
            given.add(rung.id());
            sp += rung.sp();
        }
        return new Raised(s, sp);
    }
    private static SupernaturalState reach(SupernaturalState s, SupernaturalRace race, Ladder ladder, Rung rung, boolean claimProven) {
        Set<String> claimed = new HashSet<>(s.claimed());
        if (claimProven) {
            for (TaskSpec t : rung.tasks()) {
                // the claim of a task is its own id, which for an any_of group is the group's, not a member's
                if (t.leaves().stream().map(TaskSpec::id).anyMatch(s.done()::contains)) {
                    claimed.add(t.id());
                }
                t.leaves().stream().map(TaskSpec::id).filter(s.done()::contains).forEach(claimed::add);
            }
        }
        int index = ladder.rungs().indexOf(rung);
        return s.toBuilder().claimed(claimed).rung(race, index).build();
    }

    /** The readings that can prove a task: the level, the state, the sired count, the days as each race. Never a count. */
    static List<TaskEvent> readings(Facts f, Map<String, Long> since) {
        List<TaskEvent> out = new ArrayList<>();
        if (f.factionId() != null) {
            out.add(TaskEvent.of(TaskSpec.Detector.LEVEL, f.factionLevel(), "faction", f.factionId()));
        }
        for (String id : StatePredicates.IDS) {
            OptionalLong v = StatePredicates.value(id, f.bloodline(), f.now(), f.emissary(), f.moonControl());
            if (v.orElse(0) > 0) {
                out.add(TaskEvent.of(TaskSpec.Detector.STATE, v.getAsLong(), "predicate", id));
            }
        }
        if (f.sired() > 0) {
            out.add(TaskEvent.of(TaskSpec.Detector.SIRED, f.sired()));
        }
        for (SupernaturalRace race : f.races()) {
            long start = since.getOrDefault(race.id(), f.now());
            out.add(TaskEvent.of(TaskSpec.Detector.DAYS_AS, Math.max(0, f.now() - start) / StatePredicates.DAY_TICKS, "race", race.id()));
        }
        return out;
    }

    /** The highest of the layer rung, the rank floor and the apex floor for this race; 0 when none applies. */
    static int floor(SupernaturalRace race, Facts f) {
        int floor = 0;
        SupernaturalRace layerRace = LayerSlices.raceOf(f.bloodline().species());
        if (layerRace == race) {
            floor = Math.max(floor, LayerSlices.layerRung(race));
        }
        if (race == SupernaturalRace.WEREWOLF) {
            // a rank is the proof of L-WOL-5 whether or not they are a werewolf now: a True Alpha who was cured keeps it, dormant
            WolfState.Rank rank = f.bloodline().wolf().rank();
            if (rank == WolfState.Rank.ALPHA || rank == WolfState.Rank.TRUE_ALPHA) {
                floor = Math.max(floor, ALPHA_RUNG);
            }
        }
        if (f.apexPath() != null && apexRaces(f.apexPath()).contains(race)) {
            floor = Integer.MAX_VALUE; // the top rung of that ladder (clipped by the caller)
        }
        return floor;
    }

    /** The ladders whose top rung an apex race of ours stands for. */
    public static List<SupernaturalRace> apexRaces(String path) {
        return switch (path) {
            case "primordial_vampire" -> List.of(SupernaturalRace.ORIGINAL);
            case "primordial_hybrid" -> List.of(SupernaturalRace.ORIGINAL_HYBRID);
            case "moonblood_hybrid" -> List.of(SupernaturalRace.HYBRID);
            case "tribrid_ascendant" -> List.of(SupernaturalRace.TRIBRID);
            case "ancestral_witch" -> List.of(SupernaturalRace.WITCH);
            case "void_siphon" -> List.of(SupernaturalRace.SIPHONER, SupernaturalRace.HERETIC);
            case "eternal_hunter" -> List.of(SupernaturalRace.HUNTER);
            case "elder_vampire" -> List.of(SupernaturalRace.VAMPIRE, SupernaturalRace.AUGUSTINE);
            case "alpha_primeval" -> List.of(SupernaturalRace.WEREWOLF);
            default -> List.of();
        };
    }
}
