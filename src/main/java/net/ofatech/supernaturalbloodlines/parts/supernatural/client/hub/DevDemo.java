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
    private static boolean installed;

    private DevDemo() {
    }

    /** A made-up person's id: the same name is the same id in every frame. */
    static java.util.UUID id(String name) {
        return java.util.UUID.nameUUIDFromBytes(("devdemo:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static java.util.Map<net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact, List<String>> facts(String... kv) {
        java.util.Map<net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact, List<String>> out = new java.util.EnumMap<>(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.class);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.valueOf(kv[i]), kv[i + 1].isEmpty() ? List.of() : List.of(kv[i + 1].split("\\|")));
        }
        return out;
    }

    /** The profiles, requests and admin data of the demo, set once (a client view only). */
    private static void install(HubView v) {
        if (installed) {
            return;
        }
        installed = true;
        String me = v.player().getName().getString();
        var map = new java.util.HashMap<java.util.UUID, net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload>();
        map.put(id("Derek"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Derek"), false, facts("NAME", "Derek", "SPECIES", "werewolf",
                "RANK", "coalpha", "LEVEL", "9", "PACK", me, "PACK_DAYS", "12", "SIRE", "", "LINE", "", "GENES", "werewolf", "ANCHOR", me, "PRESENCE", "online", "BONUS", "+20% attack, +4 max health")));
        map.put(id("Liam"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Liam"), false, facts("NAME", "Liam", "SPECIES", "werewolf",
                "RANK", "beta", "LEVEL", "4", "PACK", me, "PACK_DAYS", "3", "SIRE", "", "LINE", "", "ANCHOR", "Derek", "PRESENCE", "online", "BONUS", "")));
        map.put(id("Stranger"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Stranger"), false, java.util.Map.of()));
        map.put(id("Klaus"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Klaus"), false, facts("NAME", "Klaus", "SPECIES", "vampire", "LEVEL", "14", "PRESENCE", "online")));
        map.put(id("Tyler"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Tyler"), false, facts("NAME", "Tyler", "SPECIES", "hybrid", "LEVEL", "7", "SIRE", me)));
        map.put(v.player().getUUID(), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(v.player().getUUID(), false, facts("NAME", me, "SPECIES", "original_hybrid",
                "RANK", "alpha", "LEVEL", "12", "PACK", me, "PACK_DAYS", "40", "SIRE", "Klaus", "LINE", "", "GENES", "werewolf", "ANCHOR", "", "PRESENCE", "online", "BONUS", "+25% attack")));
        // the OP view: every field, whatever the OP knows or not
        map.put(id("Derek|op"), new net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload(id("Derek|op"), true, facts("NAME", "Derek", "SPECIES", "werewolf", "RANK", "coalpha",
                "LEVEL", "9", "PACK", me, "PACK_DAYS", "12", "SIRE", "", "LINE", "Mikael", "GENES", "werewolf|doppelganger", "ANCHOR", me, "PRESENCE", "online", "BONUS", "+20% attack")));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.ProfileClient.demo(map);
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.RequestsClient.demo(List.of(
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.SIRE_CALL, id("Klaus"), "Klaus", "ab12cd34", 5000),
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.PACK_INVITE, id("Derek"), "Derek", "0f0f0f0f", 4000),
                new net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Request(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.COMPULSION, net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.NOBODY, "", "11223344", 3000)));
        net.ofatech.supernaturalbloodlines.parts.supernatural.client.AdminClient.demo(net.ofatech.supernaturalbloodlines.parts.supernatural.net.AdminNet.snapshot(Optional.empty()),
                new net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubCommandReplyPayload("D1", true, List.of(net.minecraft.network.chat.Component.literal(me + ": BloodlineState[species=VAMPIRE, sire=Klaus]"))));
    }

    static HubView apply(HubView v) {
        String mode = DevShots.demo();
        install(v);
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
        HubPage.Context ghoul = new HubPage.Context(SupernaturalRace.GHOUL, Optional.empty(), c.werewolves(), c.enchanted(), c.loadout(), false, false);
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
        List<HubSocial.Member> pack = List.of(new HubSocial.Member(me, "alpha", true, Optional.of(v.player().getUUID())), new HubSocial.Member("Derek", "coalpha", true, Optional.of(id("Derek"))),
                new HubSocial.Member("Liam", "beta", true, Optional.of(id("Liam"))),
                new HubSocial.Member("Malia", "beta", false, Optional.of(id("Malia"))), new HubSocial.Member("Isaac", "beta", true, Optional.of(id("Isaac"))), new HubSocial.Member("Deaton", "emissary", true, Optional.of(id("Deaton"))),
                new HubSocial.Member("Stiles", "packmate", true, Optional.of(id("Stiles"))), new HubSocial.Member("Lydia", "packmate", false, Optional.of(id("Lydia"))), new HubSocial.Member("Scott", "ally", true, Optional.of(id("Scott"))),
                // brothers and NPCs are plan 15's ranks, not sent yet: shown here so the ring's colours can be seen
                new HubSocial.Member("Ashen", "brother", true), new HubSocial.Member("Slime", "npc", true), new HubSocial.Member("Goblin", "npc", true));
        HubSnapshotPayload.Family family = new HubSnapshotPayload.Family(Optional.of("Klaus"), false, List.of("Tyler", "Ana", "Bo", "Cy", "Di"),
                List.of("Mikael"), List.of(new HubSocial.Kin("Tyler", 0), new HubSocial.Kin("Ana", 0), new HubSocial.Kin("Bo", 0), new HubSocial.Kin("Cy", 0),
                        new HubSocial.Kin("Di", 0)), HubSocial.Origin.NONE);
        List<HubSocial.Rite> rites = new ArrayList<>();
        rites.addAll(v.snapshot().rites());
        List<HubSocial.Person> people = new ArrayList<>(List.of(new HubSocial.Person("Klaus", id("Klaus"))));
        family.sired().forEach(n -> people.add(new HubSocial.Person(n, id(n))));
        HubSnapshotPayload snap = new HubSnapshotPayload(family, pack, 2, Map.of(Genes.WEREWOLF, Genes.Knowledge.TRUE, Genes.DOPPELGANGER, Genes.Knowledge.UNKNOWN),
                rites, people);
        return new HubView(v.player(), v.bloodline(), s, v.races(), HubModel.ladders(LadderTable.clientCopy(), s, v.races(), HubView.levels(v.player())), v.skills(), snap,
                v.context().withOp(true).withPack(true));
    }
}
