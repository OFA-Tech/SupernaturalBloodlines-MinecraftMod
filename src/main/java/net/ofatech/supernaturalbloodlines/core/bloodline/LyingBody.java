package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.joml.Quaternionf;

/**
 * R184: the body of a desiccated NPC Original lies on its back, head to heel as long as the player model, and the hitbox is the box that body fills.
 * A box is square in x and z, so it is as wide as the body is long, whichever way it faces. {@code HumanoidBossRenderer} puts the middle of the body
 * over the entity with {@link #OFFSET_X}, {@link #OFFSET_Y} and {@link #rotation()}; {@link #onBody} says where a point of the body then is.
 */
public final class LyingBody {
    /** Head to heel of the player model. */
    public static final float LENGTH = 1.8F;
    /** The thickest part of a body on its back is the head (8 pixels deep): the box is that tall and the body rests on the floor. */
    public static final float HEIGHT = 0.5F;
    public static final float EYE_HEIGHT = 0.3F;

    /** Slide the body along its length by half of it, so its middle (not its heels) is over the entity, and up by half its thickness, so its back is on the floor. */
    public static final float OFFSET_X = LENGTH / 2.0F;
    public static final float OFFSET_Y = HEIGHT / 2.0F;

    /**
     * Klaus's wolf in its death pose (measured with {@code tools/measure_wolf_lying.py} from {@code klaus_hybrid.geo.json} and the end of the {@code death}
     * animation: it lies on its side with the head up, from 2.0 blocks in front of the entity to 1.7 behind, 1.2 high). The box is as wide as the
     * circle that holds the head's middle ({@link #BEAST_HEAD}), so whichever way he faces the head is in it, and as tall as the body with the head raised.
     */
    public static final float BEAST_LENGTH = 3.0F;
    public static final float BEAST_HEIGHT = 1.0F;
    public static final float BEAST_EYE_HEIGHT = 0.75F;
    /** The middle of the wolf's head in the death pose, in the model's frame (x, y, z; its front is -z), in blocks from the entity's feet. */
    public static final double[] BEAST_HEAD = {-0.231, 0.753, -1.466};

    private LyingBody() {
    }

    /** The turn that lays an upright model on its back, head toward -x: vanilla's sleeping pose without its pivot on the heels. */
    public static Quaternionf rotation() {
        return new Quaternionf().rotateZ((float) Math.toRadians(90.0)).rotateY((float) Math.toRadians(270.0));
    }

    /** A point of the model's frame (x, z; y is up) in the world, relative to the entity, for a body turned {@code yawDegrees}: the renderer's turn of 180 - yaw. */
    public static double[] inFrame(float yawDegrees, double x, double z) {
        double a = Math.toRadians(180.0 - yawDegrees);
        return new double[] {x * Math.cos(a) + z * Math.sin(a), -x * Math.sin(a) + z * Math.cos(a)};
    }

    /** {@code along} blocks from the middle of a lying body toward its head (negative: toward the heels), as (x, z) from the entity. */
    public static double[] onBody(float yawDegrees, double along) {
        return inFrame(yawDegrees, -along, 0.0);
    }
}
