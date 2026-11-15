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
        BloodlineState b = BloodlineAccess.get(p);
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        List<SupernaturalRace> out = new ArrayList<>();
        out.add(SupernaturalRace.creature(b, f == null ? null : f.getID().toString()));
        SupernaturalRace.magic(b).ifPresent(out::add);
        return out;
    }
}
