package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileViewTest {
    private static final Set<Fact> NOTHING = EnumSet.noneOf(Fact.class);

    /** Answers every fact and remembers which it was asked for. */
    private static final class Reader implements java.util.function.Function<Fact, Optional<List<String>>> {
        final List<Fact> asked = new ArrayList<>();

        @Override
        public Optional<List<String>> apply(Fact f) {
            asked.add(f);
            return Optional.of(List.of(f.name().toLowerCase()));
        }
    }

    @Test
    void aDiscoveredFactIsShownWhateverTheRelationsLetterSays() {
        // the table says "?" for an ally's level genes; a discovery (a pooled one, an ability) still shows it
        assertEquals(EnumSet.of(Fact.NAME, Fact.SPECIES, Fact.RANK, Fact.PACK, Fact.PACK_DAYS, Fact.PRESENCE, Fact.GENES), ProfileView.shown(Relation.ALLY_LEADER, EnumSet.of(Fact.GENES)));
        assertEquals(EnumSet.of(Fact.GENES), ProfileView.shown(Relation.STRANGER, EnumSet.of(Fact.GENES)));
        assertFalse(ProfileView.shown(Relation.ALLY_LEADER, NOTHING).contains(Fact.GENES));
    }

    @Test
    void theNpcFlagIsSentOnlyWithFactsSoAnUndiscoveredOriginalLooksLikeAStranger() {
        assertFalse(ProfileView.npc(Relation.NPC_ANCESTOR, Map.of()), "the flag alone names the Original");
        assertTrue(ProfileView.npc(Relation.NPC_ANCESTOR, Map.of(Fact.NAME, List.of("x"))));
        assertTrue(ProfileView.npc(Relation.NPC_CREATURE, Map.of(Fact.NAME, List.of("x"))));
        assertFalse(ProfileView.npc(Relation.STRANGER, Map.of(Fact.NAME, List.of("x"))));
    }

    @Test
    void aStrangersProfileHasNoFieldsAndNothingWasEvenRead() {
        Reader r = new Reader();
        assertTrue(ProfileView.build(Relation.STRANGER, NOTHING, r).isEmpty());
        assertTrue(r.asked.isEmpty(), "the server looked up " + r.asked + " for a stranger");
        assertTrue(ProfileView.build(Relation.STRANGER_OFFLINE, NOTHING, r).isEmpty());
        assertTrue(r.asked.isEmpty());
    }

    @Test
    void aMemberOfTheSamePackSeesAllButTheUndiscoveredSecrets() {
        Reader r = new Reader();
        Map<Fact, List<String>> seen = ProfileView.build(Relation.SAME_PACK, NOTHING, r);
        EnumSet<Fact> expected = EnumSet.allOf(Fact.class);
        expected.removeAll(EnumSet.of(Fact.LINE, Fact.GENES));
        assertEquals(expected, seen.keySet());
        assertFalse(r.asked.contains(Fact.GENES) || r.asked.contains(Fact.LINE), "a secret nobody discovered was read");
    }

    @Test
    void aDiscoveredSecretIsSentAndOnlyThatOne() {
        Map<Fact, List<String>> seen = ProfileView.build(Relation.SAME_PACK, EnumSet.of(Fact.GENES), new Reader());
        assertTrue(seen.containsKey(Fact.GENES));
        assertFalse(seen.containsKey(Fact.LINE));
    }

    @Test
    void anAbilityCanTeachAStrangerOneFactAndNothingElse() {
        Map<Fact, List<String>> seen = ProfileView.build(Relation.STRANGER, Knowledge.learnedBy(Knowledge.Ability.MEMORY_CLAWS), new Reader());
        assertEquals(EnumSet.of(Fact.SIRE, Fact.GENES), seen.keySet());
    }

    @Test
    void aFactTheServerHasNoValueForIsLeftOutNotSentEmpty() {
        Map<Fact, List<String>> seen = ProfileView.build(Relation.SAME_PACK, NOTHING, f -> f == Fact.LEVEL ? Optional.empty() : Optional.of(List.of("x")));
        assertFalse(seen.containsKey(Fact.LEVEL), "an offline subject's level must read as unknown");
    }

    @Test
    void anExplicitNoneIsKept() {
        Map<Fact, List<String>> seen = ProfileView.build(Relation.SAME_PACK, NOTHING, f -> Optional.of(f == Fact.SIRE ? List.of() : List.of("x")));
        assertEquals(List.of(), seen.get(Fact.SIRE));
    }

    @Test
    void theOpViewShowsEverything() {
        assertEquals(EnumSet.allOf(Fact.class), ProfileView.build(Relation.OP_VIEW, NOTHING, new Reader()).keySet());
    }

    @Test
    void aSireSeesTheSameLineWithoutTheRootAndNoGenes() {
        Set<Fact> shown = ProfileView.shown(Relation.DIRECT_SIRE, NOTHING);
        assertTrue(shown.contains(Fact.LINE) && shown.contains(Fact.SIRE) && shown.contains(Fact.LEVEL));
        assertFalse(shown.contains(Fact.GENES) || shown.contains(Fact.RANK) || shown.contains(Fact.PACK), "pack is only known once discovered");
    }
}

