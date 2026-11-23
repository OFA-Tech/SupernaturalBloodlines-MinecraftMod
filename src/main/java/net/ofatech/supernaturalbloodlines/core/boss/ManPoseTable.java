package net.ofatech.supernaturalbloodlines.core.boss;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * The man's half of a shift (UI overhaul phase 7): a small table of poses that an artist reads off the man's skeleton in Blockbench, and the sampler that the
 * client draws from. The man is the vanilla player model, not a GeckoLib model, so his half is not a clip but twelve numbers per key, smoothed between keys.
 * Pure (no Minecraft): the client's {@code ManPose} only finds the files and passes their text in.
 *
 * <p>File format ({@code assets/supernaturalbloodlines/morph/man_<boss>.json}), degrees for rotations, blocks for {@code offset_y}, a factor for {@code scale}; a
 * channel that is missing is neutral (0, or 1 for the scale):
 * <pre>
 * { "keys": [ { "tick": 0, "head_x": 0, ... }, { "tick": 6, ... }, ... ], "match": { "head_x": 0, ... } }
 * </pre>
 * Keys are at ticks 0, 6, 12, 16 and 19 by the plan, but any ascending ticks from 0 work. {@code match} is the pose the wolf's first frame shares, and it
 * equals the last key (a test pins it).
 */
public final class ManPoseTable {
    /** The channels, in the order {@link #sample} writes them. */
    public static final String[] CHANNELS = {"head_x", "head_y", "head_z", "body_x", "right_arm_x", "right_arm_z", "left_arm_x", "left_arm_z", "right_leg_x", "left_leg_x", "offset_y", "scale"};
    public static final int SIZE = CHANNELS.length;
    public static final int SCALE = SIZE - 1;

    private final int[] ticks;
    private final float[][] keys;
    private final float[] match;

    private ManPoseTable(int[] ticks, float[][] keys, float[] match) {
        this.ticks = ticks;
        this.keys = keys;
        this.match = match;
    }

    /** A table that does nothing: every channel neutral at every tick. */
    public static ManPoseTable neutral() {
        float[] n = neutralKey();
        return new ManPoseTable(new int[] {0, ShiftRules.SWAP_TICK - 1}, new float[][] {n.clone(), n.clone()}, n.clone());
    }

    private static float[] neutralKey() {
        float[] k = new float[SIZE];
        k[SCALE] = 1.0F;
        return k;
    }

    /** Reads a table from the text of its file. Throws {@link IllegalArgumentException} for one that cannot be used (no keys, ticks not ascending from 0). */
    public static ManPoseTable parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray array = root.has("keys") ? root.getAsJsonArray("keys") : new JsonArray();
        if (array.isEmpty()) {
            throw new IllegalArgumentException("a pose table needs keys");
        }
        List<Integer> at = new ArrayList<>();
        List<float[]> keys = new ArrayList<>();
        for (JsonElement e : array) {
            JsonObject o = e.getAsJsonObject();
            int tick = o.get("tick").getAsInt();
            if (at.isEmpty() ? tick != 0 : tick <= at.get(at.size() - 1)) {
                throw new IllegalArgumentException("key ticks must start at 0 and go up: " + tick);
            }
            at.add(tick);
            keys.add(read(o));
        }
        float[] match = root.has("match") ? read(root.getAsJsonObject("match")) : keys.get(keys.size() - 1).clone();
        return new ManPoseTable(at.stream().mapToInt(Integer::intValue).toArray(), keys.toArray(new float[0][]), match);
    }

    private static float[] read(JsonObject o) {
        float[] k = neutralKey();
        for (int i = 0; i < SIZE; i++) {
            if (o.has(CHANNELS[i])) {
                k[i] = finite(o.get(CHANNELS[i]).getAsFloat(), k[i]);
            }
        }
        return k;
    }

    private static float finite(float v, float fallback) {
        return Float.isNaN(v) || Float.isInfinite(v) ? fallback : v;
    }

    public int keyCount() {
        return keys.length;
    }

    public int keyTick(int i) {
        return ticks[i];
    }

    /** A copy of key {@code i}. */
    public float[] key(int i) {
        return keys[i].clone();
    }

    /** A copy of the pose the wolf's first frame shares. */
    public float[] match() {
        return match.clone();
    }

    /**
     * The pose at {@code progress} (0 to 1 over the man's half, as {@link MorphRules#manProgress} gives it), written into {@code out} (length at least
     * {@link #SIZE}) with a smoothstep between the two keys around it. Any input is safe: NaN counts as 0, anything outside 0 to 1 is clamped, and past the
     * last key the last key holds. Writes into the array it is given and allocates nothing.
     */
    public void sample(float progress, float[] out) {
        float p = Float.isNaN(progress) ? 0.0F : Math.max(0.0F, Math.min(1.0F, progress));
        float tick = p * ShiftRules.SWAP_TICK;
        int last = keys.length - 1;
        if (tick >= ticks[last]) {
            System.arraycopy(keys[last], 0, out, 0, SIZE);
            return;
        }
        int i = 0;
        while (i < last - 1 && tick >= ticks[i + 1]) {
            i++;
        }
        float u = (tick - ticks[i]) / (float) (ticks[i + 1] - ticks[i]);
        float s = u * u * (3.0F - 2.0F * u);
        float[] a = keys[i];
        float[] b = keys[i + 1];
        for (int c = 0; c < SIZE; c++) {
            out[c] = a[c] + (b[c] - a[c]) * s;
        }
    }
}
