package net.ofatech.supernaturalbloodlines.parts.species.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.KnownFacts;
import org.junit.jupiter.api.Test;

class ProfileKnowledgeTest {
    private static final UUID ME = UUID.randomUUID();
    private static final UUID THEM = UUID.randomUUID();

    @Test
    void askingForAProfileNeverCreatesAnEntryOrPushesARealObserverOut() {
        ProfileKnowledge k = new ProfileKnowledge();
        UUID real = new UUID(7, 7);
        UUID subject = new UUID(8, 8);
        k.discover(null, real, subject, Set.of(Fact.GENES));
        int evictedBefore = k.evictedOwners();
        for (int i = 0; i < ProfileKnowledge.MAX_OWNERS * 2; i++) {
            assertTrue(k.known(null, new UUID(1, i), subject).isEmpty());
        }
        assertEquals(1, k.observerCount(), "reads created entries");
        assertEquals(evictedBefore, k.evictedOwners());
        assertEquals(0, new ProfileKnowledge().evictedOwners(), "the counter belongs to one index, not to the server run");
        assertEquals(EnumSet.of(Fact.GENES), k.known(null, real, subject), "the real observer was pushed out by strangers");
    }

    private static final UUID LEAD = new UUID(5, 1);
    private static final UUID MATE = new UUID(5, 2);
    private static final UUID OTHER = new UUID(5, 3);
    private static final UUID SUBJECT = new UUID(5, 9);

    /** Leader LEAD with members MATE and OTHER; OTHER discovers the line of SUBJECT, MATE discovers the genes of SUBJECT on their own account. */
    private static PackIndex pack(ProfileKnowledge k) {
        PackIndex packs = new PackIndex();
        assertTrue(packs.join(LEAD, MATE));
        assertTrue(packs.join(LEAD, OTHER));
        k.discover(packs, OTHER, SUBJECT, EnumSet.of(Fact.LINE));
        k.discover(packs, MATE, SUBJECT, EnumSet.of(Fact.GENES));
        return packs;
    }

    @Test
    void aPacksDiscoveriesAreReadByEveryMemberWhileTheyAreIn() {
        ProfileKnowledge k = new ProfileKnowledge();
        PackIndex packs = pack(k);
        assertEquals(EnumSet.of(Fact.LINE, Fact.GENES), k.known(packs, MATE, SUBJECT));
        assertEquals(EnumSet.of(Fact.LINE, Fact.GENES), k.known(packs, LEAD, SUBJECT));
    }

    @Test
    void aMemberWhoLeavesKeepsOnlyWhatTheyDiscoveredThemselves() {
        ProfileKnowledge k = new ProfileKnowledge();
        PackIndex packs = pack(k);
        k.known(packs, MATE, SUBJECT); // reading while in the pack must not copy the pool into their own
        packs.leaveVoluntarily(MATE);
        assertEquals(EnumSet.of(Fact.GENES), k.known(packs, MATE, SUBJECT), "the pack's pool still counted after leaving");
    }

    @Test
    void aKickedMemberLosesThePoolToo() {
        ProfileKnowledge k = new ProfileKnowledge();
        PackIndex packs = pack(k);
        k.known(packs, OTHER, SUBJECT);
        assertTrue(packs.kick(OTHER));
        assertEquals(EnumSet.of(Fact.LINE), k.known(packs, OTHER, SUBJECT), "a kicked member kept what the others found (only their own LINE stays)");
        assertEquals(EnumSet.of(Fact.LINE, Fact.GENES), k.known(packs, MATE, SUBJECT), "the rest of the pack lost something");
    }

    @Test
    void whenThePackDissolvesNobodyKeepsThePoolButTheirOwn() {
        ProfileKnowledge k = new ProfileKnowledge();
        PackIndex packs = pack(k);
        k.known(packs, MATE, SUBJECT);
        packs.dissolve(LEAD);
        k.dropPool(LEAD);
        assertEquals(EnumSet.of(Fact.GENES), k.known(packs, MATE, SUBJECT));
        assertEquals(EnumSet.of(Fact.LINE), k.known(packs, OTHER, SUBJECT));
        assertTrue(k.known(packs, LEAD, SUBJECT).isEmpty(), "the leader never discovered anything themselves");
    }

