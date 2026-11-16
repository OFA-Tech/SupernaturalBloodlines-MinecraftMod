package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceUtils;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;

/**
 * Plan 13d M7: {@code undead_treatment_enabled=false} also stops Tensura's undead and holy-exposure answers for our apex races. They sit in Tensura's
 * {@code tensura:undead} race tag through a data file, and tags cannot carry load conditions in this NeoForge (checked: neither the file nor an entry
 * is evaluated), so the answers are switched in {@code RaceUtilsMixin} instead. A direct tag reader other than those two is not covered (documented).
 */
@PrefixGameTestTemplate(false)
public final class ApexUndeadSwitchGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ApexUndeadSwitchGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anApexRaceIsUndeadForTensuraOnlyWhileTheSwitchIsOn(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("supernaturalbloodlines:primordial_vampire"), false), "could not set the apex race");
        helper.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().is(RaceTags.UNDEAD), "setup: the apex race is not in tensura:undead");
        helper.assertTrue(RaceUtils.isUndead(p), "with the switch on the apex race is not undead for Tensura");
        SpeciesTensuraConfig.overrideUndeadTreatment(false);
        try {
            helper.assertTrue(!RaceUtils.isUndead(p), "undead_treatment_enabled=false still makes the apex race undead");
            helper.assertTrue(!RaceUtils.isAffectedByHolyExposure(p), "undead_treatment_enabled=false still exposes the apex race to holy damage");
            // not ours to touch: a Tensura race that is undead by its own tag stays so
            ServerPlayer ghoul = TestPlayers.create(helper);
            helper.assertTrue(RaceAPI.getRaceFrom(ghoul).setRace(ResourceLocation.parse("tensura:ghoul"), false), "could not set a ghoul");
            helper.assertTrue(RaceUtils.isUndead(ghoul), "the switch made a Tensura ghoul not undead");
        } finally {
            SpeciesTensuraConfig.overrideUndeadTreatment(null);
        }
        helper.succeed();
    }
}
