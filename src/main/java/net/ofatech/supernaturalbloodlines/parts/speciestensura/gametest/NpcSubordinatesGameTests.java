package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.tensura.entity.monster.DirewolfEntity;
import io.github.manasmods.tensura.registry.entity.MonsterEntityTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcBond;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcCommands;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcIndex;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcMembers;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcOrders;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.species.npc.WerewolfBrotherRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.NpcSubordinates;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;

/**
 * Plan 15d, R149: a Tensura subordinate of a pack member joins the pack, keeps Tensura's own orders, counts less than a Brother, and leaves when its owner leaves
 * the pack or the bridge is switched off. Needs Tensura and Werewolves; this checks plan question U6 with a real subordinate (a direwolf).
 */
@PrefixGameTestTemplate(false)
public final class NpcSubordinatesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private NpcSubordinatesGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BORN).rank(rank).geneRolled(true).protectPath(false).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "the test wolf could not join the werewolves");
        p.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        return p;
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new java.util.HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
    }

    private static void clean(GameTestHelper helper, List<? extends Entity> mobs, ServerPlayer... players) {
        for (Entity m : mobs) {
            NpcIndex.get(helper.getLevel()).remove(m.getUUID());
            m.discard();
        }
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            PackIndex.get(helper.getLevel()).leave(p.getUUID());
            PackBonus.tick(p);
            p.removeAllEffects();
        }
        AlphaRoar.reset();
        AlphaRoar.overridePlayers(null);
        NpcOrders.reset();
        SpeciesConfig.overrideMembers(null);
        SpeciesTensuraConfig.overrideSubordinatesJoin(null);
        SpeciesTensuraConfig.overrideMaxTensuraMembers(null);
        SireCall.overridePlayerLookup(null);
    }

    /** A tamed direwolf of {@code owner}: a real Tensura subordinate whose Tensura owner link is the owner (U6). */
    private static DirewolfEntity direwolf(GameTestHelper helper, UUID owner, double x) {
        DirewolfEntity d = helper.spawn(MonsterEntityTypes.DIREWOLF.get(), new Vec3(x, 1, 4.5));
        d.setOwnerUUID(owner);
        d.setTame(true, false);
        return d;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSubordinateOfAPackMemberJoinsAndKeepsTensurasOrders(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA);
        ServerPlayer beta = wolf(helper, Rank.BETA);
        online(alpha, beta);
        PackIndex.get(helper.getLevel()).join(alpha.getUUID(), beta.getUUID());
        DirewolfEntity d = direwolf(helper, beta.getUUID(), 5.5);
        try {
            helper.assertTrue(beta.getUUID().equals(d.getOwnerUUID()) && d.isTame(), "U6: the subordinate's owner link is not the player");
            boolean sitting = d.isOrderedToSit();
            helper.assertTrue(NpcSubordinates.evaluate(d), "the subordinate did not join");
            NpcBond b = d.getData(SpeciesRegistries.NPC_BOND);
            helper.assertTrue(b.source() == NpcBond.Source.TENSURA && b.owner().equals(beta.getUUID()), "the bond is " + b);
            NpcOrders.tickNow(d);
            helper.assertTrue(d.isOrderedToSit() == sitting && d.isTame() && beta.getUUID().equals(d.getOwnerUUID()), "our order tick touched a Tensura subordinate");
            helper.assertTrue(NpcMembers.near(alpha, 64).contains(d), "the pack does not list it");
            helper.assertTrue(NpcCommands.order(beta, NpcIndex.get(helper.getLevel()).of(d.getUUID()).orElseThrow().token(), "rest") != null, "an order was taken from the pack for a Tensura subordinate");
            helper.assertTrue(NpcMembers.weight(b) == 0.25, "it should count 0.25, got " + NpcMembers.weight(b));
            // the pack's regeneration reaches it: 1 Beta and nobody else is not enough alone, so give it the room: a second Beta and the subordinate reach 3
            // it leaves with its owner's pack
            PackIndex.get(helper.getLevel()).leave(beta.getUUID());
            helper.assertTrue(!NpcSubordinates.evaluate(d) && !d.hasData(SpeciesRegistries.NPC_BOND), "it stayed in a pack its owner left");
            helper.assertTrue(d.isTame() && beta.getUUID().equals(d.getOwnerUUID()), "leaving the pack touched the subordinate itself");
        } finally {
            clean(helper, List.of(d), alpha, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSubordinateOfABrotherJoinsButNotTheSubordinateOfASubordinate(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA);
        ServerPlayer beta = wolf(helper, Rank.BETA);
        online(alpha, beta);
        PackIndex.get(helper.getLevel()).join(alpha.getUUID(), beta.getUUID());
        Mob brother = NpcPackSpawner.place(helper.getLevel(), NpcPackSpawner.asMob(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("werewolves:werewolf_beast"))),
                helper.absolutePos(new BlockPos(4, 1, 2)), NpcPackSpawner.HALF_LEVEL);
        helper.assertTrue(brother != null, "no Brother placed");
        brother.setHealth(brother.getMaxHealth() * 0.3f);
        helper.assertTrue(WerewolfBrotherRiteFactory.recruit(beta, brother) == null, "the Brother was refused");
        DirewolfEntity ofBrother = direwolf(helper, brother.getUUID(), 5.5);
        DirewolfEntity ofSubordinate = null;
        try {
            helper.assertTrue(NpcSubordinates.evaluate(ofBrother), "the subordinate of a Brother did not join");
            helper.assertTrue(ofBrother.getData(SpeciesRegistries.NPC_BOND).owner().equals(beta.getUUID()), "it joined the Brother's owner's pack as the Brother: "
                    + ofBrother.getData(SpeciesRegistries.NPC_BOND));
            ofSubordinate = direwolf(helper, ofBrother.getUUID(), 6.5);
            helper.assertTrue(!NpcSubordinates.evaluate(ofSubordinate), "the subordinate of a subordinate joined (depth is one step)");
        } finally {
            List<Entity> all = new ArrayList<>(List.of(brother, ofBrother));
            if (ofSubordinate != null) {
                all.add(ofSubordinate);
            }
            clean(helper, all, alpha, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCapAndTheSwitchLimitTheSubordinates(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA);
        ServerPlayer beta = wolf(helper, Rank.BETA);
        online(alpha, beta);
        PackIndex.get(helper.getLevel()).join(alpha.getUUID(), beta.getUUID());
        DirewolfEntity one = direwolf(helper, alpha.getUUID(), 4.5);
        DirewolfEntity two = direwolf(helper, beta.getUUID(), 5.5);
        try {
            SpeciesTensuraConfig.overrideMaxTensuraMembers(1);
            helper.assertTrue(NpcSubordinates.evaluate(one), "the first did not join");
            helper.assertTrue(!NpcSubordinates.evaluate(two), "a second joined a pack that holds one");
            helper.assertTrue(NpcSubordinates.evaluate(one), "a member was put out by the cap");
            SpeciesTensuraConfig.overrideSubordinatesJoin(false);
            helper.assertTrue(!NpcSubordinates.evaluate(one) && !one.hasData(SpeciesRegistries.NPC_BOND), "tensura_subordinates_join = false kept a member");
            helper.assertTrue(one.isTame() && alpha.getUUID().equals(one.getOwnerUUID()), "the switch touched the subordinate itself");
        } finally {
            clean(helper, List.of(one, two), alpha, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSubordinateGetsThePackBonusAndIsProtectedLikeAMember(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA);
        ServerPlayer b1 = wolf(helper, Rank.BETA);
        ServerPlayer b2 = wolf(helper, Rank.BETA);
        online(alpha, b1, b2);
        PackIndex packs = PackIndex.get(helper.getLevel());
        packs.join(alpha.getUUID(), b1.getUUID());
        packs.join(alpha.getUUID(), b2.getUUID());
        List<DirewolfEntity> wolves = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                DirewolfEntity d = direwolf(helper, i % 2 == 0 ? b1.getUUID() : b2.getUUID(), 4.5 + i);
                wolves.add(d);
                helper.assertTrue(NpcSubordinates.evaluate(d), "subordinate " + i + " did not join");
            }
            PackBonus.tick(alpha);
            // 2 Betas + 4 subordinates (4 x 0.25 = 1.0): the minimum of 3 is reached and the sum is 2 + 1.0 = 3 steps
            var mod = alpha.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(PackBonus.MODIFIER_ID);
            helper.assertTrue(mod != null && Math.abs(mod.amount() - 0.15) < 1e-9, "2 Betas and 4 subordinates should give 15%, got " + (mod == null ? 0 : mod.amount()));
            helper.assertTrue(wolves.stream().allMatch(w -> w.hasEffect(MobEffects.REGENERATION)), "a subordinate got no Regeneration");
            float before = wolves.get(0).getHealth();
            wolves.get(0).hurt(b1.damageSources().playerAttack(b1), 4f);
            helper.assertTrue(wolves.get(0).getHealth() == before, "a pack mate hurt a member subordinate");
        } finally {
            clean(helper, wolves, alpha, b1, b2);
        }
        helper.succeed();
    }
}
