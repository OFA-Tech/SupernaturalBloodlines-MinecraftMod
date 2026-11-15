package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * Which blood or food display the player sees (plan 13, "one blood value"). Pure.
 * <ul>
 * <li>A Vampirism vampire with Tensura installed, the setting on and the renderer ready: Vampirism's blood in Tensura's food spot, with Vampirism's own
 * bar and Tensura's food number both hidden.</li>
 * <li>Without Tensura: Vampirism's bar, untouched.</li>
 * <li>Not a vampire (or no Vampirism): Tensura's food, untouched.</li>
 * <li>Setting off, or the renderer could not reach what it needs from Tensura: both originals stay (nothing is hidden that we cannot replace).</li>
 * </ul>
 */
public final class BloodSpotRule {
    public enum Show {
        VAMPIRISM_BAR(false, false),
        TENSURA_FOOD(false, false),
        BLOOD_IN_FOOD_SPOT(true, true);

        private final boolean hidesBar;
        private final boolean hidesFood;

        Show(boolean hidesBar, boolean hidesFood) {
            this.hidesBar = hidesBar;
            this.hidesFood = hidesFood;
        }

        public boolean hidesVampirismBar() {
            return hidesBar;
        }

        public boolean hidesTensuraFood() {
            return hidesFood;
        }
    }

    private BloodSpotRule() {
    }

    public static Show decide(boolean tensura, boolean vampirism, boolean settingOn, boolean isVampire, boolean rendererReady) {
        if (!tensura) {
            return Show.VAMPIRISM_BAR;
        }
        if (!vampirism || !isVampire) {
            return Show.TENSURA_FOOD;
        }
        return settingOn && rendererReady ? Show.BLOOD_IN_FOOD_SPOT : Show.VAMPIRISM_BAR;
    }
}
