package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import net.minecraft.network.chat.Component;

/** The words only a brotherhood has (the lead by mark, the Code, the curse, the warning). None of them names a species or a show. */
public final class BrotherhoodText {
    public static final String PREFIX = "supernaturalbloodlines.brotherhood.";

    private BrotherhoodText() {
    }

    public static String key(String base) {
        return PREFIX + base;
    }

    public static Component of(String base, Object... args) {
        return Component.translatable(PREFIX + base, args);
    }

    /** A refusal: {@code brotherhood.err.<base>}. */
    public static Component error(String base, Object... args) {
        return Component.translatable(PREFIX + "err." + base, args);
    }
}