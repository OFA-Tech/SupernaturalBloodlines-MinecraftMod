package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.ArrayList;
import java.util.List;

/** The keys of other mods that the HUD covers (unbinds and hides while it works, restores when it does not): ids as in each mod's lang file. Pure. */
public final class CoveredKeyIds {
    public static final String WEREWOLVES_BITE = "keys.werewolves.bite";
    /** Werewolves' own leap key (Left Control by default, which is vanilla sprint); the leap is a jump while sprinting now ({@link SprintLeap}). */
    public static final String WEREWOLVES_LEAP = "keys.werewolves.leap";
    /** Our own Alpha Roar key: the Roar is an action in a HUD slot while the HUD works, so the key is hidden then (it stays for a game or server without the HUD). */
    public static final String ALPHA_ROAR_KEY = "key.supernaturalbloodlines.alpha_roar";

    private CoveredKeyIds() {
    }

    public static List<String> all() {
        return all(true);
    }

    /**
     * @param sprintLeap the client setting {@code sprint_leap}: Werewolves' leap key is covered only while the sprint leap replaces it. With it off the
     *                   key must work again, so it is left out of the list and {@code CoverRun} restores a recorded bind of an id no longer listed.
     */
    public static List<String> all(boolean sprintLeap) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ids.add("tensura.keybinding.ability.slot_" + i);
        }
        for (int i = 1; i <= 3; i++) {
            ids.add("tensuraslots.keybinding.secondary_ability.slot_" + i);
        }
        ids.add("keys.vampirism.suck");
        for (int i = 1; i <= 9; i++) {
            ids.add("keys.vampirism.action" + i);
        }
        ids.add(WEREWOLVES_BITE);
        ids.add(ALPHA_ROAR_KEY);
        if (sprintLeap) {
            ids.add(WEREWOLVES_LEAP);
        }
        return List.copyOf(ids);
    }
}
