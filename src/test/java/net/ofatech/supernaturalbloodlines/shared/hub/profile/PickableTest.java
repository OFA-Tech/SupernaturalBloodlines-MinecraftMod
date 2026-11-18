package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.Pickable.Person;
import org.junit.jupiter.api.Test;

class PickableTest {
    @Test
    void whatTheCapLeavesOutIsCountedAndWhoLeftIsDropped() {
        List<Person> online = new java.util.ArrayList<>();
        for (int i = 0; i < 45; i++) {
            online.add(new Person(new UUID(9, i), "P" + i, i));
        }
        UUID me = new UUID(9, 0);
        assertEquals(14, Pickable.beyondCap(online, me, Set.of()), "44 eligible, 30 shown");
        assertEquals(0, Pickable.beyondCap(online.subList(0, 20), me, Set.of()));
        List<Person> offered = Pickable.of(online, me, Set.of());
        Set<UUID> left = new java.util.HashSet<>(offered.stream().map(Person::id).toList());
        left.remove(offered.get(0).id());
        assertEquals(29, Pickable.stillHere(offered, left::contains).size());
        assertEquals(false, Pickable.stillHere(offered, left::contains).contains(offered.get(0)));
    }

    @Test
    void aBigServerIsCappedToTheNearestThirty() {
        List<Person> online = new java.util.ArrayList<>();
        for (int i = 0; i < 200; i++) {
            online.add(new Person(new UUID(9, i), "P" + i, 200 - i));
        }
        List<Person> picked = Pickable.of(online, new UUID(0, 0), Set.of());
        assertEquals(Pickable.MAX, picked.size());
        assertEquals("P199", picked.get(0).name(), "the nearest first");
        assertEquals("P170", picked.get(29).name());
    }

    @Test
    void youAndTheExcludedAreLeftOutAndTheNearestComeFirst() {
        UUID me = UUID.randomUUID();
        UUID in = UUID.randomUUID();
        List<Person> online = List.of(new Person(me, "Me", 0), new Person(in, "Inside", 3), new Person(UUID.randomUUID(), "Zed", -1),
                new Person(UUID.randomUUID(), "Far", 40), new Person(UUID.randomUUID(), "Near", 5), new Person(UUID.randomUUID(), "Abe", -1));
        assertEquals(List.of("Near", "Far", "Abe", "Zed"), Pickable.of(online, me, Set.of(in)).stream().map(Person::name).toList());
        assertEquals(List.of("Inside", "Near", "Far", "Abe", "Zed"), Pickable.of(online, me, Set.of()).stream().map(Person::name).toList());
    }
}

