package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.supernatural.Ladder;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.QuestRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.QuestSpec;
import net.ofatech.supernaturalbloodlines.core.supernatural.Rung;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec;

/**
 * What the Path and Quests pages show, worked out from the synced state and the client's ladder table. Pure, no client classes: the
 * themes only draw it. It decides nothing: a claim is still the server's, this only says which stamp to offer.
 */
public final class HubModel {
    /** @param claimed tasks of this rung claimed so far (all of them once reached) */
    public record RungRow(String id, String name, int index, RungStatus status, int claimed, int tasks, int sp, long ep) {
    }

    /** One ladder: its rungs (index 0 is the race itself) and the tasks of the active rung, if not at the top. */
    public record LadderView(SupernaturalRace race, List<RungRow> rungs, Optional<RungRow> active, List<TaskRow> tasks) {
        public boolean atTop() {
            return active.isEmpty();
        }
    }

    /**
     * @param progress  in the task's data unit (days for the day counts); equal to {@code target} once done
     * @param counted   the progress is known (a counting task, or one done); a level or state task shows only done or not
     */
    public record TaskRow(SupernaturalRace race, TaskSpec task, TaskState state, long progress, long target, boolean counted, String source,
                          TaskText text, List<TaskRow> members) {
    }

    public sealed interface Arg permits Num, Key, Item {
    }

    public record Num(long value) implements Arg {
    }

    /** A translated word with an English fallback, for ids the lang file may not know (a datapack's rite). */
    public record Key(String key, String fallback) implements Arg {
    }

    public record Item(String id) implements Arg {
    }

    /**
     * @param override a per-task text a datapack or the lang file can give ({@code supernaturalbloodlines.hub.task.id.<task id>}); used when it exists
     * @param hint     the source mod's own way of doing it (shown in the tooltip), or null
     */
    public record TaskText(String key, String fallback, List<Arg> args, String override, String hint) {
    }

    private static final String HUB = "supernaturalbloodlines.hub.";

    private HubModel() {
    }

    /** The ladders of the player's races, creature first. {@code levels} is the player's level per faction id (only the current faction has one). */
    public static List<LadderView> ladders(LadderTable table, SupernaturalState s, List<SupernaturalRace> races, Map<String, Integer> levels) {
        boolean frozen = races.contains(SupernaturalRace.GHOUL);
        List<LadderView> out = new ArrayList<>();
        for (SupernaturalRace race : races) {
            Ladder ladder = table.get(race);
            int reached = s.rung(race);
            List<RungRow> rows = new ArrayList<>();
            for (int i = 0; i < ladder.rungs().size(); i++) {
                Rung r = ladder.rungs().get(i);
                RungStatus status = i <= reached ? RungStatus.REACHED : i == reached + 1 ? RungStatus.CURRENT : RungStatus.LOCKED;
                int claimed = status == RungStatus.REACHED ? r.tasks().size()
                        : (int) r.tasks().stream().filter(t -> status == RungStatus.CURRENT && s.claimed().contains(t.id())).count();
                rows.add(new RungRow(r.id(), r.name(), i, status, claimed, r.tasks().size(), r.sp(), r.ep()));
            }
            Optional<RungRow> active = rows.stream().filter(r -> r.status() == RungStatus.CURRENT).findFirst();
            List<TaskRow> tasks = new ArrayList<>();
            ladder.rung(reached + 1).ifPresent(r -> r.tasks().forEach(t -> tasks.add(row(race, t, s, frozen, levels))));
            tasks.sort(Comparator.comparingInt(t -> order(t.state())));
            out.add(new LadderView(race, rows, active, tasks));
        }
        return out;
    }

    /** A skill quest (Q-VAM-6) still open, with its tasks; a done task shows as ticked (a quest has no claim, it unlocks its skill). */
    public record QuestView(String id, String name, List<TaskRow> tasks) {
    }

    public static List<QuestView> quests(SkillTable skills, SupernaturalState s, List<SupernaturalRace> races) {
        boolean frozen = races.contains(SupernaturalRace.GHOUL);
        List<QuestView> out = new ArrayList<>();
        for (QuestSpec q : QuestRules.open(skills, s, races)) {
            List<TaskRow> rows = q.tasks().stream().map(t -> row(races.get(0), t, s, frozen, Map.of()))
                    .map(r -> r.state() == TaskState.CLAIMABLE ? withState(r, TaskState.CLAIMED) : r).toList();
            out.add(new QuestView(q.id(), q.name(), rows));
        }
        return out;
    }

    private static TaskRow withState(TaskRow r, TaskState state) {
        return new TaskRow(r.race(), r.task(), state, r.progress(), r.target(), r.counted(), r.source(), r.text(), r.members());
    }

    /** The task the Quests page shows in detail: the selected one while it is still listed, else the first claimable, else the first. */
    public static Optional<TaskRow> pick(List<TaskRow> rows, String selectedId) {
        return rows.stream().filter(r -> r.task().id().equals(selectedId)).findFirst()
                .or(() -> rows.stream().filter(r -> r.state() == TaskState.CLAIMABLE).findFirst())
                .or(() -> rows.stream().findFirst());
    }

    /** Claimable first, then open, then claimed; the data order within each. */
    private static int order(TaskState s) {
        return switch (s) {
            case CLAIMABLE -> 0;
            case OPEN, FROZEN, UNAVAILABLE -> 1;
            case CLAIMED -> 2;
        };
    }

    /** False when this client cannot see rites finish (Enchanted's hook is missing); set by the hub client when Enchanted is installed. */
    static volatile java.util.function.BooleanSupplier ritesCount = () -> true;

