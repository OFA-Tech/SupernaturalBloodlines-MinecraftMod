package net.ofatech.supernaturalbloodlines.parts.supernatural.task;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.parts.supernatural.LadderService;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.StatePredicates;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector;
import net.ofatech.supernaturalbloodlines.shared.supernatural.SireCount;
import net.ofatech.supernaturalbloodlines.shared.werewolves.EmissaryLines;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import org.jetbrains.annotations.Nullable;

/**
 * Turns what happens to a player into {@link TaskEvent}s and hands them to {@link LadderRules#apply}, the one door that
 * knows about the Ghoul freeze. One state write per call, however many events.
 */
public final class TaskTracker {
    /** Counting checks that run on a timer (thirst, pack time) add this many ticks each time. */
    public static final int PERIOD = 200;

    private TaskTracker() {
    }

    /** Mods that drive a FakePlayer (deployers, turrets) must not climb ladders for their owners. */
    public static boolean tracked(ServerPlayer p) {
        return SupernaturalAccess.tracksFakePlayers() || !(p instanceof FakePlayer);
    }

    /** Creature race, then the magic race if there is one. */
    public static List<SupernaturalRace> races(ServerPlayer p) {
        BloodlineState b = BloodlineAccess.get(p);
        List<SupernaturalRace> out = new ArrayList<>();
        out.add(SupernaturalRace.creature(b, factionId(p)));
        SupernaturalRace.magic(b).ifPresent(out::add);
        return out;
    }

    @Nullable
    private static String factionId(ServerPlayer p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f == null ? null : f.getID().toString();
    }

    /** One event now. */
    public static void emit(ServerPlayer p, TaskEvent event) {
        run(p, s -> List.of(event));
    }

    /**
     * Everything that can be read off the player right now (level, states, sired, days as a race), so a task that becomes active
     * after its condition was met completes at once. Called on login, on a change of faction, species or rung, and by {@link #periodic}.
     */
    public static void recheck(ServerPlayer p) {
        run(p, s -> readings(p, s));
    }

    /** Every {@link #PERIOD} ticks: the readings, plus the checks that count time (thirst, pack time). */
    public static void periodic(ServerPlayer p) {
        run(p, s -> {
            List<TaskEvent> events = new ArrayList<>(readings(p, s));
            BloodlineState b = BloodlineAccess.get(p);
            for (var task : LadderRules.activeTasks(LadderTable.current(), s, races(p))) {
                for (var leaf : task.leaves()) {
                    if (leaf.detector() == Detector.THIRST_DAYS && b.thirst() >= intArg(leaf.args().get("min"))) {
                        events.add(TaskEvent.of(Detector.THIRST_DAYS, PERIOD, "min", leaf.args().get("min")));
                    }
                }
            }
            if (StatePredicates.inPack(b)) {
                events.add(TaskEvent.of(Detector.PACK_DAYS, PERIOD));
            }
            return events;
        });
    }

    private static int intArg(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // a bad arg never counts
        }
    }

    private static List<TaskEvent> readings(ServerPlayer p, SupernaturalState s) {
        List<TaskEvent> events = new ArrayList<>();
        var handler = VampirismAPI.factionPlayerHandler(p);
        IPlayableFaction<?> faction = handler.getCurrentFaction();
        if (faction != null) {
            events.add(TaskEvent.of(Detector.LEVEL, handler.getCurrentLevel(), "faction", faction.getID().toString()));
        }
        BloodlineState b = BloodlineAccess.get(p);
        long now = p.level().getGameTime();
        boolean emissary = EmissaryLines.isEmissary(p);
        boolean moon = ModList.get().isLoaded("werewolves") && MoonControl.of(p) != MoonControl.Control.NONE;
        for (String id : StatePredicates.IDS) {
            long value = StatePredicates.value(id, b, now, emissary, moon).orElse(0);
            if (value > 0) {
                events.add(TaskEvent.of(Detector.STATE, value, "predicate", id));
            }
        }
        int sired = SireCount.of(p);
        if (sired > 0) {
            events.add(TaskEvent.of(Detector.SIRED, sired));
        }
        for (SupernaturalRace race : races(p)) {
            Long since = s.raceSince().get(race.id());
            if (since != null) {
                events.add(TaskEvent.of(Detector.DAYS_AS, Math.max(0, now - since) / StatePredicates.DAY_TICKS, "race", race.id()));
            }
        }
        return events;
    }

    private static void run(ServerPlayer p, Function<SupernaturalState, List<TaskEvent>> eventsFor) {
        if (!tracked(p)) {
            return;
        }
        LadderTable table = LadderTable.current();
        SupernaturalState before = SupernaturalAccess.get(p);
        List<SupernaturalRace> races = races(p);
        SupernaturalState s = stamped(before, races, p.level().getGameTime());
        for (TaskEvent e : eventsFor.apply(s)) {
            s = LadderRules.apply(table, s, races, e);
        }
        s = LadderRules.refreshTotals(table, s, races);
        // a rung left with no tasks (its mod or part is missing) is reached on its own, with its reward
        int sp = 0;
        long ep = 0;
        SupernaturalRace reachedRace = null;
        int reachedRung = -1;
        if (!races.contains(SupernaturalRace.GHOUL)) {
            for (SupernaturalRace race : races) {
                LadderRules.ClaimResult settled = LadderRules.settle(table, s, race);
                s = settled.state();
                sp += settled.sp();
                ep += settled.ep();
                if (settled.rungReached() >= 0) {
                    reachedRace = race;
                    reachedRung = settled.rungReached();
                }
            }
        }
        if (!s.equals(before)) {
            SupernaturalAccess.set(p, s);
        }
        if (reachedRung >= 0) {
            LadderService.pay(p, sp, ep, reachedRace, reachedRung);
        }
    }

    /** The first time a race is seen, now is when the player became it (migration may know better). */
    private static SupernaturalState stamped(SupernaturalState s, List<SupernaturalRace> races, long now) {
        if (s.schema() == 0) {
            return s; // not migrated yet: the migration knows when the player really became this race (turnedAt), "now" would be wrong
        }
        Map<String, Long> since = null;
        for (SupernaturalRace race : races) {
            if (!s.raceSince().containsKey(race.id())) {
                since = since == null ? new HashMap<>(s.raceSince()) : since;
                since.put(race.id(), now);
            }
        }
        return since == null ? s : s.toBuilder().raceSince(since).build();
    }
}
