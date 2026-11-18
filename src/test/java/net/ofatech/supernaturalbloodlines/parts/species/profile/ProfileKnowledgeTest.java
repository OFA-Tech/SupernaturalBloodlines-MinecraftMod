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

