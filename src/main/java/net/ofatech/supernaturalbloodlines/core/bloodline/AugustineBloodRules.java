package net.ofatech.supernaturalbloodlines.core.bloodline;

/** R203: how much blood an Augustine takes from one drink of Vampirism's. Pure. */
public final class AugustineBloodRules {
    private AugustineBloodRules() {
    }

    /**
     * Blood an Augustine gets from one drink of Vampirism's {@code amount}: times {@code fromVampire} when the victim is a vampire, times {@code fromOther}
     * otherwise. Nothing stays nothing (a bite something refused is still refused), and a drink that gave some never rounds down to none while its factor is above 0.
     */
    public static int scale(int amount, boolean victimIsVampire, double fromVampire, double fromOther) {
        double factor = victimIsVampire ? fromVampire : fromOther;
        if (amount <= 0 || factor <= 0) {
            return 0;
        }
        long scaled = Math.round(amount * factor);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }
}
