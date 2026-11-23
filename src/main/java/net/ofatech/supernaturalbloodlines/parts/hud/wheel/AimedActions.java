package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Vampirism and Werewolves actions that wait for the player to aim (Vampirism has no flag for it, so this is our table). */
public final class AimedActions {
    public static final Set<ResourceLocation> IDS = Set.of(ResourceLocation.fromNamespaceAndPath("vampirism", "teleport"));

    private AimedActions() {
    }
}
