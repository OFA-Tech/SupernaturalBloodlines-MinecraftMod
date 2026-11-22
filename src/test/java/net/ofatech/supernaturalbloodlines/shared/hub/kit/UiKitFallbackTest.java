package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class UiKitFallbackTest {
    /** A probe that behaves like the client's: it throws on a path a resource location cannot hold, as {@code ResourceLocation.fromNamespaceAndPath} does. */
    private static Predicate<String> realProbe(Set<String> pack) {
        return path -> {
            if (!KitSprites.validName(path)) {
                throw new IllegalStateException("Non [a-z0-9/._-] character in path of location: supernaturalbloodlines:" + path);
            }
            return pack.contains(path);
        };
    }

    @Test
    void aMissingSpriteIsReportedMissingSoTheCallerDrawsFlatColours() {
        SpritePresence p = new SpritePresence(realProbe(Set.of("kit/grimoire/panel")));
        assertTrue(p.has("kit/grimoire/panel"));
        assertFalse(p.has("kit/grimoire/button_normal"));
    }

    @Test
    void aPathTheProbeCannotTakeIsMissingAndNeverThrowsIntoAFrame() {
        SpritePresence p = new SpritePresence(realProbe(Set.of("icons/16/unknown")));
        assertFalse(p.has("icons/16/mymod:fireball"));
        assertFalse(p.has("icons/16/S-Vam-1"));
        assertFalse(p.has("icons/16/mymod:fireball"), "and the same on the next frame");
    }

    @Test
    void theAnswerIsAskedOnceNotEveryFrame() {
        int[] asked = {0};
        SpritePresence p = new SpritePresence(s -> {
            asked[0]++;
            return true;
        });
        for (int frame = 0; frame < 100; frame++) {
            p.has("kit/tensura/panel");
        }
        assertEquals(1, asked[0]);
    }

    @Test
    void aResourcePackReloadMakesItAskAgain() {
        Set<String> pack = new HashSet<>();
        SpritePresence p = new SpritePresence(realProbe(pack));
        assertFalse(p.has("kit/bloodmoon/panel"));
        pack.add("kit/bloodmoon/panel"); // a pack that brings the sprite
        assertFalse(p.has("kit/bloodmoon/panel"), "remembered until the reload");
        p.clear();
        assertTrue(p.has("kit/bloodmoon/panel"));
    }

    @Test
    void aSkillOfAnotherModShowsTheUnknownRuneWithoutAskingForAnInvalidPath() {
        Set<String> pack = Set.of("icons/16/s-hum-1", "icons/16/unknown");
        Predicate<String> probe = realProbe(pack);
        assertEquals("s-hum-1", KitSprites.skillIcon("S-HUM-1", probe));
        // would throw if the id were asked as it is
        assertEquals("unknown", KitSprites.skillIcon("mymod:fireball", probe));
        assertEquals("unknown", KitSprites.skillIcon("Some Skill", probe));
        assertEquals("unknown", KitSprites.skillIcon("", probe));
        assertEquals("unknown", KitSprites.skillIcon("s-vam-1", probe), "a valid name with no picture");
    }

    @Test
    void whatAResourceLocationCanHoldIsWhatValidNameAccepts() {
        assertTrue(KitSprites.validName("s-wol-11"));
        assertTrue(KitSprites.validName("icons/16/moon_full.v2"));
        assertFalse(KitSprites.validName("mymod:fireball"));
        assertFalse(KitSprites.validName("Fireball"));
        assertFalse(KitSprites.validName("a b"));
    }
}