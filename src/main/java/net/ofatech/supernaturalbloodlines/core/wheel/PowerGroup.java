package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.List;

/** The middle ring: groups of one mod. Every source but Tensura uses MAIN; Tensura has skills, spells (Magic) and battlewill arts. */
public enum PowerGroup {
    MAIN, SKILLS, MAGIC, BATTLEWILL;

    private static final List<PowerGroup> ORDER = List.of(SKILLS, MAGIC, BATTLEWILL);

    /** The groups a ring can show, in order (MAIN is never a ring). */
    public static List<PowerGroup> displayOrder() {
        return ORDER;
    }
}
