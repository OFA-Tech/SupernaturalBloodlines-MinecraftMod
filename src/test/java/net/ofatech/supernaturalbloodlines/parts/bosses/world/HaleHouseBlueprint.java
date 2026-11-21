package net.ofatech.supernaturalbloodlines.parts.bosses.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;

/**
 * The burned Hale house as code (plan 17g, U4): an 11 by 9 house, two storeys and a cellar, with the roof and half the upper walls burned away, a cold
 * fireplace, a charred loot chest and ash on the floor. {@link #toNbt()} writes it in the vanilla structure format, and {@link #main} writes
 * {@code data/supernaturalbloodlines/structure/hale_house.nbt}; the test keeps that file equal to this. The structure sinks {@link PeterRules#FLOOR_LAYER} layers into the
 * ground, so the layers below it are the cellar. Cells nobody wrote are not in the file (a template places nothing there), so the ground around stays.
 *
 * <p>Run {@code java ... HaleHouseBlueprint <path-to-hale_house.nbt>} (or the environment variable {@code SB_WRITE_HALE_HOUSE=<path>} for the test) to write it again.
 */
public final class HaleHouseBlueprint {
    public static final int DATA_VERSION = 3955;
    public static final int W = 11;
    public static final int H = 13;
    public static final int D = 9;
    /** The middle of the floor: where Peter stands, one block above {@link PeterRules#FLOOR_LAYER}. */
    public static final int CX = W / 2;
    public static final int CZ = D / 2;
    public static final String CHEST_LOOT = "supernaturalbloodlines:chests/hale_house";

    public record Cell(String state, CompoundTag nbt) {
    }

    private final Map<Long, Cell> cells = new LinkedHashMap<>();
    private final Random random = new Random(0x4A1EL);

    private static long key(int x, int y, int z) {
        return ((long) y * D + z) * W + x;
    }

    public HaleHouseBlueprint() {
        build();
    }

    public Map<Long, Cell> cells() {
        return cells;
    }

    public String at(int x, int y, int z) {
        Cell c = cells.get(key(x, y, z));
        return c == null ? null : c.state;
    }

    public Cell cellAt(int x, int y, int z) {
        return cells.get(key(x, y, z));
    }

    private void set(int x, int y, int z, String state) {
        cells.put(key(x, y, z), new Cell(state, null));
    }

    private void set(int x, int y, int z, String state, CompoundTag nbt) {
        cells.put(key(x, y, z), new Cell(state, nbt));
    }

    private void remove(int x, int y, int z) {
        cells.remove(key(x, y, z));
    }

    /** A wall block of a house that burned: scorched planks, and a good part of it blackstone, basalt or coal. */
    private String burnt() {
        double r = random.nextDouble();
        return r < 0.22 ? "minecraft:blackstone" : r < 0.34 ? "minecraft:basalt[axis=y]" : r < 0.40 ? "minecraft:coal_block" : "minecraft:dark_oak_planks";
    }

    private String charredPost() {
        return chance(0.35) ? "minecraft:basalt[axis=y]" : "minecraft:stripped_dark_oak_log[axis=y]";
    }

    private boolean chance(double p) {
        return random.nextDouble() < p;
    }

