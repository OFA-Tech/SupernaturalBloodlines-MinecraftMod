package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The shared clock of the groups of one player: for each group the side that is on (if any), the game time its cooldown ends and the length that cooldown started with (so a sweep
 * progresses). Immutable, and saved with the player, so a relog keeps the end tick ({@code now} is the game time). A side is saved by <b>the key of its member</b>, never by its place
 * in the table, so reordering the table file never moves a saved "on" to another power.
 * <p>
 * Rules (research section 6, risk 1; owner, 2026-11-22):
 * <ul>
 * <li>An instant side ({@link SideEvent#FIRED}) starts the group's cooldown at once.</li>
 * <li>A lasting side starts <b>nothing</b> when it goes {@code ON} (it only becomes the active side, which blocks the others) and starts the cooldown when it goes {@code OFF}, whether the
 * player turned it off early or its time ran out; the length is always <b>the main side's own cooldown</b>, whichever side was on ({@code mainCooldownTicks}, 0 for none).</li>
 * <li>An {@code OFF} of a side that is not the active one is ignored (no double start).</li>
 * </ul>
 */
public final class GroupClock {
    public record State(String active, long cooldownEnd, int total) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.optionalFieldOf("active", "").forGetter(State::active),
                Codec.LONG.fieldOf("end").forGetter(State::cooldownEnd), Codec.INT.optionalFieldOf("total", 0).forGetter(State::total)).apply(i, State::new));

        boolean on() {
            return !active.isEmpty();
        }
    }

    private static final State NONE = new State("", 0L, 0);
    private static final GroupClock IDLE = new GroupClock(Map.of());

    public static final Codec<GroupClock> CODEC = Codec.unboundedMap(Codec.STRING, State.CODEC).xmap(GroupClock::new, c -> c.states);

    private final Map<String, State> states;

    private GroupClock(Map<String, State> states) {
        this.states = Map.copyOf(states);
    }

    public static GroupClock idle() {
        return IDLE;
    }

    /** @param side the key of the member the event is about (never empty) */
    public GroupClock onEvent(String groupId, String side, SideEvent e, boolean lasting, int mainCooldownTicks, long now) {
        State old = states.getOrDefault(groupId, NONE);
        boolean starts = mainCooldownTicks > 0;
        long end = starts ? now + mainCooldownTicks : old.cooldownEnd;
        int total = starts ? mainCooldownTicks : old.total;
        State next;
        switch (e) {
            case FIRED -> next = new State(old.active, end, total);
            case ON -> next = lasting ? new State(side, old.cooldownEnd, old.total) : new State(side, end, total);
            case OFF -> {
                if (!old.active.equals(side)) {
                    return this;
                }
                next = lasting ? new State("", end, total) : new State("", old.cooldownEnd, old.total);
            }
            default -> throw new IllegalStateException();
        }
        return with(groupId, next, now);
    }

    /** Puts the state of one group in and drops every group that has nothing left (no side on, cooldown over), so the saved map never grows. */
    private GroupClock with(String groupId, State s, long now) {
        Map<String, State> m = new HashMap<>(states);
        m.put(groupId, s);
        m.values().removeIf(x -> !x.on() && x.cooldownEnd <= now);
        return new GroupClock(m);
    }

    /** Ticks of the shared cooldown left. */
    public int cooldownLeft(String groupId, long now) {
        State s = states.get(groupId);
        return s == null ? 0 : (int) Math.max(0L, Math.min(s.cooldownEnd - now, Integer.MAX_VALUE));
    }

    /** The game time the cooldown ends (0 when none). */
    public long cooldownEnd(String groupId) {
        State s = states.get(groupId);
        return s == null ? 0L : s.cooldownEnd;
    }

    /** The length the running cooldown started with, in ticks (0 when none). */
    public int cooldownTotal(String groupId) {
        State s = states.get(groupId);
        return s == null ? 0 : s.total;
    }

    /** The key of the member that is on in this group. */
    public Optional<String> activeSide(String groupId) {
        State s = states.get(groupId);
        return s == null || !s.on() ? Optional.empty() : Optional.of(s.active);
    }

    public boolean isIdle() {
        return states.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupClock c && c.states.equals(states);
    }

    @Override
    public int hashCode() {
        return states.hashCode();
    }

    @Override
    public String toString() {
        return "GroupClock" + states;
    }
}