    public static void ritesCountWhen(java.util.function.BooleanSupplier supplier) {
        ritesCount = supplier;
    }

    static TaskRow row(SupernaturalRace race, TaskSpec t, SupernaturalState s, boolean frozen, Map<String, Integer> levels) {
        boolean done = LadderRules.isDone(t, s);
        TaskState state = frozen ? TaskState.FROZEN : s.claimed().contains(t.id()) ? TaskState.CLAIMED : done ? TaskState.CLAIMABLE : TaskState.OPEN;
        if (state == TaskState.OPEN && !t.isGroup() && t.detector() == TaskSpec.Detector.RITE && !ritesCount.getAsBoolean()) {
            state = TaskState.UNAVAILABLE; // 13d M9: the hook that sees a rite finish is not in place, so the task would stay at 0 for no visible reason
        }
        if (t.isGroup()) {
            List<TaskRow> members = t.anyOf().stream().map(m -> row(race, m, s, frozen, levels)).toList();
            return new TaskRow(race, t, state, done ? 1 : 0, 1, true, source(t), new TaskText(HUB + "task.any_of", "One of:", List.of(),
                    HUB + "task.id." + t.id(), null), members);
        }
        long unit = t.detector().unit();
        long target = Math.max(1, t.target() / unit);
        if (t.detector() == TaskSpec.Detector.LEVEL) {
            // a level step shows the level reached so far in that faction: the mod's own number, read from its synced client data
            long level = levels.getOrDefault(t.args().getOrDefault("faction", ""), 0);
            return new TaskRow(race, t, state, done ? target : Math.min(target, level), target, true, source(t), text(t), List.of());
        }
        long raw = t.total() ? s.lifetime().getOrDefault(t.totalKey(race), 0L) : s.counters().getOrDefault(t.id(), 0L);
        long progress = done ? target : Math.min(target, raw / unit);
        return new TaskRow(race, t, state, progress, target, done || t.detector().counting(), source(t), text(t), List.of());
    }

    /** The mod the task is about, for its tag: a level step is its faction's mod, a rite is Enchanted's, the apex tier Tensura's, anything else ours. */
    public static String source(TaskSpec t) {
        if (t.isGroup()) {
            List<String> sources = t.anyOf().stream().map(HubModel::source).distinct().toList();
            return sources.size() == 1 ? sources.get(0) : "species";
        }
        return switch (t.detector()) {
            case RITE -> "enchanted";
            case TENSURA_TIER -> "tensura";
            case LEVEL -> {
                String faction = t.args().getOrDefault("faction", "");
                yield faction.startsWith("werewolves:") ? "werewolves" : "vampirism";
            }
            default -> "species";
        };
    }

    /** The words of a leaf task. Counting tasks carry no number: the row shows the progress next to it. */
    public static TaskText text(TaskSpec t) {
        Map<String, String> a = t.args();
        String override = HUB + "task.id." + t.id();
        return switch (t.detector()) {
            case LEVEL -> {
                String faction = a.getOrDefault("faction", "");
                String id = faction.replace(':', '.');
                yield new TaskText(HUB + "task.level", "%s level",
                        List.of(new Key(HUB + "faction." + id, pretty(path(faction)))), override, HUB + "hint.level." + id);
            }
            case STATE -> new TaskText(HUB + "task.when." + a.get("predicate"), pretty(a.get("predicate")), List.of(new Num(t.target())), override, null);
            case FEED -> plain(HUB + "task.feed", "Feed on human-like blood", override);
            case KILL -> new TaskText(HUB + "task.kill", "Kill: %s", List.of(new Key(HUB + "kill." + a.get("category"), pretty(a.get("category")))),
                    override, null);
            case RITE -> {
                String rite = a.getOrDefault("rite", "any");
                String role = "target".equals(a.get("role")) ? "target" : "performer";
                // a rite's own name is Enchanted's lang key for it, rite.<namespace>.<path>
                Key name = rite.equals("any") ? new Key(HUB + "rite.any", "any rite") : new Key("rite." + rite.replace(':', '.'), pretty(path(rite)));
                yield new TaskText(HUB + "task.rite." + role, role.equals("target") ? "Be the target of %s" : "Perform %s", List.of(name),
                        override, HUB + "hint.rite");
            }
            case SIGNAL -> plain(HUB + "task.signal." + a.get("signal"), pretty(a.get("signal")), override);
            case ITEM_USE -> new TaskText(HUB + "task.item_use", "Use %s", List.of(new Item(a.getOrDefault("item", ""))), override, null);
            case SIRED -> plain(HUB + "task.sired", "Sire others", override);
            case DAYS_AS -> new TaskText(HUB + "task.days_as", "Live %s days as a %s", List.of(new Num(t.target()),
                    new Key("supernaturalbloodlines.supernatural.race." + a.get("race"), pretty(a.get("race")))), override, null);
            case THIRST_DAYS -> new TaskText(HUB + "task.thirst_days", "Endure thirst of %s or more (days)", List.of(new Num(parse(a.get("min")))),
                    override, null);
            case PACK_DAYS -> plain(HUB + "task.pack_days", "Run with a pack (days)", override);
            case TENSURA_TIER -> new TaskText(HUB + "task.tensura_tier", "Evolve to your line's apex tier", List.of(), override, HUB + "hint.tensura_tier");
        };
    }

    private static TaskText plain(String key, String fallback, String override) {
        return new TaskText(key, fallback, List.of(), override, null);
    }

    private static String path(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    /** {@code rite_of_the_moon} -> {@code Rite of the moon}: the English fallback for an id the lang file does not name. */
    public static String pretty(String id) {
        if (id == null || id.isEmpty()) {
            return "?";
        }
        String words = id.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}
