package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
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
        SupernaturalState s = v.state().toBuilder().skills(Set.of("S-HYB-1", "S-HYB-2", "S-VAM-1", "S-VAM-2", "S-WOL-1", "S-WOL-5", "S-WOL-7"))
                .sp(6).spEarned(11).build();
        List<HubSocial.Member> pack = List.of(new HubSocial.Member("Derek", "alpha", true), new HubSocial.Member("Liam", "beta", true),
                new HubSocial.Member("Malia", "beta", false), new HubSocial.Member("Isaac", "beta", true), new HubSocial.Member("Deaton", "emissary", true),
                // packmates, brothers and NPCs are not ranks the server sends yet (plans 14 and 15): shown here so the ring's colours can be seen
                new HubSocial.Member("Stiles", "packmate", true), new HubSocial.Member("Lydia", "packmate", false), new HubSocial.Member("Ashen", "brother", true),
                new HubSocial.Member("Slime", "npc", true), new HubSocial.Member("Goblin", "npc", true));
        HubSnapshotPayload.Family family = new HubSnapshotPayload.Family(Optional.of("Klaus"), false, List.of("Tyler", "Ana", "Bo", "Cy", "Di"),
                List.of("Mikael"), List.of(new HubSocial.Kin("Tyler", 0), new HubSocial.Kin("Ana", 0), new HubSocial.Kin("Bo", 0), new HubSocial.Kin("Cy", 0),
                        new HubSocial.Kin("Di", 0)));
        List<HubSocial.Rite> rites = new ArrayList<>();
        rites.add(new HubSocial.Rite("enchanted:rite_of_revelation", 1000, List.of("1 minecraft:glass_bottle", "1 minecraft:white_wool", "2 minecraft:glowstone_dust"),
                List.of("enchanted:small_circle"), "demo", "enchanted"));
        v.snapshot().rites().stream().filter(r -> !r.id().equals("enchanted:rite_of_revelation")).forEach(rites::add);
        HubSnapshotPayload snap = new HubSnapshotPayload(family, pack, 2, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN),
                rites);
        return new HubView(v.player(), v.bloodline(), s, v.races(), HubModel.ladders(LadderTable.clientCopy(), s, v.races(), HubView.levels(v.player())), v.skills(), snap,
                v.context());
    }
}
