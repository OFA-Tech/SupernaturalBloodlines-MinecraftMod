package net.ofatech.supernaturalbloodlines.core.supernatural;

import java.util.List;
import java.util.Optional;

/** The rungs of one race; index 0 is the race itself. */
public record Ladder(SupernaturalRace race, List<Rung> rungs) {
    public Ladder {
        rungs = List.copyOf(rungs);
    }

    public static Ladder empty(SupernaturalRace race) {
        return new Ladder(race, List.of());
    }

    public Optional<Rung> rung(int index) {
        return index >= 0 && index < rungs.size() ? Optional.of(rungs.get(index)) : Optional.empty();
    }
}
