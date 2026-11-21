package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * Where the Beast of Gevaudan's encounter is (R184), written in the boss index. {@code HOST}: the Host walks as a man and waits for a full moon night. {@code BEAST}: the
 * Beast is out. {@code SPIRIT}: it collapsed, the Host lies protected and the Spirit flees for a while. {@code RETURN_PENDING}: the Spirit got away and comes back
 * once, on a later full moon night. {@code GONE}: it got away for good (the encounter ends without a kill and without a reward). {@code KILLED}: the Spirit was
 * struck with the Valet Pike. {@code FAILED}: the Host died, so the encounter is off until a later full moon night.
 */
public enum BeastPhase {
    NONE, HOST, BEAST, SPIRIT, RETURN_PENDING, GONE, KILLED, FAILED;

    /** The phases in which a Host entity stands in the world. */
    public boolean hasHost() {
        return this == HOST || this == SPIRIT || this == RETURN_PENDING || this == GONE || this == KILLED;
    }

    /** The phases that end the story: nothing more happens until an operator resets it. */
    public boolean over() {
        return this == GONE || this == KILLED;
    }

    public static BeastPhase of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return NONE;
        }
    }
}
