package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanTime;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import org.junit.jupiter.api.Test;

/** Round 2: the clan's waits are said in a unit, and a refusal says how long it lasts. */
class ClanWordingTest {
    @Test
    void aSpanSaysItsUnit() {
        assertEquals("45 s", ClanTime.span(45_000));
        assertEquals("1 s", ClanTime.span(1));
        assertEquals("0 s", ClanTime.span(-5));
        assertEquals("12 min", ClanTime.span(12 * 60_000L));
        assertEquals("1 h", ClanTime.span(ClanTime.HOUR));
        assertEquals("24 h", ClanTime.span(ClanTime.HOUR * 23 + ClanTime.HOUR / 2));
        assertEquals("2 h", ClanTime.span(ClanTime.HOUR + 1));
    }

    private static ClanView view(int waitDays, int why) {
        return new ClanView(true, 3, -1, new ClanView.Charter(ClanView.CHARTER_HELD, "Rowan", false, false, 0),
                new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, waitDays, false, why, false), true, 10, 0);
    }

    private static boolean has(ClanPlan.Plan plan, String key, Object... args) {
        return plan.lines().stream().anyMatch(l -> l.key().equals(ClanPlan.KEY + key) && l.args().equals(java.util.List.of(args)));
    }

    @Test
    void aNewDayWalkerIsToldHowManyDaysUntilTheyMayChallenge() {
        int why = ContestRules.Refusal.wire(Optional.of(ContestRules.Refusal.TOO_NEW));
        assertTrue(has(ClanPlan.of(view(5, why), GroupView.DAY_WALKER), "contest.too_new", 5), "days left not said");
        assertTrue(has(ClanPlan.of(view(1, why), GroupView.DAY_WALKER), "contest.too_new.one", 1), "one day not said as one day");
        assertTrue(!has(ClanPlan.of(view(5, why), GroupView.DAY_WALKER), "contest.cooldown", 5), "a new member was told of a contest cooldown");
    }

    @Test
    void aCooldownStillSaysItsOwnLine() {
        int why = ContestRules.Refusal.wire(Optional.of(ContestRules.Refusal.COOLDOWN));
        assertTrue(has(ClanPlan.of(view(2, why), GroupView.DAY_WALKER), "contest.cooldown", 2));
    }

    @Test
    void theReforgeWaitSaysItsUnit() {
        ClanView lost = new ClanView(true, 3, -1, new ClanView.Charter(ClanView.CHARTER_LOST, "", false, false, 45), ClanView.Contest.NONE, true, 10, 0);
        assertTrue(has(ClanPlan.of(lost, GroupView.LEADER), "charter.reforge_wait", "45 s"));
        ClanView later = new ClanView(true, 3, -1, new ClanView.Charter(ClanView.CHARTER_LOST, "", false, false, 12 * 60), ClanView.Contest.NONE, true, 10, 0);
        assertTrue(has(ClanPlan.of(later, GroupView.LEADER), "charter.reforge_wait", "12 min"));
        ClanView hours = new ClanView(true, 3, -1, new ClanView.Charter(ClanView.CHARTER_LOST, "", false, false, 5 * 3600), ClanView.Contest.NONE, true, 10, 0);
        assertTrue(has(ClanPlan.of(hours, GroupView.LEADER), "charter.reforge_wait", "5 h"));
    }
}
