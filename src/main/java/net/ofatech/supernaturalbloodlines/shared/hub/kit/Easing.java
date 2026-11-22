package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** The easings of the motion rules (style guide frame 11): a time from 0 to 1 in, a progress from 0 to 1 out. Pure. */
public enum Easing {
    LINEAR {
        @Override
        float curve(float t) {
            return t;
        }
    },
    OUT_QUAD {
        @Override
        float curve(float t) {
            return 1f - (1f - t) * (1f - t);
        }
    },
    OUT_CUBIC {
        @Override
        float curve(float t) {
            float u = 1f - t;
            return 1f - u * u * u;
        }
    },
    /** Four steps (0, 1/3, 2/3, 1): sprites swap in 2 to 4 frames and never blend. */
    STEP4 {
        @Override
        float curve(float t) {
            return Math.min(3, (int) (t * 4f)) / 3f;
        }
    };

    abstract float curve(float t);

    /** The progress at time {@code t}; {@code t} outside 0 to 1 is clamped, so a finished tween stays at 1. */
    public float apply(float t) {
        return curve(Math.max(0f, Math.min(1f, t)));
    }
}
