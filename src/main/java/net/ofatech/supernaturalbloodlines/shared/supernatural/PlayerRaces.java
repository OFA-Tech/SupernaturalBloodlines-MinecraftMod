package net.ofatech.supernaturalbloodlines.shared.supernatural;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;

/** The supernatural races of a player (computed, never stored): the creature race, then the magic race if there is one. */
public final class PlayerRaces {
    private PlayerRaces() {
    }

    public static List<SupernaturalRace> of(Player p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return of(BloodlineAccess.get(p), f == null ? null : f.getID().toString());
    }

    /** The races from the two things they are made of: the bloodline and the Vampirism faction id (null for none). */
    public static List<SupernaturalRace> of(BloodlineState b, @org.jetbrains.annotations.Nullable String factionId) {
        List<SupernaturalRace> out = new ArrayList<>();
        out.add(SupernaturalRace.creature(b, factionId));
        SupernaturalRace.magic(b).ifPresent(out::add);
        return out;
    }
}
