package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The man's pose table: how it is read and sampled, and what the four files an artist delivers must satisfy (plan phase 7, T7 and the art contract). */
class ManPoseTest {
    static final Path MORPH = MorphClipsTest.root().resolve("src/main/resources/assets/supernaturalbloodlines/morph");
    static final String[] BOSSES = {"klaus", "deucalion", "peter", "beast_host"};

    static ManPoseTable load(String boss) throws IOException {
        return ManPoseTable.parse(Files.readString(MORPH.resolve("man_" + boss + ".json"), StandardCharsets.UTF_8));
    }

    private static final String TWO = "{\"keys\":[{\"tick\":0,\"head_x\":0},{\"tick\":19,\"head_x\":-30,\"scale\":0.9}]}";

    @Test
    void clampsAnyInput() {
        ManPoseTable t = ManPoseTable.parse(TWO);
        float[] out = new float[ManPoseTable.SIZE];
        for (float p : new float[] {Float.NaN, -5.0F, 99.0F, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0F, 1.0F, 0.5F}) {
            t.sample(p, out);
            for (float v : out) {
                assertTrue(Float.isFinite(v), "progress " + p);
            }
        }
        t.sample(Float.NaN, out);
        assertEquals(0.0F, out[0], 1e-6);
        t.sample(99.0F, out);
        assertEquals(-30.0F, out[0], 1e-6, "past the end the last key holds");
        t.sample(-3.0F, out);
        assertEquals(0.0F, out[0], 1e-6);
    }

    @Test
    void smoothBetweenKeysAndHoldsPastTheLast() {
        ManPoseTable t = ManPoseTable.parse(TWO);
        float[] out = new float[ManPoseTable.SIZE];
        t.sample(0.25F, out);
        assertTrue(out[0] < 0.0F && out[0] > -15.0F, "early on a smoothstep is slower than a line: " + out[0]);
        t.sample(19.0F / ShiftRules.SWAP_TICK, out);
        assertEquals(-30.0F, out[0], 1e-4);
        assertEquals(0.9F, out[ManPoseTable.SCALE], 1e-4);
        float last = 1.0F;
        for (int i = 0; i <= 40; i++) {
            t.sample(i / 40.0F, out);
            assertTrue(out[0] <= last + 1e-6, "monotone for a monotone pair of keys");
            last = out[0];
        }
    }

    @Test
    void aMissingChannelIsNeutral() {
        float[] out = new float[ManPoseTable.SIZE];
        ManPoseTable.parse("{\"keys\":[{\"tick\":0}]}").sample(0.5F, out);
        for (int i = 0; i < ManPoseTable.SIZE; i++) {
            assertEquals(i == ManPoseTable.SCALE ? 1.0F : 0.0F, out[i], 1e-6, ManPoseTable.CHANNELS[i]);
        }
    }

    @Test
    void aTableThatCannotBeUsedIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ManPoseTable.parse("{\"keys\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> ManPoseTable.parse("{\"keys\":[{\"tick\":3}]}"));
        assertThrows(IllegalArgumentException.class, () -> ManPoseTable.parse("{\"keys\":[{\"tick\":0},{\"tick\":6},{\"tick\":6}]}"));
    }

    @Test
    void sampleAllocatesNothing() {
        ManPoseTable t = ManPoseTable.parse(TWO);
        float[] out = new float[ManPoseTable.SIZE];
        float[] same = out;
        for (int i = 0; i < 10_000; i++) {
            t.sample(i / 10_000.0F, out);
        }
        assertSame(same, out);
        // the table itself is not touched by sampling: a copy of its keys is what the accessors give
        float[] k = t.key(0);
        k[0] = 99.0F;
        assertEquals(0.0F, t.key(0)[0]);
    }

    @Test
    void neutralDoesNothing() {
        float[] out = new float[ManPoseTable.SIZE];
        ManPoseTable.neutral().sample(0.7F, out);
        assertEquals(0.0F, out[0]);
        assertEquals(1.0F, out[ManPoseTable.SCALE]);
    }

    // ---- the four files an artist delivers ----

    @Test
    void allFourFilesLoad() throws IOException {
        for (String boss : BOSSES) {
            ManPoseTable t = load(boss);
            assertTrue(t.keyCount() >= 2, boss);
            assertEquals(0, t.keyTick(0), boss);
        }
    }

    @Test
    void theKeysAreAtTheTicksOfThePlan() throws IOException {
        for (String boss : BOSSES) {
            ManPoseTable t = load(boss);
            int[] want = {0, 6, 12, 16, 19};
            assertEquals(want.length, t.keyCount(), boss);
            for (int i = 0; i < want.length; i++) {
                assertEquals(want[i], t.keyTick(i), boss);
            }
        }
    }

    @Test
    void lastKeyIsTheMatchPose() throws IOException {
        for (String boss : BOSSES) {
            ManPoseTable t = load(boss);
            assertArrayEquals(t.match(), t.key(t.keyCount() - 1), 0.01F, boss + ": the wolf's first frame must share the last key's silhouette");
        }
    }

    @Test
    void noChannelJumpsAtAKey() throws IOException {
        for (String boss : BOSSES) {
            ManPoseTable t = load(boss);
            float[] before = new float[ManPoseTable.SIZE];
            float[] after = new float[ManPoseTable.SIZE];
            for (int i = 1; i < t.keyCount() - 1; i++) {
                float p = t.keyTick(i) / (float) ShiftRules.SWAP_TICK;
                t.sample(p - 1e-4F, before);
                t.sample(p + 1e-4F, after);
                for (int c = 0; c < ManPoseTable.SIZE; c++) {
                    assertEquals(before[c], after[c], 0.05F, boss + " " + ManPoseTable.CHANNELS[c] + " jumps at tick " + t.keyTick(i));
                }
            }
        }
    }

    @Test
    void theBodyStaysInsideASensibleRange() throws IOException {
        for (String boss : BOSSES) {
            ManPoseTable t = load(boss);
            for (int i = 0; i < t.keyCount(); i++) {
                float[] k = t.key(i);
                for (int c = 0; c < ManPoseTable.SIZE - 2; c++) {
                    assertTrue(Math.abs(k[c]) <= 120.0F, boss + " " + ManPoseTable.CHANNELS[c] + " at key " + i + " is " + k[c] + " degrees");
                }
                assertTrue(Math.abs(k[ManPoseTable.SIZE - 2]) <= 0.6F, boss + ": offset_y moves the body " + k[ManPoseTable.SIZE - 2] + " blocks");
                assertTrue(k[ManPoseTable.SCALE] >= 0.7F && k[ManPoseTable.SCALE] <= 1.1F, boss + ": scale " + k[ManPoseTable.SCALE]);
            }
        }
    }

    @Test
    void theFilesAreNotEmptyPlaceholdersOfADifferentBoss() throws IOException {
        for (String boss : BOSSES) {
            assertFalse(Files.readString(MORPH.resolve("man_" + boss + ".json")).isBlank(), boss);
        }
    }
}
