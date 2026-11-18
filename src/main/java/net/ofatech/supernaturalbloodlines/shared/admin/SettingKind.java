package net.ofatech.supernaturalbloodlines.shared.admin;

/** What a setting holds. The two GAMERULE kinds have no config spec: the game rule itself checks them. */
public enum SettingKind {
    BOOL, INT, DOUBLE, CHOICE, TEXT, LIST, GAMERULE_BOOL, GAMERULE_INT;

    public boolean isRule() {
        return this == GAMERULE_BOOL || this == GAMERULE_INT;
    }
}
