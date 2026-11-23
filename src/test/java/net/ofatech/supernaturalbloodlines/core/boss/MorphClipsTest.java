package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * What a shift clip must satisfy (UI overhaul phase 7, T5): the contract the art is delivered against. The four animation files and the four geo files are read
 * as JSON, no Minecraft. A boss's tests pass as its clips land; the placeholders that stand in until then satisfy all of them (a held idle pose, the old howl
 * stretched), so a regenerated file that breaks the contract fails here.
 */
public class MorphClipsTest {
    private static final Path ASSETS = root().resolve("src/main/resources/assets/supernaturalbloodlines");
    private static final Path UNTOUCHED = root().resolve("art/creatures/morph/untouched.sha256");

    /** The repository root: the tests run from a build folder, so walk up to the one that holds src/main/java. */
    public static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    /** model -> the howl clip, whether it has a shift_out, the clips this phase removes. */
    private record Boss(String model, String howl, boolean out, List<String> removed) {
    }

    private static final List<Boss> BOSSES = List.of(
            new Boss("klaus_hybrid", "howl", true, List.of()),
            new Boss("deucalion_demon_wolf", "roar", true, List.of("drop_to_fours")),
            new Boss("peter_hale_alpha", "roar", true, List.of("rise_up")),
            new Boss("beast_of_gevaudan", "roar", false, List.of()));

    private static final double SHIFT_IN_SECONDS = (ShiftRules.SHIFT_TICKS - ShiftRules.SWAP_TICK) / 20.0;
    private static final double HOWL_SECONDS = ShiftRules.HOWL_TICKS / 20.0;

    private static JsonObject animations(Boss b) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("animations/entity/" + b.model() + ".animation.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
    }

    private static JsonObject clip(Boss b, String name) throws IOException {
        JsonObject all = animations(b);
        String full = "animation." + b.model() + "." + name;
        assertTrue(all.has(full), b.model() + " has no " + name);
        return all.getAsJsonObject(full);
    }

