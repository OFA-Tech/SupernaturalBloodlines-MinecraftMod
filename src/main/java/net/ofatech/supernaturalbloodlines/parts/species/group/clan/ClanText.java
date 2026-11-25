package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import net.minecraft.network.chat.Component;

/** The words only a clan has (ranks, exile, the Charter, the contest). A pack or a coven never reads these keys, and none of them names a species. */
public final class ClanText {
    public static final String PREFIX = "supernaturalbloodlines.clan.";

    private ClanText() {
    }

    public static String key(String base) {
        return PREFIX + base;
    }

    public static Component of(String base, Object... args) {
        return Component.translatable(PREFIX + base, args);
    }

    /** A refusal: {@code clan.err.<base>}. */
    public static Component error(String base, Object... args) {
        return Component.translatable(PREFIX + "err." + base, args);
    }
}
