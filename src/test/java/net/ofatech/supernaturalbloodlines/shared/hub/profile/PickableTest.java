package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.Pickable.Person;
import org.junit.jupiter.api.Test;

class PickableTest {
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

