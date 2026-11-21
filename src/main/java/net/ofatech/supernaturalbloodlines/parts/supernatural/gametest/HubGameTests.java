package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import io.netty.buffer.Unpooled;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;

/** Phase 12e Task 1: the hub snapshot carries names and the player's own genes as their client may know them, nothing of anyone else. */
@PrefixGameTestTemplate(false)
public final class HubGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HubGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotNamesThePackForAlphaAndBeta(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !ModList.get().isLoaded("werewolves")) {
            helper.succeed(); // no pack without the species part and Werewolves: the other test covers the empty snapshot
            return;
        }
        ServerPlayer alpha = TestPlayers.create(helper);
        ServerPlayer beta = TestPlayers.create(helper);
        BloodlineState b = BloodlineAccess.get(beta);
        BloodlineAccess.set(beta, b.toBuilder().wolf(b.wolf().toBuilder().packAlpha(alpha.getUUID()).build()).build());
        try {
            // fake players are on no player list and in no profile cache, so names may be ids here; SpeciesHubGameTests checks the names
            for (ServerPlayer viewer : List.of(alpha, beta)) {
                List<HubSocial.Member> pack = HubSnapshotPayload.of(viewer).pack();
                helper.assertTrue(pack.stream().map(HubSocial.Member::rank).toList().equals(List.of("alpha", "beta")), "the pack seen: " + pack);
                helper.assertTrue(pack.stream().noneMatch(HubSocial.Member::online), "a fake player shows as online: " + pack);
            }
        } finally {
            BloodlineAccess.set(beta, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** The line of the player crosses the wire as it is: states, names, the freed kind and which ancestors are hidden. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotRoundTripsTheOrigin(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        var previous = HubSocial.source();
        try {
            for (HubSocial.Origin origin : List.of(HubSocial.Origin.NONE,
                    new HubSocial.Origin(LineRules.State.FREE, Optional.of("npc:klaus"), Optional.of("npc:klaus"), HubSocial.FreedKind.IMPRISONED, List.of(true, false)),
                    new HubSocial.Origin(LineRules.State.IN_LINE, Optional.empty(), Optional.empty(), HubSocial.FreedKind.NONE, List.of()))) {
                HubSocial.set(x -> new HubSocial.Social(Optional.empty(), List.of(), List.of(), 0, List.of("A", "B"), List.of(), List.of(), origin));
                HubSnapshotPayload payload = HubSnapshotPayload.of(p);
                helper.assertTrue(payload.family().origin().equals(origin), "the origin changed on the way into the payload: " + payload.family().origin());
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                HubSnapshotPayload.STREAM_CODEC.encode(buf, payload);
                helper.assertTrue(HubSnapshotPayload.STREAM_CODEC.decode(buf).equals(payload), "the origin does not survive the wire: " + origin);
            }
        } finally {
            HubSocial.set(previous);
        }
        helper.succeed();
    }

    /** The ids that let a click open a Profile ride only on people the snapshot already names, and survive the wire. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotCarriesIdsOnlyForNamedPeople(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        var previous = HubSocial.source();
        java.util.UUID sire = java.util.UUID.randomUUID();
        java.util.UUID mate = java.util.UUID.randomUUID();
        try {
            HubSocial.set(x -> new HubSocial.Social(Optional.of("Klaus"), List.of(), List.of(new HubSocial.Member("Derek", "beta", true, Optional.of(mate))), 0, List.of(), List.of(), List.of(),
                    HubSocial.Origin.NONE, List.of(new HubSocial.Person("Klaus", sire))));
            HubSnapshotPayload s = HubSnapshotPayload.of(p);
            helper.assertTrue(s.idOf("Klaus").equals(Optional.of(sire)) && s.idOf("Derek").equals(Optional.of(mate)), "the ids of the named people are missing");
            helper.assertTrue(s.idOf("Nobody Named").isEmpty(), "an id for a person the snapshot does not name");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            HubSnapshotPayload.STREAM_CODEC.encode(buf, s);
            helper.assertTrue(HubSnapshotPayload.STREAM_CODEC.decode(buf).equals(s), "the ids do not survive the wire");
        } finally {
            HubSocial.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotHasNoOtherPlayersAttachments(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        // the sire carries a secret werewolf gene; the child knows its own doppelgänger gene
        BloodlineState secret = BloodlineState.NONE.toBuilder()
                .wolf(BloodlineState.NONE.wolf().toBuilder().latentGene(true).geneRolled(true).build()).build();
        BloodlineAccess.set(sire, secret);
        BloodlineAccess.set(child, BloodlineState.NONE.toBuilder().sire(sire.getUUID()).doppelgangerGene(true).doppelgangerRolled(true)
                .revealedGenes(Set.of(Genes.DOPPELGANGER)).build());
        try {
            for (RecordComponent c : HubSnapshotPayload.class.getRecordComponents()) {
                helper.assertTrue(Set.of(HubSnapshotPayload.Family.class, boolean.class, List.class, int.class, Map.class,
                        net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook.Flags.class).contains(c.getType()), // Flags: the viewer's own two booleans
                        "the snapshot carries a " + c.getType().getSimpleName() + " (" + c.getName() + ")");
            }
            HubSnapshotPayload ofSire = HubSnapshotPayload.of(sire);
            HubSnapshotPayload ofChild = HubSnapshotPayload.of(child);
            for (String gene : Genes.ALL) {
                helper.assertTrue(ofSire.genes().get(gene) == Genes.Knowledge.UNKNOWN, "the sire's undiscovered " + gene + " gene was sent");
            }
            helper.assertTrue(ofChild.genes().get(Genes.DOPPELGANGER) == Genes.Knowledge.TRUE, "the child's own discovered gene is missing");
            helper.assertTrue(ofChild.genes().get(Genes.WEREWOLF) == Genes.Knowledge.UNKNOWN, "the child's werewolf gene was never discovered");
            if (EnabledParts.isEnabled("species")) {
                String childName = child.getGameProfile().getName();
                helper.assertTrue(ofSire.sired().equals(List.of(childName)), "sired " + ofSire.sired());
                // the sire's name is only known from a cache a GameTest server lacks (then it is an empty name, still a sire): SpeciesHubGameTests names a sire through SireRecorder
                helper.assertTrue(ofChild.sire().map(n -> n.isEmpty() || n.equals(sire.getGameProfile().getName())).orElse(false), "sire " + ofChild.sire());
            } else {
                helper.assertTrue(ofSire.equals(new HubSnapshotPayload(HubSnapshotPayload.Family.NONE, List.of(), 0, ofSire.genes(), List.of())),
                        "without the species part there is nobody to show: " + ofSire);
            }
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            HubSnapshotPayload.STREAM_CODEC.encode(buf, ofSire);
            helper.assertTrue(HubSnapshotPayload.STREAM_CODEC.decode(buf).equals(ofSire), "the snapshot does not survive the wire");
        } finally {
            BloodlineAccess.set(child, BloodlineState.NONE);
            BloodlineAccess.set(sire, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
