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

    /** Positions whose right click a (test) protection refuses: stands in for a claim of Open Parties and Claims. */
    private static volatile BlockPos deny;

    static {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            BlockPos d = deny;
            if (d != null && e.getPos().equals(d)) {
                e.setCanceled(true);
            }
        });
    }

    private static int count(net.minecraft.world.Container c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    private static int stacks(net.minecraft.world.Container c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n++;
            }
        }
        return n;
    }

    private static final java.util.concurrent.atomic.AtomicInteger clicks = new java.util.concurrent.atomic.AtomicInteger();

    static {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> clicks.incrementAndGet());
    }

    private static List<Cow> cows(GameTestHelper helper, int n) {
        List<Cow> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(helper.spawn(EntityType.COW, new Vec3(3.5 + i, 1, 6.5)));
        }
        return out;
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
            helper.assertTrue("supernaturalbloodlines.npc.release_owner".equals(key(NpcCommands.release(leader, t))) && b.hasData(SpeciesRegistries.NPC_BOND), "the leader freed another member's Brother");
            helper.assertTrue(NpcCommands.order(owner, t, "follow") == null, "the owner could not make it follow");
            helper.assertTrue("supernaturalbloodlines.npc.needs_post".equals(key(NpcCommands.toggleHunt(owner, t))) && !bond(b).hunt(), "Hunt was turned on for a follower");
            helper.assertTrue(NpcCommands.order(owner, t, "guard") == null, "the owner was refused");
            helper.assertTrue(NpcCommands.toggleHunt(leader, t) == null && bond(b).hunt(), "the leader could not turn Hunt on for a guard");
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
            int killed = NpcHunt.go(b, hunting, true, true);
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

    // ---- panel round 1: containers, the ledger, release, the cap ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherOnlyUsesPlainChestsAndBarrelsTheOwnerMayOpen(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        BlockPos furnaceAt = new BlockPos(4, 1, 5);
        BlockPos deniedAt = new BlockPos(5, 1, 3);
        BlockPos barrelAt = new BlockPos(8, 1, 4);
        helper.setBlock(furnaceAt, Blocks.FURNACE);
        helper.setBlock(deniedAt, Blocks.CHEST);
        helper.setBlock(barrelAt, Blocks.BARREL);
        var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) helper.getBlockEntity(furnaceAt);
        furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        ChestBlockEntity denied = (ChestBlockEntity) helper.getBlockEntity(deniedAt);
        denied.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        denied.setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        denied.setItem(5, new ItemStack(Items.COBBLESTONE, 20));
        var barrel = (net.minecraft.world.level.block.entity.BarrelBlockEntity) helper.getBlockEntity(barrelAt);
        barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        barrel.setItem(2, new ItemStack(Items.COBBLESTONE, 20));
        List<Cow> cows = cows(helper, 2);
        try {
            deny = helper.absolutePos(deniedAt);
            NpcBond hunting = bond(b).withOrder(NpcBond.Order.GUARD, helper.absolutePos(new BlockPos(4, 1, 4)).asLong()).withHunt(true).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, hunting);
            helper.assertTrue(NpcHunt.go(b, hunting, true, true) == 2, "the two cows were not hunted");
            helper.assertTrue(furnace.getItem(0).getCount() == 5 && furnace.getItem(1).isEmpty() && furnace.getItem(2).isEmpty(), "a furnace was used as a store");
            helper.assertTrue(denied.getItem(0).getCount() == 20 && denied.getItem(3).is(Items.DIAMOND_SWORD) && denied.getItem(5).getCount() == 20 && count(denied, Items.BEEF) == 0,
                    "a chest the owner may not open was sorted or filled");
            helper.assertTrue(stacks(barrel, Items.COBBLESTONE) == 1 && count(barrel, Items.COBBLESTONE) == 40, "the barrel the owner may open was not tidied");
            helper.assertTrue(count(barrel, Items.BEEF) + count(barrel, Items.LEATHER) > 0, "the drops did not reach the barrel");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(30)).isEmpty(), "items lie on the ground");
        } finally {
            deny = null;
            cows.forEach(Entity::discard);
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leftoversStayWhereTheAnimalFellAndNothingIsLost(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        BlockPos barrelAt = new BlockPos(6, 1, 4);
        helper.setBlock(barrelAt, Blocks.BARREL);
        var barrel = (net.minecraft.world.level.block.entity.BarrelBlockEntity) helper.getBlockEntity(barrelAt);
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            barrel.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        List<Cow> cows = cows(helper, 2);
        try {
            NpcBond hunting = bond(b).withOrder(NpcBond.Order.GUARD, helper.absolutePos(new BlockPos(4, 1, 4)).asLong()).withHunt(true).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, hunting);
            helper.assertTrue(NpcHunt.go(b, hunting, true, true) == 2, "the cows were not hunted");
            helper.assertTrue(count(barrel, Items.COBBLESTONE) == 64 * barrel.getContainerSize(), "the full barrel lost or gained stone");
            helper.assertTrue(count(barrel, Items.BEEF) + count(barrel, Items.LEATHER) == 0, "something went into a full barrel");
            int onGround = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 6))).inflate(6)).stream().mapToInt(i -> i.getItem().getCount()).sum();
            helper.assertTrue(onGround > 0, "the drops that did not fit vanished");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(0.4)).isEmpty(), "leftovers were dropped at the post");
        } finally {
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(30)).forEach(Entity::discard);
            cows.forEach(Entity::discard);
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherNeitherHuntsNorSortsWhileItsOwnerIsOffline(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        BlockPos chestAt = new BlockPos(6, 1, 4);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestAt);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        chest.setItem(7, new ItemStack(Items.COBBLESTONE, 20));
        List<Cow> cows = cows(helper, 2);
        try {
            NpcBond hunting = bond(b).withOrder(NpcBond.Order.GUARD, helper.absolutePos(new BlockPos(4, 1, 4)).asLong()).withHunt(true).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, hunting);
            online(); // the owner leaves the server
            helper.assertTrue(NpcHunt.maybeHunt(b, true) == 0 && cows.stream().allMatch(Entity::isAlive), "it hunted for an absent owner");
            helper.assertTrue(NpcHunt.go(b, hunting, true, true) == 0 && cows.stream().allMatch(Entity::isAlive), "a trip with no owner killed animals");
            helper.assertTrue(stacks(chest, Items.COBBLESTONE) == 2, "it sorted a chest for an absent owner");
            online(owner); // the owner is back: the same Brother hunts
            helper.assertTrue(NpcHunt.maybeHunt(b, true) == 2, "it did not hunt with its owner here");
            helper.assertTrue(stacks(chest, Items.COBBLESTONE) == 1, "it did not sort with its owner here");
        } finally {
            cows.forEach(Entity::discard);
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTransformedBrotherKeepsItsBondAndNoGhostIsLeft(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        // a human werewolf that turns into a beast (the way a wild wolf is made), then recruited: its transformation back needs the first body
        Mob human0 = wildWolf(helper, "werewolves:human_werewolf", 4, 1f);
        Mob beast = (Mob) ((de.teamlapen.werewolves.api.entities.werewolf.WerewolfTransformable) human0).transformToWerewolf(de.teamlapen.werewolves.api.entities.werewolf.TransformType.TIME_LIMITED);
        List<Mob> mobs = new ArrayList<>(List.of(human0, beast));
        try {
            beast.setHealth(beast.getMaxHealth() * 0.3f);
            String refused = WerewolfBrotherRiteFactory.recruit(owner, beast);
            helper.assertTrue(refused == null, "the recruit was refused: " + refused);
            int token = token(helper, beast);
            java.util.UUID beastId = beast.getUUID();
            Mob human = (Mob) ((de.teamlapen.werewolves.api.entities.werewolf.WerewolfTransformable) beast).transformBack();
            mobs.add(human);
            helper.assertTrue(beast.isRemoved() && human != beast, "the transformation did not replace the wolf");
            helper.assertTrue(NpcMembers.bondOf(human).filter(b -> b.owner().equals(owner.getUUID()) && b.source() == NpcBond.Source.BROTHER).isPresent(), "the human form lost the bond");
            helper.assertTrue(NpcIndex.get(helper.getLevel()).of(beastId).isEmpty(), "a ghost entry is left for the old body");
            helper.assertTrue(NpcIndex.get(helper.getLevel()).of(human.getUUID()).filter(e -> e.token() == token).isPresent(), "the member changed its token or was lost");
            helper.assertTrue(NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 1, "the owner has more than one Brother after a transformation");
            helper.assertTrue(human.hasCustomName(), "the name was lost");
            Mob again = (Mob) ((de.teamlapen.werewolves.api.entities.werewolf.WerewolfTransformable) human).transformToWerewolf(de.teamlapen.werewolves.api.entities.werewolf.TransformType.TIME_LIMITED);
            mobs.add(again);
            helper.assertTrue(NpcMembers.bondOf(again).isPresent() && NpcIndex.get(helper.getLevel()).of(human.getUUID()).isEmpty()
                    && NpcIndex.get(helper.getLevel()).of(again.getUUID()).filter(e -> e.token() == token).isPresent(), "the second transformation lost the member");
            helper.assertTrue(NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 1, "a second body was counted");
        } finally {
            clean(helper, mobs, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherThatLeavesTheWorldIsForgottenAndAGhostEntryHeals(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        Mob ghost = (Mob) type("werewolves:werewolf_beast").create(helper.getLevel());
        NpcIndex index = NpcIndex.get(helper.getLevel());
        List<Mob> mobs = new ArrayList<>(List.of(b, ghost));
        try {
            helper.assertTrue(NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 1, "setup");
            b.discard(); // removed for good, with no successor: not a death, not an unload
            helper.assertTrue(index.of(b.getUUID()).isEmpty() && NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 0, "a Brother that left the world still fills a place");
            // an entry for a wolf that has no bond, and the wolf joins a level: the entry goes
            index.put(ghost.getUUID(), owner.getUUID(), NpcBond.Source.BROTHER, "Ghost", "werewolves:werewolf_beast", "minecraft:overworld", 0, 0, 0, 0);
            helper.assertTrue(NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 1, "the ghost was not written");
            ghost.setPos(helper.absoluteVec(new Vec3(5.5, 1, 2.5)));
            helper.getLevel().addFreshEntity(ghost);
            helper.assertTrue(index.of(ghost.getUUID()).isEmpty() && NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 0, "a ledger entry for a wolf with no bond survived its joining");
        } finally {
            clean(helper, mobs, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRiteTakesOnlyBeastsAndPicksTheNearestRecruitableOne(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob taken = brother(helper, owner, 4);
        Mob free = wildWolf(helper, "werewolves:werewolf_beast", 6, 0.3f);
        Mob alpha = wildWolf(helper, de.teamlapen.werewolves.core.ModEntities.ALPHA_WEREWOLF.getId().toString(), 3, 0.3f);
        Mob human = wildWolf(helper, "werewolves:human_werewolf", 5, 0.3f);
        List<Mob> mobs = List.of(taken, free, alpha, human);
        try {
            BlockPos centre = helper.absolutePos(new BlockPos(4, 1, 2));
            helper.assertTrue(WerewolfBrotherRiteFactory.wolfNear(helper.getLevel(), centre) == free, "the rite did not skip the bonded wolf, the Alpha and the human form that stand nearer");
            helper.assertTrue(WerewolfBrotherRiteFactory.check(owner, human) == NpcRules.Refusal.NOT_A_WOLF, "the human form was taken");
            free.discard();
            helper.assertTrue(WerewolfBrotherRiteFactory.wolfNear(helper.getLevel(), centre) == null, "with no wolf left the rite still found one");
        } finally {
            clean(helper, mobs, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherReleasedWhileUnloadedStaysReleasedWhenItLoads(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        NpcIndex index = NpcIndex.get(helper.getLevel());
        // a Brother in a chunk that is not loaded: its bond is on the entity (not in the world yet), its entry is in the ledger
        Mob m = (Mob) type("werewolves:werewolf_beast").create(helper.getLevel());
        m.setData(SpeciesRegistries.NPC_BOND, NpcBond.brother(owner.getUUID(), 0, 0));
        index.put(m.getUUID(), owner.getUUID(), NpcBond.Source.BROTHER, "Far", "werewolves:werewolf_beast", "minecraft:overworld", 0, 0, 0, 0);
        try {
            int t = index.of(m.getUUID()).orElseThrow().token();
            helper.assertTrue(NpcMembers.live(helper.getLevel().getServer(), m.getUUID()).isEmpty(), "setup: the wolf is loaded");
            helper.assertTrue(NpcCommands.release(owner, t) == null, "an unloaded Brother could not be released");
            helper.assertTrue(index.of(m.getUUID()).isEmpty() && NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 0, "the entry stayed after the release");
            m.setPos(helper.absoluteVec(new Vec3(4.5, 1, 2.5)));
            helper.getLevel().addFreshEntity(m); // the chunk loads
            helper.assertTrue(!NpcMembers.bondOf(m).isPresent(), "the released wolf got its bond back when it loaded");
            helper.assertTrue(index.of(m.getUUID()).isEmpty() && NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 0, "the released wolf is in the ledger again");
            // no list of the released is kept, so there is nothing to overflow: 300 wolves released while unloaded are all freed when they load
            List<Mob> many = new ArrayList<>();
            try {
                for (int i = 0; i < 300; i++) {
                    Mob far = (Mob) type("werewolves:werewolf_beast").create(helper.getLevel());
                    far.setData(SpeciesRegistries.NPC_BOND, NpcBond.brother(owner.getUUID(), 0, 0)); // bonded, and no entry: freed while out of reach
                    far.setPos(helper.absoluteVec(new Vec3(4.5, 1, 2.5)));
                    many.add(far);
                }
                many.forEach(far -> helper.getLevel().addFreshEntity(far));
                helper.assertTrue(many.stream().allMatch(far -> NpcMembers.bondOf(far).isEmpty()) && NpcMembers.brothers(helper.getLevel(), owner.getUUID()) == 0, "a released wolf came back after many others were released");
            } finally {
                many.forEach(Entity::discard);
            }
        } finally {
            clean(helper, List.of(m), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerJoiningAFullPackMakesTheNewestBrothersRest(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BORN);
        List<ServerPlayer> betas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            betas.add(wolf(helper, Rank.BETA, Lineage.BORN));
        }
        ServerPlayer newcomer = wolf(helper, Rank.BETA, Lineage.BORN);
        List<ServerPlayer> everyone = new ArrayList<>(betas);
        everyone.add(alpha);
        everyone.add(newcomer);
        online(everyone.toArray(new ServerPlayer[0]));
        PackIndex packs = PackIndex.get(helper.getLevel());
        List<Mob> mobs = new ArrayList<>();
        try {
            betas.forEach(b -> packs.join(alpha.getUUID(), b.getUUID()));
            List<Mob> old = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                old.add(brother(helper, betas.get(i), 4 + i));
                old.add(brother(helper, betas.get(i), 4 + i));
            }
            Mob n1 = brother(helper, newcomer, 7); // a lone werewolf may keep two
            Mob n2 = brother(helper, newcomer, 8);
            mobs.addAll(old);
            mobs.add(n1);
            mobs.add(n2);
            packs.join(alpha.getUUID(), newcomer.getUUID()); // the pack now holds 8
            helper.assertTrue(NpcMembers.brothersInPack(helper.getLevel(), alpha.getUUID()) == 8, "setup: the pack should hold 8");
            helper.assertTrue(old.stream().noneMatch(m -> NpcMembers.overCap(helper.getLevel(), m.getUUID())), "an old Brother lost its place");
            helper.assertTrue(NpcMembers.overCap(helper.getLevel(), n1.getUUID()) && NpcMembers.overCap(helper.getLevel(), n2.getUUID()), "the newest Brothers kept their place in a full pack");
            helper.assertTrue(NpcMembers.near(alpha, 64).size() == 6 && !NpcMembers.near(alpha, 64).contains(n1), "the extra Brothers count for the pack");
            n1.setData(SpeciesRegistries.NPC_BOND, bond(n1).withOrder(NpcBond.Order.GUARD, n1.blockPosition().asLong()));
            helper.assertTrue(NpcMembers.effectiveOrder(helper.getLevel().getServer(), n1) == NpcBond.Order.REST, "an extra Brother did not rest");
            // room is made: the older of the two takes a place
            NpcMembers.release(old.get(0));
            helper.assertTrue(!NpcMembers.overCap(helper.getLevel(), n1.getUUID()) && NpcMembers.overCap(helper.getLevel(), n2.getUUID()), "a freed place did not go to the oldest extra");
        } finally {
            clean(helper, mobs, everyone.toArray(new ServerPlayer[0]));
        }
        helper.succeed();
    }

    // ---- panel round 2: the protection check needs the owner near, in the same dimension ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherUsesNoChestWhileItsOwnerIsInAnotherDimension(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        BlockPos chestAt = new BlockPos(6, 1, 4);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestAt);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        chest.setItem(7, new ItemStack(Items.COBBLESTONE, 20));
        List<Cow> cows = cows(helper, 2);
        ServerLevel home = (ServerLevel) owner.level();
        try {
            NpcBond hunting = bond(b).withOrder(NpcBond.Order.GUARD, helper.absolutePos(new BlockPos(4, 1, 4)).asLong()).withHunt(true).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, hunting);
            // the claim is only protected in this world: the owner stands in the Nether, where the same click would be allowed
            owner.setServerLevel(helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER));
            clicks.set(0);
            helper.assertTrue(NpcHunt.go(b, hunting, true, true) == 2, "the Brother did not hunt");
            helper.assertTrue(clicks.get() == 0, "the protection check was fired for an owner in another dimension");
            helper.assertTrue(stacks(chest, Items.COBBLESTONE) == 2 && count(chest, Items.BEEF) + count(chest, Items.LEATHER) == 0, "the chest was sorted or filled with the owner in another dimension");
            owner.setServerLevel(home); // back in the Brother's world: the same trip may use the chest
            helper.assertTrue(NpcHunt.go(b, hunting, false, true) == 0 && clicks.get() > 0 && stacks(chest, Items.COBBLESTONE) == 1, "the chest was not used with the owner in the same dimension");
        } finally {
            owner.setServerLevel(home);
            cows.forEach(Entity::discard);
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(30)).forEach(Entity::discard);
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherFiresNoProtectionCheckAtAChestFarFromItsOwner(GameTestHelper helper) {
        ServerPlayer owner = wolf(helper, Rank.OMEGA, Lineage.BORN);
        online(owner);
        Mob b = brother(helper, owner, 4);
        BlockPos chestAt = new BlockPos(6, 1, 4);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestAt);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 20));
        chest.setItem(7, new ItemStack(Items.COBBLESTONE, 20));
        net.minecraft.world.phys.Vec3 here = owner.position();
        try {
            NpcBond sorting = bond(b).withOrder(NpcBond.Order.REST, helper.absolutePos(new BlockPos(4, 1, 4)).asLong()).withSort(true);
            b.setData(SpeciesRegistries.NPC_BOND, sorting);
            owner.setPos(here.x + 200, here.y, here.z); // far from the post
            clicks.set(0);
            NpcHunt.go(b, sorting, false, true);
            helper.assertTrue(clicks.get() == 0, "a right click was fired at a chest 200 blocks from its owner");
            helper.assertTrue(stacks(chest, Items.COBBLESTONE) == 2, "a chest far from its owner was sorted");
            owner.setPos(helper.absoluteVec(new Vec3(4.5, 1, 1.5))); // near
            NpcHunt.go(b, sorting, false, true);
            helper.assertTrue(clicks.get() > 0 && stacks(chest, Items.COBBLESTONE) == 1, "a chest near its owner was not sorted");
        } finally {
            owner.setPos(here);
            clean(helper, List.of(b), owner);
        }
        helper.succeed();
    }
}