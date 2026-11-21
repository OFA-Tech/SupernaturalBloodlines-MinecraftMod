package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R181: Peter Hale and his burned house (plan 17g). Houses are a worldgen structure: many can generate (the switch {@code peter_house_enabled} and the chance
 * {@code peter_house_chance} are read for each one), but there is one Peter. He appears when a player in the overworld is within {@link #HOUSE_RADIUS} blocks of a
 * piece of any house and the index says he has not been seen (or was lost); every other house stays empty. He waits in the house as a burned man, shifts at half
 * health or on a full moon night (while {@code peter_feral_full_moon} is on) and does not change back. As the wolf he is <b>feral</b> on a full moon night: he
 * attacks anything alive that is not a boss or somebody who cannot be hurt, not only who he was provoked by. He leaves no way to be called and no hunt: the house is
 * where he is. He dies for good unless {@code peter_respawn} is on and enough days have passed.
 */
public final class PeterRules {
    /** A player this near a piece of a house calls him (the house is some 12 blocks across). */
    public static final int HOUSE_RADIUS = 24;
    /** The floor of the house is this many layers above the bottom of its template: the cellar is below it. */
    public static final int FLOOR_LAYER = 3;
    /** He stands one block above the floor layer. */
    public static final int SPAWN_OFFSET = FLOOR_LAYER + 1;
    public static final float SHIFT_AT_HEALTH = 0.5F;
    /** A guarded player this close is attacked even by the man. */
    public static final double GUARD_RANGE = 10.0;
    public static final double FERAL_RANGE = 24.0;

    public static final String SKIN_BURNED = "peter_hale_burned";
    public static final String SKIN_HEALED = "peter_hale";

    /** The pounce: ticks the animation takes (1.2 s), the tick it lands, and how far its root moves the model (30 pixels). */
    public static final int POUNCE_TICKS = 24;
    public static final int POUNCE_LAND_TICK = 18;
    public static final double POUNCE_DISTANCE = 30.0 / 16.0;
    public static final double POUNCE_NEAREST = 3.0;
    /** The model ends {@link #POUNCE_DISTANCE} ahead and the bite reaches {@link #POUNCE_REACH} from there: nothing farther than this can be hit. */
    public static final double POUNCE_FARTHEST = 4.3;
    public static final double POUNCE_REACH = 2.5;
    public static final int POUNCE_PAUSE = 100;

    private PeterRules() {
    }

    /** One house generates: the switch is on and the chance (clamped to 0 to 1) beats the roll (0 up to, not including, 1). */
    public static boolean houseGenerates(boolean enabled, double chance, double roll) {
        return enabled && roll < Math.max(0.0, Math.min(1.0, chance));
    }

    /** A player calls Peter: in the overworld, not a spectator, and near a house. */
    public static boolean calledBy(boolean overworld, boolean spectator, boolean nearHouse) {
        return overworld && !spectator && nearHouse;
    }

    /** Whether the house at {@code distanceSquared} from the player is near enough. */
    public static boolean near(double distanceSquared) {
        return distanceSquared <= (double) HOUSE_RADIUS * HOUSE_RADIUS;
    }

    /** The squared distance from a point to the nearest point of a box of blocks (0 inside it): a block spans its coordinate to the next, so the far face is max + 1. */
    public static double boxDistanceSquared(double x, double y, double z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double dx = Math.max(Math.max(minX - x, 0.0), x - (maxX + 1));
        double dy = Math.max(Math.max(minY - y, 0.0), y - (maxY + 1));
        double dz = Math.max(Math.max(minZ - z, 0.0), z - (maxZ + 1));
        return dx * dx + dy * dy + dz * dz;
    }

    /** Whether he starts to shift now: only the man, alive, at half health or less, or on a full moon night when the setting lets him. */
    public static boolean shouldShift(BossForm form, float healthFraction, boolean fullMoonNight, boolean feralSetting) {
        return form == BossForm.HUMAN && healthFraction > 0F && (healthFraction <= SHIFT_AT_HEALTH || feralSetting && fullMoonNight);
    }

    /** Feral: the wolf, on a full moon night, with the setting on. */
    public static boolean feral(BossForm form, boolean fullMoonNight, boolean feralSetting) {
        return feralSetting && fullMoonNight && form == BossForm.BEAST;
    }

    /**
     * Whether a feral Peter may pick this creature: anything alive that is not himself, not another boss, and not a player who cannot be hurt (creative,
     * spectator). The pack-target order of the werewolves (who a wolf of a pack leaves alone) does not apply to him.
     */
    public static boolean mayTarget(boolean self, boolean boss, boolean alive, boolean invulnerablePlayer) {
        return !self && !boss && alive && !invulnerablePlayer;
    }

    /** The man's skin: burned until he has seen a full moon, whole after. */
    public static String skin(boolean metFullMoon) {
        return metFullMoon ? SKIN_HEALED : SKIN_BURNED;
    }

    /** Whether he has now met a full moon: he did, or this is one. */
    public static boolean metFullMoon(boolean before, boolean fullMoonNight) {
        return before || fullMoonNight;
    }

    /** The wolf is long and low: its hitbox is 1.4 wide and 1.7 high from the swap tick (the model reaches 2.5 when it rears); the man is a man. */
    public static ShiftRules.Size dimensionsFor(BossForm form, int shiftTicks) {
        return ShiftRules.showsBeast(form, shiftTicks) ? new ShiftRules.Size(1.4F, 1.7F) : new ShiftRules.Size(0.6F, 1.95F);
    }

    /** Whether he starts a pounce: the wolf, on the ground, ready, with a target he can see between 3 and 4.3 blocks away (never through a wall or a floor). */
    public static boolean pounces(BossForm form, double distanceSquared, boolean onGround, int pauseLeft, boolean lineOfSight) {
        return lineOfSight && form == BossForm.BEAST && onGround && pauseLeft <= 0
                && distanceSquared >= POUNCE_NEAREST * POUNCE_NEAREST && distanceSquared <= POUNCE_FARTHEST * POUNCE_FARTHEST;
    }

    /** The pounce lands: the target is within reach of where the animation puts his front paws, and he can still see them. */
    public static boolean pounceLands(double distanceToLandingSquared, boolean lineOfSight) {
        return lineOfSight && distanceToLandingSquared <= POUNCE_REACH * POUNCE_REACH;
    }

    /** The respawn, when the switch is on: at least {@code days} in-game days after the day he died. {@code diedDay} below 0 means nothing is written down. */
    public static boolean mayRespawn(boolean on, long today, long diedDay, int days) {
        return on && diedDay >= 0 && today - diedDay >= Math.max(1, days);
    }
}
