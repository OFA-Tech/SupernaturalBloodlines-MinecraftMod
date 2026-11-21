package net.ofatech.supernaturalbloodlines.core.bloodline;

/**
 * R184: the body of a desiccated NPC Original lies on its back, head to heel as long as the player model, and the hitbox is the box that body fills.
 * A box is square in x and z, so it is as wide as the body is long, whichever way it faces. {@code HumanoidBossRenderer} puts the middle of the body
 * over the entity.
 */
public final class LyingBody {
    /** Head to heel of the player model. */
    public static final float LENGTH = 1.8F;
    /** The thickest part of a body on its back is the head (8 pixels deep): the box is that tall and the body rests on the floor. */
    public static final float HEIGHT = 0.5F;
    public static final float EYE_HEIGHT = 0.3F;

    private LyingBody() {
    }
}
