package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelRules;
import org.junit.jupiter.api.Test;

class LoadoutRulesTest {
    private static final ResourceLocation RAGE = ResourceLocation.parse("vampirism:rage");
    private static final ResourceLocation HOWL = ResourceLocation.parse("werewolves:howl");
    private static final ResourceLocation ROAR = ResourceLocation.parse("supernaturalbloodlines:alpha_roar");

    @Test
    void visionIsNotSlottable() {
        assertFalse(LoadoutRules.slottable(PowerSource.VAMPIRISM, LoadoutRules.VISION));
        assertEquals(List.of(), WheelRules.loadoutPowers(List.of(new PowerRef(PowerSource.VAMPIRISM, LoadoutRules.VISION))), "the Loadout page lists Vision");
    }

    @Test
    void ourPowersAreSlottable() {
        assertTrue(LoadoutRules.slottable(PowerSource.BLOODLINE, Loadout.BITE));
        assertTrue(LoadoutRules.slottable(PowerSource.BLOODLINE, ROAR));
    }

    @Test
    void aFactionActionIsSlottable() {
        assertTrue(LoadoutRules.slottable(PowerSource.VAMPIRISM, RAGE));
        assertTrue(LoadoutRules.slottable(PowerSource.WEREWOLVES, HOWL));
    }

    @Test
    void aTensuraPowerIsNeverInTheLoadout() {
        for (PowerSource s : List.of(PowerSource.TENSURA, PowerSource.INFINITY, PowerSource.GREAT_SAGE)) {
            assertFalse(LoadoutRules.slottable(s, ResourceLocation.parse("tensura:coercion")), s.name());
        }
    }

    @Test
    void biteIsNotListedForAHuman() {
        assertFalse(LoadoutRules.biteListed(false));
    }

    @Test
    void biteIsListedForAWerewolfAVampireAHybrid() {
        assertTrue(LoadoutRules.biteListed(true));
    }

    private static Loadout of(ResourceLocation... ids) {
        Loadout l = Loadout.DEFAULT;
        for (int i = 0; i < ids.length; i++) {
            l = l.with(i, ids[i]);
        }
        return l;
    }

    @Test
    void staleIsWhatIsNotAvailableNow() {
        Loadout l = of(Loadout.BITE, RAGE, null, ROAR);
        assertEquals(Set.of(Loadout.BITE, RAGE), LoadoutRules.stale(l, id -> id.equals(ROAR)));
        assertEquals(Set.of(), LoadoutRules.stale(l, id -> true));
        Loadout pruned = LoadoutRules.prune(l, id -> id.equals(ROAR));
        assertEquals(List.of(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(ROAR), Optional.empty(), Optional.empty()), pruned.slots());
    }

    @Test
    void aHandClearedAvailableActionIsNotOfferedAgain() {
        Loadout offered = Loadout.DEFAULT.withActionOffered(ROAR);
        Loadout cleared = offered.with(offered.slotOf(ROAR), null);
        Loadout pruned = LoadoutRules.prune(cleared, id -> true);
        assertEquals(cleared, pruned, "a prune touched a loadout with nothing stale");
        assertFalse(pruned.withActionOffered(ROAR).contains(ROAR), "a power the player cleared came back");
    }

    @Test
    void aClearedUnavailableActionIsOfferedWhenItReturns() {
        Loadout l = Loadout.DEFAULT.withBiteOffered().withActionOffered(ROAR);
        Loadout gone = LoadoutRules.prune(l, id -> false);
        assertFalse(gone.contains(Loadout.BITE) || gone.contains(ROAR));
        assertFalse(gone.offered().contains(ROAR) || gone.biteOffered(), "a power taken away is still marked as offered");
        Loadout back = gone.withBiteOffered().withActionOffered(ROAR);
        assertEquals(Optional.of(Loadout.BITE), back.get(0));
        assertTrue(back.contains(ROAR));
    }

    /** A save from before plan 23 that gave every new player Bite in slot 1: a human loses it, and gets it back when they can bite. */
    @Test
    void anOldSavesBiteIsFreedForAHuman() {
        Loadout old = of(Loadout.BITE).withBiteOffered();
        Loadout human = LoadoutRules.prune(old, id -> false);
        assertEquals(Optional.empty(), human.get(0));
        assertEquals(Optional.of(Loadout.BITE), human.withBiteOffered().get(0));
    }
}