    @Test
    void aMemberWhoJoinsLaterReadsThePoolFromTheDayTheyJoin() {
        ProfileKnowledge k = new ProfileKnowledge();
        PackIndex packs = pack(k);
        UUID late = new UUID(5, 4);
        assertTrue(k.known(packs, late, SUBJECT).isEmpty());
        assertTrue(packs.join(LEAD, late));
        assertEquals(EnumSet.of(Fact.LINE, Fact.GENES), k.known(packs, late, SUBJECT));
    }

    @Test
    void whatIsDiscoveredIsSavedAndLoadedBack() {
        ProfileKnowledge k = new ProfileKnowledge();
        k.discover(null, ME, THEM, EnumSet.of(Fact.GENES, Fact.LINE));
        ProfileKnowledge back = ProfileKnowledge.load(k.save(new CompoundTag(), null), null);
        assertEquals(EnumSet.of(Fact.GENES, Fact.LINE), back.known(null, ME, THEM));
        assertTrue(back.known(null, UUID.randomUUID(), THEM).isEmpty(), "somebody else learned it too");
    }

    @Test
    void aDamagedSaveLoadsWhatItCanAndNeverThrows() {
        CompoundTag tag = new CompoundTag();
        ListTag observers = new ListTag();
        CompoundTag noId = new CompoundTag();
        observers.add(noId);
        CompoundTag good = new CompoundTag();
        good.putString("owner", ME.toString());
        ListTag known = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("subject", THEM.toString());
        ListTag facts = new ListTag();
        facts.add(StringTag.valueOf("GENES"));
        facts.add(StringTag.valueOf("A_FACT_FROM_THE_FUTURE"));
        entry.put("facts", facts);
        known.add(entry);
        CompoundTag badSubject = new CompoundTag();
        badSubject.putString("subject", "nope");
        known.add(badSubject);
        good.put("known", known);
        observers.add(good);
        tag.put("observers", observers);
        tag.putString("pools", "not a list");
        ListTag ints = new ListTag();
        ints.add(IntTag.valueOf(7));
        tag.put("other", ints);
        ProfileKnowledge k = ProfileKnowledge.load(tag, null);
        assertEquals(EnumSet.of(Fact.GENES), k.known(null, ME, THEM));
    }

    @Test
    void aSaveWithMoreThanTheCapKeepsTheNewestAndStaysUsable() {
        CompoundTag tag = new CompoundTag();
        ListTag subjects = new ListTag();
        UUID newest = UUID.randomUUID();
        for (int i = 0; i < KnownFacts.MAX_SUBJECTS + 40; i++) {
            subjects.add(subject(i == KnownFacts.MAX_SUBJECTS + 39 ? newest : UUID.randomUUID()));
        }
        CompoundTag owner = new CompoundTag();
        owner.putString("owner", ME.toString());
        owner.put("known", subjects);
        ListTag observers = new ListTag();
        observers.add(owner);
        tag.put("observers", observers);
        ProfileKnowledge k = ProfileKnowledge.load(tag, null);
        Set<Fact> got = k.known(null, ME, newest);
        assertEquals(EnumSet.of(Fact.LINE), got);
        ProfileKnowledge back = ProfileKnowledge.load(k.save(new CompoundTag(), null), null);
        assertEquals(EnumSet.of(Fact.LINE), back.known(null, ME, newest));
    }

    private static CompoundTag subject(UUID id) {
        CompoundTag s = new CompoundTag();
        s.putString("subject", id.toString());
        ListTag f = new ListTag();
        f.add(StringTag.valueOf("LINE"));
        s.put("facts", f);
        return s;
    }

    @Test
    void ownersAreCappedToo() {
        ProfileKnowledge k = new ProfileKnowledge();
        UUID first = UUID.randomUUID();
        k.discover(null, first, THEM, EnumSet.of(Fact.GENES));
        for (int i = 0; i < ProfileKnowledge.MAX_OWNERS; i++) {
            k.discover(null, UUID.randomUUID(), THEM, EnumSet.of(Fact.GENES));
        }
        assertTrue(k.known(null, first, THEM).isEmpty(), "the oldest owner was kept past the cap");
    }
}

