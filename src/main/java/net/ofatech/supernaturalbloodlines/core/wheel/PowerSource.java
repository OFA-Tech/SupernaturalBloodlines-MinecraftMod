package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.List;

/** Where a power comes from: the inner ring of the wheel, in this order. */
public enum PowerSource {
    BLOODLINE("supernaturalbloodlines"),
    VAMPIRISM("vampirism"),
    WEREWOLVES("werewolves"),
    TENSURA("tensura"),
    INFINITY("infinity"),
    GREAT_SAGE("greatsage");

    private static final List<PowerSource> ORDER = List.of(values());

    private final String modId;

    PowerSource(String modId) {
        this.modId = modId;
    }

    public String modId() {
        return modId;
    }

    public static List<PowerSource> displayOrder() {
        return ORDER;
    }
}
