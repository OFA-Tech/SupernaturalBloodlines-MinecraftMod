package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/** What the narrator says about the wheel and how Tab walks it: no colour carries a state, so every state is also in words. Pure. */
public final class WheelNarration {
    public static final String KEY_PAGE = "supernaturalbloodlines.wheel.narrate.page";
    public static final String KEY_POSITION = "supernaturalbloodlines.wheel.narrate.position";
    public static final String KEY_ON = "supernaturalbloodlines.wheel.state.on";
    public static final String KEY_READY = "supernaturalbloodlines.wheel.state.ready";
    public static final String KEY_COOLING = "supernaturalbloodlines.wheel.state.cooling";

    private WheelNarration() {
    }

    /**
     * The lines said when focus lands on a power: the mod, the group (only when the mod has more than one: pass an empty string when it has not), "page 2 of 4" only when
     * paged, "n of total", the power, its current mode (empty: none), and its state in words, or the lock reason when it is locked (the caller writes the reason as the
     * words "Locked: ..." ).
     *
     * @param translate turns a key and its arguments into the player's language ({@link #KEY_PAGE} gets page and pages, {@link #KEY_POSITION} index and total)
     * @param index     1-based position in the group
     */
    public static List<String> lines(String mod, String group, String power, String mode, String state, String lockReason, int index, int total, int page, int pages,
            BiFunction<String, Object[], String> translate) {
        List<String> out = new ArrayList<>();
        out.add(mod);
        if (group != null && !group.isEmpty()) {
            out.add(group);
        }
        if (pages > 1) {
            out.add(translate.apply(KEY_PAGE, new Object[] {page, pages}));
        }
        out.add(translate.apply(KEY_POSITION, new Object[] {index, total}));
        out.add(power);
        if (mode != null && !mode.isEmpty()) {
            out.add(mode);
        }
        out.add(lockReason != null && !lockReason.isEmpty() ? lockReason : state);
        return out;
    }

    /** The state in words: on, how long a cooldown has left (whole seconds, rounded up), or ready. No colour carries it. */
    public static String stateWord(boolean on, long cooldownLeftTicks, BiFunction<String, Object[], String> translate) {
        if (on) {
            return translate.apply(KEY_ON, new Object[0]);
        }
        if (cooldownLeftTicks > 0) {
            return translate.apply(KEY_COOLING, new Object[] {(cooldownLeftTicks + 19) / 20});
        }
        return translate.apply(KEY_READY, new Object[0]);
    }

    /** Tab walks the mods and then the groups as one list, and wraps; Shift+Tab walks it back. {@code focus} is -1 before the first Tab. */
    public static int tabStep(int focus, int mods, int groups, boolean backwards) {
        int total = mods + (groups >= 2 ? groups : 0);
        if (total == 0) {
            return -1;
        }
        if (focus < 0 || focus >= total) {
            return backwards ? total - 1 : 0;
        }
        return Math.floorMod(focus + (backwards ? -1 : 1), total);
    }

    /** Enter on a power: empty when it may be used, else why it may not (the caller says it). */
    public static Optional<LockReason> enter(PowerState state) {
        return state.lock() == LockReason.NONE ? Optional.empty() : Optional.of(state.lock());
    }
}
