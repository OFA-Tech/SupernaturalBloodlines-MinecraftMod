package net.ofatech.supernaturalbloodlines.parts.hud.client.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Whether the player is in the werewolf faction (the full moon warning is theirs). Linked to Vampirism's API: reached only with Vampirism and Werewolves loaded. */
public final class WerewolfCheck {
    private static final ResourceLocation WEREWOLF = ResourceLocation.parse("werewolves:werewolf");

    private WerewolfCheck() {
    }

    public static boolean isWerewolf(Player p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f != null && WEREWOLF.equals(f.getID());
    }
}
