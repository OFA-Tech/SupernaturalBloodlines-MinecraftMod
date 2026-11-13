package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PackIndexTest {
    private static UUID id() {
        return UUID.randomUUID();
    }

    @Test
    void joinAndLeave() {
        PackIndex i = new PackIndex();
        UUID alpha = id();
        UUID a = id();
        UUID b = id();
        assertTrue(i.join(alpha, a));
        assertTrue(i.join(alpha, b));
        assertEquals(List.of(a, b), i.betasOf(alpha));
        assertEquals(alpha, i.alphaOf(a).orElseThrow());
        i.leave(a);
        assertTrue(i.alphaOf(a).isEmpty());
        assertEquals(List.of(b), i.betasOf(alpha));
    }

    @Test
    void aBetaNeverSwitchesPackThroughJoin() {
        PackIndex i = new PackIndex();
        UUID one = id();
        UUID two = id();
        UUID beta = id();
        assertTrue(i.join(one, beta));
        assertFalse(i.join(two, beta));
        assertEquals(one, i.alphaOf(beta).orElseThrow());
        assertFalse(i.join(one, one));
    }

    @Test
    void transferMovesTheWholePackAndDropsTheOldAlpha() {
        PackIndex i = new PackIndex();
        UUID old = id();
        UUID heir = id();
        UUID other = id();
        i.join(old, heir);
        i.join(old, other);
        i.setEmissary(old, id());
        i.transfer(old, heir);
        assertTrue(i.alphaOf(heir).isEmpty());
        assertEquals(List.of(other), i.betasOf(heir));
        assertTrue(i.betasOf(old).isEmpty());
        assertTrue(i.emissaryOf(old).isEmpty());
    }

    @Test
    void survivesSaveAndLoad() {
        PackIndex i = new PackIndex();
        UUID alpha = id();
        UUID beta = id();
        UUID witch = id();
        UUID anchor = id();
        i.join(alpha, beta);
        i.setEmissary(alpha, witch);
        i.setAnchor(beta, anchor);
        PackIndex back = PackIndex.load(i.save(new CompoundTag(), null), null);
        assertEquals(alpha, back.alphaOf(beta).orElseThrow());
        assertEquals(witch, back.emissaryOf(alpha).orElseThrow());
        assertEquals(anchor, back.anchorOf(beta).orElseThrow());
        back.setAnchor(beta, null);
        assertTrue(back.anchorOf(beta).isEmpty());
    }
}
