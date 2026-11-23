package net.ofatech.supernaturalbloodlines.core.wheel;

/** Remembers the hash of the last catalogue sent to one viewer: only a different hash is worth a packet. */
public final class ChangeGate {
    private boolean has;
    private int last;

    /** @return true when {@code hash} differs from the last one offered (or none was), and remembers it */
    public boolean offer(int hash) {
        if (has && last == hash) {
            return false;
        }
        has = true;
        last = hash;
        return true;
    }

    public void reset() {
        has = false;
    }
}
