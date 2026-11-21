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
import net.minecraft.world.Difficulty;
import net.ofatech.supernaturalbloodlines.core.boss.FrequencyRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.rite.FrequencyRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.scores.PlayerTeam;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.core.boss.BeastPhaseRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEvents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
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

    /** A test that needs what this run does not have (Werewolves, a rite that is not there): it logs that it was skipped, and passes. */
    private static void skipped(GameTestHelper helper) {
        String test = new Throwable().getStackTrace()[1].getMethodName();
        SupernaturalBloodlines.LOGGER.warn("GameTest skipped: {} (Werewolves or the rite it needs is not part of this run)", test);
        helper.succeed();
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
            helper.assertTrue(alpha.count(BeastDirector.HOST_NOTICE) == 2 && alpha.count(BeastDirector.HOST_HINT) == 1 && alpha.chat() == 2, "the Alpha was not told once, on the action bar and in chat: " + alpha.heard());
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).hostsMade == 1 && alpha.count(BeastDirector.HOST_NOTICE) == 2, "a second Host came the same night, or the Alpha was told again");
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
            if (net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("species")) {
                var pages = helper.getLevel().getEntitiesOfClass(ItemEntity.class, b.getBoundingBox().inflate(8.0),
                        i -> i.getItem().is(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.RITE_PAGE.get()));
                helper.assertTrue(pages.size() == 1 && net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem.loreOf(pages.get(0).getItem()).equals("beast"),
                        "the fall left " + pages.size() + " Valet's Notes");
                BeastDirector.collapse(helper.getLevel(), b);
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, b.getBoundingBox().inflate(8.0),
                        i -> i.getItem().is(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.RITE_PAGE.get())).size() == 1, "a second look at the same fall left more notes");
            }
            helper.assertTrue(entry(rig).spiritUntil == helper.getLevel().getGameTime() + BossesConfig.beastSpiritSeconds() * 20L, "the Spirit's time is " + entry(rig).spiritUntil);
            helper.assertTrue(!b.isInvulnerable() && b.isInvulnerableTo(helper.getLevel().damageSources().magic()), "the falling Beast can still be hurt");
            // a body that is falling is not the index's: it is not discarded by the index, and it is gone when the collapse is over
            b.tickCount = 9;
            run(b, 3);
            helper.assertTrue(b.isAlive(), "the falling Beast was discarded by the index at its next look");
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
            skipped(helper);
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
            int[] deaths = {0};
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> count = ev -> {
                if (ev.getEntity() == b) {
                    deaths[0]++;
                }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, true, net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class, count);
            try {
                hit(b, helper.getLevel().damageSources().magic(), 1_000_000F);
            } finally {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(count);
            }
            helper.assertTrue(deaths[0] == 0, "a blow made the Beast die: " + deaths[0] + " death events");
            helper.assertTrue(b.isAlive() && b.getHealth() >= 1F && b.getHealth() < 2F, "a blow killed it or left it at " + b.getHealth());
            BeastDirector.watch(helper.getLevel(), b);
            helper.assertTrue(entry(rig).phase == BeastPhase.SPIRIT, "a Beast at 1 health did not collapse");
            sweep(helper, rig);
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            BeastEntity c = beast(helper, rig);
            hit(c, helper.getLevel().damageSources().genericKill(), 1_000_000F);
            helper.assertTrue(c.isAlive() && entry(rig).phase == BeastPhase.SPIRIT && c.collapsing(), "/kill ended the Beast instead of making it collapse: " + entry(rig).phase);
            sweep(helper, rig);
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            BeastEntity d = beast(helper, rig);
            var death = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(d, helper.getLevel().damageSources().genericKill());
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(death);
            helper.assertTrue(death.isCanceled(), "the Beast's death was not cancelled"); 
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
            int sparksBefore = PackAccess.wolf(b).sparks();
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
            helper.assertTrue(PackAccess.wolf(b).sparks() == sparksBefore && itemsNear(helper, spirit, BossItems.VALET_PIKE.get()) == 0, "a spark or a pike was paid");
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
        for (LineTestPlayer p : new LineTestPlayer[] {omega, beta, alpha, truth, far}) {
            helper.getLevel().addNewPlayer(p);
        }
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
            for (LineTestPlayer p : new LineTestPlayer[] {omega, beta, alpha, truth, far}) {
                helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            clean(omega, beta, alpha, truth, far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBeastRearsUpWhenItHasATargetInRangeStandsStillWhileItRoarsAndWaitsItsCooldown(GameTestHelper helper) {
        LineTestPlayer victim = player(helper, Rank.OMEGA, Species.NONE, 3.5);
        helper.getLevel().addNewPlayer(victim);
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
            helper.getLevel().removePlayerImmediately(victim, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
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
            // the poison's damage type belongs to the artifacts part: without it there is no pulse to cap
            var poisonType = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key);
            if (poisonType.isPresent()) {
                before = b.getHealth();
                hit(b, new DamageSource(poisonType.get()), 500F);
                helper.assertTrue(Math.abs(before - b.getHealth() - 1.0F) < 0.01F, "a pulse of poison did " + (before - b.getHealth()));
            }
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
            skipped(helper);
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

    // ---- the chance, the peace, one try a night ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostNeedsTheRollUnderTheChanceAndNoPeaceAndOneTryANightEvenAfterItDied(GameTestHelper helper) {
        LineTestPlayer alpha = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true, alpha);
            // the roll is over the chance: nothing, and that was the one try tonight
            BossesConfig.override("beast_host_chance", 0.25);
            BossDirector.useRoll(() -> 0.5);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.NONE && body(rig) == null, "a Host came on a roll over the chance");
            BossDirector.useRoll(() -> 0.0);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.NONE, "a second try was made the same night");
            // a new night, but in peace
            entry(rig).lastRollDay = -1;
            BossDirector.useDifficulty(l -> Difficulty.PEACEFUL);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.NONE, "a Host came in a peaceful world");
            BossDirector.useDifficulty(null);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST, "the Host did not come on a roll under the chance");
            // it died: the night is lost, and no new Host the same night
            hit(body(rig), helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED, "the encounter is " + entry(rig).phase);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && entry(rig).hostsMade == 1, "a Host was made again the same night");
            entry(rig).lastRollDay = -1;
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST && entry(rig).hostsMade == 2, "a Host was not made again on a later night");
            sweep(helper, rig);
        } finally {
            clean(alpha);
        }
        helper.succeed();
    }

    // ---- the rites ----

    private static final long FULL_MOON_CYCLE = 24000L * 8;
    private static final long NIGHT = 14000;

    private static void atFullMoon(GameTestHelper helper, Runnable body) {
        atFullMoonAt(helper, NIGHT, body);
    }

    /** The day of the full moon at this time of day (14000 is its night, 6000 its noon). */
    private static void atFullMoonAt(GameTestHelper helper, long timeOfDay, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        level.setDayTime(base + timeOfDay);
        try {
            helper.assertTrue(level.getMoonPhase() == 0, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    private static boolean rite(GameTestHelper helper, String path) {
        return RiteTestSupport.type(helper, path) != null;
    }

    private static RecordingPlayer performer(GameTestHelper helper, Species species) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).witchLayer(species == Species.WITCH).build());
        RiteTestSupport.place(helper, p);
        return p;
    }

    private static List<ItemStack> pikeItems(UUID blood) {
        return List.of(SiresBloodItem.of(blood, "Somebody"), new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.GLOWSTONE_DUST, 2));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theValetPikeRiteMakesThePikeFromStrongBloodAndRefusesEveryOtherBloodTheSameWayAtTheSameCost(GameTestHelper helper) {
        if (!rite(helper, "rite_of_the_valet_pike")) {
            skipped(helper);
            return;
        }
        LineTestPlayer plain = player(helper, Rank.OMEGA, Species.NONE, 2.5);
        LineTestPlayer hunter = player(helper, Rank.NONE, Species.HUNTER_OF_THE_FIVE, 3.5);
        RecordingPlayer witch = performer(helper, Species.WITCH);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, plain, hunter);
            atFullMoon(helper, () -> {
                RiteTestSupport.start(helper, "rite_of_the_valet_pike", witch, pikeItems(hunter.getUUID()));
                helper.assertTrue(witch.receivedOnActionBar(PikeRiteFactory.DONE), "the witch was not told the pike is made: " + witch.heard());
                helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 1 && RiteTestSupport.dropped(helper, "minecraft:iron_ingot") == 0, "the pike is not in the circle, or the iron was given back");
                RiteTestSupport.clearDropped(helper);
                // a plain player's blood, the blood of nobody here, and the blood of somebody who has gone: the same message, and the ingredients are spent
                for (UUID weak : new UUID[] {plain.getUUID(), UUID.randomUUID()}) {
                    int before = witch.count(PikeRiteFactory.WEAK);
                    RiteTestSupport.start(helper, "rite_of_the_valet_pike", witch, pikeItems(weak));
                    helper.assertTrue(witch.count(PikeRiteFactory.WEAK) == before + 1 && !witch.received(PikeRiteFactory.NOT_WITCH), "a weak blood was not refused the one way: " + witch.heard());
                    helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 0 && RiteTestSupport.dropped(helper, "minecraft:iron_ingot") == 0,
                            "a refusal gave the ingredients back, or made a pike: " + RiteTestSupport.dropped(helper, "minecraft:iron_ingot"));
                }
                // somebody who is no witch type is told so, and everything comes back
                RecordingPlayer plainCaster = performer(helper, Species.NONE);
                RiteTestSupport.remove(helper, witch);
                RiteTestSupport.start(helper, "rite_of_the_valet_pike", plainCaster, pikeItems(hunter.getUUID()));
                helper.assertTrue(plainCaster.receivedOnActionBar(PikeRiteFactory.NOT_WITCH), "a plain caster was not told why");
                helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:iron_ingot") == 3 && RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 0, "a plain caster lost the ingredients, or made a pike");
                RiteTestSupport.clearDropped(helper);
                RiteTestSupport.remove(helper, plainCaster);
            });
        } finally {
            RiteTestSupport.remove(helper, witch);
            clean(plain, hunter);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFrequencyRiteBringsTheHostOnceThenMakesItChangeThenSaysBusyAndGivesBackWhatItDidNotUse(GameTestHelper helper) {
        if (!rite(helper, "rite_of_the_frequency") || !werewolves()) {
            skipped(helper);
            return;
        }
        RecordingPlayer wolf = performer(helper, Species.NONE);
        helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 5), "the caster could not join the werewolves");
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true);
            List<ItemStack> items = List.of(with(BossItems.VALET_PIKE.get()), new ItemStack(RiteTestSupport.item("supernaturalbloodlines:hearing_device")), new ItemStack(Items.ECHO_SHARD));
            atFullMoon(helper, () -> {
                RiteTestSupport.start(helper, "rite_of_the_frequency", wolf, items);
                helper.assertTrue(wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.MAKE_HOST)) && entry(rig).phase == BeastPhase.HOST && entry(rig).hostsMade == 1,
                        "the first frequency did not bring the Host: " + entry(rig).phase + " / " + wolf.heard());
                helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 1 && RiteTestSupport.dropped(helper, "minecraft:echo_shard") == 0, "the pike did not come back, or the echo shard did");
                RiteTestSupport.clearDropped(helper);
                RiteTestSupport.start(helper, "rite_of_the_frequency", wolf, items);
                helper.assertTrue(wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.FORCE_CHANGE)) && entry(rig).phase == BeastPhase.BEAST && body(rig) instanceof BeastEntity,
                        "the second frequency did not change the Host: " + entry(rig).phase);
                RiteTestSupport.clearDropped(helper);
                RiteTestSupport.start(helper, "rite_of_the_frequency", wolf, items);
                helper.assertTrue(wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.BUSY)) && entry(rig).phase == BeastPhase.BEAST, "a Beast already out was not answered with busy");
                helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:echo_shard") == 1, "a refusal did not give the ingredients back");
                RiteTestSupport.clearDropped(helper);
                sweep(helper, rig);
                rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
                BossesConfig.override("beast_of_gevaudan_enabled", false);
                RiteTestSupport.start(helper, "rite_of_the_frequency", wolf, items);
                helper.assertTrue(wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.DISABLED)) && entry(rig).phase == BeastPhase.NONE, "the switched-off Beast answered the frequency");
                RiteTestSupport.clearDropped(helper);
            });
            sweep(helper, rig);
        } finally {
            if (de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).getCurrentFaction() != null) {
                de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).leaveFaction(false);
            }
            RiteTestSupport.remove(helper, wolf);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritThatDiesOfAnythingButThePikeHasGotAwayAndPaysNothing(GameTestHelper helper) {
        LineTestPlayer a = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, a);
            BeastEntity beast = beast(helper, rig);
            hit(beast, a.damageSources().playerAttack(a), 40F);
            BeastDirector.collapse(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            hit(spirit, helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(!spirit.isAlive() && entry(rig).phase == BeastPhase.RETURN_PENDING && !entry(rig).rewarded, "a Spirit that died without the pike counted as killed: " + entry(rig).phase);
            helper.assertTrue(a.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0 && itemsNear(helper, spirit, BossItems.MANTLE_OF_THE_BEAST.get()) == 0, "a Mantle was paid");
            sweep(helper, rig);
        } finally {
            clean(a);
        }
        helper.succeed();
    }

    // ---- panel round 1: the pike, the rites, the Host, the reward, the stuck encounter, the Spirit, the Mantle ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyADirectBlowOfThePikeHurtsTheSpiritNotOneThrownOrShotWhileHoldingIt(GameTestHelper helper) {
        LineTestPlayer striker = player(helper, Rank.NONE, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, striker);
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            ServerLevel level = helper.getLevel();
            striker.setItemInHand(InteractionHand.MAIN_HAND, with(BossItems.VALET_PIKE.get()));
            Snowball ball = EntityType.SNOWBALL.create(level);
            ball.setOwner(striker);
            hit(spirit, level.damageSources().thrown(ball, striker), 500F);
            // an arrow that says it was fired from the pike: the weapon is the pike, the blow is not direct
            Arrow arrow = new Arrow(level, striker, new ItemStack(Items.ARROW), with(BossItems.VALET_PIKE.get()));
            helper.assertTrue(arrow.getWeaponItem() != null && arrow.getWeaponItem().is(BossItems.VALET_PIKE.get()), "the control arrow does not carry the pike");
            hit(spirit, level.damageSources().arrow(arrow, striker), 500F);
            net.minecraft.world.entity.monster.Zombie carrier = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 1, 6));
            carrier.setItemInHand(InteractionHand.MAIN_HAND, with(BossItems.VALET_PIKE.get()));
            hit(spirit, level.damageSources().mobAttack(carrier), 500F);
            helper.assertTrue(!BeastEvents.pike(level.damageSources().mobAttack(carrier)), "a mob that carries the pike strikes with it");
            carrier.discard();
            helper.assertTrue(spirit.isAlive() && !spirit.pikeHit() && entry(rig).phase == BeastPhase.SPIRIT, "something thrown or shot, or a mob that carries it, hurt the Spirit: " + entry(rig).phase);
            helper.assertTrue(!BeastEvents.pike(level.damageSources().arrow(arrow, striker)) && !BeastEvents.pike(level.damageSources().thrown(ball, striker)), "a thrown or shot blow counts as the pike");
            helper.assertTrue(BeastEvents.pike(striker.damageSources().playerAttack(striker)), "a direct blow of the pike does not count");
            BeastDirector.killed(level, spirit, striker);
            helper.assertTrue(spirit.isAlive() && entry(rig).phase == BeastPhase.SPIRIT && !entry(rig).rewarded, "a Spirit the pike had not touched was ended");
            hit(spirit, striker.damageSources().playerAttack(striker), 1F);
            helper.assertTrue(!spirit.isAlive() && entry(rig).phase == BeastPhase.KILLED, "the direct blow did not end it: " + entry(rig).phase);
            helper.assertTrue(striker.count(BeastDirector.ENDED) == 1, "the end was not told: " + striker.heard());
            sweep(helper, rig);
        } finally {
            clean(striker);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theValetPikeRiteWorksOnlyOnAFullMoonNightAndByDayGivesEverythingBack(GameTestHelper helper) {
        if (!rite(helper, "rite_of_the_valet_pike")) {
            skipped(helper);
            return;
        }
        LineTestPlayer hunter = player(helper, Rank.NONE, Species.HUNTER_OF_THE_FIVE, 3.5);
        RecordingPlayer witch = performer(helper, Species.WITCH);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, hunter);
            atFullMoonAt(helper, 6000, () -> {
                RiteTestSupport.start(helper, "rite_of_the_valet_pike", witch, pikeItems(hunter.getUUID()));
                helper.assertTrue(witch.receivedOnActionBar("supernaturalbloodlines.rite.needs_full_moon"), "the witch was not told it needs a full moon night: " + witch.heard());
                helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 0 && RiteTestSupport.dropped(helper, "minecraft:iron_ingot") == 3,
                        "a rite by day made a pike, or kept the iron: " + RiteTestSupport.dropped(helper, "minecraft:iron_ingot"));
                RiteTestSupport.clearDropped(helper);
            });
            atFullMoon(helper, () -> {
                RiteTestSupport.start(helper, "rite_of_the_valet_pike", witch, pikeItems(hunter.getUUID()));
                helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:valet_pike") == 1, "the same rite at night did not make the pike");
                RiteTestSupport.clearDropped(helper);
            });
        } finally {
            RiteTestSupport.remove(helper, witch);
            clean(hunter);
        }
        helper.succeed();
    }

    private static final String[] FREQUENCY_ITEMS = {"supernaturalbloodlines:valet_pike", "supernaturalbloodlines:hearing_device", "minecraft:echo_shard"};

    private static List<ItemStack> frequencyItems() {
        return List.of(with(BossItems.VALET_PIKE.get()), new ItemStack(RiteTestSupport.item("supernaturalbloodlines:hearing_device")), new ItemStack(Items.ECHO_SHARD));
    }

    private static void joinWerewolves(RecordingPlayer p, GameTestHelper helper) {
        helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 5), "the caster could not join the werewolves");
    }

    private static void leaveWerewolves(RecordingPlayer p) {
        if (de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFrequencyRiteRefusesACasterWhoIsNoWerewolfAndAGroundlessHostWithTheirOwnWordsAndGivesEverythingBack(GameTestHelper helper) {
        if (!rite(helper, "rite_of_the_frequency") || !werewolves()) {
            skipped(helper);
            return;
        }
        RecordingPlayer plain = performer(helper, Species.NONE);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true);
            atFullMoon(helper, () -> {
                RiteTestSupport.start(helper, "rite_of_the_frequency", plain, frequencyItems());
                helper.assertTrue(plain.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.NOT_WEREWOLF)), "a caster who is no werewolf was not told so: " + plain.heard());
                helper.assertTrue(entry(rig).phase == BeastPhase.NONE && entry(rig).hostsMade == 0, "a caster who is no werewolf brought the Host");
                for (String id : FREQUENCY_ITEMS) {
                    helper.assertTrue(RiteTestSupport.dropped(helper, id) == 1, "the refusal did not give back " + id);
                }
                RiteTestSupport.clearDropped(helper);
                RiteTestSupport.remove(helper, plain);
                // a werewolf, but no ground for the Host to stand on
                RecordingPlayer wolf = performer(helper, Species.NONE);
                joinWerewolves(wolf, helper);
                BossDirector.useGround((level, from, min, max) -> java.util.Optional.empty());
                RiteTestSupport.start(helper, "rite_of_the_frequency", wolf, frequencyItems());
                helper.assertTrue(wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.NO_GROUND)) && !wolf.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.BUSY)),
                        "a Host with no ground was not given its own word: " + wolf.heard());
                helper.assertTrue(entry(rig).phase == BeastPhase.NONE && entry(rig).hostsMade == 0, "a Host was made with no ground");
                for (String id : FREQUENCY_ITEMS) {
                    helper.assertTrue(RiteTestSupport.dropped(helper, id) == 1, "the groundless refusal did not give back " + id);
                }
                RiteTestSupport.clearDropped(helper);
                leaveWerewolves(wolf);
                RiteTestSupport.remove(helper, wolf);
            });
        } finally {
            RiteTestSupport.remove(helper, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostCannotBeLeashedCarriedPushedOrSentAwayAndAHitSaysHeDoesNotFeelItAndAFailedNightKeepsWhatTheStoryDid(GameTestHelper helper) {
        LineTestPlayer striker = player(helper, Rank.NONE, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, striker);
            GevaudanHostEntity h = host(helper);
            helper.assertTrue(!h.canBeLeashed() && !h.isPushable() && !h.canChangeDimensions(helper.getLevel(), helper.getLevel()), "the Host can be leashed, pushed or sent to another dimension");
            net.minecraft.world.entity.vehicle.Boat boat = helper.spawn(EntityType.BOAT, new BlockPos(6, 1, 6));
            helper.assertTrue(!h.startRiding(boat, true) && h.getVehicle() == null, "the Host was put on a boat");
            helper.assertTrue(!h.startRiding(boat) && h.getVehicle() == null, "the Host was put on a boat the other way");
            boat.discard();
            hit(h, striker.damageSources().playerAttack(striker), 50F);
            helper.assertTrue(striker.count(GevaudanHostEntity.UNFELT) == 1, "the hit was not answered with one line: " + striker.heard());
            hit(h, helper.getLevel().damageSources().source(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL, striker), 1000F);
            helper.assertTrue(!h.isAlive() && striker.count(GevaudanHostEntity.UNFELT) == 1, "a /kill by a player was told he does not feel it, or did not kill him");
            // the story stays as it was: the Spirit had come back once, the Host was lost, a new one is made, and he does not forget
            sweep(helper, rig);
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            GevaudanHostEntity first = host(helper);
            entry(rig).returned = true;
            hit(first, helper.getLevel().damageSources().genericKill(), 1000F);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && entry(rig).returned, "the failed night forgot the Spirit had been back");
            GevaudanHostEntity again = host(helper);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST && entry(rig).returned && again.isAlive(), "a new Host made the story forget what it did: returned is " + entry(rig).returned);
            sweep(helper, rig);
        } finally {
            clean(striker);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMantleIsKeptForAnAbsentTopDealerNeverGivenToTheStrikerAndHandedOverOnceAtLoginAndSurvivesASave(GameTestHelper helper) {
        LineTestPlayer away = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        LineTestPlayer striker = player(helper, Rank.NONE, Species.NONE, 3.5);
        LineTestPlayer near = player(helper, Rank.NONE, Species.NONE, 4.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            // the one who did the most is not among the players the server knows
            world(false, striker, near);
            entry(rig).tally.add(away.getUUID(), 500F);
            entry(rig).tally.add(striker.getUUID(), 5F);
            entry(rig).witnesses.add(striker.getUUID());
            striker.setPos(striker.position().add(300, 0, 0));
            helper.assertTrue(BeastDirector.pay(helper.getLevel(), striker, spot(helper)), "nothing was paid");
            helper.assertTrue(entry(rig).owed.contains(away.getUUID()) && entry(rig).owed.size() == 1, "the Mantle is not kept for the one who did the most: " + entry(rig).owed);
            helper.assertTrue(striker.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0, "the striker was given what is another's");
            helper.assertTrue(!striker.received(BeastDirector.REWARD) && striker.received(BeastDirector.REWARD_OWED), "the striker was not told, in its own line, where the Mantle went: " + striker.heard());
            helper.assertTrue(near.received(BeastDirector.REWARD_OWED) && itemsNear(helper, near, BossItems.MANTLE_OF_THE_BEAST.get()) == 0, "those near were not told, or the Mantle fell on the ground");
            helper.assertTrue(!rig.index.isDirty(), "the payment was not written to disk");
            // it is in the saved record, with the witnesses
            var registries = helper.getLevel().registryAccess();
            CompoundTag saved = rig.index.save(new CompoundTag(), registries);
            BossIndex reloaded = BossIndex.load(saved, registries);
            helper.assertTrue(reloaded.of(BossId.BEAST_OF_GEVAUDAN).owed.contains(away.getUUID()) && reloaded.of(BossId.BEAST_OF_GEVAUDAN).witnesses.contains(striker.getUUID()),
                    "the owed Mantle or the witnesses did not survive a save");
            // it is handed over once, at login
            java.util.List<ServerPlayer> savedPlayers = new java.util.ArrayList<>();
            BeastDirector.useSaver(savedPlayers::add);
            try {
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(striker));
                helper.assertTrue(striker.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0 && savedPlayers.isEmpty(), "somebody else logging in was given it, or saved");
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(away));
                helper.assertTrue(away.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1 && away.received(BeastDirector.REWARD_LATE) && entry(rig).owed.isEmpty(), "the login did not hand the Mantle over");
                helper.assertTrue(savedPlayers.size() == 1 && savedPlayers.get(0) == away, "the player was not saved after the hand-over: " + savedPlayers);
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(away));
                helper.assertTrue(away.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1 && savedPlayers.size() == 1, "it was handed over twice");
            } finally {
                BeastDirector.useSaver(null);
            }
            sweep(helper, rig);
        } finally {
            clean(away, striker, near);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOnlineTopDealerGetsTheMantleAndTheStrikerAndEveryoneNearAreToldWhoDid(GameTestHelper helper) {
        LineTestPlayer top = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        LineTestPlayer striker = player(helper, Rank.NONE, Species.NONE, 3.5);
        LineTestPlayer near = player(helper, Rank.NONE, Species.NONE, 4.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, top, striker, near);
            entry(rig).tally.add(top.getUUID(), 500F);
            helper.assertTrue(BeastDirector.pay(helper.getLevel(), striker, spot(helper)), "nothing was paid");
            helper.assertTrue(top.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1 && top.received(BeastDirector.REWARD) && !top.received(BeastDirector.REWARD_TO), "the top dealer was not paid, or was told twice");
            helper.assertTrue(striker.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 0 && striker.count(BeastDirector.REWARD_TO) == 1, "the striker has the Mantle or was not told who has it: " + striker.heard());
            helper.assertTrue(near.count(BeastDirector.REWARD_TO) == 1 && entry(rig).owed.isEmpty(), "somebody near was not told once, or something is owed");
            // the pay is once
            helper.assertTrue(!BeastDirector.pay(helper.getLevel(), striker, spot(helper)) && top.getInventory().countItem(BossItems.MANTLE_OF_THE_BEAST.get()) == 1, "it paid twice");
            sweep(helper, rig);
        } finally {
            clean(top, striker, near);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpiritNobodyCanFindIsTakenAsEscapedAfterItsTimeAndTheGraceAndTheWitnessesAreToldWhereverTheyAreNow(GameTestHelper helper) {
        LineTestPlayer witness = player(helper, Rank.NONE, Species.NONE, 2.5);
        LineTestPlayer stranger = player(helper, Rank.NONE, Species.NONE, 3.5);
        LineTestPlayer latecomer = player(helper, Rank.NONE, Species.NONE, 4.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, witness, stranger, latecomer);
            Vec3 nearSpot = latecomer.position();
            stranger.setPos(stranger.position().add(300, 0, 0));
            latecomer.setPos(nearSpot.add(300, 0, 0));
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            helper.assertTrue(entry(rig).witnesses.contains(witness.getUUID()) && !entry(rig).witnesses.contains(stranger.getUUID()), "the witnesses are " + entry(rig).witnesses);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            witness.setPos(witness.position().add(300, 0, 0));
            latecomer.setPos(nearSpot);
            spirit.discard();
            long now = helper.getLevel().getGameTime();
            entry(rig).spiritUntil = now - (BeastPhaseRules.SPIRIT_GRACE_TICKS - 1);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.SPIRIT, "it was taken as escaped inside the grace: " + entry(rig).phase);
            entry(rig).spiritUntil = now - BeastPhaseRules.SPIRIT_GRACE_TICKS;
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.RETURN_PENDING && entry(rig).returned && entry(rig).spirit == null, "a Spirit nobody can find did not escape: " + entry(rig).phase);
            helper.assertTrue(witness.count(BeastDirector.ESCAPED) == 1, "the witness, far away now, was not told it escaped: " + witness.heard());
            helper.assertTrue(stranger.count(BeastDirector.ESCAPED) == 0, "somebody who was never near was told");
            helper.assertTrue(latecomer.count(BeastDirector.ESCAPED) == 1, "somebody who came near after the collapse was not told: " + latecomer.heard());
            BossDirector.check(rig.server);
            helper.assertTrue(witness.count(BeastDirector.ESCAPED) == 1, "it escaped twice");
            sweep(helper, rig);
        } finally {
            clean(witness, stranger, latecomer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSecondEscapeAndTheFirstWithReturnsOffAreToldToTheWitnessesInTheirOwnWords(GameTestHelper helper) {
        LineTestPlayer witness = player(helper, Rank.NONE, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, witness);
            Vec3 here = witness.position();
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            witness.setPos(here.add(300, 0, 0));
            BeastDirector.escape(helper.getLevel(), (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit));
            helper.assertTrue(witness.count(BeastDirector.ESCAPED) == 1 && witness.count(BeastDirector.GONE) == 0, "the first escape is not told as one that comes back");
            witness.setPos(here);
            helper.assertTrue(BeastDirector.change(helper.getLevel()), "the Host did not change again");
            BeastDirector.collapse(helper.getLevel(), (BeastEntity) body(rig));
            witness.setPos(here.add(300, 0, 0));
            BeastDirector.escape(helper.getLevel(), (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit));
            helper.assertTrue(entry(rig).phase == BeastPhase.GONE && witness.count(BeastDirector.GONE) == 1 && witness.count(BeastDirector.GONE_FINAL) == 0, "the second escape is told wrongly: " + witness.heard());
            sweep(helper, rig);
            // with the return off the first escape is final, and says so in its own words
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            BossesConfig.override("beast_returns_once", false);
            witness.setPos(here);
            BeastEntity third = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), third);
            witness.setPos(here.add(300, 0, 0));
            BeastDirector.escape(helper.getLevel(), (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit));
            helper.assertTrue(entry(rig).phase == BeastPhase.GONE && witness.count(BeastDirector.GONE_FINAL) == 1 && witness.count(BeastDirector.GONE) == 1, "a first escape with the return off is told as the second: " + witness.heard());
            sweep(helper, rig);
        } finally {
            clean(witness);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBeastOrAHostWhoseBodyIsGoneForAMinuteFailsTheNightAndABodyThatComesBackAfterwardsIsDiscarded(GameTestHelper helper) {
        LineTestPlayer near = player(helper, Rank.NONE, Species.NONE, 2.5);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, near);
            BeastEntity beast = beast(helper, rig);
            UUID beastId = beast.getUUID();
            beast.discard();
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST, "one miss was enough to lose the Beast");
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.LOST && entry(rig).phase == BeastPhase.BEAST, "ten seconds of missing already failed the night: " + entry(rig).phase);
            long now = helper.getLevel().getGameTime();
            entry(rig).lostSince = now - (BeastPhaseRules.BODY_LOST_TICKS - 1);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.BEAST && near.count(BeastDirector.FAILED) == 0, "a body gone for less than a minute failed the night");
            entry(rig).lostSince = now - BeastPhaseRules.BODY_LOST_TICKS;
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.LOST, "a Beast gone for a minute did not fail the night: " + entry(rig).phase + " lostSince=" + entry(rig).lostSince + " now=" + helper.getLevel().getGameTime() + " state=" + rig.index.state(BossId.BEAST_OF_GEVAUDAN));
            helper.assertTrue(near.count(BeastDirector.FAILED) == 1, "those near were not told once: " + near.heard());
            BossDirector.check(rig.server);
            helper.assertTrue(near.count(BeastDirector.FAILED) == 1 && entry(rig).phase == BeastPhase.FAILED, "the failure was told twice");
            // the old Beast loads in after all: it is not the story's any more, so it goes, and the index is not told it was seen
            BeastEntity ghost = BeastEntity.create(helper.getLevel());
            ghost.moveTo(helper.absoluteVec(new Vec3(5.5, 1, 5.5)));
            helper.getLevel().addFreshEntity(ghost);
            ghost.tickCount = 9;
            run(ghost, 1);
            helper.assertTrue(ghost.isRemoved(), "a Beast that loaded into a failed night was left alive");
            helper.assertTrue(rig.index.state(BossId.BEAST_OF_GEVAUDAN) == BossState.LOST && !ghost.getUUID().equals(entry(rig).entity) && entry(rig).phase == BeastPhase.FAILED, "the ghost was written down as the Beast");
            sweep(helper, rig);
            // the same for a Host
            rig.index.forget(BossId.BEAST_OF_GEVAUDAN);
            GevaudanHostEntity h = host(helper);
            h.discard();
            BossDirector.check(rig.server);
            BossDirector.check(rig.server);
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.HOST, "a Host gone for ten seconds failed the night");
            entry(rig).lostSince = helper.getLevel().getGameTime() - BeastPhaseRules.BODY_LOST_TICKS;
            BossDirector.check(rig.server);
            helper.assertTrue(entry(rig).phase == BeastPhase.FAILED && near.count(BeastDirector.FAILED) == 2, "a vanished Host did not fail the night: " + entry(rig).phase);
            GevaudanHostEntity ghostHost = GevaudanHostEntity.create(helper.getLevel());
            ghostHost.moveTo(helper.absoluteVec(new Vec3(5.5, 1, 5.5)));
            helper.getLevel().addFreshEntity(ghostHost);
            ghostHost.tickCount = 9;
            run(ghostHost, 1);
            helper.assertTrue(ghostHost.isRemoved(), "a Host that loaded into a story with no Host was left alive");
            // and a Host is made again once
            helper.assertTrue(BeastDirector.spawnHost(helper.getLevel(), null, spot(helper)).isPresent() && entry(rig).phase == BeastPhase.HOST, "a Host was not made again after the failure");
            sweep(helper, rig);
        } finally {
            clean(near);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSpiritIsSlowerThanASprintingPlayerStopsEveryFourSecondsAndLooksBack(GameTestHelper helper) {
        LineTestPlayer follower = player(helper, Rank.NONE, Species.NONE, 2.5);
        helper.getLevel().addNewPlayer(follower);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, follower);
            BeastEntity beast = beast(helper, rig);
            BeastDirector.collapse(helper.getLevel(), beast);
            GevaudanSpiritEntity spirit = (GevaudanSpiritEntity) helper.getLevel().getEntity(entry(rig).spirit);
            spirit.setPos(helper.absoluteVec(new Vec3(8.5, 3.0, 8.5)));
            // it runs: never faster than the cap, by the distance it really moves
            spirit.tickCount = 10;
            Vec3 before = spirit.position();
            spirit.setDeltaMovement(3.0, 0.0, 0.0);
            spirit.travel(new Vec3(0.0, 0.0, 1.0));
            double moved = spirit.position().subtract(before).length();
            helper.assertTrue(moved > 0.05 && moved <= BeastRules.SPIRIT_SPEED + 0.025, "the Spirit moved " + moved + " blocks in a tick, the cap is " + BeastRules.SPIRIT_SPEED + " and the air adds a little");
            helper.assertTrue(moved < BeastRules.SPRINT_SPEED * 0.8, "a sprinting player does not clearly gain on it");
            // it stops for a second every four
            spirit.tickCount = 70;
            before = spirit.position();
            spirit.setDeltaMovement(3.0, 0.0, 0.0);
            spirit.travel(new Vec3(0.0, 0.0, 1.0));
            helper.assertTrue(spirit.position().equals(before) && spirit.getDeltaMovement().equals(Vec3.ZERO), "the Spirit moved while it paused");
            // and it looks back at whoever follows
            spirit.tickCount = 59;
            run(spirit, 1);
            helper.assertTrue(BeastRules.spiritPauses(spirit.tickCount) && Math.abs(spirit.getLookControl().getWantedX() - follower.getX()) < 1e-6
                    && Math.abs(spirit.getLookControl().getWantedZ() - follower.getZ()) < 1e-6, "the Spirit did not look at the player behind it");
            sweep(helper, rig);
        } finally {
            helper.getLevel().removePlayerImmediately(follower, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            clean(follower);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMantleWithNoEnemyStaysReadyAndSilentAndSparesPacksTeamsAndPlayersWherePvpIsOff(GameTestHelper helper) {
        if (!werewolves()) {
            skipped(helper);
            return;
        }
        LineTestPlayer.useRealHarm(true);
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        helper.getLevel().getServer().setPvpAllowed(true);
        LineTestPlayer wolf = player(helper, Rank.ALPHA, Species.NONE, 2.5);
        LineTestPlayer mate = player(helper, Rank.ALPHA, Species.NONE, 3.5);
        LineTestPlayer rival = player(helper, Rank.OMEGA, Species.NONE, 4.5);
        LineTestPlayer teammate = player(helper, Rank.OMEGA, Species.NONE, 5.5);
        LineTestPlayer[] inLevel = {wolf, mate, rival, teammate};
        for (LineTestPlayer q : inLevel) {
            helper.getLevel().addNewPlayer(q);
        }
        net.minecraft.world.scores.Scoreboard board = helper.getLevel().getServer().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("beast_mantle_test");
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false, wolf, mate, rival, teammate);
            MantleEvents.reset();
            helper.assertTrue(de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 5), "the wolf could not join the werewolves");
            wolf.setItemInHand(InteractionHand.OFF_HAND, with(BossItems.MANTLE_OF_THE_BEAST.get()));
            MantleEvents.useShifted(p -> true);
            // nobody to wrap but the wearer's own pack, a teammate, and nobody else: nothing is wrapped, nothing is spent, nothing is said
            helper.assertTrue(PackCommand.invite(wolf, mate) == null
                    && PackCommand.accept(mate, PackCommand.requests().tokenOf(wolf.getUUID(), mate.getUUID()).orElseThrow()) == null, "the mate could not join the wolf's pack");
            board.addPlayerToTeam(wolf.getScoreboardName(), team);
            board.addPlayerToTeam(teammate.getScoreboardName(), team);
            rival.setPos(wolf.position().add(40, 0, 0));
            helper.getLevel().getServer().setPvpAllowed(false);
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && MantleEvents.readyAt(wolf.getUUID()) == 0 && !wolf.received(MantleEvents.WRAPPED), "a Mantle with no enemy was spent or said something: " + wolf.heard());
            helper.getLevel().getServer().setPvpAllowed(true);
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && !mate.hasEffect(MobEffects.WEAKNESS) && !teammate.hasEffect(MobEffects.WEAKNESS), "the wrap took a packmate or a teammate");
            helper.assertTrue(MantleEvents.readyAt(wolf.getUUID()) == 0 && !wolf.received(MantleEvents.WRAPPED), "a wrap that took nobody was spent: " + wolf.heard());
            // a rival in reach: where PvP is off they are spared, where it is on they are wrapped and the wrap is spent
            rival.setPos(wolf.position().add(2, 0, 0));
            helper.getLevel().getServer().setPvpAllowed(false);
            helper.assertTrue(MantleEvents.tick(wolf) == 0 && !rival.hasEffect(MobEffects.WEAKNESS), "the wrap took a player where PvP is off");
            helper.getLevel().getServer().setPvpAllowed(true);
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(MantleEvents.tick(wolf) == 1 && rival.hasEffect(MobEffects.WEAKNESS) && !mate.hasEffect(MobEffects.WEAKNESS) && !teammate.hasEffect(MobEffects.WEAKNESS), "the wrap did not take the one rival");
            helper.assertTrue(MantleEvents.readyAt(wolf.getUUID()) == now + 2400 && wolf.received(MantleEvents.WRAPPED), "the wrap that took somebody was not spent and announced");
        } finally {
            MantleEvents.useShifted(null);
            MantleEvents.reset();
            board.removePlayerTeam(team);
            for (LineTestPlayer q : inLevel) {
                helper.getLevel().removePlayerImmediately(q, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            helper.getLevel().getServer().setPvpAllowed(pvp);
            PackIndex.get(helper.getLevel()).leave(mate.getUUID());
            PackIndex.get(helper.getLevel()).leave(wolf.getUUID());
            if (de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).getCurrentFaction() != null) {
                de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(wolf).leaveFaction(false);
            }
            LineTestPlayer.useRealHarm(false);
            clean(wolf, mate, rival, teammate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHostIsNotMovedByFlowingWaterShovesExplosionsOrPistons(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(false);
            GevaudanHostEntity h = host(helper);
            h.setNoAi(true);
            // a source beside him and the block he stands in flowing: the water is a current that carries anything it can
            BlockPos at = helper.relativePos(h.blockPosition());
            helper.setBlock(at.east(), Blocks.WATER);
            helper.setBlock(at, Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1));
            helper.setBlock(at.above(), Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1));
            helper.assertTrue(h.getPistonPushReaction() == PushReaction.IGNORE, "a piston can push the Host");
            Vec3 start = h.position();
            h.push(2.0, 0.0, 2.0);
            helper.assertTrue(h.getDeltaMovement().horizontalDistanceSqr() < 1e-6, "a shove moved the Host");
            helper.getLevel().explode(null, start.x + 1.0, start.y, start.z, 3.0F, Level.ExplosionInteraction.NONE);
            helper.assertTrue(h.isAlive() && h.getDeltaMovement().horizontalDistanceSqr() < 1e-6, "an explosion moved or hurt the Host: " + h.getDeltaMovement());
            run(h, 60);
            helper.assertTrue(h.isAlive() && h.position().subtract(start).horizontalDistanceSqr() < 0.0001, "the Host was carried off by the water: " + h.position().subtract(start));
            helper.setBlock(at.east(), Blocks.AIR);
            helper.setBlock(at, Blocks.AIR);
            helper.setBlock(at.above(), Blocks.AIR);
            sweep(helper, rig);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anyWerewolfTypeCanStrikeTheFrequencyAHybridToo(GameTestHelper helper) {
        if (!rite(helper, "rite_of_the_frequency") || !werewolves()) {
            skipped(helper);
            return;
        }
        RecordingPlayer hybrid = performer(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(true);
            atFullMoon(helper, () -> {
                RiteTestSupport.start(helper, "rite_of_the_frequency", hybrid, frequencyItems());
                helper.assertTrue(hybrid.receivedOnActionBar(FrequencyRules.messageKey(FrequencyRules.Result.MAKE_HOST)) && entry(rig).phase == BeastPhase.HOST,
                        "a hybrid was not let strike the frequency: " + entry(rig).phase + " / " + hybrid.heard());
                RiteTestSupport.clearDropped(helper);
                sweep(helper, rig);
            });
        } finally {
            RiteTestSupport.remove(helper, hybrid);
        }
        helper.succeed();
    }
}