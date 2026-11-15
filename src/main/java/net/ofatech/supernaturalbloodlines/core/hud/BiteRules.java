package net.ofatech.supernaturalbloodlines.core.hud;

/** R110, the Bite action by form. First match wins. No game classes. */
public final class BiteRules {
    private BiteRules() {
    }

    public static BiteAction decide(BiteContext c) {
        if (c.werewolfFaction() && c.wolfForm()) {
            return BiteAction.WEREWOLF_BITE; // 1
        }
        if (c.vampirismVampire()) {
            return BiteAction.VAMPIRISM_DRINK; // 2
        }
        if (c.hybridType()) {
            return BiteAction.HYBRID_FEED; // 3: a hybrid is in the Werewolves faction, so Vampirism's drinking cannot serve it
        }
        if (c.inTurning() && c.turningWouldComplete()) {
            return BiteAction.TURNING_BITE; // T
        }
        if (c.werewolfFaction()) {
            return BiteAction.WEREWOLF_BITE; // 4: Werewolves itself refuses a form that cannot bite
        }
        return BiteAction.NONE; // 5
    }
}
