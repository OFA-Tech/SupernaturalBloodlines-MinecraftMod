package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.VervainRules;

/** Phase 12c Task 2, the skills that change a number of the artifacts part. */
@PrefixGameTestTemplate(false)
public final class ArtifactsSkillRuleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ArtifactsSkillRuleGameTests() {}

    /** Drinks the tea as the game does: the food gives the short effect, then the use is finished. */
    private static int drink(ServerPlayer p) {
        p.removeAllEffects();
        ItemStack tea = new ItemStack(ArtifactItems.VERVAIN_TEA.get());
        p.addEffect(new MobEffectInstance(ArtifactItems.VERVAIN_EFFECT, ArtifactItems.VERVAIN_TICKS));
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(p, tea, 0, ItemStack.EMPTY));
        return p.getEffect(ArtifactItems.VERVAIN_EFFECT).getDuration();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainBloodDoublesTheTea(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).build()); // the hunter tree lists S-HUM-2
        helper.assertTrue(VervainRules.teaTicks(p) == 12000, "base " + VervainRules.teaTicks(p));
        int plain = drink(p);
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-HUM-2")).build());
        int skilled = drink(p);
        helper.assertTrue(plain == 12000 && skilled == 24000, "the tea lasts " + plain + " ticks without the skill, " + skilled + " with it");
        helper.succeed();
    }
}
