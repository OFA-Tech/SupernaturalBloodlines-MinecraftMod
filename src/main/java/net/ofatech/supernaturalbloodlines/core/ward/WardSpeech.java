package net.ofatech.supernaturalbloodlines.core.ward;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** The spoken invitation (R305): the phrase, and who it is said to. Pure. */
public final class WardSpeech {
    public static final String COME_IN = "come in";
    public static final String GET_OUT = "get out";
    public static final double LOOK_RANGE = 8.0;
    public static final double NEAR_RANGE = 5.0;

    /** A player near the speaker: how far, and whether the speaker is looking at them with a clear line. */
    public record Candidate(UUID id, double distance, boolean lookedAt) {}

    private WardSpeech() {
    }

    /** The whole message, trimmed, lower-cased, without trailing . , ! ? and spaces, is exactly {@code phrase}. */
    public static boolean matches(String raw, String phrase) {
        String t = raw.strip().toLowerCase(Locale.ROOT).replaceAll("[.,!?\\s]+$", "");
        return !t.isEmpty() && t.equals(phrase);
    }

    /**
     * The one the words are for: the player looked at within {@value #LOOK_RANGE} blocks, else the nearest other player within {@value #NEAR_RANGE};
     * never the speaker (the caller leaves them out).
     */
    public static Optional<UUID> pick(List<Candidate> others) {
        Optional<Candidate> looked = others.stream().filter(c -> c.lookedAt() && c.distance() <= LOOK_RANGE)
                .min(Comparator.comparingDouble(Candidate::distance));
        if (looked.isPresent()) {
            return looked.map(Candidate::id);
        }
        return others.stream().filter(c -> c.distance() <= NEAR_RANGE).min(Comparator.comparingDouble(Candidate::distance)).map(Candidate::id);
    }
}
