package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.LayerTable;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraSync;

/** The three COMMON keys that clients also read: the server's values win on a client until it leaves, and the wire carries exactly those three. */
@PrefixGameTestTemplate(false)
public final class SpeciesTensuraSyncGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SpeciesTensuraSyncGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pushedValuesWinUntilCleared(GameTestHelper helper) {
        LayerTable before = SpeciesTensuraConfig.table();
        boolean undead = SpeciesTensuraConfig.undeadTreatment();
        boolean apex = SpeciesTensuraConfig.apexEnabled();
        try {
            SpeciesTensuraConfig.applyPushed(List.of(), !undead, !apex);
            helper.assertTrue(SpeciesTensuraConfig.table() != before, "the pushed layers were not used");
            helper.assertTrue(SpeciesTensuraConfig.undeadTreatment() == !undead, "the pushed undead switch was not used");
            helper.assertTrue(SpeciesTensuraConfig.apexEnabled() == !apex, "the pushed apex switch was not used");
            helper.assertTrue(SpeciesTensuraConfig.localUndeadTreatment() == undead && SpeciesTensuraConfig.localApexEnabled() == apex,
                    "what a server sends its clients must be its own file, not what it was pushed");
        } finally {
            SpeciesTensuraConfig.clearPushed();
        }
        helper.assertTrue(SpeciesTensuraConfig.table() == before && SpeciesTensuraConfig.undeadTreatment() == undead && SpeciesTensuraConfig.apexEnabled() == apex,
                "leaving the world did not bring the local file back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSyncPayloadCarriesTheLocalValuesAndSurvivesTheWire(GameTestHelper helper) {
        SpeciesTensuraSync.Payload p = SpeciesTensuraSync.Payload.ofLocal();
        helper.assertTrue(p.layers().equals(SpeciesTensuraConfig.localLayers()) && p.undead() == SpeciesTensuraConfig.localUndeadTreatment()
                && p.apex() == SpeciesTensuraConfig.localApexEnabled(), "the payload is not the local file: " + p);
        helper.assertTrue(!p.layers().isEmpty(), "the default layers were not sent");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SpeciesTensuraSync.Payload.STREAM_CODEC.encode(buf, p);
        helper.assertTrue(SpeciesTensuraSync.Payload.STREAM_CODEC.decode(buf).equals(p), "the payload does not survive the wire");
        helper.succeed();
    }
}

