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

    private static ServerPlayer predator(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p)
                .setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-VAM-6")).build());
        return p;
    }

    private static int blood(ServerPlayer p) {
        return de.teamlapen.vampirism.api.VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
    }

    /** Predator's Thirst must not turn the empty bite of R33 (a desiccated Original cannot feed) back into blood. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void predatorsThirstDoesNotFeedADesiccatedBiter(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        ServerPlayer biter = predator(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(victim)
                .setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        de.teamlapen.vampirism.api.VampirismAPI.vampirePlayer(victim).drinkBlood(10, 0.1f, ResurrectionVervainGameTests.bitten(net.minecraft.world.entity.EntityType.VILLAGER.create(helper.getLevel())));
        int before = blood(victim);
        helper.assertTrue(ResurrectionVervainGameTests.bite(biter, victim).getAmount() == 10, "control: a plain biter did not get Vampirism's blood");
        biter.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, 600));
        int drained = blood(victim);
        var empty = ResurrectionVervainGameTests.bite(biter, victim);
        helper.assertTrue(empty.getAmount() == 0, "a desiccated biter was fed " + empty.getAmount());
        helper.assertTrue(blood(victim) == drained && before > drained, "a desiccated biter drained the victim");
        helper.succeed();
    }

    /** ...nor the empty bite on a vervain-protected victim (R94). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void predatorsThirstDoesNotFeedOnAVervainVictim(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        ServerPlayer biter = predator(helper);
        ServerPlayer victim = TestPlayers.create(helper);
        helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(victim)
                .setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        de.teamlapen.vampirism.api.VampirismAPI.vampirePlayer(victim).drinkBlood(10, 0.1f, ResurrectionVervainGameTests.bitten(net.minecraft.world.entity.EntityType.VILLAGER.create(helper.getLevel())));
        victim.addEffect(new MobEffectInstance(ArtifactItems.VERVAIN_EFFECT, 1200));
        int before = blood(victim);
        var empty = ResurrectionVervainGameTests.bite(biter, victim);
        helper.assertTrue(empty.getAmount() == 0, "a vervain victim fed the biter " + empty.getAmount());
        helper.assertTrue(blood(victim) == before, "a vervain victim was drained");
        helper.succeed();
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
