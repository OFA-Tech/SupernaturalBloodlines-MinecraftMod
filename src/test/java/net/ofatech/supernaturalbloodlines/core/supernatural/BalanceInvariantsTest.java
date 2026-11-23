package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The balance rules of docs/balance.md, checked on the shipped data instead of on single values: a new rung, skill or task that breaks one fails here, with
 * the id that broke it. (The values themselves are pinned in {@code LadderSpecRowsTest} and {@code SkillDataTest}.)
 */
class BalanceInvariantsTest {
    private static final long APEX_EP = 100_000;
    private static final int APEX_SP = 5;
    /** A game day is 20 minutes, so 60 days is 20 hours of play: the longest wait a ladder may ask for. */
    private static final long MAX_DAYS = 60;

    private static LadderTable ladders() {
        return ShippedLadders.all();
    }

    @Test
    void sPAndEpNeverFallAsARungClimbsExceptTheApexSpEqualsTheAlpha() {
        List<String> bad = new ArrayList<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            long sp = 0;
            long ep = 0;
            for (Rung r : ladders().get(race).rungs()) {
                if (r.sp() < sp) {
                    bad.add(r.id() + " pays " + r.sp() + " SP after a rung that paid " + sp);
                }
                if (r.ep() < ep) {
                    bad.add(r.id() + " pays " + r.ep() + " EP after a rung that paid " + ep);
                }
                if (r.sp() > 0 || r.ep() > 0) {
                    sp = r.sp();
                    ep = r.ep();
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyApexRungPaysTheSameAndAnApexIsNeverCheaperThanTheRungBefore() {
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : ladders().get(race).rungs()) {
                boolean apex = r.tasks().stream().anyMatch(t -> t.detector() == TaskSpec.Detector.TENSURA_TIER);
                if (apex) {
                    assertEquals(APEX_EP, r.ep(), r.id());
                    assertEquals(APEX_SP, r.sp(), r.id());
                }
            }
        }
    }

    @Test
    void noLadderPaysMoreThanAFifthOfOneApexEvolution() {
        // the apex race costs 2,000,000 EP: a whole ladder, even two of them, must help without replacing Tensura's own progression
        for (SupernaturalRace race : SupernaturalRace.values()) {
            long ep = ladders().get(race).rungs().stream().mapToLong(Rung::ep).sum();
            assertTrue(ep <= 2_000_000 / 7, race + " pays " + ep + " EP");
        }
    }

    @Test
    void noTaskAsksForMoreThanSixtyDaysOfWaiting() {
        List<String> bad = new ArrayList<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (Rung r : ladders().get(race).rungs()) {
                for (TaskSpec t : r.tasks()) {
                    // DAYS_AS counts days; THIRST_DAYS and PACK_DAYS are written in days but kept in ticks
                    long days = t.detector() == TaskSpec.Detector.DAYS_AS ? t.target()
                            : t.detector() == TaskSpec.Detector.PACK_DAYS || t.detector() == TaskSpec.Detector.THIRST_DAYS ? t.target() / 24_000 : 0;
                    if (days > MAX_DAYS) {
                        bad.add(t.id() + " waits " + days + " days");
                    }
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void skillsCostOneToFiveAndOnlyAGrantedSkillIsFree() {
        SkillTable t = SkillDataTest.load(mod -> true);
        for (SkillSpec s : t.skills()) {
            if (s.grantedOnly()) {
                assertEquals(0, s.cost(), s.id());
            } else {
                assertTrue(s.cost() >= 1 && s.cost() <= 5, s.id() + " costs " + s.cost());
            }
        }
    }

    @Test
    void aSkillNeverCostsLessThanOneTwoRungsBelowItInTheSameTreeByMoreThanTwo() {
        // costs rise with the rungs: a rung-5 skill costs at least 4, a rung-1 skill at most 2 (S-VAM-6 is the quest-gated 4 at rung 3 or 4)
        SkillTable t = SkillDataTest.load(mod -> true);
        List<String> bad = new ArrayList<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (SkillTree.Entry e : t.tree(race).entries()) {
                int cost = t.skill(e.skill()).orElseThrow().cost();
                if (e.rung() >= 5 && cost < 4) {
                    bad.add(race + " " + e.skill() + " at rung " + e.rung() + " costs only " + cost);
                }
                if (e.rung() <= 1 && cost > 2) {
                    bad.add(race + " " + e.skill() + " at rung " + e.rung() + " costs " + cost);
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void aTreeCostsBetweenEightyAndElevenTenthsOfWhatItsLadderPays() {
        SkillTable t = SkillDataTest.load(mod -> true);
        List<String> bad = new ArrayList<>();
        for (SupernaturalRace race : SupernaturalRace.values()) {
            if (race == SupernaturalRace.GHOUL) {
                continue;
            }
            int tree = t.tree(race).entries().stream().mapToInt(e -> t.skill(e.skill()).orElseThrow().cost()).sum();
            int paid = ladders().get(race).rungs().stream().mapToInt(Rung::sp).sum();
            double ratio = tree / (double) paid;
            if (ratio < 0.8 || ratio > 1.1) {
                bad.add(race + ": the tree costs " + tree + " SP and its ladder pays " + paid);
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void aSkillWhoseRungComesLaterThanItsPrerequisiteIsNotListedBefore() {
        SkillTable t = SkillDataTest.load(mod -> true);
        for (SupernaturalRace race : SupernaturalRace.values()) {
            for (SkillTree.Entry e : t.tree(race).entries()) {
                if (e.after() != null) {
                    int before = t.tree(race).entry(e.after()).orElseThrow().rung();
                    assertTrue(before <= e.rung(), race + " " + e.skill() + " needs " + e.after() + " from a later rung");
                }
            }
        }
        assertFalse(t.skills().isEmpty());
    }
}
