package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;

/** Everything a frame of the hub reads, from the client's synced copies only. Rebuilt every frame, so a sync shows at once. */
public record HubView(Player player, BloodlineState bloodline, SupernaturalState state, List<SupernaturalRace> races,
                      List<HubModel.LadderView> ladders, SkillTable skills, HubSnapshotPayload snapshot, HubPage.Context context) {

    public static HubView of(Player p) {
        BloodlineState b = BloodlineAccess.get(p);
        SupernaturalState s = SupernaturalAccess.get(p);
        List<SupernaturalRace> races = PlayerRaces.of(p);
        HubSnapshotPayload snap = HubClient.snapshot();
        HubPage.Context c = new HubPage.Context(races.get(0), races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty(),
                ModList.get().isLoaded("werewolves"), ModList.get().isLoaded("enchanted"));
        return new HubView(p, b, s, races, HubModel.ladders(LadderTable.clientCopy(), s, races, levels(p)), SkillTable.clientCopy(), snap, c);
    }

    /** The level of the current faction, from Vampirism's synced client data (Werewolves too: it uses Vampirism's faction API). */
    static Map<String, Integer> levels(Player p) {
        var h = VampirismAPI.factionPlayerHandler(p);
        IPlayableFaction<?> f = h.getCurrentFaction();
        return f == null ? Map.of() : Map.of(f.getID().toString(), h.getCurrentLevel());
    }

    public boolean ghoul() {
        return races.get(0) == SupernaturalRace.GHOUL;
    }
}
