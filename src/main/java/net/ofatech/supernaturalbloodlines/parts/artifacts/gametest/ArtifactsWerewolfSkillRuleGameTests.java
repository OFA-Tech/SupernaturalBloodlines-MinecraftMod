package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains;

/** Phase 12c Task 2, Wolfsbane Tolerance (S-WOL-4); it links Werewolves, so it is registered only when that mod is loaded. */
@PrefixGameTestTemplate(false)
public final class ArtifactsWerewolfSkillRuleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ArtifactsWerewolfSkillRuleGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wolfsbaneToleranceHalvesTheCommonStrainOnly(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        WolfsbaneStrains.apply(p, WolfsbaneStrains.Strain.COMMON);
        int plain = p.getEffect(MobEffects.WEAKNESS).getDuration();
        p.removeAllEffects();
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-WOL-4")).build());
        WolfsbaneStrains.apply(p, WolfsbaneStrains.Strain.COMMON);
        int skilled = p.getEffect(MobEffects.WEAKNESS).getDuration();
        int confusion = p.getEffect(MobEffects.CONFUSION).getDuration();
        helper.assertTrue(plain == 600 && skilled == 300 && confusion == 300, "weakness " + plain + " without the skill, " + skilled + " with it, nausea " + confusion);
        p.removeAllEffects();
        WolfsbaneStrains.apply(p, WolfsbaneStrains.Strain.YELLOW);
        helper.assertTrue(p.getEffect(WolfsbaneStrains.YELLOW_EFFECT).getDuration() == 1200, "another strain changed");
        helper.succeed();
    }
}