package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

/** What the brotherhood perks listen to, with or without Werewolves (the species part registers it through {@code GroupsRegistry}). */
public final class BrotherhoodRegistry {
    private static boolean registered;

    private BrotherhoodRegistry() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
    }

    public static synchronized boolean registered() {
        return registered;
    }
}