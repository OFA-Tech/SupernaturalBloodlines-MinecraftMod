package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.server.level.ServerPlayer;

/** The only class of this part that links Werewolves; call it only behind {@code ModList.isLoaded("werewolves")}. */
public final class WerewolfFaction {
    private WerewolfFaction() {
    }

    public static IPlayableFaction<?> faction() {
        return WReference.WEREWOLF_FACTION;
    }

    /** Joins at level 1, or keeps the current level when already a werewolf. */
    public static boolean join(ServerPlayer p) {
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        int level = h.getCurrentFaction() == WReference.WEREWOLF_FACTION ? Math.max(1, h.getCurrentLevel()) : 1;
        return h.setFactionAndLevel(WReference.WEREWOLF_FACTION, level);
    }
}
