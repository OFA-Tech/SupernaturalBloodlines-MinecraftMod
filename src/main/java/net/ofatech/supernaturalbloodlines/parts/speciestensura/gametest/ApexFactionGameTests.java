package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexSpec;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceUtils;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;
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

    /** The siphoner's apex race is not undead, so none of the undead rules (R103, bite refusal) touch it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonerOnVoidSiphonIsNotUndeadNorBloodless(GameTestHelper helper) {
        ServerPlayer siphoner = TestPlayers.create(helper);
        species(siphoner, Species.SIPHONER);
        RaceAPI.getRaceFrom(siphoner).setRace(ResourceLocation.parse(NS + ":void_siphon"), false);
        helper.assertTrue(!RaceUtils.isUndead(siphoner), "a siphoner on void_siphon is undead");
        helper.assertTrue(!RaceUtils.isBloodless(siphoner), "a siphoner on void_siphon is bloodless (Tensura)");
        helper.assertTrue(!FactionRaceRules.isBloodless(siphoner), "a siphoner on void_siphon is bloodless (bite refusal)");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(siphoner).canJoin(VReference.HUNTER_FACTION), "a siphoner on void_siphon is refused by the hunters");

        // a heretic stays undead through the layer, not through the race
        ServerPlayer heretic = TestPlayers.create(helper);
        species(heretic, Species.HERETIC);
        RaceAPI.getRaceFrom(heretic).setRace(ResourceLocation.parse(NS + ":void_siphon"), false);
        helper.assertTrue(RaceUtils.isUndead(heretic), "a heretic is not undead through its layer");
        helper.assertTrue(!RaceUtils.isBloodless(heretic), "a heretic on void_siphon is bloodless");
        helper.succeed();
    }

    /** Decision 3: an undead apex race is bitten like anyone else, and stays undead (holy damage). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexRaceIsBitableButUndead(GameTestHelper helper) {
        ServerPlayer biter = TestPlayers.create(helper);
        VampirismAPI.factionPlayerHandler(biter).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        biter.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 1.5)), 0, 0);
        IVampirePlayer vampire = VampirismAPI.vampirePlayer(biter);

        // the test server has PvP off, and Vampirism refuses to bite a player then
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            int checked = 0;
            for (ApexSpec spec : ApexSpec.ALL) {
                if (!spec.undead()) {
                    continue;
                }
                checked++;
                ServerPlayer victim = TestPlayers.create(helper);
                victim.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 3.5)), 0, 0);
                victim.setYHeadRot(0);
                RaceAPI.getRaceFrom(victim).setRace(ResourceLocation.parse(NS + ":" + spec.id()), false);
                helper.assertTrue(RaceUtils.isUndead(victim), spec.id() + " is not undead");
                helper.assertTrue(!FactionRaceRules.isBloodless(victim), spec.id() + " is bloodless");
                helper.assertTrue(vampire.determineBiteType(victim) == IVampirePlayer.BITE_TYPE.SUCK_BLOOD_PLAYER,
                        spec.id() + " victim gave " + vampire.determineBiteType(victim));
            }
            helper.assertTrue(checked == 4, "vampire-type apex races: " + checked);
            // control: an undead race of Tensura's own is still bloodless
            ServerPlayer ghoul = TestPlayers.create(helper);
            ghoul.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 3.5)), 0, 0);
            ghoul.setYHeadRot(0);
            RaceAPI.getRaceFrom(ghoul).setRace(ResourceLocation.parse("tensura:ghoul"), false);
            helper.assertTrue(vampire.determineBiteType(ghoul) == IVampirePlayer.BITE_TYPE.NONE, "control: a ghoul can be bitten");
        } finally {
            server.setPvpAllowed(pvp);
        }
        helper.succeed();
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
