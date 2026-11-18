package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaTrial;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
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
    public static void aJoiningAlphaNeverTakesOverAndLevelsDoNotMoveTheLead(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 3, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, a.getUUID(), 1, 2.5);
        ServerPlayer b = alpha(helper, 8, 3.5);
        online(a, beta, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(a.getUUID()), "the Beta's pack is not A's");
            helper.assertTrue(join(helper, a, b), "the stronger Alpha could not join");
            helper.assertTrue(index.leaderOf(a.getUUID()).equals(a.getUUID()) && index.leaderOf(beta.getUUID()).equals(a.getUUID()) && index.leaderOf(b.getUUID()).equals(a.getUUID()),
                    "the joining Alpha took over");
            helper.assertTrue(a.getUUID().equals(state(beta).packAlpha()), "the Beta follows someone else: " + state(beta).packAlpha());
            helper.assertTrue(index.alphaOf(b.getUUID()).isPresent() && index.isLeader(a.getUUID()), "the joiner is not a second Alpha");

            // levelling up past the leader, and becoming a True Alpha, do not move the lead either
            helper.assertTrue(VampirismAPI.factionPlayerHandler(b).setFactionAndLevel(WReference.WEREWOLF_FACTION, 12), "B could not level up");
            BloodlineAccess.set(b, BloodlineAccess.get(b).toBuilder().wolf(state(b).toBuilder().rank(Rank.TRUE_ALPHA).build()).build());
            helper.assertTrue(index.leaderOf(b.getUUID()).equals(a.getUUID()) && a.getUUID().equals(state(beta).packAlpha()), "a level or a rank moved the lead");
        } finally {
            clean(helper, a, beta, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSuccessorIsTheStrongestAlphaNotTheFirstOrTheBiggestBeta(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer weak = alpha(helper, 2, 2.5);
        ServerPlayer strong = alpha(helper, 9, 3.5);
        ServerPlayer plainHigh = alpha(helper, 12, 4.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 11, 5.5);
        online(leader, weak, strong, plainHigh, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, weak) && join(helper, leader, strong) && join(helper, leader, plainHigh), "the Alphas could not join");
            BloodlineAccess.set(strong, BloodlineAccess.get(strong).toBuilder().wolf(state(strong).toBuilder().rank(Rank.TRUE_ALPHA).build()).build());
            PackLeader.refresh(strong);
            helper.assertTrue(index.isLeader(leader.getUUID()), "somebody took over before the leader left");
            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false);
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(strong.getUUID()), "the True Alpha did not succeed: " + index.leaderOf(beta.getUUID()));
        } finally {
            clean(helper, leader, weak, strong, plainHigh, beta);
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
    public static void aKillInsideThePackOutsideADuelChangesNothingInEitherDirection(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            kill(leader, second); // the leader kills a co-Alpha
            kill(second, leader); // a co-Alpha kills the leader
            kill(beta, second);   // a Beta kills a co-Alpha
            kill(beta, leader);   // and the leader
            helper.assertTrue(state(leader).rank() == Rank.ALPHA && state(second).rank() == Rank.ALPHA && state(beta).rank() == Rank.BETA, "a rank changed: "
                    + state(leader).rank() + " " + state(second).rank() + " " + state(beta).rank());
            helper.assertTrue(!state(leader).stoleSpark() && !state(second).stoleSpark() && !state(beta).stoleSpark(), "somebody stole a spark");
            helper.assertTrue(state(leader).sparks() == 1 && state(second).sparks() == 1, "a spark moved");
            helper.assertTrue(index.isLeader(leader.getUUID()) && index.alphaOf(second.getUUID()).map(leader.getUUID()::equals).orElse(false)
                    && index.alphaOf(beta.getUUID()).map(leader.getUUID()::equals).orElse(false), "the pack or its lead moved");
            helper.assertTrue(!index.isFormerAlpha(leader.getUUID()) && !index.isFormerAlpha(second.getUUID()), "somebody was marked as having lost the spark");
        } finally {
            clean(helper, leader, second, beta);
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
        UUID partyId = UUID.randomUUID();
        index.recordParty(l, partyId);
        index.setPartyEnded(other, true);
        index.addNotice(m, "supernaturalbloodlines.pack.dissolved", "x");
        index.addNotice(m, "supernaturalbloodlines.pack.kicked", "y");
        CompoundTag tag = index.save(new CompoundTag(), helper.getLevel().registryAccess());
        PackIndex back = PackIndex.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(back.betasOf(l).equals(List.of(m, co)), "the members were lost: " + back.betasOf(l));
        helper.assertTrue(back.strengthOf(co) == 1003 && back.strengthOf(l) == 1006, "the strengths were lost");
        helper.assertTrue(back.alliedPlayers(l, other) && back.alliedPlayers(other, l), "the alliance was lost or is one way");
        helper.assertTrue(l.equals(back.mirrorOwner(m).orElse(null)), "the party mirror was lost");
        helper.assertTrue(back.recordedParty(List.of(l)).map(r -> r.id().equals(partyId)).orElse(false) && back.partyEnded(List.of(other)), "the recorded party or its end was lost");
        List<String[]> waiting = back.takeNotices(m);
        helper.assertTrue(waiting.size() == 2 && waiting.get(0)[0].equals("supernaturalbloodlines.pack.dissolved") && waiting.get(1)[0].equals("supernaturalbloodlines.pack.kicked"), "the notices were lost or came in the wrong order");

        // a file from before plan 14 has none of the new lists
        tag.remove("strengths");
        tag.remove("allies");
        tag.remove("party_mirrors");
        tag.remove("party_ids");
        tag.remove("party_ended");
        tag.remove("notices");
        PackIndex old = PackIndex.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(old.betasOf(l).equals(List.of(m, co)) && old.alliesOf(l).isEmpty() && old.strengthOf(co) == 0 && old.mirrorOwner(m).isEmpty(),
                "an old file did not load as it was");
        helper.succeed();
    }

    // ---- the party link ----

    /** Open Parties and Claims, as far as the link uses it. */
    private static final class FakeParties implements PartyBackend {
        final Map<UUID, UUID> ids = new HashMap<>();
        final Map<UUID, Set<UUID>> parties = new HashMap<>();
        /** Players who are "already in another party": adding them is refused. */
        final Set<UUID> refuse = new HashSet<>();
        boolean unreachable;
        /** Removing from a party fails (the party mod did not answer). */
        boolean failRemove;
        int creates;

        /** A party somebody made by hand. */
        void manual(UUID owner, UUID... members) {
            ids.put(owner, UUID.randomUUID());
            Set<UUID> m = new HashSet<>(Set.of(owner));
            m.addAll(List.of(members));
            parties.put(owner, m);
        }

        @Override
        public Found find(MinecraftServer server, UUID owner, UUID id) {
            if (unreachable) {
                return new Found(false, null);
            }
            if (!id.equals(ids.get(owner))) {
                return new Found(true, null);
            }
            return new Found(true, new Party(owner, new HashSet<>(parties.get(owner))));
        }

        @Override
        public Optional<UUID> create(MinecraftServer server, UUID owner) {
            if (parties.containsKey(owner)) {
                return Optional.empty();
            }
            creates++;
            manual(owner);
            return Optional.of(ids.get(owner));
        }

        @Override
        public boolean add(MinecraftServer server, UUID owner, UUID member) {
            // like the real thing, somebody who is in another party cannot be added to this one
            boolean inAnother = parties.entrySet().stream().anyMatch(e -> !e.getKey().equals(owner) && e.getValue().contains(member));
            if (refuse.contains(member) || inAnother || !parties.containsKey(owner)) {
                return false;
            }
            parties.get(owner).add(member);
            return true;
        }

        @Override
        public boolean remove(MinecraftServer server, UUID owner, UUID member) {
            if (failRemove) {
                return false;
            }
            parties.get(owner).remove(member);
            return true;
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
            helper.assertTrue(index.recordedParty(index.pack(leader.getUUID())).map(r -> r.id().equals(fake.ids.get(leader.getUUID()))).orElse(false), "the party id was not recorded");

            // the Beta leaves the party: they leave the pack
            fake.parties.get(leader.getUUID()).remove(beta.getUUID());
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.alphaOf(beta.getUUID()).isEmpty() && state(beta).rank() == Rank.OMEGA, "leaving the party did not leave the pack: " + state(beta));

            // the Packmate leaves the pack: they leave the party
            helper.assertTrue(PackCommand.leave(mate) == null, "the Packmate could not leave");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(mate.getUUID()), "leaving the pack did not leave the party");

            // the owner ends the party: it is not made again by itself, only when the roster changes
            fake.parties.clear();
            fake.ids.clear();
            PartyLink.sync(server, leader.getUUID(), false);
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(fake.parties.isEmpty() && fake.creates == 1, "an ended party came back by itself");
            helper.assertTrue(index.alphaOf(second.getUUID()).isPresent(), "ending the party broke the pack");
            helper.assertTrue(join(helper, leader, mate), "the Packmate could not join again");
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the party was not made again when the roster changed");
        } finally {
            clean(helper, leader, second, beta, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aManualPartyIsNeverAdoptedAndTheSwitchTurnsTheLinkOff(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        UUID stray = UUID.randomUUID();
        online(leader, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        fake.manual(leader.getUUID(), stray);
        Set<UUID> manual = new HashSet<>(fake.parties.get(leader.getUUID()));
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(fake.parties.get(leader.getUUID()).equals(manual), "the leader's own party was changed: " + fake.parties.get(leader.getUUID()));
            helper.assertTrue(index.recordedParty(index.pack(leader.getUUID())).isEmpty(), "a manual party was recorded as ours");
            helper.assertTrue(index.alphaOf(beta.getUUID()).isPresent(), "the pack was touched");
            // gone for good when its owner ends it: nothing of ours to forget
            fake.parties.clear();
            fake.ids.clear();

            SpeciesConfig.overrideOpacLink(false);
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.isEmpty() && fake.creates == 0, "a party was made with opac_link_enabled off");
        } finally {
            SpeciesConfig.overrideOpacLink(null);
            clean(helper, leader, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerInAnotherPartyStaysInThePackAndIsAddedWhenFree(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer busy = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer free = human(helper, 3.5);
        online(leader, busy, free);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        fake.refuse.add(busy.getUUID());
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, free), "the Packmate could not join");
            for (int i = 0; i < 3; i++) {
                PartyLink.sync(server, leader.getUUID(), false);
            }
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(free.getUUID()) && !fake.parties.get(leader.getUUID()).contains(busy.getUUID()), "the party is " + fake.parties.get(leader.getUUID()));
            helper.assertTrue(index.alphaOf(busy.getUUID()).isPresent() && state(busy).rank() == Rank.BETA, "a player in another party was taken out of the pack: " + state(busy));
            helper.assertTrue(index.mirrorOwner(busy.getUUID()).isEmpty(), "they were recorded as ours");

            fake.refuse.clear();
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(busy.getUUID()) && index.mirrorOwner(busy.getUUID()).isPresent(), "they were not added once free");
        } finally {
            clean(helper, leader, busy, free);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPartySystemThatDoesNotAnswerChangesNothing(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        online(leader, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the party was not made");
            fake.unreachable = true;
            fake.parties.clear();
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.recordedParty(index.pack(leader.getUUID())).isPresent() && index.alphaOf(beta.getUUID()).isPresent(), "a silent party system made the link forget its party");
            fake.unreachable = false;
            fake.manual(leader.getUUID()); // somebody made a party of their own meanwhile: a different id, so not ours
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.recordedParty(index.pack(leader.getUUID())).isEmpty() && index.alphaOf(beta.getUUID()).isPresent(), "a party of the same owner but another id was taken for ours");
        } finally {
            clean(helper, leader, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whenTheOwnerStopsLeadingThePackGetsAPartyOfItsOwn(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 3, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer stronger = alpha(helper, 9, 3.5);
        online(leader, beta, stronger);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the first party was not made");
            helper.assertTrue(join(helper, leader, stronger) && index.isLeader(leader.getUUID()), "the stronger Alpha could not join");
            helper.assertTrue(PackDuelGameTests.win(stronger, leader) && index.leaderOf(leader.getUUID()).equals(stronger.getUUID()), "the duel winner does not lead");
            helper.assertTrue(fake.parties.containsKey(stronger.getUUID()), "the new leader got no party");
            helper.assertTrue(fake.parties.get(stronger.getUUID()).contains(beta.getUUID()), "the pack is not in the new party");
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()) && fake.parties.get(leader.getUUID()).contains(leader.getUUID()) && !fake.parties.get(leader.getUUID()).contains(beta.getUUID()),
                    "the old party was ended or still holds the pack: " + fake.parties.get(leader.getUUID()));
            helper.assertTrue(index.recordedParty(index.pack(stronger.getUUID())).map(r -> r.owner().equals(stronger.getUUID())).orElse(false), "the new party is not the recorded one");
        } finally {
            clean(helper, leader, beta, stronger);
        }
        helper.succeed();
    }

    // ---- the party follows the leadership (plan 14 review) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDissolvedOrStolenPackLeavesNoMirroredMembersInItsOldParty(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 3.5);
        online(leader, beta, thief);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(beta.getUUID()) && index.mirrorOwner(beta.getUUID()).isPresent(), "the pack was not mirrored to start with");

            kill(thief, leader); // an outside werewolf takes the spark and not the pack, which has no other Alpha: it dissolves
            helper.assertTrue(index.alphaOf(beta.getUUID()).isEmpty() && index.betasOf(thief.getUUID()).isEmpty(), "the pack did not dissolve");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "a member of the dissolved pack is still in its party");
            helper.assertTrue(index.recordedParty(List.of(leader.getUUID())).isEmpty() && index.mirrorOwner(beta.getUUID()).isEmpty(), "the record of the dead party stayed");
            helper.assertTrue(fake.parties.containsKey(leader.getUUID()), "the old owner's own party was ended");
        } finally {
            clean(helper, leader, beta, thief);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackHandedToACoAlphaGetsHisPartyAndTheOldOneLosesOurMembers(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        ServerPlayer patient = human(helper, 5.5);
        online(leader, second, beta, thief, patient);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).containsAll(Set.of(second.getUUID(), beta.getUUID())), "the pack was not mirrored to start with");

            kill(thief, leader); // stolen from outside: the co-Alpha leads the pack
            helper.assertTrue(index.isLeader(second.getUUID()) && index.alphaOf(beta.getUUID()).map(second.getUUID()::equals).orElse(false), "the co-Alpha does not lead");
            helper.assertTrue(fake.parties.containsKey(second.getUUID()) && fake.parties.get(second.getUUID()).contains(beta.getUUID()), "the new leader got no party with the pack");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(beta.getUUID()) && !fake.parties.get(leader.getUUID()).contains(second.getUUID()), "the old party still holds our members");
            helper.assertTrue(index.recordedParty(index.pack(second.getUUID())).map(rec -> rec.owner().equals(second.getUUID())).orElse(false), "the recorded party is not the new one");
            helper.assertTrue(index.recordedParty(List.of(leader.getUUID())).isEmpty(), "the dead record stayed");
        } finally {
            clean(helper, leader, second, beta, thief, patient);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCuredLeaderAndAGivenUpSparkMoveThePartyToo(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer heir = wolf(helper, Rank.BETA, leader.getUUID(), 9, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, heir, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).containsAll(Set.of(heir.getUUID(), beta.getUUID())), "the pack was not mirrored to start with");

            VampirismAPI.factionPlayerHandler(leader).leaveFaction(false); // cured: the strongest Beta inherits
            helper.assertTrue(index.isLeader(heir.getUUID()) && index.alphaOf(beta.getUUID()).map(heir.getUUID()::equals).orElse(false), "the Beta did not inherit");
            helper.assertTrue(fake.parties.containsKey(heir.getUUID()) && fake.parties.get(heir.getUUID()).contains(beta.getUUID()), "the new leader got no party");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(heir.getUUID()) && !fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "the old party still holds our members");
            helper.assertTrue(index.recordedParty(List.of(leader.getUUID())).isEmpty(), "the dead record stayed");
        } finally {
            clean(helper, leader, heir, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderGivingUpTheSparkToACoAlphaMovesTheParty(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer patient = human(helper, 4.5);
        online(leader, second, beta, patient);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(PackCommand.heal(leader, patient) == null, "the heal was refused");
            helper.assertTrue(index.isLeader(second.getUUID()), "the co-Alpha does not lead");
            helper.assertTrue(fake.parties.containsKey(second.getUUID()) && fake.parties.get(second.getUUID()).contains(beta.getUUID()), "the new leader got no party");
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "the old party still holds our members");
            // the old leader stays in the pack as a Beta of the new one, but owns the old party still, so the party mod cannot put them in
            // the new one: they stay in the pack (it is no reason to take anyone out of it) and in their own old party
            PartyLink.syncAll(server);
            helper.assertTrue(index.alphaOf(leader.getUUID()).map(second.getUUID()::equals).orElse(false), "the old leader left the pack because the party mod refused them");
            helper.assertTrue(!fake.parties.get(second.getUUID()).contains(leader.getUUID()) && fake.parties.get(leader.getUUID()).contains(leader.getUUID()), "the old leader is in two parties or lost their own");
        } finally {
            clean(helper, leader, second, beta, patient);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberTheirPartyRefusesIsToldOnceAndStaysInThePack(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        UUID away = UUID.randomUUID();
        online(leader);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        fake.refuse.add(away);
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            index.join(leader.getUUID(), away);
            for (int i = 0; i < 4; i++) {
                PartyLink.sync(server, leader.getUUID(), false);
            }
            helper.assertTrue(index.noticeCount(away) == 1, "told " + index.noticeCount(away) + " times, not once");
            helper.assertTrue(index.alphaOf(away).isPresent() && index.mirrorOwner(away).isEmpty(), "they left the pack or were mirrored anyway");

            fake.refuse.clear();
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.mirrorOwner(away).isPresent(), "they were not added once free");
            // refused again later: told again once, because the last attempt worked
            fake.parties.get(leader.getUUID()).remove(away);
            index.setMirror(away, null);
            fake.refuse.add(away);
            PartyLink.sync(server, leader.getUUID(), false);
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.noticeCount(away) == 2, "a second refusal was not told, or was told twice: " + index.noticeCount(away));
        } finally {
            index.leave(away);
            index.takeNotices(away);
            clean(helper, leader);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAbsentLeadersPoolMovesToTheNewLeaderAndADissolvedPackLosesIt(GameTestHelper helper) {
        UUID goneId = UUID.randomUUID();
        ServerPlayer strong = alpha(helper, 9, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, goneId, 1, 2.5);
        online(strong, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.ofatech.supernaturalbloodlines.parts.species.profile.ProfileKnowledge pool = net.ofatech.supernaturalbloodlines.parts.species.profile.ProfileKnowledge.get(helper.getLevel());
        UUID subject = UUID.randomUUID();
        long now = System.currentTimeMillis();
        try {
            index.join(goneId, strong.getUUID());
            index.join(goneId, beta.getUUID());
            index.setStrength(strong.getUUID(), 1009);
            pool.discover(index, beta.getUUID(), subject, java.util.EnumSet.of(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.LINE));
            helper.assertTrue(pool.hasPool(goneId), "the pool was not made under the leader");
            index.setLastSeen(goneId, now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(helper.getLevel().getServer(), now) == 1 && index.isLeader(strong.getUUID()), "the absent leader was not deposed");
            helper.assertTrue(pool.hasPool(strong.getUUID()) && !pool.hasPool(goneId), "the pool did not follow the new leader");
            helper.assertTrue(pool.known(index, strong.getUUID(), subject).contains(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.LINE), "the new leader lost the discovery");

            index.dissolve(strong.getUUID());
            helper.assertTrue(!pool.hasPool(strong.getUUID()), "a dissolved pack kept its pool");
        } finally {
            index.leave(goneId);
            index.takeNotices(goneId);
            clean(helper, strong, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAbsentLeadersPartyMovesToTheNewLeaderToo(GameTestHelper helper) {
        UUID goneId = UUID.randomUUID();
        ServerPlayer strong = alpha(helper, 9, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, goneId, 1, 2.5);
        online(strong, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        MinecraftServer server = helper.getLevel().getServer();
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        long now = System.currentTimeMillis();
        try {
            index.join(goneId, strong.getUUID());
            index.join(goneId, beta.getUUID());
            index.setStrength(strong.getUUID(), 1009);
            PartyLink.sync(server, goneId, true);
            helper.assertTrue(fake.parties.containsKey(goneId) && fake.parties.get(goneId).contains(beta.getUUID()), "the pack was not mirrored to start with");

            index.setLastSeen(goneId, now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 1 && index.isLeader(strong.getUUID()), "the absent leader was not deposed");
            helper.assertTrue(fake.parties.containsKey(strong.getUUID()) && fake.parties.get(strong.getUUID()).contains(beta.getUUID()), "the new leader got no party with the pack");
            helper.assertTrue(!fake.parties.get(goneId).contains(beta.getUUID()) && !fake.parties.get(goneId).contains(strong.getUUID()), "the old party still holds our members");
            helper.assertTrue(index.recordedParty(index.pack(strong.getUUID())).map(r -> r.owner().equals(strong.getUUID())).orElse(false), "the recorded party is not the new one");
        } finally {
            index.leave(goneId);
            index.takeNotices(goneId);
            clean(helper, strong, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFailedRemovalKeepsTheRecordAndTheNextPassTriesAgain(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        online(leader, second, beta, thief);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, second), "the co-Alpha could not join");
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "the pack was not mirrored to start with");

            fake.failRemove = true;
            kill(thief, leader); // stolen from outside: the co-Alpha leads, and the old party must let our members go
            helper.assertTrue(index.isLeader(second.getUUID()), "the co-Alpha does not lead");
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "the failed removal took the member out anyway");
            helper.assertTrue(index.recordedParty(List.of(leader.getUUID())).isPresent() && index.mirrorOwner(beta.getUUID()).isPresent(),
                    "a record or a mirror was forgotten after a removal that failed");

            int logged = PartyLink.failedOwners();
            PartyLink.syncAll(server);
            PartyLink.syncAll(server);
            helper.assertTrue(PartyLink.failedOwners() == logged, "a failing owner was counted again at every pass (it is logged once)");
            helper.assertTrue(logged >= 1, "the failure was never logged");
            fake.failRemove = false;
            PartyLink.syncAll(server);
            PartyLink.syncAll(server);
            helper.assertTrue(!fake.parties.get(leader.getUUID()).contains(beta.getUUID()), "the retry did not take the member out");
            helper.assertTrue(index.recordedParty(List.of(leader.getUUID())).isEmpty(), "the dead record stayed after the retry");
            helper.assertTrue(fake.parties.containsKey(second.getUUID()) && fake.parties.get(second.getUUID()).contains(beta.getUUID()), "the new leader got no party after the retry");
        } finally {
            clean(helper, leader, second, beta, thief);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeavesThePartyThemselvesLeavesThePackWithTheKillGrace(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        online(leader, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(beta.getUUID()) && index.mirrorOwner(beta.getUUID()).isPresent(), "the pack was not mirrored to start with");
            // the member leaves the party in the party mod's own screen: the next pass takes them out of the pack, by their own choice
            fake.parties.get(leader.getUUID()).remove(beta.getUUID());
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(!index.inPack(beta.getUUID()), "leaving the party did not leave the pack");
            helper.assertTrue(index.leftRecently(beta.getUUID(), 10) && index.killsInPack(beta.getUUID(), leader.getUUID(), 10), "leaving the party is leaving by choice: no kill grace was recorded");
        } finally {
            clean(helper, leader, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelistWhoLeavesThePartyMidDuelStaysInThePackUntilItEnds(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        PackIndex index = PackIndex.get(helper.getLevel());
        FakeParties fake = new FakeParties();
        PartyLink.setBackend(fake);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(join(helper, leader, second), "the second Alpha could not join");
            PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(fake.parties.get(leader.getUUID()).contains(second.getUUID()), "the pack was not mirrored to start with");
            helper.assertTrue(PackDuel.challenge(second, leader.getUUID().toString()) == null && PackDuel.accept(leader) == null, "the duel did not start");
            fake.parties.get(leader.getUUID()).remove(second.getUUID());
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.alphaOf(second.getUUID()).isPresent(), "a duelist was taken out of the pack in the middle of the duel");
            PackDuel.reset();
            PartyLink.sync(server, leader.getUUID(), false);
            helper.assertTrue(index.alphaOf(second.getUUID()).isEmpty(), "the removal never took effect after the duel");
        } finally {
            PackDuel.reset();
            clean(helper, leader, second);
        }
        helper.succeed();
    }
}
