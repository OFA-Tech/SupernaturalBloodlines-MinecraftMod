package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * Development only: demo data for the dev screenshots ({@code -Dsupernaturalbloodlines.devshots.demo=true}), so every populated state of the hub can be
 * photographed in a world that has no pack, no family, no other mods' quests and no learned skills. It rebuilds the client's view of the hub; it
 * changes nothing on the server and is reached only from {@link DevShots}.
 */
final class DevDemo {
    private DevDemo() {
    }

    static HubView apply(HubView v) {
        String mode = DevShots.demo();
        if ("ghoul".equals(mode)) {
            return ghoul(v);
        }
        HubView base = "empty".equals(mode) ? empty(v) : populated(v);
        if ("two".equals(mode)) {
            List<HubModel.LadderView> both = new ArrayList<>(base.ladders());
            HubModel.LadderView first = both.get(0);
            both.add(new HubModel.LadderView(SupernaturalRace.WITCH, first.rungs(), first.active(), first.tasks()));
            return new HubView(base.player(), base.bloodline(), base.state(), base.races(), both, base.skills(), base.snapshot(), base.context());
        }
        return base;
    }

    /** A Ghoul with 14:20 left to finish turning: Path and Guide open, the rest locked. */
    private static HubView ghoul(HubView v) {
        List<SupernaturalRace> races = List.of(SupernaturalRace.GHOUL);
        HubPage.Context c = v.context();
        HubPage.Context ghoul = new HubPage.Context(SupernaturalRace.GHOUL, Optional.empty(), c.werewolves(), c.enchanted(), c.loadout());
        return new HubView(v.player(), v.bloodline().toBuilder().ghoul(true).turningTicksLeft(17_200).build(), v.state(), races,
                HubModel.ladders(LadderTable.clientCopy(), v.state(), races, Map.of()), v.skills(), v.snapshot(), ghoul);
    }

    /** Nothing yet: no pack, no sire or children, no open task (the three empty states). */
    private static HubView empty(HubView v) {
        HubSnapshotPayload.Family none = HubSnapshotPayload.Family.NONE;
        HubSnapshotPayload snap = new HubSnapshotPayload(none, List.of(), 0, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN),
                v.snapshot().rites());
        List<HubModel.LadderView> done = v.ladders().stream().map(l -> new HubModel.LadderView(l.race(), l.rungs(), Optional.empty(), List.of())).toList();
        return new HubView(v.player(), v.bloodline(), v.state().toBuilder().skills(Set.of()).build(), v.races(), done, v.skills(), snap, v.context());
    }

    private static HubView populated(HubView v) {
        SupernaturalState s = v.state().toBuilder().skills(Set.of("S-HYB-1", "S-HYB-2", "S-VAM-1", "S-VAM-2", "S-WOL-1", "S-WOL-5", "S-WOL-7"))
                .sp(6).spEarned(11).build();
        // the player leads the demo pack (the first of the list), so the kick and ally actions can be photographed
        String me = v.player().getName().getString();
        List<HubSocial.Member> pack = List.of(new HubSocial.Member(me, "alpha", true), new HubSocial.Member("Derek", "coalpha", true), new HubSocial.Member("Liam", "beta", true),
                new HubSocial.Member("Malia", "beta", false), new HubSocial.Member("Isaac", "beta", true), new HubSocial.Member("Deaton", "emissary", true),
                new HubSocial.Member("Stiles", "packmate", true), new HubSocial.Member("Lydia", "packmate", false), new HubSocial.Member("Scott", "ally", true),
                // brothers and NPCs are plan 15's ranks, not sent yet: shown here so the ring's colours can be seen
                new HubSocial.Member("Ashen", "brother", true), new HubSocial.Member("Slime", "npc", true), new HubSocial.Member("Goblin", "npc", true));
        HubSnapshotPayload.Family family = new HubSnapshotPayload.Family(Optional.of("Klaus"), false, List.of("Tyler", "Ana", "Bo", "Cy", "Di"),
                List.of("Mikael"), List.of(new HubSocial.Kin("Tyler", 0), new HubSocial.Kin("Ana", 0), new HubSocial.Kin("Bo", 0), new HubSocial.Kin("Cy", 0),
                        new HubSocial.Kin("Di", 0)), HubSocial.Origin.NONE);
        List<HubSocial.Rite> rites = new ArrayList<>();
        rites.addAll(v.snapshot().rites());
        HubSnapshotPayload snap = new HubSnapshotPayload(family, pack, 2, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN),
                rites);
        return new HubView(v.player(), v.bloodline(), s, v.races(), HubModel.ladders(LadderTable.clientCopy(), s, v.races(), HubView.levels(v.player())), v.skills(), snap,
                v.context());
    }
}
