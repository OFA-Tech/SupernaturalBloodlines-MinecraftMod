package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.Member;
import org.junit.jupiter.api.Test;

/** The Home page enables Moonblood Surge only when the server would count someone else in the pack (R146). */
class HubSocialSurgeMatesTest {
    private static final UUID ME = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static Member m(String name, String rank, UUID id) {
        return new Member(name, rank, true, Optional.ofNullable(id));
    }

    @Test
    void aloneThereIsNobody() {
        assertEquals(0, HubSocial.surgeMates(List.of(m("Me", "alpha", ME)), ME));
        assertEquals(0, HubSocial.surgeMates(List.of(), ME));
    }

    @Test
    void aBetaAnAlphaAndAPackmateCount() {
        assertEquals(3, HubSocial.surgeMates(List.of(m("Me", "alpha:2", ME), m("B", "beta", OTHER), m("C", "alpha", UUID.randomUUID()), m("D", "mate", UUID.randomUUID())), ME));
        assertEquals(1, HubSocial.surgeMates(List.of(m("Me", "beta", ME), m("L", "emissary", OTHER)), ME));
    }

    @Test
    void anAllyABrotherAndASubordinateDoNot() {
        assertEquals(0, HubSocial.surgeMates(List.of(m("Me", "alpha", ME), m("A", "ally", OTHER), m("B", "brother", null), m("S", "subordinate", null)), ME));
    }

    @Test
    void oneOfThemAmongTheOthersIsEnough() {
        assertEquals(1, HubSocial.surgeMates(List.of(m("Me", "alpha", ME), m("A", "ally", OTHER), m("B", "brother", null), m("Z", "beta", UUID.randomUUID())), ME));
    }
}
