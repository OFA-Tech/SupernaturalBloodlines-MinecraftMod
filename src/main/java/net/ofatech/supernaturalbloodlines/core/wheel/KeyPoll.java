package net.ofatech.supernaturalbloodlines.core.wheel;

/** Whether a wheel key that is down but not yet held long enough to open the wheel is still down. Pure. */
public final class KeyPoll {
    public enum Kind { KEYSYM, SCANCODE, MOUSE }

    private KeyPoll() {
    }

    /**
     * A key bound by scan code cannot be polled by its code, so the release event is all that ends its press: the window losing focus (a release that goes to another
     * window never arrives) must end it too. A key by key code and a mouse button are polled, and also end when the window is not the active one.
     *
     * @param polledDown what the poll said (ignored for a scan-code key)
     */
    public static boolean pendingStillDown(Kind kind, boolean polledDown, boolean windowActive) {
        if (!windowActive) {
            return false;
        }
        return kind == Kind.SCANCODE || polledDown;
    }
}
