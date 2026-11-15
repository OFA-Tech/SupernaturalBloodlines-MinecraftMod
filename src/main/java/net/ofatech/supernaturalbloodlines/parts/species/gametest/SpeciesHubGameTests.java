package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** Phase 12e: the hub's names from this part's indexes (the hook behind the hub snapshot). */
@PrefixGameTestTemplate(false)
public final class SpeciesHubGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SpeciesHubGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSnapshotNamesTheSireAndTheSired(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        helper.assertTrue(SireRecorder.record(sire, child), "the sire was not recorded");
        try {
            helper.assertTrue(HubSocial.of(child).sire().equals(Optional.of(sire.getGameProfile().getName())), "sire " + HubSocial.of(child).sire());
            helper.assertTrue(HubSocial.of(sire).sired().equals(List.of(child.getGameProfile().getName())), "sired " + HubSocial.of(sire).sired());
        } finally {
            BloodlineAccess.set(child, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFamilyTreeGoesUpAndDown(GameTestHelper helper) {
        ServerPlayer grand = TestPlayers.create(helper);
        ServerPlayer sire = TestPlayers.create(helper);
        ServerPlayer child = TestPlayers.create(helper);
        helper.assertTrue(SireRecorder.record(grand, sire) && SireRecorder.record(sire, child), "the line was not recorded");
        try {
            helper.assertTrue(HubSocial.of(child).ancestors().equals(List.of(grand.getGameProfile().getName())), "ancestors " + HubSocial.of(child).ancestors());
            List<HubSocial.Kin> line = HubSocial.of(grand).line();
            helper.assertTrue(line.equals(List.of(new HubSocial.Kin(sire.getGameProfile().getName(), 1), new HubSocial.Kin(child.getGameProfile().getName(), 2))),
                    "line " + line);
        } finally {
            BloodlineAccess.set(child, BloodlineState.NONE);
            BloodlineAccess.set(sire, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSnapshotListsTheLoadedRitesWithPowerAndItems(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<HubSocial.Rite> rites = HubSocial.of(p).rites();
        HubSocial.Rite revelation = rites.stream().filter(r -> r.id().equals(SupernaturalBloodlines.MODID + ":rite_of_revelation")).findFirst().orElse(null);
        helper.assertTrue(revelation != null, "no Rite of Revelation in " + rites.stream().map(HubSocial.Rite::id).toList());
        helper.assertTrue(revelation.power() == 1000 && !revelation.shapes().isEmpty(), "revelation " + revelation);
        helper.assertTrue(rites.stream().allMatch(r -> r.id().startsWith(SupernaturalBloodlines.MODID + ":")), "another mod's rite listed");
        HubSocial.Rite five = rites.stream().filter(r -> r.id().endsWith(":rite_of_the_five")).findFirst().orElse(null);
        helper.assertTrue(five != null && five.faction().equals("hunter") && five.factory().endsWith("faction_rite"), "the factory is not read: " + five);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEmissarySeesThePackTheyServe(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("werewolves")) {
            helper.succeed(); // no pack without Werewolves
            return;
        }
        ServerPlayer alpha = TestPlayers.create(helper);
        ServerPlayer beta = TestPlayers.create(helper);
        ServerPlayer emissary = TestPlayers.create(helper);
        BloodlineState b = BloodlineAccess.get(beta);
        BloodlineAccess.set(beta, b.toBuilder().wolf(b.wolf().toBuilder().packAlpha(alpha.getUUID()).build()).build());
        PackIndex index = PackIndex.get(helper.getLevel());
        index.setEmissary(alpha.getUUID(), emissary.getUUID());
        try {
            // names of fake players are ids here (no player list, no profile cache): the ranks and their order are the point
            List<String> ranks = HubSocial.of(emissary).pack().stream().map(HubSocial.Member::rank).toList();
            helper.assertTrue(ranks.equals(List.of("alpha", "beta", "emissary")), "the emissary sees " + ranks);
        } finally {
            index.setEmissary(alpha.getUUID(), null);
            BloodlineAccess.set(beta, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
