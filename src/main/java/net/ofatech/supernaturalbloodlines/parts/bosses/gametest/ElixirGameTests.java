package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.RingProbe;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;

/** The Elixir (plan 17d, R168): a second life from any death but a line death, /kill and the end of an Original, and the Resurrection Ring first. */
@PrefixGameTestTemplate(false)
public final class ElixirGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String USED = "supernaturalbloodlines.boss.elixir.used";

    private ElixirGameTests() {
    }

    private static LineTestPlayer holder(GameTestHelper helper, int elixirs) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        for (int i = 0; i < elixirs; i++) {
            p.getInventory().add(new ItemStack(BossItems.THE_ELIXIR.get()));
        }
        return p;
    }

    private static int elixirs(ServerPlayer p) {
        return p.getInventory().items.stream().filter(s -> s.is(BossItems.THE_ELIXIR.get())).mapToInt(ItemStack::getCount).sum();
    }

    /** Whether the Elixir (and nothing else) cancelled this death. */
    private static boolean saved(ServerPlayer p, DamageSource source) {
        LivingDeathEvent e = new LivingDeathEvent(p, source);
        NeoForge.EVENT_BUS.post(e);
        return e.isCanceled();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itBringsTheHolderBackFromLavaOnceAtTheConfiguredShareOfHealth(GameTestHelper helper) {
        LineTestPlayer p = holder(helper, 1);
        helper.assertTrue(saved(p, helper.getLevel().damageSources().lava()), "the Elixir did not save from lava");
        helper.assertTrue(elixirs(p) == 0, "the Elixir was not spent");
        helper.assertTrue(Math.abs(p.getHealth() - p.getMaxHealth() * BossesConfig.elixirHealFraction()) < 0.01, "health is " + p.getHealth());
        helper.assertTrue(p.received(USED), "no word of it: " + p.heard());
        helper.assertTrue(!saved(p, helper.getLevel().damageSources().lava()), "a second death was saved with nothing left");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itSpendsOnlyOneOfTwo(GameTestHelper helper) {
        LineTestPlayer p = holder(helper, 2);
        helper.assertTrue(saved(p, helper.getLevel().damageSources().fall()), "the Elixir did not save from a fall");
        helper.assertTrue(elixirs(p) == 1, "two Elixirs: " + elixirs(p) + " left, expected 1");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itDoesNotSaveFromALineDeathOrTheKillCommand(GameTestHelper helper) {
        LineTestPlayer p = holder(helper, 1);
        helper.assertTrue(!saved(p, helper.getLevel().damageSources().source(LineDeath.DAMAGE_TYPE)), "the Elixir saved from a line death");
        helper.assertTrue(!saved(p, helper.getLevel().damageSources().genericKill()), "the Elixir saved from /kill");
        helper.assertTrue(elixirs(p) == 1, "the Elixir was spent on a death it does not save");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itDoesNotKeepAnOriginalFromAWhiteOakEndButSavesAnotherFromTheSameBlow(GameTestHelper helper) {
        LineTestPlayer original = holder(helper, 1);
        LineTestPlayer human = holder(helper, 1);
        LineTestPlayer killer = LineTestPlayer.create(helper);
        try {
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            killer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
            DamageSource stake = killer.damageSources().playerAttack(killer);
            helper.assertTrue(!saved(original, stake), "the Elixir kept an Original from the white oak end that starts a line death");
            helper.assertTrue(elixirs(original) == 1, "the Elixir was spent on it");
            helper.assertTrue(saved(human, stake), "the Elixir did not save a human from a stake");
        } finally {
            BloodlineAccess.set(original, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itDoesNotUndoATurningOrATransitionOrTheVoid(GameTestHelper helper) {
        long now = helper.getLevel().getGameTime();
        DamageSource mob = helper.getLevel().damageSources().lava();
        LineTestPlayer p = holder(helper, 1);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(now + 1000).build());
            saved(p, mob); // the turning stands them up (it cancels the death itself): the Elixir must not be what did it
            helper.assertTrue(elixirs(p) == 1 && !p.received(USED), "the Elixir undid the death that turns someone who drank vampire blood");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.original.Downed.isDyingForTurning(p), "the turning did not start");
            net.ofatech.supernaturalbloodlines.parts.species.original.Downed.clearForTest(p);
            p.setHealth(p.getMaxHealth());
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(Math.max(1, now - 1)).build());
            helper.assertTrue(saved(p, mob), "an old, ended vampire blood window kept the Elixir from working");
            p.getInventory().add(new ItemStack(BossItems.THE_ELIXIR.get()));
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().inTransition(true).unstable(true).build());
            helper.assertTrue(!saved(p, mob), "the Elixir undid an unstable hybrid transition");
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().inTransition(true).unstable(false).build());
            helper.assertTrue(saved(p, mob), "a steady transition kept the Elixir from working");
            p.getInventory().add(new ItemStack(BossItems.THE_ELIXIR.get()));
            BloodlineAccess.set(p, BloodlineState.NONE);
            if (net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.ORIGINAL_BLOOD_EFFECT.isBound()) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.ORIGINAL_BLOOD_EFFECT, 1000));
                helper.assertTrue(!saved(p, mob), "the Elixir undid a death with Original blood in the veins");
                p.removeAllEffects();
            }
            helper.assertTrue(!saved(p, helper.getLevel().damageSources().fellOutOfWorld()), "the Elixir rescued someone from the void, to drop them again");
            helper.assertTrue(elixirs(p) >= 1, "an Elixir was spent on a death it does not save");
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeathTheResurrectionRingWouldSaveIsLeftToTheRing(GameTestHelper helper) {
        LineTestPlayer p = holder(helper, 1);
        try {
            RingProbe.pin((who, source) -> true);
            helper.assertTrue(!saved(p, helper.getLevel().damageSources().lava()), "the Elixir went before the ring");
            helper.assertTrue(elixirs(p) == 1, "the Elixir was spent although the ring would save");
            RingProbe.pin((who, source) -> false);
            helper.assertTrue(saved(p, helper.getLevel().damageSources().lava()), "with no ring the Elixir did not save");
        } finally {
            RingProbe.pin(null);
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRealResurrectionRingGoesBeforeTheElixirAndTheElixirTakesOverWhenTheRingIsEmpty(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("artifacts")) {
            helper.succeed(); // no ring in this run
            return;
        }
        ServerPlayer victim = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createMortal(helper);
        net.minecraft.world.entity.Mob vampire = helper.spawnWithNoFreeWill(
                (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Mob>) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                        net.minecraft.resources.ResourceLocation.parse("vampirism:vampire")), new net.minecraft.core.BlockPos(1, 1, 1));
        ItemStack ring = new ItemStack(net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems.RESURRECTION_RING.get());
        ring.set(net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems.CHARGES.get(), 1);
        victim.getInventory().items.set(5, ring);
        victim.getInventory().items.set(6, new ItemStack(BossItems.THE_ELIXIR.get()));
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().mobAttack(vampire), 1000f);
        helper.assertTrue(victim.isAlive() && elixirs(victim) == 1, "the ring did not go first: alive " + victim.isAlive() + ", Elixirs " + elixirs(victim));
        helper.assertTrue(ring.getOrDefault(net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems.CHARGES.get(), -1) == 0, "the ring was not spent");
        victim.invulnerableTime = 0;
        victim.setHealth(victim.getMaxHealth());
        victim.hurt(victim.damageSources().mobAttack(vampire), 1000f);
        helper.assertTrue(victim.isAlive() && elixirs(victim) == 0, "with the ring empty the Elixir did not take over: alive " + victim.isAlive() + ", Elixirs " + elixirs(victim));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutAnElixirNothingIsSaved(GameTestHelper helper) {
        LineTestPlayer p = holder(helper, 0);
        helper.assertTrue(!saved(p, helper.getLevel().damageSources().lava()), "something saved a player with no Elixir");
        helper.succeed();
    }
}