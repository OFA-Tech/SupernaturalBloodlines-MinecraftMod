package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;

/** What may go in a Loadout slot, and what a slot loses when its power is gone. Pure. */
public final class LoadoutRules {
    /** Vampirism's vision key: a one-key cycle the wheel runs, never a slot (owner, plan 23 answer 3). */
    public static final ResourceLocation VISION = ResourceLocation.fromNamespaceAndPath("vampirism", "vision");

    private LoadoutRules() {
    }

    /** The one answer to "may this power be put in a Loadout slot": ours, and Vampirism's and Werewolves' actions except Vision. */
    public static boolean slottable(PowerSource source, ResourceLocation id) {
        return switch (source) {
            case BLOODLINE, WEREWOLVES -> true;
            case VAMPIRISM -> !VISION.equals(id);
            case TENSURA, INFINITY, GREAT_SAGE -> false;
        };
    }

    /** Bite is listed only for a player who can bite. */
    public static boolean biteListed(boolean canBite) {
        return canBite;
    }

    /** The ids in the slots that the player can no longer use ({@code available} says what they can use now), each once, in slot order. */
    public static Set<ResourceLocation> stale(Loadout l, Predicate<ResourceLocation> available) {
        Set<ResourceLocation> out = new java.util.LinkedHashSet<>();
        for (var slot : l.slots()) {
            slot.filter(id -> !available.test(id)).ifPresent(out::add);
        }
        return out;
    }

    /**
     * The loadout with every slot holding a stale id cleared, and those ids taken out of {@code offered}, so each is offered again when it comes back. A slot the
     * player cleared by hand while its power was still there is not touched (it is empty), so that power stays out.
     */
    public static Loadout prune(Loadout l, Predicate<ResourceLocation> available) {
        Loadout out = l;
        for (ResourceLocation id : stale(l, available)) {
            out = out.without(id).withoutOffered(id);
        }
        return out;
    }
}
