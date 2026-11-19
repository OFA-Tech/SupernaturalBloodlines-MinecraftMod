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
        return new HubSocial.Npc(7, "Ash", "werewolves:wolf", "brother", order, false, true, loaded, 5, 80, 2, "Derek", canOrder, canOrder, 50);
    }

    /** The viewer leads the owner's pack: may give orders, is not the owner. */
    private static HubSocial.Npc ofAMate(String order, boolean hunt) {
        return new HubSocial.Npc(7, "Ash", "werewolves:wolf", "brother", order, hunt, false, true, 5, 80, 2, "Derek", true, false, 50);
    }

    private static HubSocial.Npc subordinate() {
        return new HubSocial.Npc(8, "Rimuru's Wolf", "tensura:direwolf", "subordinate", "", false, false, true, 3, 100, 1, "Derek", true, true, 25);
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

    @Test
    void theLeaderOrdersAnotherMembersBrotherButOnlyTheOwnerReleasesIt() {
        List<NpcActions.Act> acts = NpcActions.of(ofAMate("guard", false));
        assertTrue(acts.stream().filter(a -> a.id().equals("guard") || a.id().equals("rest") || a.id().equals("hunt")).allMatch(NpcActions.Act::enabled), "the leader gives orders");
        NpcActions.Act release = acts.stream().filter(a -> a.id().equals("release")).findFirst().orElseThrow();
        assertFalse(release.enabled());
        assertEquals("owner_release", release.why());
        assertTrue(NpcActions.of(brother(true, true, "guard")).stream().filter(a -> a.id().equals("release")).findFirst().orElseThrow().enabled(), "the owner releases");
    }

    @Test
    void huntAndSortAreOffWhileItFollowsButTurningThemOffIsAlwaysPossible() {
        List<NpcActions.Act> following = NpcActions.of(new HubSocial.Npc(7, "Ash", "werewolves:wolf", "brother", "follow", false, false, true, 5, 80, 2, "Derek", true, true, 50));
        assertEquals("follow", following.stream().filter(a -> a.id().equals("hunt")).findFirst().orElseThrow().why());
        assertEquals("follow", following.stream().filter(a -> a.id().equals("sort")).findFirst().orElseThrow().why());
        assertTrue(following.stream().filter(a -> a.id().equals("follow") || a.id().equals("guard")).allMatch(NpcActions.Act::enabled), "the orders themselves stay on");
        assertTrue(NpcActions.of(new HubSocial.Npc(7, "Ash", "werewolves:wolf", "brother", "guard", false, false, true, 5, 80, 2, "Derek", true, true, 50)).stream()
                .filter(a -> a.id().equals("hunt") || a.id().equals("sort")).allMatch(NpcActions.Act::enabled));
        // Hunt already on while it follows (set before the rule): it can still be turned off
        assertTrue(NpcActions.of(ofAMate("follow", true)).stream().filter(a -> a.id().equals("hunt")).findFirst().orElseThrow().enabled());
    }

    @Test
    void theFloatingBarKeepsFollowGuardAndRestOnScreenWhenItIsNarrow() {
        List<NpcActions.Act> bar = NpcActions.forBar(brother(true, true, "guard"));
        assertEquals(List.of("release", "sort", "hunt", "follow", "guard", "rest"), bar.stream().map(NpcActions.Act::id).toList());
        List<Integer> widths = bar.stream().map(a -> 40).toList();
        // a bar that holds the title, the "More" button and three pills: the last three are the orders
        net.ofatech.supernaturalbloodlines.shared.hub.kit.BarPlan plan = net.ofatech.supernaturalbloodlines.shared.hub.kit.BarPlan.of(
                new net.ofatech.supernaturalbloodlines.shared.hub.Rect(0, 0, 330, 34), widths, -1, 60);
        for (int i = 0; i < bar.size(); i++) {
            boolean isOrder = List.of("follow", "guard", "rest").contains(bar.get(i).id());
            if (isOrder) {
                assertTrue(plan.pills().get(i) != null, bar.get(i).id() + " went behind More");
            }
        }
        assertTrue(plan.overflow() != null && !plan.hidden().isEmpty(), "the bar was meant to be too narrow for all six");
        assertTrue(plan.hidden().stream().noneMatch(i -> List.of("follow", "guard", "rest").contains(bar.get(i).id())), "an order is behind More");
    }
}