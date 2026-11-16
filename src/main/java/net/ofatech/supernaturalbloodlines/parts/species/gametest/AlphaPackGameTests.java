package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrial;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RoarEffectPayload;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyBackend;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Plan 14: mixed packs, Alpha Packs (several Alphas, the strongest leads), kicking, allies and the party link. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class AlphaPackGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private AlphaPackGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, UUID packAlpha, int level, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).packAlpha(packAlpha)
                .betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    private static ServerPlayer alpha(GameTestHelper helper, int level, double x) {
        return wolf(helper, Rank.ALPHA, null, level, x);
    }

    private static ServerPlayer human(GameTestHelper helper, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        return p;
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
    }

    private static void clean(GameTestHelper helper, ServerPlayer... players) {
        PackIndex index = PackIndex.get(helper.getLevel());
        SireCall.overridePlayerLookup(null);
        AlphaRoar.overridePlayers(null);
        AlphaRoar.reset();
        RoarEffectPayload.overrideSpy(null);
        PartyLink.setBackend(null);
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            for (UUID ally : index.alliesOf(p.getUUID())) {
                index.unally(p.getUUID(), ally);
            }
            PackBonus.tick(p);
            p.removeAllEffects();
            p.removeData(SpeciesRegistries.PENDING_ALPHA);
            p.removeData(SpeciesRegistries.PENDING_WILD);
        }
    }

    private static WolfState state(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    /** What /pack invite and the Accept click do, with the token the invitation made. */
    private static boolean join(GameTestHelper helper, ServerPlayer leader, ServerPlayer target) {
        if (PackCommand.invite(leader, target) != null) {
            return false;
        }
        String token = PackCommand.requests().open(leader.getUUID(), target.getUUID(), target.serverLevel().getGameTime());
        return PackCommand.accept(target, token) == null;
    }

    private static double bonus(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        var m = a == null ? null : a.getModifier(PackBonus.MODIFIER_ID);
        return m == null ? 0 : m.amount();
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, killer.damageSources().playerAttack(killer)));
    }

    // ---- mixed packs ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mixedPackPackmatesGetEveryPerk(GameTestHelper helper) {
        ServerPlayer alpha = alpha(helper, 5, 1.5);
        ServerPlayer h1 = human(helper, 2.5);
        ServerPlayer h2 = human(helper, 3.5);
        ServerPlayer h3 = human(helper, 4.5);
        online(alpha, h1, h2, h3);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, alpha, h1) && join(helper, alpha, h2) && join(helper, alpha, h3), "a human could not join the pack");
            helper.assertTrue(alpha.getUUID().equals(index.alphaOf(h1.getUUID()).orElse(null)), "the Packmate is not in the index");
            helper.assertTrue(state(h1).equals(WolfState.NONE), "joining changed the Packmate's werewolf state: " + state(h1));
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) > 0, "three Packmates near the Alpha gave no pack bonus");
            helper.assertTrue(h1.hasEffect(MobEffects.REGENERATION), "a Packmate got no regeneration from the bonus");
            helper.assertTrue(AlphaRoar.roar(alpha, 100_000L) == null, "the roar was refused");
            helper.assertTrue(h2.hasEffect(MobEffects.DAMAGE_BOOST), "the roar did not reach a Packmate");
            // a Packmate leaves like anyone: the index forgets them
            helper.assertTrue(PackCommand.leave(h1) == null && index.alphaOf(h1.getUUID()).isEmpty(), "a Packmate could not leave");
        } finally {
            clean(helper, alpha, h1, h2, h3);
        }
        helper.succeed();
    }

    // ---- who may invite and kick ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheLeaderInvitesAndKicks(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 9, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer mate = human(helper, 3.5);
        ServerPlayer other = human(helper, 4.5);
        online(leader, second, mate, other);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, second) && join(helper, leader, mate), "the pack could not be built");
            helper.assertTrue(leader.getUUID().equals(index.leaderOf(second.getUUID())), "the weaker Alpha leads");
            helper.assertTrue(PackCommand.invite(second, other) != null, "a co-Alpha invited");
            helper.assertTrue(PackCommand.kick(second, mate.getUUID(), "m") != null && index.alphaOf(mate.getUUID()).isPresent(), "a co-Alpha kicked");
            helper.assertTrue(PackCommand.kick(mate, second.getUUID(), "s") != null, "a Packmate kicked");
            helper.assertTrue(PackCommand.invite(leader, other) == null, "the leader could not invite");
        } finally {
            clean(helper, leader, second, mate, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kickRemovesMembersOnlineAndOffline(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 5, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer away = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer mate = human(helper, 4.5);
        ServerPlayer late = human(helper, 5.5);
        online(leader, beta, mate, late); // `away` is offline for the server
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, mate), "the Packmate could not join");
            // no size limit: thirty more members and one more real join
            for (int i = 0; i < 30; i++) {
                helper.assertTrue(index.join(leader.getUUID(), UUID.randomUUID()), "member " + i + " was refused");
            }
            helper.assertTrue(join(helper, leader, late), "the pack refused a member at size " + index.betasOf(leader.getUUID()).size());

            helper.assertTrue(PackCommand.kick(leader, beta.getUUID(), "beta") == null, "the kick of a Beta failed");
            helper.assertTrue(index.alphaOf(beta.getUUID()).isEmpty() && state(beta).rank() == Rank.OMEGA && state(beta).packAlpha() == null,
                    "the kicked Beta is " + state(beta) + " and in " + index.alphaOf(beta.getUUID()));
            helper.assertTrue(PackCommand.kick(leader, mate.getUUID(), "mate") == null && index.alphaOf(mate.getUUID()).isEmpty(), "the Packmate was not removed");
            helper.assertTrue(PackCommand.kick(leader, away.getUUID(), "away") == null && index.alphaOf(away.getUUID()).isEmpty(), "the offline member was not removed");
            helper.assertTrue(index.takeDissolved(away.getUUID()), "the offline Beta will not become an Omega at their next login");
            helper.assertTrue(!index.takeDissolved(beta.getUUID()), "an online kick left a leftover that would remove them from their next pack");
            helper.assertTrue(PackCommand.kick(leader, UUID.randomUUID(), "nobody") != null, "kicked a stranger");
            helper.assertTrue(PackCommand.kick(leader, leader.getUUID(), "me") != null, "the leader kicked themselves");
        } finally {
            clean(helper, leader, beta, away, mate, late);
        }
        helper.succeed();
    }

    // ---- the leader ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theStrongestAlphaLeadsAndFollowsLevelChanges(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 3, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, a.getUUID(), 1, 2.5);
        ServerPlayer b = alpha(helper, 8, 3.5);
        online(a, beta, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(a.getUUID()), "the Beta's pack is not A's");
            helper.assertTrue(join(helper, a, b), "the stronger Alpha could not join");
            helper.assertTrue(index.leaderOf(a.getUUID()).equals(b.getUUID()) && index.leaderOf(beta.getUUID()).equals(b.getUUID()), "the stronger Alpha does not lead");
            helper.assertTrue(b.getUUID().equals(state(beta).packAlpha()), "the Beta still follows the old leader: " + state(beta).packAlpha());
            helper.assertTrue(index.alphaOf(a.getUUID()).isPresent() && index.alphaOf(b.getUUID()).isEmpty(), "the old leader is not a member now");

            // A climbs past B: the level change moves the lead back
            helper.assertTrue(VampirismAPI.factionPlayerHandler(a).setFactionAndLevel(WReference.WEREWOLF_FACTION, 12), "A could not level up");
            helper.assertTrue(index.leaderOf(b.getUUID()).equals(a.getUUID()), "the level change did not move the lead");
            helper.assertTrue(a.getUUID().equals(state(beta).packAlpha()), "the Beta did not follow back");

            // a True Alpha beats any plain Alpha
            BloodlineAccess.set(b, BloodlineAccess.get(b).toBuilder().wolf(state(b).toBuilder().rank(Rank.TRUE_ALPHA).build()).build());
            helper.assertTrue(index.leaderOf(a.getUUID()).equals(b.getUUID()), "a True Alpha does not lead");
        } finally {
            clean(helper, a, beta, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCoAlphaTakesTheLeadBeforeAnyBetaWhenTheLeaderLosesTheSpark(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 9, 3.5); // the strongest Beta is not who gets it
        online(leader, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(second.getUUID()), "the pack did not go to the co-Alpha");
            helper.assertTrue(second.getUUID().equals(state(beta).packAlpha()) && state(beta).rank() == Rank.BETA, "the Beta is " + state(beta));
            helper.assertTrue(state(second).rank() == Rank.ALPHA, "the co-Alpha is " + state(second));
            helper.assertTrue(state(leader).rank() == Rank.OMEGA && index.alphaOf(leader.getUUID()).isEmpty(), "the old leader is " + state(leader));
        } finally {
            clean(helper, leader, second, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killingAnAlphaOfYourOwnPackStillStealsTheSpark(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            // the leader kills a co-Alpha of their own pack
            kill(leader, second);
            helper.assertTrue(state(second).rank() == Rank.OMEGA && index.alphaOf(second.getUUID()).isEmpty() && index.isFormerAlpha(second.getUUID()),
                    "the victim is " + state(second) + " in " + index.alphaOf(second.getUUID()));
            helper.assertTrue(state(leader).stoleSpark() && index.leaderOf(beta.getUUID()).equals(leader.getUUID()), "the killer is " + state(leader));
        } finally {
            clean(helper, leader, second, beta);
        }
        // and a co-Alpha who kills the leader takes the pack
        ServerPlayer leader2 = alpha(helper, 6, 1.5);
        ServerPlayer second2 = alpha(helper, 3, 2.5);
        ServerPlayer beta2 = wolf(helper, Rank.BETA, leader2.getUUID(), 1, 3.5);
        online(leader2, second2, beta2);
        try {
            helper.assertTrue(join(helper, leader2, second2), "the second pack could not be built");
            kill(second2, leader2);
            helper.assertTrue(index.leaderOf(beta2.getUUID()).equals(second2.getUUID()) && index.alphaOf(second2.getUUID()).isEmpty(), "the killer does not lead");
            helper.assertTrue(second2.getUUID().equals(state(beta2).packAlpha()), "the Beta did not follow the killer");
            helper.assertTrue(state(leader2).rank() == Rank.OMEGA, "the old leader is " + state(leader2));
        } finally {
            clean(helper, leader2, second2, beta2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBetaWhoWinsTheAlphaTrialIsASecondAlpha(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 2, 2.5);
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null, 2, 3.5);
        online(leader, beta, omega);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(AlphaTrial.win(omega) != null, "an Omega won the trial");
            helper.assertTrue(AlphaTrial.win(beta) == null, "the Beta could not win the trial");
            helper.assertTrue(state(beta).rank() == Rank.ALPHA, "the winner is " + state(beta));
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(beta.getUUID()).orElse(null)), "the winner left the pack");
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(leader.getUUID()), "the weaker winner leads");
            helper.assertTrue(PackCommand.invite(beta, human(helper, 4.5)) != null, "a second Alpha invited");
        } finally {
            clean(helper, leader, beta, omega);
        }
        helper.succeed();
    }

    // ---- allies ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alliedPacksCannotHurtEachOtherAndShareNoPerks(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer mine = wolf(helper, Rank.BETA, a.getUUID(), 1, 2.5);
        ServerPlayer x = alpha(helper, 5, 3.5);
        ServerPlayer theirs = wolf(helper, Rank.BETA, x.getUUID(), 1, 4.5);
        ServerPlayer stranger = wolf(helper, Rank.OMEGA, null, 1, 5.5);
        online(a, mine, x, theirs, stranger);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(PackCommand.allyInvite(a, x) == null, "the alliance could not be asked for");
            helper.assertTrue(index.ally(a.getUUID(), x.getUUID()), "the alliance was not made");
            helper.assertTrue(!index.ally(a.getUUID(), x.getUUID()), "the same alliance was made twice");
            helper.assertTrue(index.alliedPlayers(mine.getUUID(), theirs.getUUID()) && index.alliedPlayers(theirs.getUUID(), mine.getUUID()), "the alliance is not both ways");
            helper.assertTrue(PackCommand.allyInvite(mine, x) != null, "a Beta asked for an alliance");

            float before = theirs.getHealth();
            theirs.hurt(mine.damageSources().playerAttack(mine), 5f);
            helper.assertTrue(theirs.getHealth() == before, "an ally was hurt: " + before + " to " + theirs.getHealth());
            float strangerBefore = stranger.getHealth();
            stranger.hurt(mine.damageSources().playerAttack(mine), 5f);
            helper.assertTrue(stranger.getHealth() < strangerBefore, "a stranger was protected (or this test player cannot be hurt)");
            float ownBefore = a.getHealth();
            a.hurt(mine.damageSources().playerAttack(mine), 5f);
            helper.assertTrue(a.getHealth() < ownBefore, "the same pack was protected, which this plan does not ask for");

            // the roar buffs the pack, force-shifts the stranger's wolf form off, and leaves the ally alone
            helper.assertTrue(AlphaRoar.roar(a, 100_000L) == null, "the roar was refused");
            helper.assertTrue(mine.hasEffect(MobEffects.DAMAGE_BOOST), "the roar did not reach the pack");
            helper.assertTrue(!theirs.hasEffect(MobEffects.DAMAGE_BOOST), "an ally got the pack's buff");
            helper.assertTrue(!theirs.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !x.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the roar made an ally submit");
            helper.assertTrue(stranger.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the roar no longer makes a stranger submit");

            helper.assertTrue(PackCommand.allyRemove(a, theirs.getUUID(), "theirs") == null && !index.alliedPlayers(mine.getUUID(), theirs.getUUID()), "the alliance did not end");
            helper.assertTrue(PackCommand.allyRemove(a, theirs.getUUID(), "theirs") != null, "ended an alliance that was not there");
        } finally {
            clean(helper, a, mine, x, theirs, stranger);
        }
        helper.succeed();
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void packIndexRoundTripsAndOldDataLoads(GameTestHelper helper) {
        UUID l = UUID.randomUUID();
        UUID m = UUID.randomUUID();
        UUID co = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PackIndex index = new PackIndex();
        index.join(l, m);
        index.join(l, co);
        index.setStrength(co, 1003);
        index.setStrength(l, 1006);
        index.ally(l, other);
        index.setMirror(m, l);
        CompoundTag tag = index.save(new CompoundTag(), helper.getLevel().registryAccess());
        PackIndex back = PackIndex.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(back.betasOf(l).equals(List.of(m, co)), "the members were lost: " + back.betasOf(l));
        helper.assertTrue(back.strengthOf(co) == 1003 && back.strengthOf(l) == 1006, "the strengths were lost");
        helper.assertTrue(back.alliedPlayers(l, other) && back.alliedPlayers(other, l), "the alliance was lost or is one way");
        helper.assertTrue(l.equals(back.mirrorOwner(m).orElse(null)), "the party mirror was lost");

        // a file from before plan 14 has none of the new lists
        tag.remove("strengths");
        tag.remove("allies");
        tag.remove("party_mirrors");
        PackIndex old = PackIndex.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(old.betasOf(l).equals(List.of(m, co)) && old.alliesOf(l).isEmpty() && old.strengthOf(co) == 0 && old.mirrorOwner(m).isEmpty(),
                "an old file did not load as it was");
        helper.succeed();
    }

    // ---- the party link ----

    /** Open Parties and Claims, as far as the link uses it. */
    private static final class FakeParties implements PartyBackend {
        final Map<UUID, Set<UUID>> parties = new HashMap<>();
        final Map<UUID, Boolean> admin = new HashMap<>();

        @Override
        public Party find(MinecraftServer server, List<UUID> candidates) {
            for (UUID id : candidates) {
                if (parties.containsKey(id)) {
                    return new Party(id, new HashSet<>(parties.get(id)));
                }
            }
            return null;
        }

        @Override
        public boolean create(MinecraftServer server, UUID owner) {
            parties.put(owner, new HashSet<>(Set.of(owner)));
            return true;
        }

        @Override
        public void disband(MinecraftServer server, UUID owner) {
            parties.remove(owner);
        }

        @Override
        public void add(MinecraftServer server, UUID owner, UUID member, boolean isAdmin) {
            parties.get(owner).add(member);
            admin.put(member, isAdmin);
        }

        @Override
        public void remove(MinecraftServer server, UUID owner, UUID member) {
            parties.get(owner).remove(member);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePackIsMirroredAsAPartyAndLeavingOneLeavesTheOther(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer mate = human(helper, 4.5);
        online(leader, second, beta, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, second) && join(helper, leader, mate), "the pack could not be built");
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the leader got no party");
            Set<UUID> expected = new HashSet<>(List.of(leader.getUUID(), second.getUUID(), beta.getUUID(), mate.getUUID()));
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(fake.parties.get(leader.getUUID()).equals(expected), "the party is " + fake.parties.get(leader.getUUID()));
            helper.assertTrue(fake.admin.get(second.getUUID()) == Boolean.TRUE && fake.admin.get(mate.getUUID()) == Boolean.FALSE, "the ranks are " + fake.admin);

            // the Beta leaves the party: they leave the pack
            fake.parties.get(leader.getUUID()).remove(beta.getUUID());
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.alphaOf(beta.getUUID()).isEmpty() && state(beta).rank() == Rank.OMEGA, "leaving the party did not leave the pack: " + state(beta));

            // the Packmate leaves the pack: they leave the party
            helper.assertTrue(PackCommand.leave(mate) == null, "the Packmate could not leave");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(mate.getUUID()), "leaving the pack did not leave the party");

            // the owner ends the party: it is not made again by itself, only when the roster changes
            fake.parties.clear();
            PartyLink.sync(server, leader.getUUID(), false);
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(fake.parties.isEmpty(), "an ended party came back by itself");
            helper.assertTrue(index.alphaOf(second.getUUID()).isPresent(), "ending the party broke the pack");
            helper.assertTrue(join(helper, leader, mate), "the Packmate could not join again");
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the party was not made again when the roster changed");
        } finally {
            clean(helper, leader, second, beta, mate);
        }
        helper.succeed();
    }
}
