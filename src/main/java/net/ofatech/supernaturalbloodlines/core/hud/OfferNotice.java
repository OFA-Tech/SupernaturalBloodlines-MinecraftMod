package net.ofatech.supernaturalbloodlines.core.hud;

import net.minecraft.resources.ResourceLocation;

/** What to tell a player after an action was offered to them: where it went, or that there was no room. Pure. */
public record OfferNotice(Kind kind, int slot) {
    public enum Kind { NONE, PLACED, FULL }

    public static final OfferNotice NONE = new OfferNotice(Kind.NONE, -1);

    /**
     * @param before the loadout before the offer
     * @param after  the loadout after {@link Loadout#withActionOffered}
     * @param id     the action
     */
    public static OfferNotice of(Loadout before, Loadout after, ResourceLocation id) {
        if (before.offered().contains(id)) {
            return NONE;
        }
        return after.offered().contains(id) ? new OfferNotice(Kind.PLACED, after.slotOf(id)) : new OfferNotice(Kind.FULL, -1);
    }
}
