package net.ofatech.supernaturalbloodlines.core.boss;

/** The body a shifting boss has now (plan 17, decision 4): one entity, one UUID, one boss bar, a form that is synced. */
public enum BossForm {
    HUMAN, SHIFTING, BEAST;

    /** The form for a number read from the wire or from a save: anything out of range is the human form. */
    public static BossForm of(int ordinal) {
        BossForm[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : HUMAN;
    }
}
