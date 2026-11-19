package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcHunt;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcIndex;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcOrders;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackBrain;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.species.npc.PackSkills;
import net.ofatech.supernaturalbloodlines.parts.species.npc.WerewolfBrotherRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;

/**
 * Plan 15d: the pack's NPC members (R148): the Werewolf Brother rite's rules in a live world, the caps, the orders, what an absent owner means, the pack bonus,
 * the Roar and the Surge, peace inside the pack and among allies, the hunting trip and the chest sorting, and that a Brother never takes a spark. Werewolves only.
 */
@PrefixGameTestTemplate(false)
public final class NpcMemberGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private NpcMemberGameTests() {
    }

    // ---- helpers ----

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, Lineage lineage) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        WolfState w = WolfState.NONE.toBuilder().lineage(lineage).rank(rank).geneRolled(true).protectPath(false).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "the test wolf could not join the werewolves");
        p.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        return p;
    }

    private static void online(ServerPlayer... players) {
        java.util.Map<java.util.UUID, ServerPlayer> map = new java.util.HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
    }

    private static void clean(GameTestHelper helper, List<? extends Entity> mobs, ServerPlayer... players) {
        NpcIndex index = NpcIndex.get(helper.getLevel());
        for (Entity m : mobs) {
            index.remove(m.getUUID());
            m.discard();
        }
        PackIndex packs = PackIndex.get(helper.getLevel());
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            packs.leave(p.getUUID());
            PackBonus.tick(p);
            p.removeAllEffects();
        }
        PackSkills.reset();
        AlphaRoar.reset();
        AlphaRoar.overridePlayers(null);
        NpcPackBrain.reset();
        NpcOrders.reset();
        NpcHunt.reset();
        SpeciesConfig.overrideMembers(null);
        net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(null);
    }

    private static EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalStateException("no entity " + id));
    }

    /** A wild werewolf beast, wounded to {@code health} of its maximum. */
    private static Mob wildWolf(GameTestHelper helper, String id, double x, float health) {
        Mob m = NpcPackSpawner.place(helper.getLevel(), NpcPackSpawner.asMob(type(id)), helper.absolutePos(new BlockPos((int) x, 1, 2)), NpcPackSpawner.HALF_LEVEL);
        helper.assertTrue(m != null, "could not place " + id);
        m.setHealth(Math.max(1f, m.getMaxHealth() * health));
        return m;
    }

    private static Mob brother(GameTestHelper helper, ServerPlayer owner, double x) {
        Mob m = wildWolf(helper, "werewolves:werewolf_beast", x, 0.3f);
        String refused = WerewolfBrotherRiteFactory.recruit(owner, m);
        helper.assertTrue(refused == null, "the recruit was refused: " + refused);
        return m;
    }

    private static NpcBond bond(Mob m) {
        return m.getData(SpeciesRegistries.NPC_BOND);
    }

    private static int token(GameTestHelper helper, Mob m) {
        return NpcIndex.get(helper.getLevel()).of(m.getUUID()).orElseThrow().token();
    }

    private static String key(Component c) {
        return c == null ? null : c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    private static double bonus(ServerPlayer p) {
        var a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        var m = a == null ? null : a.getModifier(PackBonus.MODIFIER_ID);
        return m == null ? 0 : m.amount();
    }

    // ---- the rite ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBeatenWolfBecomesABrotherAndTheRulesRefuseTheRest(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BITTEN);
        ServerPlayer human = TestPlayers.createMortal(helper);
        online(owner, human);
        Mob fresh = wildWolf(helper, "werewolves:werewolf_beast", 4, 1f);
        Mob beaten = wildWolf(helper, "werewolves:werewolf_beast", 6, 0.4f);
        Mob alpha = wildWolf(helper, de.teamlapen.werewolves.core.ModEntities.ALPHA_WEREWOLF.getId().toString(), 8, 0.2f);
        List<Mob> mobs = List.of(fresh, beaten, alpha);
        try {
            helper.assertTrue(WerewolfBrotherRiteFactory.check(human, beaten) == NpcRules.Refusal.NOT_WEREWOLF, "a human recruited");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(owner, fresh) == NpcRules.Refusal.NOT_BEATEN, "an unhurt wolf was lured");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(owner, alpha) == NpcRules.Refusal.ALPHA, "an Alpha was recruited");
            helper.assertTrue(WerewolfBrotherRiteFactory.recruit(owner, beaten) == null, "a beaten wolf was refused");
            NpcBond b = bond(beaten);
            helper.assertTrue(b.source() == NpcBond.Source.BROTHER && b.owner().equals(owner.getUUID()) && b.order() == NpcBond.Order.FOLLOW, "the bond is " + b);
            helper.assertTrue(!beaten.hasData(SpeciesRegistries.NPC_MARK), "a Brother is still marked as a wild wolf");
            helper.assertTrue(beaten.hasCustomName() && beaten.isPersistenceRequired(), "a Brother is unnamed or may despawn");
            helper.assertTrue(NpcIndex.get(helper.getLevel()).of(beaten.getUUID()).isPresent(), "the ledger does not know it");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(owner, beaten) == NpcRules.Refusal.ALREADY_BONDED, "a Brother was recruited twice");
        } finally {
            clean(helper, mobs, owner, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCapsAreTwoPerWerewolfAndSixPerPack(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        List<ServerPlayer> betas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            betas.add(wolf(helper, Rank.BETA, Lineage.BORN));
        }
        List<ServerPlayer> everyone = new ArrayList<>(betas);
        everyone.add(alpha);
        online(everyone.toArray(new ServerPlayer[0]));
        PackIndex packs = PackIndex.get(helper.getLevel());
        List<Mob> mobs = new ArrayList<>();
        try {
            betas.forEach(b -> packs.join(alpha.getUUID(), b.getUUID()));
            mobs.add(brother(helper, betas.get(0), 4));
            mobs.add(brother(helper, betas.get(0), 5));
            Mob third = wildWolf(helper, "werewolves:werewolf_beast", 6, 0.3f);
            mobs.add(third);
            helper.assertTrue(WerewolfBrotherRiteFactory.check(betas.get(0), third) == NpcRules.Refusal.OWNER_CAP, "a third Brother for one werewolf");
            for (int i = 1; i <= 2; i++) {
                mobs.add(brother(helper, betas.get(i), 6 + i));
                mobs.add(brother(helper, betas.get(i), 6 + i));
            }
            helper.assertTrue(NpcMembers.brothersInPack(helper.getLevel(), alpha.getUUID()) == 6, "the pack should have 6");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(betas.get(3), third) == NpcRules.Refusal.PACK_CAP, "a seventh Brother for the pack");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(alpha, third) == NpcRules.Refusal.PACK_CAP, "the Alpha got a seventh too");
            SpeciesConfig.overrideMembers(new SpeciesConfig.MemberTuning(false, 2, 6, true, 10, true, 0.5, 0.25, 0.4));
            helper.assertTrue(WerewolfBrotherRiteFactory.check(betas.get(3), third) == NpcRules.Refusal.OFF, "npc_recruit_enabled = false still recruits");
        } finally {
            clean(helper, mobs, everyone.toArray(new ServerPlayer[0]));
        }
        helper.succeed();
    }

    // ---- the owner away, the orders ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherKeepsItsBondAndRestsInPlaceWhileItsOwnerIsAway(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 5);
        try {
            helper.assertTrue(key(NpcCommands.order(owner, token(helper, b), "guard")) == null, "the guard order was refused");
            NpcOrders.tickNow(b);
            helper.assertTrue(NpcMembers.effectiveOrder(helper.getLevel().getServer(), bond(b)) == NpcBond.Order.GUARD, "guarding with its owner here");
            online(); // the owner leaves the server
            helper.assertTrue(NpcMembers.effectiveOrder(helper.getLevel().getServer(), bond(b)) == NpcBond.Order.REST, "it did not rest with its owner away");
            Vec3 at = b.position();
            NpcOrders.tickNow(b);
            helper.assertTrue(bond(b).bonded() && bond(b).owner().equals(owner.getUUID()) && bond(b).order() == NpcBond.Order.GUARD, "the bond or the order was lost: " + bond(b));
            helper.assertTrue(b.position().distanceTo(at) < 1, "it left its place");
            helper.assertTrue(NpcIndex.get(helper.getLevel()).of(b.getUUID()).isPresent(), "the ledger lost it");
            online(owner); // the owner is back
            helper.assertTrue(NpcMembers.effectiveOrder(helper.getLevel().getServer(), bond(b)) == NpcBond.Order.GUARD, "it did not guard again");
        } finally {
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOrdersFollowGuardAndRest(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer mate = wolf(helper, Rank.BETA, Lineage.BORN);
        online(owner, mate);
        PackIndex.get(helper.getLevel()).join(owner.getUUID(), mate.getUUID());
        Mob b = brother(helper, owner, 5);
        Zombie foe = helper.spawn(EntityType.ZOMBIE, new Vec3(8.5, 1, 4.5));
        try {
            // Follow: it comes to an owner who went far away
            owner.setPos(owner.position().add(60, 0, 0));
            NpcOrders.tickNow(b);
            helper.assertTrue(b.distanceTo(owner) < 6, "it did not catch up with a faraway owner: " + b.distanceTo(owner));
            owner.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
            b.teleportTo(owner.getX() + 2, owner.getY(), owner.getZ());
            int t = token(helper, b);
            // Guard: it takes up what hurt its owner
            helper.assertTrue(NpcCommands.order(owner, t, "guard") == null, "guard refused");
            owner.setLastHurtByMob(foe);
            b.setTarget(null);
            NpcOrders.tickNow(b);
            helper.assertTrue(b.getTarget() == foe, "a guard did not take up what hurt its owner");
            // never a friend
            b.setTarget(null);
            owner.setLastHurtByMob(mate);
            NpcOrders.tickNow(b);
            helper.assertTrue(b.getTarget() == null, "a guard took a pack mate as its target");
            // Rest: it only fights back
            helper.assertTrue(NpcCommands.order(owner, t, "rest") == null, "rest refused");
            b.setTarget(foe);
            NpcOrders.tickNow(b);
            helper.assertTrue(b.getTarget() == null, "a resting wolf kept a target it chose itself");
            helper.assertTrue(bond(b).order() == NpcBond.Order.REST, "the order is " + bond(b).order());
            helper.assertTrue(NpcCommands.order(owner, t, "dance") != null, "a word that is no order was accepted");
        } finally {
            clean(helper, List.of(b, foe), owner, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheOwnerOrThePacksLeaderCommandsABrother(GameTestHelper helper) {
        ServerPlayer leader = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer owner = wolf(helper, Rank.BETA, Lineage.BORN);
        ServerPlayer mate = wolf(helper, Rank.BETA, Lineage.BORN);
        ServerPlayer stranger = wolf(helper, Rank.ALPHA, Lineage.BORN);
        online(leader, owner, mate, stranger);
        PackIndex packs = PackIndex.get(helper.getLevel());
        packs.join(leader.getUUID(), owner.getUUID());
        packs.join(leader.getUUID(), mate.getUUID());
        Mob b = brother(helper, owner, 5);
        try {
            int t = token(helper, b);
            helper.assertTrue("supernaturalbloodlines.npc.unknown".equals(key(NpcCommands.order(stranger, t, "rest"))), "a stranger commanded it");
            helper.assertTrue("supernaturalbloodlines.npc.unknown".equals(key(NpcCommands.order(mate, t, "rest"))), "a pack mate who does not lead commanded it");
            helper.assertTrue("supernaturalbloodlines.npc.unknown".equals(key(NpcCommands.release(stranger, t))), "a stranger released it");
            helper.assertTrue(NpcCommands.order(leader, t, "rest") == null, "the leader of the pack was refused");
            helper.assertTrue(NpcCommands.order(owner, t, "guard") == null, "the owner was refused");
            helper.assertTrue(NpcCommands.release(owner, t) == null && !b.hasData(SpeciesRegistries.NPC_BOND), "the owner could not release it");
            helper.assertTrue(NpcIndex.get(helper.getLevel()).of(b.getUUID()).isEmpty(), "the ledger kept a released wolf");
        } finally {
            clean(helper, List.of(b), leader, owner, mate, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherLeavesThePackBonusWithItsOwner(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer beta = wolf(helper, Rank.BETA, Lineage.BORN);
        online(alpha, beta);
        PackIndex packs = PackIndex.get(helper.getLevel());
        packs.join(alpha.getUUID(), beta.getUUID());
        Mob b = brother(helper, beta, 5);
        try {
            helper.assertTrue(NpcMembers.near(alpha, 64).contains(b), "the Alpha's pack does not list the Brother of a Beta");
            packs.leave(beta.getUUID());
            helper.assertTrue(!NpcMembers.near(alpha, 64).contains(b), "the Brother stayed in a pack its owner left");
            helper.assertTrue(bond(b).owner().equals(beta.getUUID()), "the Brother changed owner");
            helper.assertTrue(NpcMembers.near(beta, 64).contains(b), "the Brother is not with its owner");
        } finally {
            clean(helper, List.of(b), alpha, beta);
        }
        helper.succeed();
    }

    // ---- the pack's gifts ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brothersCountHalfTowardThePackBonusWithItsCap(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer b1 = wolf(helper, Rank.BETA, Lineage.BORN);
        ServerPlayer b2 = wolf(helper, Rank.BETA, Lineage.BORN);
        online(alpha, b1, b2);
        PackIndex packs = PackIndex.get(helper.getLevel());
        packs.join(alpha.getUUID(), b1.getUUID());
        packs.join(alpha.getUUID(), b2.getUUID());
        List<Mob> mobs = new ArrayList<>();
        try {
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) == 0, "two Betas alone gave a bonus");
            mobs.add(brother(helper, b1, 4));
            mobs.add(brother(helper, b1, 5));
            PackBonus.tick(alpha);
            helper.assertTrue(Math.abs(bonus(alpha) - 0.15) < 1e-9, "2 Betas and 2 Brothers should give 15%, got " + bonus(alpha));
            helper.assertTrue(mobs.stream().allMatch(m -> m.hasEffect(MobEffects.REGENERATION)), "a Brother got no Regeneration");
            mobs.add(brother(helper, b2, 6));
            PackBonus.tick(alpha);
            helper.assertTrue(Math.abs(bonus(alpha) - 0.175) < 1e-9, "a third Brother should add half a step, got " + bonus(alpha));
        } finally {
            clean(helper, mobs, alpha, b1, b2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLoneAlphaWithOnlyBrothersHasNoPackAndNoBonus(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        online(alpha);
        List<Mob> mobs = new ArrayList<>();
        try {
            mobs.add(brother(helper, alpha, 4));
            mobs.add(brother(helper, alpha, 5));
            PackBonus.tick(alpha);
            helper.assertTrue(!PackIndex.get(helper.getLevel()).inPack(alpha.getUUID()), "Brothers made a pack of players");
            helper.assertTrue(bonus(alpha) == 0, "two Brothers alone gave a bonus: " + bonus(alpha));
        } finally {
            clean(helper, mobs, alpha);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRoarAndTheSurgeBuffBrothers(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer beta = wolf(helper, Rank.BETA, Lineage.BORN);
        online(alpha, beta);
        PackIndex.get(helper.getLevel()).join(alpha.getUUID(), beta.getUUID());
        Mob near = brother(helper, beta, 5);
        try {
            helper.assertTrue(PackSkills.surge(alpha, 100) == null, "the surge was refused");
            helper.assertTrue(near.hasEffect(MobEffects.REGENERATION) && near.hasEffect(MobEffects.ABSORPTION) && near.hasEffect(MobEffects.DAMAGE_BOOST), "the Surge did not reach a Brother");
            near.removeAllEffects();
            helper.assertTrue(AlphaRoar.roar(alpha, 100) == null, "the roar was refused");
            helper.assertTrue(near.hasEffect(MobEffects.DAMAGE_BOOST) && near.hasEffect(MobEffects.MOVEMENT_SPEED), "the Roar did not buff a Brother");
            helper.assertTrue(near.hasData(SpeciesRegistries.NPC_BOND) && !near.hasData(SpeciesRegistries.NPC_MARK), "the Roar put a Brother back among the wild");
        } finally {
            clean(helper, List.of(near), alpha, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ownAndAlliedWolvesNeverHurtEachOther(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer mate = wolf(helper, Rank.BETA, Lineage.BORN);
        ServerPlayer ally = wolf(helper, Rank.ALPHA, Lineage.BORN);
        ServerPlayer stranger = wolf(helper, Rank.ALPHA, Lineage.BORN);
        online(owner, mate, ally, stranger);
        PackIndex packs = PackIndex.get(helper.getLevel());
        packs.join(owner.getUUID(), mate.getUUID());
        packs.ally(owner.getUUID(), ally.getUUID());
        Mob brother = brother(helper, owner, 5);
        Mob allyBrother = brother(helper, ally, 6);
        Mob strangerBrother = brother(helper, stranger, 7);
        try {
            for (ServerPlayer friend : List.of(owner, mate, ally)) {
                float before = friend.getHealth();
                friend.hurt(brother.damageSources().mobAttack(brother), 4f);
                helper.assertTrue(friend.getHealth() == before, "the Brother hurt " + friend.getName().getString());
                float wolfBefore = brother.getHealth();
                brother.hurt(friend.damageSources().playerAttack(friend), 4f);
                helper.assertTrue(brother.getHealth() == wolfBefore, friend.getName().getString() + " hurt the Brother");
            }
            float before = allyBrother.getHealth();
            allyBrother.hurt(brother.damageSources().mobAttack(brother), 4f);
            helper.assertTrue(allyBrother.getHealth() == before, "a Brother hurt the Brother of an ally");
            helper.assertTrue(allyBrother.getHealth() == before, "the ally's Brother is not safe");
            brother.setTarget(allyBrother);
            helper.assertTrue(brother.getTarget() == null, "a Brother turned on the Brother of an ally");
            brother.setTarget(mate);
            helper.assertTrue(brother.getTarget() == null, "a Brother turned on a pack mate");
            // controls: a stranger is not a friend
            float s = stranger.getHealth();
            stranger.hurt(brother.damageSources().mobAttack(brother), 4f);
            helper.assertTrue(stranger.getHealth() < s, "control: a Brother cannot hurt a stranger");
            float sb = strangerBrother.getHealth();
            strangerBrother.hurt(brother.damageSources().mobAttack(brother), 4f);
            helper.assertTrue(strangerBrother.getHealth() < sb, "control: a Brother cannot hurt the Brother of a stranger");
        } finally {
            packs.unally(owner.getUUID(), ally.getUUID());
            clean(helper, List.of(brother, allyBrother, strangerBrother), owner, mate, ally, stranger);
        }
        helper.succeed();
    }

    // ---- hunting trips, sorting, the spark ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHuntingTripCollectsStoresAndSorts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        BlockPos chestAt = new BlockPos(6, 1, 6);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestAt);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        chest.setItem(4, new ItemStack(Items.DIAMOND_SWORD));
        chest.setItem(9, new ItemStack(Items.COBBLESTONE, 20));
        chest.setItem(13, new ItemStack(Items.COBBLESTONE, 20));
        Mob b = brother(helper, owner, 4);
        Cow a = helper.spawn(EntityType.COW, new Vec3(3.5, 1, 6.5));
        Cow c = helper.spawn(EntityType.COW, new Vec3(4.5, 1, 6.5));
        Cow named = helper.spawn(EntityType.COW, new Vec3(5.5, 1, 7.5));
        named.setCustomName(Component.literal("Daisy"));
        Cow baby = helper.spawn(EntityType.COW, new Vec3(2.5, 1, 7.5));
        baby.setBaby(true);
        try {
            BlockPos post = helper.absolutePos(new BlockPos(4, 1, 4));
            NpcBond hunting = bond(b).withOrder(NpcBond.Order.GUARD, post.asLong()).withHunt(true).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, hunting);
            int killed = NpcHunt.go(b, hunting, true);
            helper.assertTrue(killed == 2, "two plain cows should have been hunted, got " + killed);
            helper.assertTrue(named.isAlive() && baby.isAlive(), "a named or baby animal was hunted");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(post).inflate(30)).isEmpty(), "loot lies on the ground instead of in the chest");
            int beef = 0;
            int stacks = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack s = chest.getItem(i);
                if (s.is(Items.BEEF) || s.is(Items.LEATHER)) {
                    beef += s.getCount();
                }
                if (s.is(Items.COBBLESTONE)) {
                    stacks++;
                    helper.assertTrue(s.getCount() == 60, "the cobblestone was not merged: " + s.getCount());
                }
            }
            helper.assertTrue(beef > 0, "the kills' drops are not in the chest");
            helper.assertTrue(stacks == 1, "partial stacks were not merged: " + stacks);
            helper.assertTrue(chest.getItem(0).is(Items.DIAMOND_SWORD), "the chest is not ordered: slot 0 is " + chest.getItem(0));
        } finally {
            a.discard();
            c.discard();
            named.discard();
            baby.discard();
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherNeverTakesTheSpark(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.BETA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        Mob alpha = NpcPackSpawner.spawnPackAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 8)), new NpcSpawnRules.Plan(true, true, 1, false), 24).get(0);
        List<Mob> mobs = new ArrayList<>(List.of(b));
        try {
            WolfState before = PackAccess.wolf(owner);
            helper.assertTrue(SparkHandler.killerOf(alpha, b.damageSources().mobAttack(b)) == null, "a Brother counts as the killer");
            alpha.hurt(b.damageSources().mobAttack(b), 10_000f);
            helper.assertTrue(!alpha.isAlive(), "the Alpha is still alive");
            WolfState after = PackAccess.wolf(owner);
            helper.assertTrue(after.sparks() == before.sparks() && after.weakSparks() == 0 && after.rank() == before.rank(), "the owner got a spark through its Brother: " + after);
            helper.assertTrue(PackAccess.effectiveSparks(owner) == 1, "the sparks changed");
        } finally {
            alpha.discard();
            mobs.addAll(helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(owner.blockPosition()).inflate(24), m -> m.hasData(SpeciesRegistries.NPC_MARK)));
            clean(helper, mobs, owner);
        }
        helper.succeed();
    }
}
