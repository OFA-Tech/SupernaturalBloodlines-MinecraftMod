package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;

/**
 * Staged werewolf venom (R206): mild, then sick, then death, with the threshold in the config. Calls {@link Venom#pulse} with the remaining time, so
 * it needs no Werewolves; how the venom is given is {@code HybridTraitsGameTests}' business.
 */
@PrefixGameTestTemplate(false)
public final class VenomStageGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    /** 100 ticks into the venom, and 4000 (the default threshold) in. */
    private static final int ELAPSED_100 = Venom.DURATION_TICKS - 100;
    private static final int ELAPSED_4000 = Venom.DURATION_TICKS - 4000;

    private VenomStageGameTests() {
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static boolean sick(net.minecraft.world.entity.LivingEntity e) {
        return e.hasEffect(MobEffects.CONFUSION) || e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mildStageOnlyHurts(GameTestHelper helper) {
        ServerPlayer victim = vampire(helper);
        float before = victim.getHealth();
        Venom.pulse(victim, ELAPSED_100);
        helper.assertTrue(before - victim.getHealth() > 0.5f && before - victim.getHealth() <= 1f, "no 1 damage in the mild stage: " + victim.getHealth());
        helper.assertTrue(!sick(victim), "the mild stage made them sick: " + victim.getActiveEffects());
        Venom.pulse(victim, ELAPSED_4000 + 1);
        helper.assertTrue(!sick(victim), "one tick before the threshold is still mild: " + victim.getActiveEffects());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sickStageAddsNauseaAndSlowness(GameTestHelper helper) {
        ServerPlayer victim = vampire(helper);
        float before = victim.getHealth();
        Venom.pulse(victim, ELAPSED_4000);
        helper.assertTrue(before - victim.getHealth() > 0.5f && before - victim.getHealth() <= 1f, "the sick stage lost its 1 damage: " + victim.getHealth());
        MobEffectInstance nausea = victim.getEffect(MobEffects.CONFUSION);
        MobEffectInstance slow = victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(nausea != null && nausea.getAmplifier() == 0 && nausea.getDuration() > 0 && nausea.getDuration() <= 60 && !nausea.isVisible(),
                "no hidden Nausea for 60 ticks: " + nausea);
        helper.assertTrue(slow != null && slow.getAmplifier() == 0 && slow.getDuration() > 0 && slow.getDuration() <= 60, "no Slowness I for 60 ticks: " + slow);
        // refreshed at every pulse: gone 60 ticks after the last one if nothing renews it, back at the next
        victim.removeAllEffects();
        Venom.pulse(victim, ELAPSED_4000 - 40);
        helper.assertTrue(sick(victim), "the next pulse did not refresh the sickness");
        // death is still death
        Venom.pulse(victim, 1);
        helper.assertTrue(victim.isDeadOrDying(), "survived the last pulse");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobFallsSickToo(GameTestHelper helper) {
        Mob cow = (Mob) helper.spawn(EntityType.COW, 1, 1, 1);
        Venom.pulse(cow, ELAPSED_100);
        helper.assertTrue(!sick(cow), "a mob was sick in the mild stage");
        Venom.pulse(cow, ELAPSED_4000);
        helper.assertTrue(cow.hasEffect(MobEffects.CONFUSION) && cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a mob was not sick: " + cow.getActiveEffects());
        cow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalsAreNeverSick(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer original = vampire(helper);
            try {
                BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(s).turnedAt(1).build());
                float before = original.getHealth();
                for (int remaining : new int[] {ELAPSED_100, ELAPSED_4000, 2, 1}) {
                    Venom.pulse(original, remaining);
                }
                helper.assertTrue(!sick(original), s + ": was sick: " + original.getActiveEffects());
                MobEffectInstance weak = original.getEffect(MobEffects.WEAKNESS);
                helper.assertTrue(weak != null && weak.getAmplifier() == 1, s + ": no Weakness II: " + weak);
                helper.assertTrue(original.getHealth() == before && !original.isDeadOrDying(), s + ": was hurt: " + before + " -> " + original.getHealth());
            } finally {
                reset(original);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void configMovesTheThreshold(GameTestHelper helper) {
        try {
            SpeciesConfig.overrideVenomSickAfterTicks(0);
            ServerPlayer early = vampire(helper);
            Venom.pulse(early, Venom.DURATION_TICKS);
            helper.assertTrue(sick(early), "0 is sick from the start");

            SpeciesConfig.overrideVenomSickAfterTicks(12000);
            ServerPlayer never = vampire(helper);
            Venom.pulse(never, 80);
            helper.assertTrue(!sick(never), "12000 is never sick");
            Venom.pulse(never, 1);
            helper.assertTrue(never.isDeadOrDying(), "12000 must still kill at the end");

            SpeciesConfig.overrideVenomSickAfterTicks(6000);
            ServerPlayer middle = vampire(helper);
            Venom.pulse(middle, Venom.DURATION_TICKS - 5999);
            helper.assertTrue(!sick(middle), "one tick before 6000 is still mild");
            Venom.pulse(middle, Venom.DURATION_TICKS - 6000);
            helper.assertTrue(sick(middle), "6000 is sick");
        } finally {
            SpeciesConfig.overrideVenomSickAfterTicks(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridBloodStillCuresAtAnyStage(GameTestHelper helper) {
        for (int remaining : new int[] {ELAPSED_100, ELAPSED_4000, 80}) {
            ServerPlayer p = vampire(helper);
            p.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, remaining));
            Venom.pulse(p, remaining);
            ItemStack blood = new ItemStack(SpeciesRegistries.HYBRID_BLOOD.get());
            helper.assertTrue(blood.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), remaining + ": cannot drink Hybrid's Blood");
            p.setItemInHand(InteractionHand.MAIN_HAND, blood.finishUsingItem(helper.getLevel(), p));
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.HYBRID_VENOM), remaining + ": Hybrid's Blood did not cure it");
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), remaining + ": no bottle back");
        }
        helper.succeed();
    }

    /** A player who lies snapped is already out: no Nausea over the screen, and the damage cannot reach them; the end of the venom still does. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSnappedVampireIsSparedTheSicknessButNotTheEnd(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        try {
            Downed.startSnapped(p, 600);
            helper.assertTrue(Downed.isSnapped(p), "setup: not snapped");
            Venom.pulse(p, ELAPSED_4000);
            helper.assertTrue(!sick(p), "a snapped player was made sick: " + p.getActiveEffects());
            Venom.pulse(p, 1);
            helper.assertTrue(p.isDeadOrDying(), "the venom did not kill a snapped vampire");
            helper.assertTrue(!Downed.isDowned(p), "the snapped state outlived the player");
        } finally {
            Downed.clearForTest(p);
        }
        helper.succeed();
    }

    /** Vampirism's dying state blocks every damage event: the venom's last pulse must still end it, and leave nothing set on the dead. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireInVampirismsDyingStateDiesOfTheVenom(GameTestHelper helper) {
        ServerPlayer victim = vampire(helper);
        ServerPlayer attacker = TestPlayers.createDuelist(helper);
        MinecraftServer server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        try {
            server.setPvpAllowed(true);
            victim.hurt(attacker.damageSources().playerAttack(attacker), 1000f);
            helper.assertTrue(VampirismAPI.vampirePlayer(victim).isDBNO() && !victim.isDeadOrDying(), "setup: not in Vampirism's dying state");
            Venom.pulse(victim, ELAPSED_4000);
            helper.assertTrue(!sick(victim), "a dying player was made sick: " + victim.getActiveEffects());
            helper.assertTrue(!victim.isDeadOrDying(), "a mild or sick pulse killed a dying player");
            Venom.pulse(victim, 1);
            helper.assertTrue(victim.isDeadOrDying(), "the venom did not end Vampirism's dying state");
            helper.assertTrue(!VampirismAPI.vampirePlayer(victim).isDBNO(), "the dying state was left set on the dead");
        } finally {
            server.setPvpAllowed(pvp);
        }
        helper.succeed();
    }
}
