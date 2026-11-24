package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import org.junit.jupiter.api.Test;

/**
 * The pack file of a world from before the groups plan must load and save as it was: the groups plan keeps its own file and never rewrites this one
 * (spec 2, "data safety"). The fixture is what {@code PackIndex.save} wrote for a small world (two packs, an alliance, last-seen times, a notice, an emissary,
 * an anchor, a recorded party, sparks and a former Alpha), kept as SNBT.
 */
class PackIndexDataSafetyTest {
    private static final UUID L1 = new UUID(0xA1, 1);
    private static final UUID L2 = new UUID(0xA2, 1);
    private static final UUID M1 = new UUID(0xB1, 1);
    private static final UUID M2 = new UUID(0xB1, 2);
    private static final UUID M3 = new UUID(0xB2, 1);

    private static CompoundTag fixture() throws Exception {
        try (InputStream in = PackIndexDataSafetyTest.class.getResourceAsStream("/fixtures/pack_index_v1.snbt")) {
            return TagParser.parseTag(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
        }
    }

    /** The tag with every list in a fixed order: the index keeps maps, so the order a list is written in is not part of the data. */
    private static String norm(Tag t) {
        if (t instanceof ListTag list) {
            List<String> parts = new ArrayList<>();
            list.forEach(e -> parts.add(norm(e)));
            Collections.sort(parts);
            return parts.toString();
        }
        if (t instanceof CompoundTag c) {
            List<String> keys = new ArrayList<>(c.getAllKeys());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder("{");
            for (String k : keys) {
                sb.append(k).append('=').append(norm(c.get(k))).append(';');
            }
            return sb.append('}').toString();
        }
        return t.toString();
    }

    @Test
    void theOldFileLoadsAsItWas() throws Exception {
        PackIndex i = PackIndex.load(fixture(), null);
        assertEquals(Set.of(L1, L2), i.leaders());
        assertEquals(Set.of(M1, M2), Set.copyOf(i.betasOf(L1)));
        assertEquals(List.of(M3), i.betasOf(L2));
        assertEquals(Set.of(L2), i.alliesOf(L1));
        assertEquals(Optional.of(1_699_000_000_000L), i.lastSeen(L1));
        assertEquals(1_699_900_000_000L, i.aliveAt());
        assertEquals(2, i.sparksOf(L1));
        assertEquals(7, i.strengthOf(M2));
        assertTrue(i.isFormerAlpha(M3));
        assertEquals(Optional.of(M1), i.emissaryOf(L1));
        assertEquals(Optional.of(L1), i.anchorOf(M2));
        assertEquals(1, i.noticeCount(M1));
    }

    @Test
    void theOldFileSavesUnchanged() throws Exception {
        CompoundTag old = fixture();
        CompoundTag again = PackIndex.load(old, null).save(new CompoundTag(), null);
        assertEquals(norm(old), norm(again), "loading and saving the pack file of an older world changed it");
    }

    @Test
    void theGroupsPlanLeavesItAlone() throws Exception {
        CompoundTag old = fixture();
        PackIndex packs = PackIndex.load(old, null);
        GroupIndex groups = new GroupIndex();
        groups.seed(packs);
        groups.setType(groups.groupOf(L1), GroupType.CLAN, 5L);
        groups.save(new CompoundTag(), null);
        assertEquals(norm(old), norm(packs.save(new CompoundTag(), null)), "building the group index changed the pack file");
    }
}
