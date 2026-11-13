package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** R103 with a real apex race. Needs vampirism_tensura (FactionRaceRules), so the part registers it only when that part is on. */
@PrefixGameTestTemplate(false)
public final class ApexFactionGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String APEX = NS + ":primordial_vampire";

    private ApexFactionGameTests() {}

    private static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
    }

    private static boolean hunter(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.HUNTER_FACTION;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexRaceKeepsAnOriginalInTheHuntersButNotAPlainPlayer(GameTestHelper helper) {
        ServerPlayer original = TestPlayers.create(helper);
        species(original, Species.ORIGINAL);
        VampirismAPI.factionPlayerHandler(original).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        RaceAPI.getRaceFrom(original).setRace(ResourceLocation.parse(APEX), false);
        helper.assertTrue(APEX.equals(RaceAPI.getRaceFrom(original).getRace().orElseThrow().getRaceId().toString()), "the race did not change");
        helper.assertTrue(hunter(original), "R103: an Original on its apex race was ejected from the hunters");

        // the same undead race without the species is the ordinary R103 case
        ServerPlayer plain = TestPlayers.create(helper);
        VampirismAPI.factionPlayerHandler(plain).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        RaceAPI.getRaceFrom(plain).setRace(ResourceLocation.parse(APEX), false);
        helper.assertTrue(!hunter(plain), "control: a player with no species stayed a hunter on an undead apex race");

        // joining is judged the same way
        ServerPlayer joiner = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(joiner).setRace(ResourceLocation.parse(APEX), false);
        helper.assertTrue(!VampirismAPI.factionPlayerHandler(joiner).canJoin(VReference.HUNTER_FACTION), "control: undead race may join the hunters");
        species(joiner, Species.ORIGINAL);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(joiner).canJoin(VReference.HUNTER_FACTION), "R103: an Original on its apex race may not join the hunters");
        helper.succeed();
    }
}
