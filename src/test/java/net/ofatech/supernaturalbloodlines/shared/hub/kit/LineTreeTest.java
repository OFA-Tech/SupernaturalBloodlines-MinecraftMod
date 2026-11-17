package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Key.DOWN;
import static net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Key.LEFT;
import static net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Key.RIGHT;
import static net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Key.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules.State;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Kind;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Link;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.LinkStyle;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTree.Model;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.FreedKind;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.Kin;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.Origin;
import org.junit.jupiter.api.Test;

class LineTreeTest {
    private static Origin origin(State s, String root) {
        return new Origin(s, Optional.ofNullable(root), Optional.empty(), FreedKind.NONE, List.of());
    }

    private static Origin freed(String from, FreedKind kind) {
        return new Origin(State.FREE, Optional.empty(), Optional.ofNullable(from), kind, List.of());
    }

    private static List<String> ids(Model m) {
        return m.nodes().stream().map(LineTree.Node::id).toList();
    }

    private static List<Kin> kids(int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> new Kin("K" + i, 0)).toList();
    }

    private static Model inLine(int sired) {
        return LineTree.build(origin(State.IN_LINE, "Klaus"), "Me", Optional.of("S"), false, List.of("C", "B", "A"), kids(sired));
    }

    @Test
    void originalIsTheCrown() {
        Model m = LineTree.build(origin(State.ORIGINAL, null), "Me", Optional.of("ignored"), false, List.of("ignored"), kids(2));
        assertEquals(List.of("line:you", "line:down:0", "line:down:1"), ids(m));
        assertFalse(m.padlock());
        assertTrue(m.links().stream().allMatch(l -> l.style() == LinkStyle.SOLID));
    }

    @Test
    void humansAndTheLostHaveNoRoot() {
        Model human = LineTree.build(origin(State.NOT_VAMPIRE, null), "Me", Optional.empty(), false, List.of(), List.of());
        assertEquals(List.of("line:you"), ids(human));
        assertTrue(human.links().isEmpty());
        // a vampire nobody recorded the sire of: a "?" above them
        Model lost = LineTree.build(origin(State.SIRE_UNKNOWN, null), "Me", Optional.empty(), false, List.of(), List.of());
        assertEquals(List.of("line:sire", "line:you"), ids(lost));
        assertTrue(lost.node("line:sire").orElseThrow().hidden());
        assertEquals(List.of("line:sire", "line:you"),
                ids(LineTree.build(origin(State.SIRE_UNKNOWN, null), "Me", Optional.of("S"), false, List.of(), List.of())));
    }

    @Test
    void inLineHasTheChainAndFaintLinks() {
        Model m = inLine(2);
        assertEquals(List.of("line:root", "line:anc:0", "line:anc:1", "line:sire", "line:you", "line:down:0", "line:down:1"), ids(m));
        // the nearest two ancestors, the farthest of them first
        assertEquals("B", m.node("line:anc:0").orElseThrow().name());
        assertEquals("C", m.node("line:anc:1").orElseThrow().name());
        assertFalse(m.padlock());
        for (String down : List.of("line:down:0", "line:down:1")) {
            assertTrue(m.links().contains(new Link("line:root", down, LinkStyle.FAINT)), "no faint thread from " + down);
            assertTrue(m.links().contains(new Link("line:you", down, LinkStyle.SOLID)));
        }
        assertTrue(m.links().contains(new Link("line:sire", "line:you", LinkStyle.SOLID)));
        assertTrue(m.links().stream().filter(l -> l.style() != LinkStyle.FAINT).noneMatch(l -> l.style() == LinkStyle.BROKEN));
    }

    @Test
    void hiddenRootIsAQuestionMark() {
        Model m = LineTree.build(origin(State.IN_LINE, null), "Me", Optional.of("S"), false, List.of(), List.of());
        assertTrue(m.node("line:root").orElseThrow().hidden());
        assertEquals("", m.node("line:root").orElseThrow().name());
    }

    @Test
    void npcAncestorsAreHiddenAndPlayerAncestorsAreNot() {
        Origin o = new Origin(State.IN_LINE, Optional.of("Klaus"), Optional.empty(), FreedKind.NONE, List.of(true, false));
        // nearest first: C is an NPC, B is a player
        Model m = LineTree.build(o, "Me", Optional.of("S"), false, List.of("", "B"), List.of());
        assertTrue(m.node("line:anc:1").orElseThrow().hidden(), "the nearest ancestor is the NPC");
        assertFalse(m.node("line:anc:0").orElseThrow().hidden());
        assertEquals("B", m.node("line:anc:0").orElseThrow().name());
    }

    @Test
    void freeBreaksTheLinkAboveYouAndShowsThePadlock() {
        Model m = LineTree.build(freed("Klaus", FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), kids(1));
        assertTrue(m.padlock());
        assertTrue(m.links().contains(new Link("line:sire", "line:you", LinkStyle.BROKEN)));
        assertEquals("Klaus", m.node("line:root").orElseThrow().name());
        assertTrue(m.links().contains(new Link("line:root", "line:sire", LinkStyle.SOLID)));
    }

    @Test
    void freeByBrokenLineKeepsFaintLinksAndFreeByImprisonmentDoesNot() {
        Model broken = LineTree.build(freed("Klaus", FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), kids(2));
        assertEquals(2, broken.links().stream().filter(l -> l.style() == LinkStyle.FAINT).count());
        Model imprisoned = LineTree.build(freed("Klaus", FreedKind.IMPRISONED), "Me", Optional.of("Tyler"), false, List.of(), kids(2));
        assertEquals(0, imprisoned.links().stream().filter(l -> l.style() == LinkStyle.FAINT).count());
        assertTrue(imprisoned.links().contains(new Link("line:you", "line:down:0", LinkStyle.SOLID)));
        assertTrue(imprisoned.padlock());
    }

    @Test
    void freeWithNoSireAndAHiddenRootHasNoRootNode() {
        Model m = LineTree.build(freed(null, FreedKind.BROKEN_LINE), "Me", Optional.empty(), false, List.of(), List.of());
        assertEquals(List.of("line:you"), ids(m));
        assertTrue(m.padlock());
        // with a sire the root is a ghost above, drawn as "?"
        Model withSire = LineTree.build(freed(null, FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), List.of());
        assertTrue(withSire.node("line:root").orElseThrow().hidden());
    }

    @Test
    void severedSireLinkIsBroken() {
        Model m = LineTree.build(origin(State.IN_LINE, "Klaus"), "Me", Optional.of("S"), true, List.of(), List.of());
        assertTrue(m.links().contains(new Link("line:sire", "line:you", LinkStyle.BROKEN)));
        assertFalse(m.padlock());
    }

    @Test
    void moreThanThreeBelowCountsTheRest() {
        Model m = inLine(5);
        assertEquals(3, m.nodes().stream().filter(n -> n.kind() == Kind.DESCENDANT).count());
        assertEquals(2, m.hiddenBelow());
    }

    @Test
    void theSnapshotsDepthStartsAtOneAndTheDirectOnesAreTheShallowest() {
        List<Kin> below = List.of(new Kin("A", 1), new Kin("A1", 2), new Kin("B", 1));
        Model m = LineTree.build(origin(State.IN_LINE, "Klaus"), "Me", Optional.of("S"), false, List.of(), below);
        assertEquals(List.of("A", "B"), m.nodes().stream().filter(n -> n.kind() == Kind.DESCENDANT).map(LineTree.Node::name).toList());
        assertEquals(1, m.hiddenBelow(), "the grandchild is counted, not drawn");
    }

    @Test
    void arrowKeysWalkTheTree() {
        Model m = inLine(2);
        assertEquals(Optional.of("line:you"), LineTree.move(m, "line:down:1", UP));
        assertEquals(Optional.of("line:sire"), LineTree.move(m, "line:you", UP));
        assertEquals(Optional.of("line:anc:1"), LineTree.move(m, "line:sire", UP));
        assertEquals(Optional.empty(), LineTree.move(m, "line:root", UP));
        assertEquals(Optional.of("line:down:0"), LineTree.move(m, "line:you", DOWN));
        assertEquals(Optional.of("line:you"), LineTree.move(m, "line:sire", DOWN));
        assertEquals(Optional.of("line:down:1"), LineTree.move(m, "line:down:0", RIGHT));
        assertEquals(Optional.empty(), LineTree.move(m, "line:down:1", RIGHT));
        assertEquals(Optional.empty(), LineTree.move(m, "line:down:0", LEFT));
        assertEquals(Optional.of("line:down:0"), LineTree.move(m, "line:down:1", LEFT));
        assertEquals(Optional.empty(), LineTree.move(m, "line:down:0", DOWN));
        assertEquals(Optional.empty(), LineTree.move(m, "line:you", LEFT));
        assertEquals(Optional.empty(), LineTree.move(m, "line:nowhere", UP));
    }

    @Test
    void narrationNamesTheBreak() {
        Model m = LineTree.build(freed("npc:klaus", FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), List.of());
        assertEquals(new LineTree.Narr("supernaturalbloodlines.hub.bloodline.narr.sire_free", List.of("Tyler", "npc:klaus")),
                LineTree.narrate(m, m.node("line:sire").orElseThrow()));
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.you_free", LineTree.narrate(m, m.node("line:you").orElseThrow()).key());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.root", LineTree.narrate(m, m.node("line:root").orElseThrow()).key());
    }

    @Test
    void narrationNeverNamesAHiddenRoot() {
        Model m = LineTree.build(origin(State.IN_LINE, null), "Me", Optional.of("S"), false, List.of(), kids(1));
        for (LineTree.Node n : m.nodes()) {
            LineTree.Narr narr = LineTree.narrate(m, n);
            assertTrue(narr.args().stream().noneMatch(a -> a.contains("Klaus")), narr.toString());
        }
        LineTree.Narr root = LineTree.narrate(m, m.node("line:root").orElseThrow());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.root_hidden", root.key());
        assertEquals(List.of("?", "?"), root.args());
        assertEquals(List.of("S", "?"), LineTree.narrate(m, m.node("line:sire").orElseThrow()).args());
    }

    @Test
    void narrationPicksTheKeyOfTheKind() {
        Model crown = LineTree.build(origin(State.ORIGINAL, null), "Me", Optional.empty(), false, List.of(), kids(1));
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.you_original", LineTree.narrate(crown, crown.node("line:you").orElseThrow()).key());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.descendant", LineTree.narrate(crown, crown.node("line:down:0").orElseThrow()).key());
        Model m = inLine(0);
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.you", LineTree.narrate(m, m.node("line:you").orElseThrow()).key());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.sire", LineTree.narrate(m, m.node("line:sire").orElseThrow()).key());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.ancestor", LineTree.narrate(m, m.node("line:anc:0").orElseThrow()).key());
    }

    @Test
    void everyNodeIdIsUniqueAndHasAZone() {
        for (Model m : List.of(inLine(5), LineTree.build(freed("Klaus", FreedKind.IMPRISONED), "Me", Optional.of("S"), false, List.of("B", "A"), kids(3)))) {
            assertEquals(m.nodes().size(), new HashSet<>(ids(m)).size());
            for (String id : ids(m)) {
                assertEquals(FocusOrder.Zone.STAGE, FocusOrder.zoneOf(id), id);
            }
        }
    }

    @Test
    void narrationOfAFreePlayerWhoDoesNotKnowTheOriginalNeverNamesOne() {
        Model m = LineTree.build(freed(null, FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), List.of());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.sire_free_unknown", LineTree.narrate(m, m.node("line:sire").orElseThrow()).key());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.you_free_unknown", LineTree.narrate(m, m.node("line:you").orElseThrow()).key());
        // and the one who knows hears the named words
        Model known = LineTree.build(freed("Klaus", FreedKind.BROKEN_LINE), "Me", Optional.of("Tyler"), false, List.of(), List.of());
        assertEquals("supernaturalbloodlines.hub.bloodline.narr.you_free", LineTree.narrate(known, known.node("line:you").orElseThrow()).key());
    }
}
