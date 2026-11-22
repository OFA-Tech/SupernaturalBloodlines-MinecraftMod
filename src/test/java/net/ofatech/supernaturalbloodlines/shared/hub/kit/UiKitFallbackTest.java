package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UiKitFallbackTest {
    @Test
    void aMissingSpriteIsReportedMissingSoTheCallerDrawsFlatColours() {
        Set<String> pack = new HashSet<>(Set.of("kit/grimoire/panel"));
        SpritePresence p = new SpritePresence(pack::contains);
        assertTrue(p.has("kit/grimoire/panel"));
        assertFalse(p.has("kit/grimoire/button_normal"));
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
        SpritePresence p = new SpritePresence(pack::contains);
        assertFalse(p.has("kit/bloodmoon/panel"));
        pack.add("kit/bloodmoon/panel"); // a pack that brings the sprite
        assertFalse(p.has("kit/bloodmoon/panel"), "remembered until the reload");
        p.clear();
        assertTrue(p.has("kit/bloodmoon/panel"));
    }

    @Test
    void everyPartHasAFlatLookForWhenItsSpriteIsGone() {
        for (KitSprites.Part part : KitSprites.Part.values()) {
            assertTrue(part.flat() != null, part + " has no flat look");
        }
    }

    @Test
    void aSkillWithoutItsIconShowsTheUnknownRune() {
        Set<String> pack = Set.of("icons/16/s-hum-1");
        assertEquals("s-hum-1", KitSprites.skillIcon("S-HUM-1", pack::contains));
        assertEquals("unknown", KitSprites.skillIcon("other-mod:skill", pack::contains));
    }
}
