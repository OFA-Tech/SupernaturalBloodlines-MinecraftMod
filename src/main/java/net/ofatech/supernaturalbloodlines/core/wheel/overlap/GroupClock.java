package net.ofatech.supernaturalbloodlines.core.wheel.overlap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The shared clock of the groups of one player: for each group the side that is on (if any) and the game time its cooldown ends. Immutable, and saved with the player, so a relog keeps
 * the end tick ({@code now} is the game time).
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
    public record State(int active, long cooldownEnd) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.INT.fieldOf("active").forGetter(State::active),
                Codec.LONG.fieldOf("end").forGetter(State::cooldownEnd)).apply(i, State::new));
    }

    private static final GroupClock IDLE = new GroupClock(Map.of());

    public static final Codec<GroupClock> CODEC = Codec.unboundedMap(Codec.STRING, State.CODEC).xmap(GroupClock::new, c -> c.states);

    private final Map<String, State> states;

    private GroupClock(Map<String, State> states) {
        this.states = Map.copyOf(states);
    }

    public static GroupClock idle() {
        return IDLE;
    }

    public GroupClock onEvent(String groupId, int side, SideEvent e, boolean lasting, int mainCooldownTicks, long now) {
        State old = states.getOrDefault(groupId, new State(-1, 0L));
        State next;
        long start = mainCooldownTicks > 0 ? now + mainCooldownTicks : old.cooldownEnd;
        switch (e) {
            case FIRED -> next = new State(old.active, mainCooldownTicks > 0 ? start : old.cooldownEnd);
            case ON -> next = new State(side, lasting ? old.cooldownEnd : (mainCooldownTicks > 0 ? start : old.cooldownEnd));
            case OFF -> {
                if (old.active != side) {
                    return this;
                }
                next = new State(-1, lasting && mainCooldownTicks > 0 ? start : old.cooldownEnd);
            }
            default -> throw new IllegalStateException();
        }
        return with(groupId, next, now);
    }

    private GroupClock with(String groupId, State s, long now) {
        Map<String, State> m = new HashMap<>(states);
        if (s.active < 0 && s.cooldownEnd <= now) {
            m.remove(groupId);
        } else {
            m.put(groupId, s);
        }
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

    public OptionalInt activeSide(String groupId) {
        State s = states.get(groupId);
        return s == null || s.active < 0 ? OptionalInt.empty() : OptionalInt.of(s.active);
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
