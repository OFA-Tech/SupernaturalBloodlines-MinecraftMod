package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** The language key of a line that counts something: {@code base + ".one"} for exactly one, else {@code base}. Both keys live in the language file. Pure. */
public final class Plural {
    private Plural() {
    }

    public static String key(String base, long n) {
        return n == 1 ? base + ".one" : base;
    }

    /** The counted lines of the hub that read "skill points": each has a ".one" twin in the language file. */
    public static final java.util.List<String> SKILL_POINT_LINES = java.util.List.of("supernaturalbloodlines.hub.skills.cost_short", "supernaturalbloodlines.hub.path.reward",
            "supernaturalbloodlines.hub.path.reward_ep", "supernaturalbloodlines.hub.skills.state.buyable", "supernaturalbloodlines.hub.skills.cost_line");
}