    private void build() {
        int floor = PeterRules.FLOOR_LAYER;
        // the cellar: a ring of cobblestone round a 5 by 5 room, a stone floor, and the ground is left alone beyond it
        for (int x = 2; x <= 8; x++) {
            for (int z = 1; z <= 7; z++) {
                boolean ring = x == 2 || x == 8 || z == 1 || z == 7;
                for (int y = 0; y < floor; y++) {
                    if (y == 0) {
                        set(x, y, z, chance(0.25) ? "minecraft:mossy_cobblestone" : "minecraft:cobblestone");
                    } else if (ring) {
                        set(x, y, z, chance(0.2) ? "minecraft:mossy_cobblestone" : "minecraft:cobblestone");
                    } else {
                        set(x, y, z, "minecraft:air");
                    }
                }
            }
        }
        // the ground floor: charred planks and burned patches, with a hole for the ladder down
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                double r = random.nextDouble();
                set(x, floor, z, r < 0.2 ? "minecraft:coal_block" : r < 0.45 ? "minecraft:blackstone" : r < 0.55 ? "minecraft:coarse_dirt" : "minecraft:dark_oak_planks");
            }
        }
        set(3, floor, 4, "minecraft:ladder[facing=east]");
        for (int y = 1; y < floor; y++) {
            set(3, y, 4, "minecraft:ladder[facing=east]");
        }
        // the ground storey walls: corner posts of dark oak, charred planks between, burned through in places, a doorway in the south
        for (int y = floor + 1; y <= floor + 3; y++) {
            for (int x = 0; x < W; x++) {
                for (int z = 0; z < D; z++) {
                    boolean edge = x == 0 || x == W - 1 || z == 0 || z == D - 1;
                    if (!edge) {
                        continue;
                    }
                    boolean post = (x == 0 || x == W - 1) && (z == 0 || z == D - 1) || (z == 0 || z == D - 1) && x == 5 || (x == 0 || x == W - 1) && z == 4;
                    boolean door = z == D - 1 && x == 5 && y <= floor + 2;
                    if (door) {
                        continue;
                    }
                    if (post) {
                        set(x, y, z, charredPost());
                    } else if (!chance(0.34)) {
                        set(x, y, z, burnt());
                    }
                }
            }
        }
        // the cold fireplace in the north wall, with its chimney
        for (int x = 4; x <= 6; x++) {
            set(x, floor + 1, 1, "minecraft:cobblestone");
            set(x, floor + 3, 1, "minecraft:cobblestone");
        }
        set(4, floor + 2, 1, "minecraft:cobblestone");
        set(6, floor + 2, 1, "minecraft:cobblestone");
        set(5, floor + 1, 1, "minecraft:campfire[facing=south,lit=false,signal_fire=false,waterlogged=false]");
        for (int y = floor + 4; y <= floor + 8; y++) {
            set(5, y, 1, y > floor + 7 && chance(0.5) ? "minecraft:cobblestone_wall" : "minecraft:cobblestone");
        }
        // the loot chest and the ash
        CompoundTag chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        chest.putString("LootTable", CHEST_LOOT);
        set(8, floor + 1, 2, "minecraft:chest[facing=west,type=single,waterlogged=false]", chest);
        for (int i = 0; i < 40; i++) {
            int x = 1 + random.nextInt(W - 2);
            int z = 1 + random.nextInt(D - 2);
            if (cellAt(x, floor + 1, z) == null && !(x == 3 && z == 4)) {
                double c = random.nextDouble();
                set(x, floor + 1, z, c < 0.4 ? "minecraft:gray_carpet" : c < 0.75 ? "minecraft:black_carpet" : "minecraft:cobweb");
            }
        }
        // the ladder to the upper floor, on the west wall
        for (int y = floor + 1; y <= floor + 4; y++) {
            set(1, y, 4, "minecraft:ladder[facing=east]");
        }
        // the upper floor: planks on the west side, holes where it fell, none in the east
        int upper = floor + 4;
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                if (x <= 5 && !(x == 1 && z == 4) && !chance(0.22)) {
                    set(x, upper, z, burnt());
                } else if (x > 5 && x <= 7 && z == 0 && chance(0.5)) {
                    set(x, upper, z, "minecraft:dark_oak_planks");
                }
            }
        }
        // the upper walls, mostly gone, the corner posts higher than the rest
        for (int y = upper + 1; y <= upper + 3; y++) {
            for (int x = 0; x < W; x++) {
                for (int z = 0; z < D; z++) {
                    boolean edge = x == 0 || x == W - 1 || z == 0 || z == D - 1;
                    if (!edge) {
                        continue;
                    }
                    boolean corner = (x == 0 || x == W - 1) && (z == 0 || z == D - 1);
                    double keep = corner ? 0.9 : y == upper + 1 ? 0.55 : y == upper + 2 ? 0.3 : 0.1;
                    if (random.nextDouble() < keep) {
                        set(x, y, z, corner ? charredPost() : burnt());
                    }
                }
            }
        }
        // the roof beams that did not burn
        for (int z : new int[] {0, 4, 8}) {
            for (int x = 0; x < W; x++) {
                if (x % 2 == 0 && !chance(0.3)) {
                    set(x, upper + 4, z, "minecraft:dark_oak_log[axis=x]");
                }
            }
        }
        // nothing may stand where he does: the spawn cell and the one above it are open
        for (int dy = 1; dy <= 2; dy++) {
            remove(CX, floor + dy, CZ);
        }
        set(CX, floor, CZ, "minecraft:dark_oak_planks");
        pruneFloating();
    }

    /** A burned house is not a house of floating planks: a block that no chain of neighbours ties to the ground floor or the cellar is dropped. */
    private void pruneFloating() {
        java.util.Set<Long> keep = new java.util.HashSet<>();
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
        for (Long k : cells.keySet()) {
            if (k / ((long) W * D) <= PeterRules.FLOOR_LAYER) {
                keep.add(k);
                queue.add(k);
            }
        }
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            long k = queue.poll();
            int x = (int) (k % W);
            int z = (int) ((k / W) % D);
            int y = (int) (k / ((long) W * D));
            for (int[] d : around) {
                int nx = x + d[0];
                int ny = y + d[1];
                int nz = z + d[2];
                if (nx < 0 || ny < 0 || nz < 0 || nx >= W || ny >= H || nz >= D) {
                    continue;
                }
                long nk = key(nx, ny, nz);
                if (cells.containsKey(nk) && keep.add(nk)) {
                    queue.add(nk);
                }
            }
        }
        cells.keySet().retainAll(keep);
    }

    /** Whether any block is left that is not tied to the ground (for the test). */
    public boolean hasFloatingBlocks() {
        HaleHouseBlueprint copy = this;
        int before = copy.cells.size();
        copy.pruneFloating();
        return copy.cells.size() != before;
    }

    /** The structure NBT, the vanilla format: a palette of states, the blocks, and no entities. */
    public CompoundTag toNbt() {
        Map<String, Integer> palette = new LinkedHashMap<>();
        List<Map.Entry<Long, Cell>> ordered = new ArrayList<>(cells.entrySet());
        ordered.sort(Map.Entry.comparingByKey());
        ListTag blocks = new ListTag();
        for (Map.Entry<Long, Cell> e : ordered) {
            long k = e.getKey();
            int x = (int) (k % W);
            int z = (int) ((k / W) % D);
            int y = (int) (k / ((long) W * D));
            int state = palette.computeIfAbsent(e.getValue().state, s -> palette.size());
            CompoundTag b = new CompoundTag();
            ListTag pos = new ListTag();
            pos.add(IntTag.valueOf(x));
            pos.add(IntTag.valueOf(y));
            pos.add(IntTag.valueOf(z));
            b.put("pos", pos);
            b.putInt("state", state);
            if (e.getValue().nbt != null) {
                b.put("nbt", e.getValue().nbt.copy());
            }
            blocks.add(b);
        }
        ListTag paletteTag = new ListTag();
        palette.keySet().forEach(s -> paletteTag.add(state(s)));
        CompoundTag root = new CompoundTag();
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(W));
        size.add(IntTag.valueOf(H));
        size.add(IntTag.valueOf(D));
        root.put("size", size);
        root.put("palette", paletteTag);
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        root.putInt("DataVersion", DATA_VERSION);
        return root;
    }

    private static CompoundTag state(String s) {
        CompoundTag t = new CompoundTag();
        int bracket = s.indexOf('[');
        t.putString("Name", bracket < 0 ? s : s.substring(0, bracket));
        if (bracket >= 0) {
            CompoundTag props = new CompoundTag();
            for (String pair : s.substring(bracket + 1, s.length() - 1).split(",")) {
                String[] kv = pair.split("=");
                props.put(kv[0], StringTag.valueOf(kv[1]));
            }
            t.put("Properties", props);
        }
        return t;
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "src/main/resources/data/supernaturalbloodlines/structure/hale_house.nbt");
        Files.createDirectories(out.toAbsolutePath().getParent());
        try (var os = Files.newOutputStream(out)) {
            NbtIo.writeCompressed(new HaleHouseBlueprint().toNbt(), os);
        }
    }
}