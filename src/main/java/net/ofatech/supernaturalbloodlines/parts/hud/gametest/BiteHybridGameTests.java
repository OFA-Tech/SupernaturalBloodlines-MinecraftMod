package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/** R110 row 3: a hybrid type in human form feeds its thirst through our own bite. Needs the species part and Werewolves. */
@PrefixGameTestTemplate(false)
public final class BiteHybridGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BiteHybridGameTests() {
    }

    private static ServerPlayer hybrid(GameTestHelper helper, Species species, int thirst) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).thirst(thirst).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        BiteGameTests.place(helper, p, 1.5);
        return p;
    }

    private static void clean(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridHumanFormFeedsThirst(GameTestHelper helper) {
        for (Species s : new Species[] {Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID}) {
            ServerPlayer p = hybrid(helper, s, 0);
            PathfinderMob v = BiteGameTests.villager(helper, 2.5);
            helper.assertTrue(BiteService.actionFor(p) == BiteAction.HYBRID_FEED, s + " bites with " + BiteService.actionFor(p));
            BiteService.Result r = BiteService.press(p, v.getId());
            helper.assertTrue(r.ok(), s + ": the feed was refused: " + r.refusal());
            helper.assertTrue(BloodlineAccess.get(p).thirst() == 5, s + ": thirst " + BloodlineAccess.get(p).thirst());
            helper.assertTrue(v.getMaxHealth() - v.getHealth() >= 1.99f, s + ": the villager took " + (v.getMaxHealth() - v.getHealth()));
            v.discard();
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridHumanFormPoisonsVampire(GameTestHelper helper) {
        ServerPlayer p = hybrid(helper, Species.HYBRID, 0);
        Mob vampire = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        vampire.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 1.5));
        vampire.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(vampire);
        BiteService.Result r = BiteService.press(p, vampire.getId());
        helper.assertTrue(r.ok(), "the feed on a vampire was refused: " + r.refusal());
        helper.assertTrue(vampire.hasEffect(SpeciesRegistries.HYBRID_VENOM), "the vampire was not poisoned (R42)");
        helper.assertTrue(BloodlineAccess.get(p).thirst() == 5, "thirst " + BloodlineAccess.get(p).thirst());
        vampire.discard();
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridFeedRefusedWhenFull(GameTestHelper helper) {
        ServerPlayer p = hybrid(helper, Species.HYBRID, 20);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("thirst_full"), "a full thirst: " + r);
        helper.assertTrue(v.getHealth() == v.getMaxHealth(), "a refused feed hurt the villager");
        v.discard();
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridFeedRefusesBloodless(GameTestHelper helper) {
        ServerPlayer p = hybrid(helper, Species.HYBRID, 0);
        PathfinderMob golem = (PathfinderMob) EntityType.IRON_GOLEM.create(helper.getLevel());
        golem.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 1.5));
        golem.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(golem);
        BiteService.Result r = BiteService.press(p, golem.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("no_blood"), "a golem: " + r);
        helper.assertTrue(BloodlineAccess.get(p).thirst() == 0 && golem.getHealth() == golem.getMaxHealth(), "a refused feed changed something");
        golem.discard();
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridFeedIsRateLimited(GameTestHelper helper) {
        ServerPlayer p = hybrid(helper, Species.HYBRID, 0);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(BiteService.press(p, v.getId()).ok(), "the first feed was refused");
        BiteService.Result second = BiteService.press(p, v.getId());
        helper.assertTrue(!second.ok() && second.refusal().endsWith("cooldown") && BloodlineAccess.get(p).thirst() == 5, "a second feed in the same tick: " + second);
        v.discard();
        clean(p);
        helper.succeed();
    }
}
