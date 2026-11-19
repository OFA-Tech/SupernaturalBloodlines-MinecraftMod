package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

/** What the hub offers for a pack wolf (R148, R149): every action is a real command row, and each is off for a stated reason. */
class NpcActionsTest {
    private static HubSocial.Npc brother(boolean loaded, boolean canOrder, String order) {
        return new HubSocial.Npc(7, "Ash", "werewolves:wolf", "brother", order, false, true, loaded, 5, 80, 2, "Derek", canOrder, 50);
    }

    private static HubSocial.Npc subordinate() {
        return new HubSocial.Npc(8, "Rimuru's Wolf", "tensura:direwolf", "subordinate", "", false, false, true, 3, 100, 1, "Derek", true, 25);
    }

    @Test
    void aLoadedBrotherOfTheViewerOffersEveryAction() {
        List<NpcActions.Act> acts = NpcActions.of(brother(true, true, "guard"));
        assertEquals(List.of("follow", "guard", "rest", "hunt", "sort", "release"), acts.stream().map(NpcActions.Act::id).toList());
        assertTrue(acts.stream().allMatch(NpcActions.Act::enabled));
        assertTrue(acts.stream().filter(a -> a.id().equals("guard")).findFirst().orElseThrow().current(), "the order it has now");
        assertTrue(acts.stream().filter(a -> a.id().equals("sort")).findFirst().orElseThrow().current(), "Sort is on");
        assertTrue(acts.stream().filter(NpcActions.Act::confirm).map(NpcActions.Act::id).toList().equals(List.of("release")), "only Release asks first");
    }

    @Test
    void everyActionRunsARealPackNpcRowWithAFittingForm() {
        for (NpcActions.Act a : NpcActions.of(brother(true, true, "follow"))) {
            AdminCommands.Row row = AdminCommands.byId(a.rowId()).orElseThrow();
            assertTrue(AdminCommands.command(row, a.args()).isPresent(), a.id() + " does not fit " + row.form());
            assertEquals("pack", row.root());
            assertEquals(AdminCommands.Ui.HUB_BUTTON, row.ui());
        }
        assertEquals("pack npc order 7 rest", AdminCommands.command(AdminCommands.byId("P21").orElseThrow(), List.of("7", "rest")).orElseThrow());
    }

    @Test
    void aTensuraSubordinateKeepsTensurasOrdersSoEveryButtonIsOff() {
        assertTrue(NpcActions.of(subordinate()).stream().noneMatch(NpcActions.Act::enabled));
        assertEquals("tensura", NpcActions.refusal(subordinate()));
    }

    @Test
    void aWolfOfAnotherOwnerCanOnlyBeLookedAtUnlessTheViewerLeadsTheirPack() {
        assertEquals("owner", NpcActions.refusal(brother(true, false, "rest")));
        assertTrue(NpcActions.of(brother(true, false, "rest")).stream().noneMatch(NpcActions.Act::enabled));
    }

    @Test
    void aWolfOutOfReachTakesNoOrderButCanBeReleased() {
        List<NpcActions.Act> acts = NpcActions.of(brother(false, true, "rest"));
        assertEquals("loaded", NpcActions.refusal(brother(false, true, "rest")));
        assertFalse(acts.stream().filter(a -> a.id().equals("follow")).findFirst().orElseThrow().enabled());
        assertTrue(acts.stream().filter(a -> a.id().equals("release")).findFirst().orElseThrow().enabled(), "its ledger entry can still go");
    }
}
