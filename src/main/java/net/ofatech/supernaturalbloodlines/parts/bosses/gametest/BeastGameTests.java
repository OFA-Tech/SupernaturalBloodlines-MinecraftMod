package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhase;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.PikeRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastRoar;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanHostEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.GevaudanSpiritEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.MantleEvents;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.PikeRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * The Beast of Gevaudan (plan 17h, R184 to R191): the Host that is made once per qualifying night and is protected, the change, the collapse (health or mountain
 * ash), the Spirit that only the pike hurts, the one return and the end, the Roar and who it spares, the body (regeneration, shots, wolfsbane, the stun), the Mantle
 * and the reward. The moon, the mods and the dice are pinned. None of it needs Werewolves except the Mantle's shifted check.
 */
@PrefixGameTestTemplate(false)
public final class BeastGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BeastGameTests() {
    }

    private static boolean werewolves() {
        return ModList.get().isLoaded("werewolves");
    }

    private static LineTestPlayer player(GameTestHelper helper, Rank rank, Species species, double x) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 2.5)));
        WolfState w = WolfState.NONE.toBuilder().rank(rank).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).wolf(w).build());
        return p;
    }

    private static void clean(LineTestPlayer... ps) {
        SireCall.overridePlayerLookup(null);
        for (LineTestPlayer p : ps) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    /** The mods there, the moon as given, the dice passing, the ground fixed, and these players the only ones in the overworld. */
    private static void world(boolean moon, ServerPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMoon(l -> moon);
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
    }

    private static ServerLevel overworld(BossTestRig rig) {
        return rig.server.overworld();
    }

    private static long day(BossTestRig rig) {
        return DesiccationRules.dayOf(overworld(rig).getDayTime());
    }

    private static BlockPos spot(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(4, 1, 4));
    }

    private static BossIndex.Entry entry(BossTestRig rig) {
        return rig.index.of(BossId.BEAST_OF_GEVAUDAN);
    }

    private static BossNpc body(BossTestRig rig) {
        return BossDirector.anyBossBody(rig.server, BossId.BEAST_OF_GEVAUDAN);
    }

    private static GevaudanHostEntity host(GameTestHelper helper) {
        return BeastDirector.spawnHost(helper.getLevel(), null, spot(helper)).orElseThrow();
    }

    /** The Host, changed into the Beast the way the director does it. */
    private static BeastEntity beast(GameTestHelper helper, BossTestRig rig) {
        host(helper);
        helper.assertTrue(BeastDirector.change(helper.getLevel()), "the Host did not change");
        return (BeastEntity) body(rig);
    }

    private static void hit(LivingEntity e, DamageSource by, float amount) {
        e.invulnerableTime = 0;
        e.hurt(by, amount);
    }

    private static void run(net.minecraft.world.entity.Mob e, int ticks) {
        for (int i = 0; i < ticks && !e.isRemoved(); i++) {
            e.tickCount++;
            e.tick();
        }
    }

    private static long itemsNear(GameTestHelper helper, net.minecraft.world.entity.Entity e, net.minecraft.world.item.Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(8.0), i -> i.getItem().is(item)).size();
    }

    private static void sweep(GameTestHelper helper, BossTestRig rig) {
        BeastDirector.clear(rig.server);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30)).forEach(net.minecraft.world.entity.Entity::discard);
        helper.getLevel().getEntitiesOfClass(BossNpc.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30)).forEach(net.minecraft.world.entity.Entity::discard);
        helper.getLevel().getEntitiesOfClass(GevaudanSpiritEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(30)).forEach(net.minecraft.world.entity.Entity::discard);
    }

    private static ItemStack with(net.minecraft.world.item.Item item) {
        return new ItemStack(item);
    }

    // ---- the Host ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostIsMadeOnceOnAFullMoonNightForAnAlphaOrAHunterAndNeverForAPlainPlayerOrByDay(GameTestHelper helper) {
        LineTestPlayer plain = player(helper, Rank.OMEGA, Species.NONE, 2.5);
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 3.5);
        LineTestPlayer hunter = player(helper, Rank.NONE, Species.HUNTER_OF_THE_FIVE, 4.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("beast_host_chance", 1.0);
            world(true, plain);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.NONE && body(rig) == null, "a Host came for a plain player");
            world(false, alpha);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.NONE, "a Host came by day");
            world(true, alpha);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST && entry(rig).hostsMade == 1 && body(rig) instanceof GevaudanHostEntity, "no Host came for an Alpha: " + entry(rig).phase);
            GevaudanHostEntity h = (GevaudanHostEntity) body(rig);
            helper.assertTrue(h.getItemBySlot(EquipmentSlot.HEAD).is(BossItems.SEBASTIEN_TRICORN.get()), "the Host does not wear the tricorn");
            helper.assertTrue(rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.ACTIVE && h.getUUID().equals(entry(rig).entity), "the index does not hold the Host");
            helper.assertTrue(alpha.count(BeastDirector.HOST_NOTICE) == 1 && alpha.count(BeastDirector.HOST_HINT) == 1, "the Alpha was not told exactly once: " + alpha.heard());
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).hostsMade == 1, "a second Host came the same night");
            sweep(helper, rig);
            // a Hunter of the Five calls it too
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            world(true, hunter);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST, "a Hunter of the Five did not call the Host");
            sweep(helper, rig);
        } finally {
            clean(plain, alpha, hunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostIsProtectedAndItsDeathFailsTheNightAndAHostIsMadeAgainOnce(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            GevaudanHostEntity h = host(helper);
            LineTestPlayer striker = LineTestPlayer.create(helper);
            float before = h.getHealth();
            hit(h, striker.damageSources().playerAttack(striker), 50F);
            hit(h, helper.getLevel().damageSources().magic(), 50F);
            hit(h, helper.getLevel().damageSources().explosion(null, null), 50F);
            helper.assertTrue(h.isAlive() && h.getHealth() == before, "the Host was hurt by an ordinary blow: " + h.getHealth());
            hit(h, helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(!h.isAlive(), "/kill did not kill the Host");
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.LOST, "the encounter is " + entry(rig).phase);
            GevaudanHostEntity second = host(helper);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST && entry(rig).hostsMade == 2, "a lost Host was not made again once");
            hit(second, helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED, "the second Host's death did not fail the night");
            helper.assertTrue(BeastDirector.spawnHost(helper.getLevel(), null, spot(helper)).isEmpty(), "a third Host was made");
            sweep(helper, rig);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostBecomesTheBeastOnALaterFullMoonNightAtFullHealthAndAtHalfTheSecondTime(GameTestHelper helper) {
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true, alpha);
            GevaudanHostEntity h = host(helper);
            // the night the Host was made is not a later one
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST && body(rig) == h, "the Host changed on the night it was made");
            entry(rig).phaseDay = day(rig) - 1;
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST && body(rig) instanceof BeastEntity && !h.isAlive(), "the Host did not become the Beast on a later night: " + entry(rig).phase);
            BeastEntity b = (BeastEntity) body(rig);
            helper.assertTrue(b.getUUID().equals(entry(rig).entity) && b.getHealth() == b.getMaxHealth(), "the index or the health is wrong: " + b.getHealth());
            sweep(helper, rig);
            // the returning Beast starts at half
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            host(helper);
            entry(rig).returned = true;
            entry(rig).phase = BeastPhase.RETURN_PENDING;
            helper.assertTrue(BeastDirector.change(helper.getLevel()), "a returning Host did not change");
            BeastEntity again = (BeastEntity) body(rig);
            helper.assertTrue(Math.abs(again.getHealth() - again.getMaxHealth() * 0.5F) < 0.01F, "the returning Beast has " + again.getHealth() + " of " + again.getMaxHealth());
            sweep(helper, rig);
            // a Host that is not there cannot change
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            entry(rig).phase = BeastPhase.HOST;
            entry(rig).entity = UUID.randomUUID();
            helper.assertTrue(!BeastDirector.change(helper.getLevel()), "a Host that is not loaded changed");
        } finally {
            clean(alpha);
        }
        helper.succeed();
    }

    // ---- the Beast ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastCollapsesAtAQuarterOfItsHealthNotBeforeAndLeavesTheHostAndTheSpirit(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity b = beast(helper, rig);
            b.setHealth(b.getMaxHealth() * 0.26F);
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST && !b.collapsing(), "it collapsed above a quarter");
            b.setHealth(b.getMaxHealth() * 0.25F);
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.SPIRIT && b.collapsing(), "it did not collapse at a quarter: " + entry(rig).phase);
            helper.assertTrue(body(rig) instanceof GevaudanHostEntity, "the index does not hold the Host that stood up");
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            helper.assertTrue(spirit != null && spirit.isAlive(), "there is no Spirit");
            helper.assertTrue(entry(rig).spiritUntil == helper.getLevel().getGameTime() + BossesConfig.beastSpiritSeconds() * 20L, "the Spirit's time is " + entry(rig).spiritUntil);
            helper.assertTrue(!b.isInvulnerable() && b.isInvulnerableTo(helper.getLevel().damageSources().magic()), "the falling Beast can still be hurt");
            // a body that is falling is not the index's: it is not discarded by the index, and it is gone when the collapse is over
            run(b, 3);
            helper.assertTrue(b.isAlive(), "the falling Beast was discarded at once");
            run(b, BeastEntity.COLLAPSE_TICKS + 5);
            helper.assertTrue(b.isRemoved(), "the fallen Beast stayed");
            sweep(helper, rig);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBeastInAClosedRingOfMountainAshCollapsesAtAnyHealthUnlessTheSettingIsOff(GameTestHelper helper) {
        Block ash = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(NS, "mountain_ash_line"));
        if (ash == net.minecraft.world.level.block.Blocks.AIR) {
            helper.succeed();
            return;
        }
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity b = beast(helper, rig);
            b.tickCount = 0;
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST, "it collapsed with no ring");
            BlockPos at = b.blockPosition();
            for (int d = -3; d <= 3; d++) {
                for (BlockPos p : new BlockPos[] {at.offset(d, 0, -3), at.offset(d, 0, 3), at.offset(-3, 0, d), at.offset(3, 0, d)}) {
                    helper.getLevel().setBlockAndUpdate(p, ash.defaultBlockState());
                }
            }
            BossesConfig.override("beast_respects_ash", false);
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST, "the ring held it with the setting off");
            BossesConfig.override("beast_respects_ash", true);
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.SPIRIT && b.getHealth() == b.getMaxHealth(), "a closed ring did not collapse it at full health: " + entry(rig).phase);
            for (int d = -3; d <= 3; d++) {
                for (BlockPos p : new BlockPos[] {at.offset(d, 0, -3), at.offset(d, 0, 3), at.offset(-3, 0, d), at.offset(3, 0, d)}) {
                    helper.getLevel().setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
            sweep(helper, rig);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastNeverDiesOfBlowsAndTheVoidOrKillMakesItCollapseInstead(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity b = beast(helper, rig);
            hit(b, helper.getLevel().damageSources().magic(), 1_000_000F);
            helper.assertTrue(b.isAlive() && b.getHealth() >= 1F && b.getHealth() < 2F, "a blow killed it or left it at " + b.getHealth());
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.SPIRIT, "a Beast at 1 health did not collapse");
            sweep(helper, rig);
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            BeastEntity c = beast(helper, rig);
            hit(c, helper.getLevel().damageSources().genericKill(), 1_000_000F);
            helper.assertTrue(c.isAlive() && entry(rig).phase == BeastPhase.SPIRIT && c.collapsing(), "/kill ended the Beast instead of making it collapse: " + entry(rig).phase);
            sweep(helper, rig);
        }
        helper.succeed();
    }

    // ---- the Spirit, the pike, the reward ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheValetPikeHurtsTheSpiritAndItsBlowEndsTheBeastAndPaysTheMantleOnceToTheOneWhoDidTheMostNotWithTheBlow(GameTestHelper helper) {
        LineTestPlayer a = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        LineTestPlayer b = player(helper, Rank.ALPHA, Species.NONE, 3.5);
        LineTestPlayer striker = player(helper, Rank.NONE, Species.NONE, 4.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, a, b, striker);
            BeastEntity beast = beast(helper, rig);
            hit(beast, a.damageSources().playerAttack(a), 40F);
            hit(beast, b.damageSources().playerAttack(b), 90F);
            hit(beast, helper.getLevel().damageSources().magic(), 400F);
            helper.assertTrue(entry(rig).tally.top().map(b.getUUID()::equals).orElse(false), "the tally says " + entry(rig).tally.entries());
            beast.setHealth(beast.getMaxHealth() * 0.2F);
            BeastDirector.watch(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            float before = spirit.getHealth();
            striker.setItemInHand(InteractionHand.MAIN_HAND, with(Items.DIAMOND_SWORD));
            hit(spirit, striker.damageSources().playerAttack(striker), 500F);
            hit(spirit, helper.getLevel().damageSources().magic(), 500F);
            helper.assertTrue(spirit.isAlive() && spirit.getHealth() == before && entry(rig).phase == BeastPhase.SPIRIT, "a blow that was no pike hurt the Spirit");
            striker.setItemInHand(InteractionHand.MAIN_HAND, with(BossItems.VALET_PIKE.get()));
            hit(spirit, striker.damageSources().playerAttack(striker), 1F);
            helper.assertTrue(!spirit.isAlive() && entry(rig).phase == BeastPhase.KILLED && rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.DEAD, "the pike did not end it: " + entry(rig).phase);
            helper.assertTrue(entry(rig).rewarded, "the reward was not marked");
            helper.assertTrue(b.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1 && a.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0
                    && striker.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0, "the Mantle did not go to the one who did the most, once");
            helper.assertTrue(b.received(BeastDirector.REWARD), "the one paid was not told: " + b.heard());
            helper.assertTrue(PackAccess.wolf(b).sparks() == 0 && itemsNear(helper, spirit, BossItems.VALET_PIKE.get()) == 0, "a spark or a pike was paid");
            helper.assertTrue(!BeastDirector.pay(helper.getLevel(), striker, spirit.blockPosition()) && b.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1, "it paid twice");
            helper.assertTrue(!(helper.getLevel().getEntity(entry(rig).entity) instanceof GevaudanHostEntity), "the Host was not let go");
            sweep(helper, rig);
        } finally {
            clean(a, b, striker);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withNobodyInTheTallyOrOnlineTheMantleFallsWhereTheSpiritFellAndEveryoneNearIsTold(GameTestHelper helper) {
        LineTestPlayer near = player(helper, Rank.NONE, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, near);
            BeastEntity beast = beast(helper, rig);
            beast.setHealth(1F);
            BeastDirector.watch(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            spirit.markPikeHit();
            hit(spirit, helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(entry(rig).phase == BeastPhase.KILLED && itemsNear(helper, spirit, BossItems.MANTLE_OF_THE_BEAST.get()) == 1, "the Mantle is not where the Spirit fell");
            helper.assertTrue(near.received(BeastDirector.REWARD_DROPPED), "nobody near was told: " + near.heard());
            ItemEntity drop = helper.getLevel().getEntitiesOfClass(ItemEntity.class, spirit.getBoundingBox().inflate(8.0), i -> i.getItem().is(BossItems.MANTLE_OF_THE_BEAST.get())).get(0);
            helper.assertTrue(drop.isInvulnerable() && drop.getAge() < 0, "the Mantle can burn or despawn");
            sweep(helper, rig);
        } finally {
            clean(near);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritThatEscapesComesBackOnceAtHalfHealthThenIsGoneForGoodAndTheSettingMakesTheFirstFinal(GameTestHelper helper) {
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true, alpha);
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            // its own tick lets it go when the time is up
            entry(rig).spiritUntil = helper.getLevel().getGameTime();
            spirit.tickCount = 19;
            run(spirit, 1);
            helper.assertTrue(spirit.isRemoved() && entry(rig).phase == BeastPhase.RETURN_PENDING && entry(rig).returned, "the first escape is " + entry(rig).phase);
            helper.assertTrue(body(rig) instanceof GevaudanHostEntity && body(rig).isAlive(), "the Host did not wait");
            helper.assertTrue(rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.ACTIVE, "the index says " + rig.index.state(BossId.BEAST_OF_GEVAUDAN));
            // the night of the escape is not a later one; a later one brings it back at half
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.RETURN_PENDING, "it came back the night it escaped");
            entry(rig).phaseDay = day(rig) - 1;
            BossDirector.check(rig.server);
            BeastEntity again = body(rig) instanceof BeastEntity e ? e : null;
            helper.assertTrue(again != null && entry(rig).phase == BeastPhase.BEAST && Math.abs(again.getHealth() - again.getMaxHealth() * 0.5F) < 0.01F, "it did not come back at half health");
            // the second escape is for good: the Host goes, the index is final, nothing is paid
            BeastDirector.collapse(helper.getLevel(), again);
            GevaudanSpiritEntity second = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            BeastDirector.escape(helper.getLevel(), second);
            helper.assertTrue(entry(rig).phase == BeastPhase.GONE && rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.DEAD && !entry(rig).rewarded, "the second escape is " + entry(rig).phase);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(GevaudanHostEntity.class, new AABB(spot(helper)).inflate(30)).isEmpty(), "the Host stayed");
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.GONE && body(rig) == null, "something came after the end");
            sweep(helper, rig);
            // with the setting off the first escape is final
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            BossesConfig.override("beast_returns_once", false);
            BeastEntity third = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), third);
            BeastDirector.escape(helper.getLevel(), (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit));
            helper.assertTrue(entry(rig).phase == BeastPhase.GONE, "it returned with the setting off: " + entry(rig).phase);
            sweep(helper, rig);
        } finally {
            clean(alpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHostThatDiesWhileTheSpiritRunsFailsTheEncounterAndTheSpiritGoes(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            hit(body(rig), helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && spirit.isRemoved() && entry(rig).spirit == null, "the Spirit stayed after the Host died: " + entry(rig).phase);
            sweep(helper, rig);
        }
        helper.succeed();
    }

    // ---- the Roar ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarShakesEveryoneButAlphasAndTrueAlphasInRangeAndAMobThatBelongsToNoPackRuns(GameTestHelper helper) {
        LineTestPlayer omega = player(helper, Rank.OMEGA, Species.NONE, 2.5);
        LineTestPlayer beta = player(helper, Rank.BETA, Species.NONE, 3.5);
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 4.5);
        LineTestPlayer truth = player(helper, Rank.TRUE_ALPHA, Species.NONE, 5.5);
        LineTestPlayer far = player(helper, Rank.OMEGA, Species.NONE, 14.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, omega, beta, alpha, truth, far);
            BossesConfig.override("beast_roar_range", 8);
            BeastEntity beast = beast(helper, rig);
            beast.setPos(helper.absoluteVec(new Vec3(3.5, 1, 3.5)));
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
            zombie.setTarget(omega);
            int shaken = BeastRoar.let(helper.getLevel(), beast);
            helper.assertTrue(omega.hasEffect(MobEffects.WEAKNESS) && omega.getEffect(MobEffects.WEAKNESS).getAmplifier() == 1 && omega.hasEffect(MobEffects.DARKNESS), "an Omega was not shaken");
            helper.assertTrue(beta.hasEffect(MobEffects.WEAKNESS), "a Beta was not shaken");
            helper.assertTrue(!alpha.hasEffect(MobEffects.WEAKNESS) && !truth.hasEffect(MobEffects.WEAKNESS), "an Alpha or a True Alpha was shaken");
            helper.assertTrue(alpha.received(BeastRoar.STOOD) && truth.received(BeastRoar.STOOD) && omega.received(BeastRoar.SHAKEN) && !alpha.received(BeastRoar.SHAKEN), "the notices are wrong: " + alpha.heard() + " / " + omega.heard());
            helper.assertTrue(!far.hasEffect(MobEffects.WEAKNESS), "somebody out of range was shaken");
            helper.assertTrue(zombie.getTarget() == null, "a mob of no pack kept its target");
            helper.assertTrue(shaken == 3, "the Roar shook " + shaken + " instead of an Omega, a Beta and a zombie");
            helper.assertTrue(omega.getEffect(MobEffects.WEAKNESS).getDuration() == BeastRules.fearTicks(BossesConfig.beastRoarSeconds()), "the fear is " + omega.getEffect(MobEffects.WEAKNESS).getDuration() + " ticks");
            zombie.discard();
            sweep(helper, rig);
        } finally {
            clean(omega, beta, alpha, truth, far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastRearsUpWhenItHasATargetInRangeStandsStillWhileItRoarsAndWaitsItsCooldown(GameTestHelper helper) {
        LineTestPlayer victim = player(helper, Rank.OMEGA, Species.NONE, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, victim);
            BeastEntity beast = beast(helper, rig);
            beast.setPos(helper.absoluteVec(new Vec3(5.5, 1, 4.5)));
            beast.setTarget(victim);
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(beast.roarReady(now), "a new Beast waits to roar");
            beast.roarNow(now);
            helper.assertTrue(beast.roarTicks() == 1 && !beast.roarReady(now + 1), "the Roar did not begin, or has no cooldown");
            for (int i = 0; i < BeastRules.ROAR_WINDUP_TICKS - 1; i++) {
                beast.tickCount++;
                beast.tick();
            }
            helper.assertTrue(!victim.hasEffect(MobEffects.WEAKNESS), "the fear landed before the wind-up was over");
            beast.tickCount++;
            beast.tick();
            helper.assertTrue(victim.hasEffect(MobEffects.WEAKNESS), "the fear did not land when the wind-up ended");
            helper.assertTrue(beast.roarReady(now + BossesConfig.beastRoarCooldownSeconds() * 20L) && !beast.roarReady(now + BossesConfig.beastRoarCooldownSeconds() * 20L - 1), "the cooldown is not the setting");
            sweep(helper, rig);
        } finally {
            clean(victim);
        }
        helper.succeed();
    }

    // ---- the body ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastHealsUnlessWolfsbaneIsOnItTakesLessFromShotsAndAPulseOfWolfsbaneTakesAHeartAtMost(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity b = beast(helper, rig);
            b.setHealth(b.getMaxHealth() * 0.5F);
            float start = b.getHealth();
            b.tickCount = 9;
            run(b, 1);
            helper.assertTrue(b.getHealth() > start && b.getHealth() <= start + 1.01F, "it did not heal about a heart a period: " + (b.getHealth() - start));
            // the common strain's Weakness with Nausea: no healing
            b.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            b.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
            helper.assertTrue(b.underWolfsbane(), "Weakness with Nausea is not a wolfsbane strain");
            float held = b.getHealth();
            b.tickCount = 19;
            run(b, 1);
            helper.assertTrue(b.getHealth() == held, "it healed under wolfsbane");
            b.removeAllEffects();
            // a shot does 0.6, a blow all
            Arrow arrow = EntityType.ARROW.create(helper.getLevel());
            float before = b.getHealth();
            hit(b, helper.getLevel().damageSources().arrow(arrow, null), 10F);
            helper.assertTrue(Math.abs(before - b.getHealth() - 6.0F) < 0.01F, "a shot of 10 did " + (before - b.getHealth()));
            before = b.getHealth();
            hit(b, helper.getLevel().damageSources().magic(), 10F);
            helper.assertTrue(Math.abs(before - b.getHealth() - 10.0F) < 0.01F, "a blow of 10 did " + (before - b.getHealth()));
            // a pulse of wolfsbane poison takes a heart
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NS, "wolfsbane_poison"));
            DamageSource poison = new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
            before = b.getHealth();
            hit(b, poison, 500F);
            helper.assertTrue(Math.abs(before - b.getHealth() - 1.0F) < 0.01F, "a pulse of poison did " + (before - b.getHealth()));
            sweep(helper, rig);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHearingDevicesStunLastsASecondAtMostOnTheBeast(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            BeastEntity b = beast(helper, rig);
            b.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
            b.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
            b.tickCount = 9;
            run(b, 1);
            helper.assertTrue(b.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() <= BeastRules.STUN_CAP_TICKS && b.getEffect(MobEffects.CONFUSION).getDuration() <= BeastRules.STUN_CAP_TICKS,
                    "the stun is " + b.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() + " ticks");
            // another slowness is left alone
            b.removeAllEffects();
            b.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            b.tickCount = 19;
            run(b, 1);
            helper.assertTrue(b.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() > BeastRules.STUN_CAP_TICKS, "an ordinary slowness was cut short");
            sweep(helper, rig);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastsDamageIsTalliedForThePlayerBehindEachBlowAndNothingElse(GameTestHelper helper) {
        LineTestPlayer a = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        LineTestPlayer b = player(helper, Rank.ALPHA, Species.NONE, 3.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, a, b);
            BeastEntity beast = beast(helper, rig);
            hit(beast, a.damageSources().playerAttack(a), 20F);
            net.minecraft.world.entity.animal.Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(4, 1, 4));
            pet.tame(b);
            hit(beast, helper.getLevel().damageSources().mobAttack(pet), 30F);
            hit(beast, helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5))), 100F);
            hit(beast, helper.getLevel().damageSources().magic(), 100F);
            helper.assertTrue(entry(rig).tally.entries().size() == 2 && entry(rig).tally.top().map(b.getUUID()::equals).orElse(false), "the tally is " + entry(rig).tally.entries());
            // it is kept in the index, so it outlives the body
            sweep(helper, rig);
            helper.assertTrue(entry(rig).tally.entries().size() == 2, "the tally went with the body");
        } finally {
            clean(a, b);
        }
        helper.succeed();
    }

    // ---- the pike and its blood ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBloodOfAPlainPlayerIsNotEnoughButAHunterAnAlphaAnNpcOriginalAndAnAdvancedVampireAre(GameTestHelper helper) {
        LineTestPlayer plain = player(helper, Rank.OMEGA, Species.NONE, 2.5);
        LineTestPlayer hunter = player(helper, Rank.NONE, Species.HUNTER_OF_THE_FIVE, 3.5);
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 4.5);
        LineTestPlayer original = player(helper, Rank.NONE, Species.ORIGINAL, 5.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, plain, hunter, alpha, original);
            helper.assertTrue(!PikeRules.qualifies(PikeRiteFactory.giver(rig.server, plain.getUUID())), "a plain player's blood qualified");
            helper.assertTrue(PikeRules.qualifies(PikeRiteFactory.giver(rig.server, hunter.getUUID())), "a hunter's blood did not");
            helper.assertTrue(PikeRules.qualifies(PikeRiteFactory.giver(rig.server, alpha.getUUID())), "an Alpha's blood did not");
            helper.assertTrue(PikeRules.qualifies(PikeRiteFactory.giver(rig.server, original.getUUID())), "an Original's blood did not");
            helper.assertTrue(PikeRules.qualifies(PikeRiteFactory.giver(rig.server, NpcOriginal.KLAUS.uuid())), "an NPC Original's blood did not");
            helper.assertTrue(!PikeRules.qualifies(PikeRiteFactory.giver(rig.server, UUID.randomUUID())) && !PikeRules.qualifies(PikeRiteFactory.giver(rig.server, null)), "blood of nobody qualified");
        } finally {
            clean(plain, hunter, alpha, original);
        }
        helper.succeed();
    }

    // ---- the Mantle ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMantleWrapsEnemiesOnlyForAShiftedWerewolfWhoWearsItAndWaitsTwoMinutes(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        LineTestPlayer wolf = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, wolf);
            MantleEvents.reset();
            helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 5), "the test wolf could not join the werewolves");
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 2));
            Zombie far = helper.spawn(EntityType.ZOMBIE, new BlockPos(13, 1, 12));
            MantleEvents.useShifted(p -> true);
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && !zombie.hasEffect(MobEffects.WEAKNESS), "a wolf with no Mantle wrapped somebody");
            wolf.setItemInHand(InteractionHand.OFF_HAND, with(BossItems.MANTLE_OF_THE_BEAST.get()));
            MantleEvents.useShifted(p -> false);
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && !zombie.hasEffect(MobEffects.WEAKNESS), "a wolf who is not shifted wrapped somebody");
            MantleEvents.useShifted(p -> true);
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(MantleEvents.tick(wolf) == 1, "the Mantle did not wrap the one enemy in 6 blocks");
            helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS) && zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && zombie.getEffect(MobEffects.WEAKNESS).getDuration() == 400, "the enemy is not weakened and slowed for 20 seconds");
            helper.assertTrue(!far.hasEffect(MobEffects.WEAKNESS) && !wolf.hasEffect(MobEffects.WEAKNESS), "something out of reach, or the wearer, was wrapped");
            helper.assertTrue(MantleEvents.readyAt(wolf.getUUID()) == now + 2400, "the wait is " + (MantleEvents.readyAt(wolf.getUUID()) - now));
            Zombie late = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && !late.hasEffect(MobEffects.WEAKNESS), "the Wrap came again at once");
            // somebody who is no werewolf wears a trophy
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).leaveFaction(false);
            MantleEvents.reset();
            helper.assertTrue(MantleEvents.tick(wolf) == 0, "a player who is no werewolf wrapped somebody");
            late.discard();
            zombie.discard();
            far.discard();
            sweep(helper, rig);
        } finally {
            MantleEvents.useShifted(null);
            MantleEvents.reset();
            if (de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).getCurrentFaction() != null) {
                de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).leaveFaction(false);
            }
            clean(wolf);
        }
        helper.succeed();
    }
}
