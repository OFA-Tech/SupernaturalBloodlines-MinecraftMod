package net.ofatech.supernaturalbloodlines.parts.bosses.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;
import org.junit.jupiter.api.Test;

class HaleHouseBlueprintTest {
    private static final String PATH = "/data/supernaturalbloodlines/structure/hale_house.nbt";
    private static final HaleHouseBlueprint HOUSE = new HaleHouseBlueprint();

    /** The dev switch: the environment variable {@code SB_WRITE_HALE_HOUSE=<file>} writes the template first (then run the test again: the check reads the built resources). */
    @Test
    void theCommittedTemplateIsTheBlueprint() throws Exception {
        String write = System.getenv("SB_WRITE_HALE_HOUSE");
        if (write != null) {
            HaleHouseBlueprint.main(new String[] {write});
        }
        try (var in = HaleHouseBlueprintTest.class.getResourceAsStream(PATH)) {
            assertNotNull(in, "structure/hale_house.nbt is missing: run HaleHouseBlueprint.main (or SB_WRITE_HALE_HOUSE)");
            CompoundTag committed = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            assertEquals(HOUSE.toNbt(), committed, "hale_house.nbt is not what HaleHouseBlueprint writes: run HaleHouseBlueprint.main again");
        }
    }

    @Test
    void thePeterSpotIsOneBlockAboveASolidFloorWithTwoOpenCellsAndTheCellarIsBelow() {
        int floor = PeterRules.FLOOR_LAYER;
        assertEquals(floor + 1, PeterRules.SPAWN_OFFSET);
        String under = HOUSE.at(HaleHouseBlueprint.CX, floor, HaleHouseBlueprint.CZ);
        assertEquals("minecraft:dark_oak_planks", under, "Peter does not stand on a floor");
        assertEquals(null, HOUSE.at(HaleHouseBlueprint.CX, floor + 1, HaleHouseBlueprint.CZ), "his cell is filled");
        assertEquals(null, HOUSE.at(HaleHouseBlueprint.CX, floor + 2, HaleHouseBlueprint.CZ), "the cell above him is filled");
        // the cellar is under the floor: an open room of 3 by 3 by 2 and a ladder up to it
        for (int y = 1; y < floor; y++) {
            assertEquals("minecraft:air", HOUSE.at(HaleHouseBlueprint.CX, y, HaleHouseBlueprint.CZ), "no cellar at layer " + y);
        }
        assertEquals("minecraft:ladder[facing=east]", HOUSE.at(3, floor, 4), "the ladder does not reach the floor");
    }

    @Test
    void theHouseIsBurnedTwoStoreysWithAChestAColdFireAndNothingOutsideItsBox() {
        assertEquals(HaleHouseBlueprint.CHEST_LOOT, HOUSE.cellAt(8, PeterRules.FLOOR_LAYER + 1, 2).nbt().getString("LootTable"));
        assertEquals("minecraft:chest[facing=west,type=single,waterlogged=false]", HOUSE.cellAt(8, PeterRules.FLOOR_LAYER + 1, 2).state());
        assertTrue(HOUSE.at(5, PeterRules.FLOOR_LAYER + 1, 1).contains("campfire") && HOUSE.at(5, PeterRules.FLOOR_LAYER + 1, 1).contains("lit=false"), "the fireplace is not cold");
        CompoundTag nbt = HOUSE.toNbt();
        assertEquals(3, nbt.getList("size", Tag.TAG_INT).size());
        assertEquals(HaleHouseBlueprint.H, nbt.getList("size", Tag.TAG_INT).getInt(1));
        for (var e : HOUSE.cells().keySet()) {
            long k = e;
            int x = (int) (k % HaleHouseBlueprint.W);
            int z = (int) ((k / HaleHouseBlueprint.W) % HaleHouseBlueprint.D);
            int y = (int) (k / ((long) HaleHouseBlueprint.W * HaleHouseBlueprint.D));
            assertTrue(x >= 0 && x < HaleHouseBlueprint.W && z >= 0 && z < HaleHouseBlueprint.D && y >= 0 && y < HaleHouseBlueprint.H, "a block outside the box: " + x + "," + y + "," + z);
        }
        boolean upperFloor = HOUSE.cells().keySet().stream().anyMatch(k -> (int) (k / ((long) HaleHouseBlueprint.W * HaleHouseBlueprint.D)) == PeterRules.FLOOR_LAYER + 4);
        assertTrue(upperFloor, "no upper storey");
        long highest = HOUSE.cells().keySet().stream().mapToLong(k -> k / ((long) HaleHouseBlueprint.W * HaleHouseBlueprint.D)).max().orElse(0);
        assertTrue(highest <= HaleHouseBlueprint.H - 1, "the house is taller than its box");
    }

    @Test
    void everyBlockIsAVanillaOneAndOnlyBurnedMaterials() {
        Set<String> allowed = Set.of("minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:air", "minecraft:coal_block", "minecraft:blackstone", "minecraft:coarse_dirt",
                "minecraft:dark_oak_planks", "minecraft:ladder[facing=east]", "minecraft:stripped_dark_oak_log[axis=y]", "minecraft:gray_carpet", "minecraft:cobweb",
                "minecraft:cobblestone_wall", "minecraft:dark_oak_log[axis=x]", "minecraft:chest[facing=west,type=single,waterlogged=false]",
                "minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]");
        for (var c : HOUSE.cells().values()) {
            assertTrue(allowed.contains(c.state()), "an unexpected block: " + c.state());
        }
        assertFalse(HOUSE.cells().values().stream().anyMatch(c -> c.state().contains("fire") && !c.state().contains("campfire")), "a real fire in the house");
    }

    @Test
    void theTemplateFileIsSmallAndGzipped() throws Exception {
        try (var in = HaleHouseBlueprintTest.class.getResourceAsStream(PATH)) {
            byte[] bytes = in.readAllBytes();
            assertTrue(bytes.length > 100 && bytes.length < 40_000, "the template is " + bytes.length + " bytes");
            assertEquals((byte) 0x1f, bytes[0]);
            assertEquals((byte) 0x8b, bytes[1]);
        }
    }
}