    private static Set<String> bonesOf(Boss b) throws IOException {
        JsonObject geo = JsonParser.parseString(Files.readString(ASSETS.resolve("geo/entity/" + b.model() + ".geo.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> names = new HashSet<>();
        for (JsonElement g : geo.getAsJsonArray("minecraft:geometry")) {
            for (JsonElement bone : g.getAsJsonObject().getAsJsonArray("bones")) {
                names.add(bone.getAsJsonObject().get("name").getAsString());
            }
        }
        return names;
    }

    private static double length(JsonObject clip) {
        return clip.get("animation_length").getAsDouble();
    }

    private static boolean loops(JsonObject clip) {
        return clip.has("loop") && clip.get("loop").isJsonPrimitive() && clip.get("loop").getAsJsonPrimitive().isBoolean() && clip.get("loop").getAsBoolean();
    }

    @Test
    void shiftInIsOneSecondAndPlaysOnce() throws IOException {
        assertEquals(1.0, SHIFT_IN_SECONDS, 1e-9, "the arrival is the second half of the 2 s shift");
        for (Boss b : BOSSES) {
            JsonObject c = clip(b, "shift_in");
            assertEquals(SHIFT_IN_SECONDS, length(c), 1e-6, b.model());
            assertFalse(loops(c), b.model() + ": shift_in loops");
        }
    }

    @Test
    void howlFitsTheRest() throws IOException {
        for (Boss b : BOSSES) {
            double sum = length(clip(b, "shift_in")) + length(clip(b, b.howl()));
            assertEquals(HOWL_SECONDS, sum, 1e-6, b.model() + ": shift_in + " + b.howl() + " must be the 3.2 s of the howl");
            assertFalse(loops(clip(b, b.howl())), b.model());
        }
    }

    @Test
    void everyBoneExists() throws IOException {
        for (Boss b : BOSSES) {
            Set<String> bones = bonesOf(b);
            List<String> names = b.out() ? List.of("shift_in", "shift_out", b.howl()) : List.of("shift_in", b.howl());
            for (String n : names) {
                for (String bone : clip(b, n).has("bones") ? clip(b, n).getAsJsonObject("bones").keySet() : Set.<String>of()) {
                    assertTrue(bones.contains(bone), b.model() + " " + n + " animates " + bone + ", which is not a bone");
                }
            }
        }
    }

    /** The value of a channel at its first keyframe or its only value, as numbers; an object keyframe is read by its "vector" (or "post"). */
    private static double[] at(JsonElement channel, boolean first) {
        JsonElement v = channel;
        if (channel.isJsonObject()) {
            JsonObject o = channel.getAsJsonObject();
            if (o.has("vector") || o.has("post") || o.has("pre")) {
                v = o.has("vector") ? o.get("vector") : o.has("post") ? o.get("post") : o.get("pre");
            } else {
                String key = o.keySet().stream().reduce((a, c) -> (Double.parseDouble(a) <= Double.parseDouble(c)) == first ? a : c).orElseThrow();
                return at(o.get(key), first);
            }
        }
        if (v.isJsonArray()) {
            JsonArray a = v.getAsJsonArray();
            double[] out = new double[a.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = a.get(i).isJsonPrimitive() && a.get(i).getAsJsonPrimitive().isNumber() ? a.get(i).getAsDouble() : 0.0;
            }
            return out;
        }
        return new double[] {v.getAsDouble()};
    }

    @Test
    void shiftInEndsInTheIdlePose() throws IOException {
        for (Boss b : BOSSES) {
            JsonObject in = clip(b, "shift_in").has("bones") ? clip(b, "shift_in").getAsJsonObject("bones") : new JsonObject();
            JsonObject idle = clip(b, "idle").getAsJsonObject("bones");
            for (String bone : idle.keySet()) {
                JsonObject idleChannels = idle.getAsJsonObject(bone);
                JsonObject inChannels = in.has(bone) ? in.getAsJsonObject(bone) : new JsonObject();
                for (String channel : idleChannels.keySet()) {
                    double[] want = at(idleChannels.get(channel), true);
                    if (!inChannels.has(channel)) {
                        continue; // not animated by shift_in: it stays as the model has it, which is where idle starts from
                    }
                    double[] got = at(inChannels.get(channel), false);
                    double tolerance = channel.equals("rotation") ? 1.0 : 0.02 * (channel.equals("position") ? 16.0 : 1.0);
                    for (int i = 0; i < Math.min(want.length, got.length); i++) {
                        assertEquals(want[i], got[i], tolerance, b.model() + " " + bone + "." + channel + "[" + i + "]: the last shift_in frame must be the first idle frame");
                    }
                }
            }
        }
    }

    @Test
    void shiftOutExistsAndIsOneSecond() throws IOException {
        for (Boss b : BOSSES) {
            String full = "animation." + b.model() + ".shift_out";
            assertEquals(b.out(), animations(b).has(full), b.model() + ": shift_out is for Klaus, Deucalion and Peter, not the Beast");
            if (b.out()) {
                assertEquals(1.0, length(clip(b, "shift_out")), 1e-6, b.model());
                assertFalse(loops(clip(b, "shift_out")), b.model());
            }
        }
    }

    /**
     * A clip that moves says how it is eased. A Blockbench whose GeckoLib plugin is half loaded exports every eased key as linear, and neither the lengths nor
     * the bones change, so this is the only thing that notices. A held placeholder (two keys a channel) has nothing to ease and is let through.
     */
    @Test
    void theClipsKeepTheirEasing() throws IOException {
        for (Boss b : BOSSES) {
            List<String> names = b.out() ? List.of("shift_in", "shift_out", b.howl()) : List.of("shift_in", b.howl());
            for (String n : names) {
                int eased = 0;
                int densest = 0;
                JsonObject bones = clip(b, n).has("bones") ? clip(b, n).getAsJsonObject("bones") : new JsonObject();
                for (String bone : bones.keySet()) {
                    for (JsonElement channel : bones.getAsJsonObject(bone).asMap().values()) {
                        if (!channel.isJsonObject() || channel.getAsJsonObject().has("vector") || channel.getAsJsonObject().has("post")) {
                            continue;
                        }
                        densest = Math.max(densest, channel.getAsJsonObject().size());
                        for (JsonElement key : channel.getAsJsonObject().asMap().values()) {
                            if (key.isJsonObject() && (key.getAsJsonObject().has("easing") || key.getAsJsonObject().has("lerp_mode"))) {
                                eased++;
                            }
                        }
                    }
                }
                assertTrue(densest <= 2 || eased > 0, b.model() + " " + n + ": every key is linear, the easings were dropped on the way into the game file");
            }
        }
    }

    @Test
    void noRootDrift() throws IOException {
        for (Boss b : BOSSES) {
            JsonObject bones = clip(b, "shift_in").has("bones") ? clip(b, "shift_in").getAsJsonObject("bones") : new JsonObject();
            if (!bones.has("root") || !bones.getAsJsonObject("root").has("position")) {
                continue;
            }
            JsonElement position = bones.getAsJsonObject("root").get("position");
            double[] a = at(position, true);
            double[] z = at(position, false);
            double moved = 0;
            if (position.isJsonObject() && !position.getAsJsonObject().has("vector")) {
                for (String key : position.getAsJsonObject().keySet()) {
                    double[] p = at(position.getAsJsonObject().get(key), true);
                    for (int i = 0; i < Math.min(a.length, p.length); i++) {
                        moved = Math.max(moved, Math.abs(p[i] - a[i]));
                    }
                }
            }
            for (int i = 0; i < Math.min(a.length, z.length); i++) {
                moved = Math.max(moved, Math.abs(z[i] - a[i]));
            }
            assertTrue(moved / 16.0 <= 0.3, b.model() + ": the root of shift_in travels " + moved / 16.0 + " blocks, the entity does not move");
        }
    }

    // ---- everything else is byte for byte what main had ----

    static String hash(JsonObject clip) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(clip.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte x : d) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    @Test
    void otherClipsAreUntouched() throws Exception {
        Map<String, String> expected = new HashMap<>();
        for (String line : Files.readAllLines(UNTOUCHED, StandardCharsets.UTF_8)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                String[] p = line.trim().split("\\s+");
                expected.put(p[0], p[1]);
            }
        }
        assertFalse(expected.isEmpty());
        Set<String> seen = new HashSet<>();
        for (Boss b : BOSSES) {
            JsonObject all = animations(b);
            Set<String> own = new HashSet<>(List.of("shift_in", "shift_out", b.howl()));
            own.addAll(b.removed());
            for (String full : all.keySet()) {
                String shortName = full.substring(full.lastIndexOf('.') + 1);
                if (own.contains(shortName)) {
                    continue;
                }
                String key = b.model() + ":" + shortName;
                assertTrue(expected.containsKey(key), key + " is a clip that main did not have");
                assertEquals(expected.get(key), hash(all.getAsJsonObject(full)), key + " changed (only shift_in, shift_out and the howl may)");
                seen.add(key);
            }
        }
        assertEquals(expected.keySet(), seen, "a clip that main had is gone");
    }

    @Test
    void theClipsThisPhaseReplacesAreGone() throws IOException {
        for (Boss b : BOSSES) {
            for (String gone : b.removed()) {
                assertFalse(animations(b).has("animation." + b.model() + "." + gone), b.model() + " still has " + gone + ", which shift_in replaces");
            }
        }
    }
}
