package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupTextTest;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.ClanPlan.Button;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import org.junit.jupiter.api.Test;

/**
 * The clan cards in every state: what each rank sees, that every control runs a real row and has its words, that a disabled control says why, and that the
 * buttons fit side by side or wrap inside the page at gui scales 2 and 4 (the page is 3/4 of the width at 4). The three themes draw the same plan.
 */
class ClanCardsTest {
    private static ClanView.Charter charter(int state, boolean mine, String holder, boolean forgeable, int wait) {
        return new ClanView.Charter((byte) state, holder, mine, forgeable, wait);
    }

    private static ClanView view(ClanView.Charter c, ClanView.Contest k) {
        return new ClanView(true, 20, 4, c, k, true, 10, 2, 6);
    }

    private static final ClanView.Contest NO_CONTEST = new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 0, false, 0, false);

    private static List<ClanView> states() {
        List<ClanView> out = new ArrayList<>();
        for (boolean forgeable : new boolean[] {true, false}) {
            out.add(view(charter(ClanView.CHARTER_NONE, false, "", forgeable, 0), NO_CONTEST));
        }
        out.add(view(charter(ClanView.CHARTER_HELD, true, "Esther", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_HELD, false, "Esther", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_HELD, false, "", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_STORED, false, "", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_DROPPED, false, "", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_LOST, false, "", false, 3600), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_LOST, false, "", false, 0), NO_CONTEST));
        out.add(view(charter(ClanView.CHARTER_INERT, false, "", false, 0), NO_CONTEST));
        ClanView.Charter held = charter(ClanView.CHARTER_HELD, false, "Esther", false, 0);
        out.add(view(held, new ClanView.Contest(ClanView.CONTEST_PENDING, "Rebekah", 540, 0, false, 0, true)));
        out.add(view(held, new ClanView.Contest(ClanView.CONTEST_PENDING, "", 540, 0, false, 0, false)));
        out.add(view(held, new ClanView.Contest(ClanView.CONTEST_RUNNING, "Rebekah", 0, 6, false, 0, false)));
        out.add(view(held, new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 0, true, 0, false)));
        for (ContestRules.Refusal r : ContestRules.Refusal.values()) {
            out.add(view(held, new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 3, false, r.ordinal() + 1, false)));
        }
        return out;
    }

    private static final byte[] RANKS = {GroupView.LEADER, GroupView.DAY_WALKER, GroupView.MEMBER};

    @Test
    void everyStateOfEveryRankMakesAPlan() {
        int plans = 0;
        for (ClanView v : states()) {
            for (byte rank : RANKS) {
                ClanPlan.Plan p = ClanPlan.of(v, rank);
                assertFalse(p.lines().isEmpty());
                assertTrue(p.buttons().size() <= 2, "more than two buttons: " + p.buttons());
                plans++;
            }
        }
        assertTrue(plans > 60);
    }

    @Test
    void everyControlRunsARealRowAndHasItsWords() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        Set<String> rows = Set.of("C11", "C12", "C16", "C17");
        for (ClanView v : states()) {
            for (byte rank : RANKS) {
                for (Button b : ClanPlan.of(v, rank).buttons()) {
                    assertTrue(rows.contains(b.row()) && AdminCommands.byId(b.row()).isPresent(), b.row());
                    assertTrue(GroupButtons.IDS.containsValue(b.id()) && GroupButtons.LABELS.containsKey(b.id()), b.id());
                    assertTrue(lang.has(ClanPlan.KEY + b.label()) && lang.has(ClanPlan.KEY + b.tip()), b.label());
                    if (!b.enabled()) {
                        assertFalse(b.why().isEmpty(), "a disabled " + b.id() + " does not say why");
                        assertTrue(lang.has(ClanPlan.KEY + b.why()), b.why());
                    }
                }
            }
        }
    }

    @Test
    void everyLineHasItsWords() throws Exception {
        JsonObject lang = GroupTextTest.lang();
        for (ClanView v : states()) {
            for (byte rank : RANKS) {
                for (ClanPlan.Line l : ClanPlan.of(v, rank).lines()) {
                    assertTrue(lang.has(l.key()), l.key());
                }
            }
        }
    }

    @Test
    void whoSeesWhichButton() {
        ClanView none = view(charter(ClanView.CHARTER_NONE, false, "", true, 0), NO_CONTEST);
        assertEquals(List.of("clan:charter_forge"), ids(ClanPlan.of(none, GroupView.LEADER)));
        assertTrue(ids(ClanPlan.of(none, GroupView.DAY_WALKER)).isEmpty() && ids(ClanPlan.of(none, GroupView.MEMBER)).isEmpty());
        ClanView few = view(charter(ClanView.CHARTER_NONE, false, "", false, 0), NO_CONTEST);
        assertFalse(ClanPlan.of(few, GroupView.LEADER).buttons().get(0).enabled());
        ClanView lostWaiting = view(charter(ClanView.CHARTER_LOST, false, "", false, 600), NO_CONTEST);
        Button wait = ClanPlan.of(lostWaiting, GroupView.LEADER).buttons().get(0);
        assertTrue(wait.id().equals("clan:charter_reforge") && !wait.enabled());
        ClanView lostReady = view(charter(ClanView.CHARTER_LOST, false, "", false, 0), NO_CONTEST);
        assertTrue(ClanPlan.of(lostReady, GroupView.LEADER).buttons().get(0).enabled());
        ClanView.Charter held = charter(ClanView.CHARTER_HELD, false, "Esther", false, 0);
        ClanView asked = view(held, new ClanView.Contest(ClanView.CONTEST_PENDING, "R", 60, 0, false, 0, true));
        assertEquals(List.of("pack:duel_accept"), ids(ClanPlan.of(asked, GroupView.LEADER)));
        assertTrue(ids(ClanPlan.of(asked, GroupView.DAY_WALKER)).isEmpty(), "a member may not accept");
        ClanView may = view(held, new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 0, true, 0, false));
        assertEquals(List.of("pack:challenge"), ids(ClanPlan.of(may, GroupView.DAY_WALKER)));
        assertTrue(ClanPlan.of(may, GroupView.DAY_WALKER).buttons().get(0).enabled());
        assertTrue(ids(ClanPlan.of(may, GroupView.MEMBER)).isEmpty() && ids(ClanPlan.of(may, GroupView.LEADER)).isEmpty());
    }

    @Test
    void aChallengeDisabledForEachReasonSaysThatReason() {
        for (ContestRules.Refusal r : ContestRules.Refusal.values()) {
            ClanView v = view(charter(ClanView.CHARTER_HELD, false, "x", false, 0), new ClanView.Contest(ClanView.CONTEST_NONE, "", 0, 3, false, r.ordinal() + 1, false));
            Button b = ClanPlan.of(v, GroupView.DAY_WALKER).buttons().get(0);
            assertFalse(b.enabled());
            assertEquals("why." + r.id(), b.why());
        }
    }

    @Test
    void theAcceptCardHasNoDecline() {
        ClanView.Charter held = charter(ClanView.CHARTER_HELD, false, "x", false, 0);
        ClanView asked = view(held, new ClanView.Contest(ClanView.CONTEST_PENDING, "R", 60, 0, false, 0, true));
        for (Button b : ClanPlan.of(asked, GroupView.LEADER).buttons()) {
            assertFalse(b.id().contains("decline") || b.label().contains("decline"));
        }
    }

    @Test
    void theCountsNeverNameARootOrASpecies() {
        for (ClanView v : states()) {
            for (byte rank : RANKS) {
                for (ClanPlan.Line l : ClanPlan.of(v, rank).lines()) {
                    for (Object a : l.args()) {
                        String s = String.valueOf(a).toLowerCase();
                        assertFalse(s.contains("original") || s.contains("species") || s.length() > 40 && s.matches(".*[0-9a-f]{8}-[0-9a-f]{4}.*"), s);
                    }
                }
            }
        }
    }

    @Test
    void theClanCardsAreForAClanOnly() {
        for (GroupType type : GroupType.values()) {
            GroupView gv = new GroupView(type, true, GroupView.MEMBER, "x", 0, List.of());
            assertEquals(type == GroupType.CLAN, ClanCards.applies(gv), type.name());
        }
        assertFalse(ClanCards.applies(new GroupView(GroupType.CLAN, false, GroupView.NOT_IN, "", 0, List.of())));
    }

    /** At gui scale 2 the hub page is about 300 px wide, at scale 4 about 120: buttons sit side by side or wrap, never over each other or the edge. */
    @Test
    void buttonsFitAtGuiScaleTwoAndFour() {
        for (int width : new int[] {300, 120, 96}) {
            for (ClanView v : states()) {
                for (byte rank : RANKS) {
                    List<Integer> widths = new ArrayList<>();
                    for (Button b : ClanPlan.of(v, rank).buttons()) {
                        widths.add(Math.min(width, 6 * b.label().length() + 12));
                    }
                    if (widths.isEmpty()) {
                        continue;
                    }
                    Flow.Result flow = Flow.wrap(new Rect(0, 0, width, 0), widths, 14, 3);
                    Set<String> seen = new HashSet<>();
                    for (Rect r : flow.rects()) {
                        assertTrue(r.x() >= 0 && r.right() <= width, "outside the page at " + width + ": " + r);
                        seen.add(r.x() + "," + r.y());
                    }
                    assertEquals(flow.rects().size(), seen.size(), "two buttons on one spot at " + width);
                }
            }
        }
    }

    /** The flat page, and the book page that serves the Grimoire, Blood Moon and Tensura themes, each draw the clan's cards from the same plan. */
    @Test
    void everyThemeDrawsTheClanCards() throws Exception {
        java.nio.file.Path at = java.nio.file.Path.of("").toAbsolutePath();
        while (!java.nio.file.Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        java.nio.file.Path pages = at.resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page");
        for (String page : List.of("PackPage.java", "BookPackPage.java")) {
            String src = java.nio.file.Files.readString(pages.resolve(page));
            assertTrue(src.contains("ClanCards.render(f, r, y, gv"), page + " does not draw the clan cards");
        }
        String book = java.nio.file.Files.readString(pages.resolve("BookPackPage.java"));
        assertTrue(book.contains("Style.BOOK") && book.contains("Style.MOON") && book.contains("Style.TENSURA"), "the book page no longer serves the three themes");
    }

    private static List<String> ids(ClanPlan.Plan p) {
        return p.buttons().stream().map(Button::id).toList();
    }
}
