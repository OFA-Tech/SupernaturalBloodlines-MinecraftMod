package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LyingBodyTest {
    @Test
    void theBoxIsLowAndAsWideAsTheBodyIsLong() {
        assertTrue(LyingBody.HEIGHT < 0.8F, "the box is as tall as " + LyingBody.HEIGHT);
        assertEquals(1.8F, LyingBody.LENGTH, "the player model is 1.8 from head to heel");
        assertTrue(LyingBody.EYE_HEIGHT < LyingBody.HEIGHT);
    }

    /** What the renderer does to the model (translate, then the turn), before its own turn of 180 - yaw. */
    private static Matrix4f lay() {
        return new Matrix4f().translate(LyingBody.OFFSET_X, LyingBody.OFFSET_Y, 0.0F).rotate(LyingBody.rotation());
    }

    @Test
    void theHeadAndTheHeelLandHalfALengthEitherSideOfTheMiddleAlongTheFacingWhateverTheYaw() {
        for (float yaw = -180F; yaw <= 360F; yaw += 22.5F) {
            Matrix4f world = new Matrix4f().rotateY((float) Math.toRadians(180.0 - yaw)).mul(lay());
            Vector3f head = world.transformPosition(new Vector3f(0F, LyingBody.LENGTH, 0F));
            Vector3f heel = world.transformPosition(new Vector3f(0F, 0F, 0F));
            double[] ahead = LyingBody.onBody(yaw, LyingBody.LENGTH / 2.0);
            double[] behind = LyingBody.onBody(yaw, -LyingBody.LENGTH / 2.0);
            assertEquals(ahead[0], head.x, 1.0E-4, "head x at yaw " + yaw);
            assertEquals(ahead[1], head.z, 1.0E-4, "head z at yaw " + yaw);
            assertEquals(behind[0], heel.x, 1.0E-4, "heel x at yaw " + yaw);
            assertEquals(behind[1], heel.z, 1.0E-4, "heel z at yaw " + yaw);
            assertEquals(0.9, Math.hypot(head.x, head.z), 1.0E-4, "the head is not 0.9 from the middle at yaw " + yaw);
            assertEquals(0.9, Math.hypot(heel.x, heel.z), 1.0E-4, "the heel is not 0.9 from the middle at yaw " + yaw);
            assertEquals(-1.0, Math.signum(head.x * heel.x + head.z * heel.z), "head and heel on one side at yaw " + yaw);
        }
    }

    @Test
    void theBodyLiesOnItsBackWithItsBackOnTheFloor() {
        Matrix4f lay = lay();
        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        // the model's thickness is its z: the back at +0.125 (the body), the head's front at -0.25 (a head is 8 deep, centred on the body's line)
        for (float z : new float[] {-0.25F, 0.25F}) {
            for (float y : new float[] {0F, LyingBody.LENGTH}) {
                Vector3f p = lay.transformPosition(new Vector3f(0F, y, z));
                lowest = Math.min(lowest, p.y);
                highest = Math.max(highest, p.y);
            }
        }
        // on its back: the front of the model (-z) is up, the back (+z) is down
        float front = lay.transformPosition(new Vector3f(0F, 1.0F, -0.1F)).y;
        float back = lay.transformPosition(new Vector3f(0F, 1.0F, 0.1F)).y;
        assertTrue(front > back, "the body lies on its face: front at " + front + ", back at " + back);
        assertEquals(0.0F, lowest, 1.0E-5F, "the body is not on the floor");
        assertEquals(LyingBody.HEIGHT, highest, 1.0E-5F, "the body is not as tall as its box");
    }

    @Test
    void theWolfsHeadIsInItsBoxWhicheverWayHeFaces() {
        double half = LyingBody.BEAST_LENGTH / 2.0;
        for (float yaw = 0F; yaw < 360F; yaw += 7.5F) {
            double[] d = LyingBody.inFrame(yaw, LyingBody.BEAST_HEAD[0], LyingBody.BEAST_HEAD[2]);
            assertTrue(Math.abs(d[0]) < half && Math.abs(d[1]) < half, "the head is outside the box at yaw " + yaw + ": " + d[0] + ", " + d[1]);
        }
        assertTrue(LyingBody.BEAST_HEAD[1] < LyingBody.BEAST_HEIGHT);
        assertTrue(LyingBody.BEAST_LENGTH > LyingBody.LENGTH && LyingBody.BEAST_HEIGHT > LyingBody.HEIGHT, "the wolf lies longer and higher than a man");
    }
}
