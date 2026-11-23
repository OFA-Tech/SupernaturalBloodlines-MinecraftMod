package net.ofatech.supernaturalbloodlines.core.wheel;

/**
 * Remembers the last catalogue sent to one viewer: only a list that is not {@code equals} to it is worth a packet. It compares the lists themselves, not their hash codes, so two
 * different lists with the same hash can never hide a change. The value must be immutable (the catalogue is a copied list of records).
 */
public final class ChangeGate {
    private boolean has;
    private Object last;

    /** @return true when {@code value} differs from the last one offered (or none was), and remembers it */
    public boolean offer(Object value) {
        if (has && last.equals(value)) {
            return false;
        }
        has = true;
        last = value;
        return true;
    }

    public void reset() {
        has = false;
        last = null;
    }
}
