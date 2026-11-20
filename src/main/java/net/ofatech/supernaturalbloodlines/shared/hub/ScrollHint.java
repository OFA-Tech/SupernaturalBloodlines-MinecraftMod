package net.ofatech.supernaturalbloodlines.shared.hub;

/** Whether a scrolled region has more below what is shown: the page then says so, so nobody takes a long chapter for a short one. Pure. */
public final class ScrollHint {
    private ScrollHint() {
    }

    public static boolean moreBelow(int offset, int max) {
        return max > 0 && offset < max;
    }
}