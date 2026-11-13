package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/**
 * The two weaknesses that cancel every Werewolves form action, forced or chosen: the lunar eclipse and Shocked. Links
 * Werewolves (through {@link ForcedShift}): call {@link #register} only behind {@code isLoaded("werewolves")}.
 */
public final class FormBlocks {
    public static final String ECLIPSE_MESSAGE = "supernaturalbloodlines.eclipse.blocked";
    public static final String SHOCKED_MESSAGE = "supernaturalbloodlines.shocked.blocked";

    private FormBlocks() {
    }

    public static void register() {
        ForcedShift.register();
        ForcedShift.addFormBlock(p -> LunarEclipse.active(p.server), ECLIPSE_MESSAGE);
        ForcedShift.addFormBlock(p -> p.hasEffect(SpeciesRegistries.SHOCKED), SHOCKED_MESSAGE);
    }
}
