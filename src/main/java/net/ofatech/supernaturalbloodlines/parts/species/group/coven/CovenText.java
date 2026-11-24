package net.ofatech.supernaturalbloodlines.parts.species.group.coven;

import net.minecraft.network.chat.Component;

/** The words only a coven has (circle, votes, Elders). A pack never reads these keys. */
public final class CovenText {
    public static final String PREFIX = "supernaturalbloodlines.coven.";

    private CovenText() {
    }

    public static String key(String base) {
        return PREFIX + base;
    }

    public static Component of(String base, Object... args) {
        return Component.translatable(PREFIX + base, args);
    }

    /** A refusal: {@code coven.err.<base>}. */
    public static Component error(String base, Object... args) {
        return Component.translatable(PREFIX + "err." + base, args);
    }
}