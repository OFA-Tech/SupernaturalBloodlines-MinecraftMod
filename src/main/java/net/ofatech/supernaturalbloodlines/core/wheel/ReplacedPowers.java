package net.ofatech.supernaturalbloodlines.core.wheel;

import net.minecraft.resources.ResourceLocation;

/** Powers of another mod that one of ours replaces: only ours shows. The server still lists them; the client hides them. */
public final class ReplacedPowers {
    private static final ResourceLocation LEAP = ResourceLocation.fromNamespaceAndPath("werewolves", "leap");

    private ReplacedPowers() {
    }

    public static boolean hidden(PowerRef ref, boolean sprintLeapOn) {
        return sprintLeapOn && ref.source() == PowerSource.WEREWOLVES && LEAP.equals(ref.id());
    }
}
