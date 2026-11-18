package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import java.util.List;
import java.util.Map;

/** Why a "?" is a "?", for the tooltip. Pure. */
public final class UnknownReason {
    private UnknownReason() {}

    /**
     * True when {@code fact} is missing only because the person is offline: their own record (what they have discovered of their line) cannot be read
     * while they are away, so their pack sees "?" until they are back. {@code facts} is what the server sent; it says the person is offline in PRESENCE.
     */
    public static boolean whileOffline(Fact fact, Map<Fact, List<String>> facts) {
        return fact == Fact.LINE && !facts.containsKey(fact) && List.of("offline").equals(facts.get(Fact.PRESENCE));
    }
}

