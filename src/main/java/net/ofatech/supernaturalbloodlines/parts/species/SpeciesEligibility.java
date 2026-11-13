package net.ofatech.supernaturalbloodlines.parts.species;

import net.minecraft.server.level.ServerPlayer;

/** Only human-like players can take a species. Without Tensura nobody installs a check, so everyone is. */
public final class SpeciesEligibility {
    @FunctionalInterface
    public interface HumanLikeCheck {
        boolean isHumanLike(ServerPlayer p);
    }

    private static volatile HumanLikeCheck check = p -> true;

    private SpeciesEligibility() {
    }

    /** Plan 5 installs the Tensura check (race tag {@code tensura:human_like}). */
    public static void setCheck(HumanLikeCheck c) {
        check = c;
    }

    public static HumanLikeCheck check() {
        return check;
    }

    public static boolean isHumanLike(ServerPlayer p) {
        return check.isHumanLike(p);
    }
}
