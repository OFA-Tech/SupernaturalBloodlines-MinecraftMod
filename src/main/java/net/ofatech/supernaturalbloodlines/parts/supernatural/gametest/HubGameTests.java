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
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.HubSnapshotPayload;

/** Phase 12e Task 1: the hub snapshot carries names and the player's own genes as their client may know them, nothing of anyone else. */
@PrefixGameTestTemplate(false)
public final class HubGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HubGameTests() {}

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
                helper.assertTrue(Set.of(Optional.class, boolean.class, List.class, int.class, Map.class).contains(c.getType()),
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
                helper.assertTrue(ofChild.sire().map(n -> n.equals(sire.getGameProfile().getName())).orElse(true), "sire " + ofChild.sire());
            } else {
                helper.assertTrue(ofSire.equals(new HubSnapshotPayload(Optional.empty(), false, List.of(), List.of(), 0, ofSire.genes())),
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
