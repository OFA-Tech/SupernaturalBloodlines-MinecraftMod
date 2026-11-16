package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaBite;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackLeader;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** Plan 14, the panel's review: same-pack kills, cures, bites, steals, emissaries, tokens, notices. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class PackReviewGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Holder<MobEffect> LUPUS = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("werewolves:lupus_sanguinem")).orElseThrow();

    private PackReviewGameTests() {
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

    /** An Alpha that keeps the translation keys of what it is told. */
    private static TalkingDuelist talkingAlpha(GameTestHelper helper, int level, double x) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.ALPHA).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        PackLeader.refresh(p);
        return p;
    }

    /** A werewolf Beta of {@code leader} that keeps the translation keys of what it is told, and is in the pack's index. */
    private static TalkingDuelist talkingBeta(GameTestHelper helper, UUID leader, double x) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.BETA).packAlpha(leader).geneRolled(true).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "the test wolf could not join the werewolves");
        PackIndex.get(helper.getLevel()).join(leader, p.getUUID());
        PackLeader.refresh(p);
        return p;
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
        AlphaBite.resetRoll();
        SpeciesConfig.overrideAlphaBiteDeathChance(null);
        for (ServerPlayer p : players) {
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
            BloodlineAccess.set(p, BloodlineState.NONE);
            index.leave(p.getUUID());
            index.takeDissolved(p.getUUID());
            index.takeNotice(p.getUUID());
            index.setEmissary(p.getUUID(), null);
            for (UUID ally : index.alliesOf(p.getUUID())) {
                index.unally(p.getUUID(), ally);
            }
            p.removeEffect(LUPUS);
            p.removeAllEffects();
            p.removeData(SpeciesRegistries.PENDING_ALPHA);
            p.removeData(SpeciesRegistries.PENDING_WILD);
        }
    }

    private static WolfState state(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    private static boolean join(ServerPlayer leader, ServerPlayer target) {
        if (PackCommand.invite(leader, target) != null) {
            return false;
        }
        String token = PackCommand.requests().tokenOf(leader.getUUID(), target.getUUID()).orElseThrow();
        return PackCommand.accept(target, token) == null;
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, killer.damageSources().playerAttack(killer)));
    }

    private static void hit(LivingEntity victim, DamageSource source) {
        net.minecraft.server.MinecraftServer server = victim.getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            victim.invulnerableTime = 0;
            victim.hurt(source, 1f);
        } finally {
            server.setPvpAllowed(pvp);
        }
    }

    // ---- hunters ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillInsideOnePackGivesNoMarkAndNoCurse(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 5, 1.5);
        ServerPlayer hunter = HunterMarkGameTests.marked(helper, 0);
        ServerPlayer mate = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer stranger = wolf(helper, Rank.OMEGA, null, 1, 3.5);
        ServerPlayer marked = HunterMarkGameTests.marked(helper, 6);
        ServerPlayer killer = wolf(helper, Rank.BETA, leader.getUUID(), 1, 4.5);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            HunterMarkGameTests.online(hunter, marked, mate, stranger, killer);
            helper.assertTrue(index.join(leader.getUUID(), hunter.getUUID()) && index.join(leader.getUUID(), marked.getUUID()), "the hunters could not join the pack");
            HunterMarkGameTests.kill(hunter, mate);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 0, "a kill inside the pack grew the mark: " + BloodlineAccess.get(hunter).markKills());
            HunterMarkGameTests.kill(hunter, stranger);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 1, "a kill outside the pack no longer grows the mark: " + BloodlineAccess.get(hunter).markKills());

            HunterMarkGameTests.kill(killer, marked);
            helper.assertTrue(BloodlineAccess.get(marked).species() == Species.HUNTER_OF_THE_FIVE, "a packmate's kill ended the mark: " + BloodlineAccess.get(marked).species());
            helper.assertTrue(!BloodlineAccess.get(killer).hunterCurse(), "a packmate was cursed");
        } finally {
            HunterMarkGameTests.clearHooks();
            HunterMarkGameTests.reset(hunter, marked);
            clean(helper, leader, hunter, mate, stranger, marked, killer);
        }
        helper.succeed();
    }

    // ---- cures and own sparks ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCuredBetaStaysAsAPackmateAndOnlyAnOwnSparkLeaves(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 5, 1.5);
        ServerPlayer cured = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer rising = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, cured, rising);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(cured.getUUID()).orElse(null)), "the Beta was not in the pack");
            BloodlineAccess.set(cured, BloodlineAccess.get(cured).toBuilder().wolf(WolfState.NONE).build());
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(cured.getUUID()).orElse(null)), "a cured Beta left the pack");
            helper.assertTrue(index.strengthOf(cured.getUUID()) < 0, "a cured Beta is not a Packmate: " + index.strengthOf(cured.getUUID()));

            PackAccess.applyPack(rising, PackEvent.SPARK_INHERITED, PackContext.defaults());
            helper.assertTrue(index.alphaOf(rising.getUUID()).isEmpty(), "a Beta who got a spark of their own stayed in the pack");
        } finally {
            clean(helper, leader, cured, rising);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackmateWhoBecomesAWerewolfIsABetaAndSurvivesLogin(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 5, 1.5);
        ServerPlayer mate = human(helper, 2.5);
        online(leader, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, mate), "the Packmate could not join");
            PackAccess.applyPack(mate, PackEvent.BITTEN_BY_WILD, PackContext.defaults());
            WolfState w = state(mate);
            helper.assertTrue(w.lineage() == Lineage.BITTEN && w.rank() == Rank.BETA && leader.getUUID().equals(w.packAlpha()), "the new werewolf is " + w);
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(mate.getUUID()).orElse(null)), "they left the pack");
            PackIndex.login(mate);
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(mate.getUUID()).orElse(null)) && state(mate).packAlpha() != null, "the login dropped them");
        } finally {
            clean(helper, leader, mate);
        }
        helper.succeed();
    }

    // ---- bites ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondAlphasBiteJoinsTheLeadersPackAndAnotherPacksMemberIsNotMoved(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer victim = human(helper, 3.5);
        ServerPlayer mate = human(helper, 4.5);
        ServerPlayer otherLeader = alpha(helper, 4, 5.5);
        ServerPlayer theirs = human(helper, 6.5);
        online(leader, second, victim, mate, otherLeader, theirs);
        SpeciesConfig.overrideAlphaBiteDeathChance(0.0);
        AlphaBite.useRoll(() -> 0.5);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second) && join(leader, mate) && join(otherLeader, theirs), "the packs could not be built");

            hit(victim, second.damageSources().playerAttack(second));
            helper.assertTrue(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "the bite was refused");
            helper.assertTrue(WerewolfFaction.join(victim), "the victim could not join the werewolves");
            WolfState w = state(victim);
            helper.assertTrue(w.rank() == Rank.BETA && leader.getUUID().equals(w.packAlpha()), "the bitten is " + w + ", not a Beta of the leader");
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(victim.getUUID()).orElse(null)), "the index has them in " + index.alphaOf(victim.getUUID()));
            helper.assertTrue(second.getUUID().equals(BloodlineAccess.get(victim).sire()), "the biter is not the sire");

            // a Packmate of the same pack becomes a Beta of it
            hit(mate, second.damageSources().playerAttack(second));
            mate.addEffect(new MobEffectInstance(LUPUS, 600));
            helper.assertTrue(WerewolfFaction.join(mate), "the Packmate could not join the werewolves");
            helper.assertTrue(state(mate).rank() == Rank.BETA && leader.getUUID().equals(state(mate).packAlpha()) && leader.getUUID().equals(index.alphaOf(mate.getUUID()).orElse(null)),
                    "the bitten Packmate is " + state(mate));

            // a member of another pack is bitten but not moved
            hit(theirs, second.damageSources().playerAttack(second));
            theirs.addEffect(new MobEffectInstance(LUPUS, 600));
            helper.assertTrue(WerewolfFaction.join(theirs), "the other pack's member could not join the werewolves");
            helper.assertTrue(otherLeader.getUUID().equals(index.alphaOf(theirs.getUUID()).orElse(null)), "a bite moved a member of another pack to " + index.alphaOf(theirs.getUUID()));
            helper.assertTrue(state(theirs).lineage() == Lineage.BITTEN && !leader.getUUID().equals(state(theirs).packAlpha()), "the bitten is " + state(theirs));
        } finally {
            clean(helper, leader, second, victim, mate, otherLeader, theirs);
        }
        helper.succeed();
    }

    // ---- commands ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kickAndAllyRemoveTakeAWordFromThePackAndItsAlliesOnly(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 5, 1.5);
        ServerPlayer mate = human(helper, 2.5);
        ServerPlayer stranger = human(helper, 3.5);
        ServerPlayer ally = alpha(helper, 5, 4.5);
        online(leader, mate, stranger, ally);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, mate), "the Packmate could not join");
            helper.assertTrue(PackCommand.kickNamed(leader, "nobody") != null, "kicked a name nobody has");
            helper.assertTrue(PackCommand.kickNamed(leader, stranger.getUUID().toString()) != null && stranger.isAlive(), "kicked a player who is not in the pack");
            helper.assertTrue(PackCommand.kickNamed(leader, mate.getUUID().toString().toUpperCase()) == null && index.alphaOf(mate.getUUID()).isEmpty(),
                    "the member was not matched by their id, in any case");

            index.ally(leader.getUUID(), ally.getUUID());
            helper.assertTrue(PackCommand.allyRemoveNamed(leader, stranger.getUUID().toString()) != null, "ended an alliance with someone who is no ally");
            helper.assertTrue(PackCommand.allyRemoveNamed(leader, ally.getUUID().toString()) == null && index.alliesOf(leader.getUUID()).isEmpty(), "the ally was not matched");
        } finally {
            clean(helper, leader, mate, stranger, ally);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheLeaderNamesAnEmissaryAndTheyAreListedOnce(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer witch = human(helper, 3.5);
        ServerPlayer taken = human(helper, 4.5);
        ServerPlayer rival = alpha(helper, 4, 5.5);
        online(leader, second, witch, taken, rival);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            BloodlineAccess.set(taken, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            helper.assertTrue(join(leader, second) && join(leader, witch) && join(rival, taken), "the packs could not be built");
            helper.assertTrue(PackCommand.emissary(second, witch) != null, "a second Alpha named an Emissary");
            helper.assertTrue(PackCommand.emissary(leader, taken) != null, "named an Emissary who runs with another pack");
            helper.assertTrue(PackCommand.emissary(leader, witch) == null, "the leader could not name an Emissary");
            helper.assertTrue(index.pack(leader.getUUID()).contains(witch.getUUID()), "the Emissary is not a member of the pack");
            helper.assertTrue(PackCommand.emissary(second, null) != null && index.emissaryOf(leader.getUUID()).isPresent(), "a second Alpha cleared the Emissary");

            List<HubSocial.Member> pack = HubSocial.of(leader).pack();
            long emissaries = pack.stream().filter(m -> m.rank().equals("emissary")).count();
            helper.assertTrue(emissaries == 1 && pack.size() == 3, "the hub lists " + pack);
        } finally {
            clean(helper, leader, second, witch, taken, rival);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anInvitationNeedsTheInviterToStillLead(GameTestHelper helper) {
        ServerPlayer first = alpha(helper, 3, 1.5);
        ServerPlayer guest = human(helper, 2.5);
        ServerPlayer stronger = alpha(helper, 9, 3.5);
        ServerPlayer a = alpha(helper, 2, 4.5);
        ServerPlayer b = alpha(helper, 2, 5.5);
        online(first, guest, stronger, a, b);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(PackCommand.invite(first, guest) == null, "the invitation was refused");
            String token = PackCommand.requests().tokenOf(first.getUUID(), guest.getUUID()).orElseThrow();
            helper.assertTrue(join(first, stronger) && index.isLeader(first.getUUID()), "the stronger Alpha could not join");
            helper.assertTrue(PackDuelGameTests.win(stronger, first) && !index.isLeader(first.getUUID()), "the duel winner does not lead");
            helper.assertTrue(PackCommand.accept(guest, token) != null && index.alphaOf(guest.getUUID()).isEmpty(), "an invitation from someone who no longer leads was accepted");

            helper.assertTrue(PackCommand.allyInvite(a, b) == null, "the alliance request was refused");
            String ally = PackCommand.allyRequests().tokenOf(a.getUUID(), b.getUUID()).orElseThrow();
            helper.assertTrue(join(stronger, a) && !index.isLeader(a.getUUID()), "the first Alpha could not join the new leader's pack");
            helper.assertTrue(PackCommand.allyAccept(b, ally) != null && index.alliesOf(a.getUUID()).isEmpty(), "an alliance was made with someone who no longer leads");
        } finally {
            clean(helper, first, guest, stronger, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theInvitationTellsALeaderTheirPackComesToo(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer bringing = alpha(helper, 3, 2.5);
        online(leader, bringing);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(bringing.getUUID(), UUID.randomUUID());
            index.join(bringing.getUUID(), UUID.randomUUID());
            Component withMembers = PackCommand.inviteMessage("Leader", "tok", index.betasOf(bringing.getUUID()).size());
            Component alone = PackCommand.inviteMessage("Leader", "tok", 0);
            helper.assertTrue(mentions(withMembers, "supernaturalbloodlines.pack.invite_merge"), "the invitation does not say the pack comes along");
            helper.assertTrue(!mentions(alone, "supernaturalbloodlines.pack.invite_merge"), "a lone invitee is told about a pack they do not have");
        } finally {
            clean(helper, leader, bringing);
        }
        helper.succeed();
    }

    private static boolean mentions(Component c, String key) {
        if (c.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
            return true;
        }
        return c.getSiblings().stream().anyMatch(s -> mentions(s, key));
    }

    // ---- the spark ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOutsideWerewolfKillingTheLeaderTakesTheSparkNotThePack(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        online(leader, second, beta, thief);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the co-Alpha could not join");
            kill(thief, leader);
            helper.assertTrue(state(thief).rank() == Rank.ALPHA && state(thief).stoleSpark(), "the thief is " + state(thief));
            helper.assertTrue(index.leaderOf(beta.getUUID()).equals(second.getUUID()) && second.getUUID().equals(state(beta).packAlpha()), "the pack did not go to the co-Alpha");
            helper.assertTrue(index.betasOf(thief.getUUID()).isEmpty() && !index.inPack(thief.getUUID()), "the thief got the pack");
            helper.assertTrue(state(leader).rank() == Rank.OMEGA, "the victim is " + state(leader));
        } finally {
            clean(helper, leader, second, beta, thief);
        }
        // and with no co-Alpha the pack dissolves
        ServerPlayer lone = alpha(helper, 6, 1.5);
        ServerPlayer member = wolf(helper, Rank.BETA, lone.getUUID(), 1, 3.5);
        ServerPlayer thief2 = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        online(lone, member, thief2);
        try {
            kill(thief2, lone);
            helper.assertTrue(state(thief2).rank() == Rank.ALPHA && index.betasOf(thief2.getUUID()).isEmpty(), "the thief is " + state(thief2) + " with a pack");
            helper.assertTrue(state(member).rank() == Rank.OMEGA && index.alphaOf(member.getUUID()).isEmpty(), "the dissolved pack's Beta is " + state(member));
        } finally {
            clean(helper, lone, member, thief2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlphaWhoRunsWithAPackStealsFromOutsideItAndStacksTheSpark(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer outsider = alpha(helper, 5, 3.5);
        online(leader, second, outsider);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the co-Alpha could not join");
            kill(second, outsider);
            helper.assertTrue(state(outsider).rank() == Rank.OMEGA && state(outsider).sparks() == 1, "the victim is " + state(outsider));
            helper.assertTrue(state(second).rank() == Rank.ALPHA && state(second).sparks() == 2 && state(second).stoleSpark(), "the thief is " + state(second));
            helper.assertTrue(index.alphaOf(second.getUUID()).map(leader.getUUID()::equals).orElse(false), "the thief left the pack");
        } finally {
            clean(helper, leader, second, outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCoAlphaGivingUpTheSparkStaysAsABetaOfTheLeader(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer patient = human(helper, 3.5);
        online(leader, second, patient);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the co-Alpha could not join");
            helper.assertTrue(PackCommand.heal(second, patient) == null, "the heal was refused");
            helper.assertTrue(state(second).rank() == Rank.BETA && leader.getUUID().equals(state(second).packAlpha()), "the healer is " + state(second));
            helper.assertTrue(leader.getUUID().equals(index.alphaOf(second.getUUID()).orElse(null)) && state(leader).rank() == Rank.ALPHA, "the pack changed");
            helper.assertTrue(index.betasOf(leader.getUUID()).contains(second.getUUID()), "the healer left the pack");
        } finally {
            clean(helper, leader, second, patient);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alliedPacksCannotHurtEachOtherPackmatesIncluded(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer b = alpha(helper, 5, 2.5);
        ServerPlayer mateA = human(helper, 3.5);
        ServerPlayer mateB = human(helper, 4.5);
        ServerPlayer stranger = human(helper, 5.5);
        online(a, b, mateA, mateB, stranger);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(a, mateA) && join(b, mateB), "the packs could not be built");
            helper.assertTrue(index.ally(a.getUUID(), b.getUUID()), "the packs could not ally");
            float before = mateB.getHealth();
            hit(mateB, mateA.damageSources().playerAttack(mateA));
            helper.assertTrue(mateB.getHealth() == before, "a Packmate hurt a Packmate of an allied pack: " + before + " to " + mateB.getHealth());
            hit(mateA, b.damageSources().playerAttack(b));
            helper.assertTrue(mateA.getHealth() == mateA.getMaxHealth(), "an Alpha hurt a Packmate of an allied pack");
            float s = stranger.getHealth();
            hit(stranger, mateA.damageSources().playerAttack(mateA));
            helper.assertTrue(stranger.getHealth() < s, "a Packmate cannot hurt a stranger");
        } finally {
            clean(helper, a, b, mateA, mateB, stranger);
        }
        helper.succeed();
    }

    // ---- told ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void removalsAreToldNowOrAtTheNextLogin(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        RecordingPlayer here = RecordingPlayer.create(helper);
        UUID away = UUID.randomUUID();
        UUID awayToo = UUID.randomUUID();
        PackIndex index = PackIndex.get(helper.getLevel());
        Map<UUID, ServerPlayer> map = new HashMap<>();
        map.put(leader.getUUID(), leader);
        map.put(here.getUUID(), here);
        SireCall.overridePlayerLookup(map::get);
        try {
            index.join(leader.getUUID(), here.getUUID());
            index.join(leader.getUUID(), away);
            index.join(leader.getUUID(), awayToo);

            helper.assertTrue(PackMembers.remove(helper.getLevel().getServer(), here.getUUID(), "supernaturalbloodlines.pack.kicked", "Leader"), "the online member was not removed");
            helper.assertTrue(here.received("supernaturalbloodlines.pack.kicked"), "an online member was not told");
            helper.assertTrue(PackMembers.remove(helper.getLevel().getServer(), away, "supernaturalbloodlines.pack.kicked", "Leader"), "the offline member was not removed");
            helper.assertTrue(index.takeNotice(away).map(n -> n[0].equals("supernaturalbloodlines.pack.kicked") && n[1].equals("Leader")).orElse(false), "no notice waits for the one who was away");
            helper.assertTrue(index.takeNotice(away).isEmpty(), "a notice stays after it was taken");

            // moved to a new leader
            PackMembers.moved(helper.getLevel().getServer(), List.of(awayToo, here.getUUID()), leader.getUUID());
            helper.assertTrue(index.takeNotice(awayToo).map(n -> n[0].equals("supernaturalbloodlines.pack.now_follows")).orElse(false), "the offline member was not told who leads");
            helper.assertTrue(here.received("supernaturalbloodlines.pack.now_follows"), "the online member was not told who leads");
        } finally {
            clean(helper, leader);
            index.leave(awayToo);
            index.takeNotice(awayToo);
            index.takeNotice(away);
        }
        helper.succeed();
    }

    // ---- stacked Alpha sparks (the Deucalion factor) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOutsideStealStacksTheSparkAndTakesEveryOneTheVictimHad(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer b = alpha(helper, 5, 2.5);
        ServerPlayer c = alpha(helper, 5, 3.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        online(a, b, c, thief);
        try {
            kill(a, b);
            helper.assertTrue(state(a).sparks() == 2 && state(b).rank() == Rank.OMEGA && state(b).sparks() == 1, "after one steal: " + state(a).sparks() + " / " + state(b));
            kill(a, c);
            helper.assertTrue(state(a).sparks() == 3, "after two: " + state(a).sparks());
            // a non-Alpha who kills the one with three takes all three at once
            kill(thief, a);
            helper.assertTrue(state(thief).rank() == Rank.ALPHA && state(thief).sparks() == 3, "the thief is " + state(thief));
            helper.assertTrue(state(a).rank() == Rank.OMEGA && state(a).sparks() == 1, "the victim kept sparks: " + state(a));
        } finally {
            clean(helper, a, b, c, thief);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stackedSparksAreCappedByTheConfig(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer b = alpha(helper, 5, 2.5);
        ServerPlayer c = alpha(helper, 5, 3.5);
        ServerPlayer d = alpha(helper, 5, 4.5);
        online(a, b, c, d);
        SpeciesConfig.overrideMaxStackedSparks(3);
        try {
            kill(a, b);
            kill(a, c);
            kill(a, d);
            helper.assertTrue(state(a).sparks() == 3, "the cap of 3 was not kept: " + state(a).sparks());
            helper.assertTrue(state(d).rank() == Rank.OMEGA, "the capped steal did not take the victim's spark: " + state(d));
        } finally {
            SpeciesConfig.overrideMaxStackedSparks(null);
            clean(helper, a, b, c, d);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaLosesOnlyTheStackedSparksAndKeepsTheirOwn(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, null, 5, 1.5);
        ServerPlayer victim = alpha(helper, 5, 2.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 3.5);
        ServerPlayer rival = alpha(helper, 5, 4.5);
        online(truth, victim, thief, rival);
        try {
            kill(truth, victim);
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA && state(truth).sparks() == 2, "the True Alpha did not stack: " + state(truth));
            kill(rival, truth);
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA && state(truth).sparks() == 1, "the True spark was taken or the extras stayed: " + state(truth));
            helper.assertTrue(state(rival).rank() == Rank.ALPHA && state(rival).sparks() == 2, "the killer took " + (state(rival).sparks() - 1) + " instead of the one stacked");
            // with none stacked, killing one is just a death
            kill(thief, truth);
            helper.assertTrue(state(thief).rank() == Rank.OMEGA && state(truth).rank() == Rank.TRUE_ALPHA, "a True Alpha's own spark was taken");
        } finally {
            clean(helper, truth, victim, thief, rival);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyExtraSparkRaisesTheAlphaBonusesAndTheRoarTier(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer b = alpha(helper, 5, 2.5);
        ServerPlayer mate = wolf(helper, Rank.BETA, a.getUUID(), 1, 3.5);
        online(a, b, mate);
        try {
            double one = a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            net.minecraft.world.entity.ai.attributes.AttributeModifier before = a.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(RankEffects.MODIFIER_ID);
            helper.assertTrue(before != null && Math.abs(before.amount() - 0.20) < 1e-9, "one spark: attack " + (before == null ? null : before.amount()));
            kill(a, b);
            net.minecraft.world.entity.ai.attributes.AttributeModifier after = a.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(RankEffects.MODIFIER_ID);
            helper.assertTrue(after != null && Math.abs(after.amount() - 0.22) < 1e-9, "two sparks: attack " + (after == null ? null : after.amount()));
            helper.assertTrue(a.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) > one, "the health bonus did not grow");
            helper.assertTrue(PackRules.roarTier(state(a).sparks(), SpeciesConfig.maxStackedSparks()) == 1, "the Roar tier is not 1");
            AlphaRoar.roar(a);
            var strength = mate.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
            helper.assertTrue(strength != null && strength.getAmplifier() == 1, "the Roar gave Strength " + (strength == null ? "none" : strength.getAmplifier() + 1) + " instead of II");
        } finally {
            clean(helper, a, b, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCureTheHealAndLosingTheFactionDropEveryStackedSpark(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 5, 1.5);
        ServerPlayer b = alpha(helper, 5, 2.5);
        ServerPlayer c = alpha(helper, 5, 3.5);
        ServerPlayer d = alpha(helper, 5, 4.5);
        ServerPlayer patient = human(helper, 5.5);
        online(a, b, c, d, patient);
        try {
            kill(a, b);
            kill(c, d);
            helper.assertTrue(state(a).sparks() == 2 && state(c).sparks() == 2, "the setup did not stack");

            // leaving the faction (the cure): the rank goes and the extra spark with it
            VampirismAPI.factionPlayerHandler(a).leaveFaction(false);
            helper.assertTrue(state(a).sparks() == 1 && state(a).rank() != Rank.ALPHA, "the cure left sparks: " + state(a));

            // /pack heal gives the spark up
            helper.assertTrue(PackCommand.heal(c, patient) == null, "the heal was refused");
            helper.assertTrue(state(c).sparks() == 1 && state(c).rank() != Rank.ALPHA, "the heal left sparks: " + state(c));
        } finally {
            clean(helper, a, b, c, d, patient);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHubShowsTheSparkCountOnAnAlphasRow(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer outsider = alpha(helper, 5, 3.5);
        online(leader, second, outsider);
        try {
            helper.assertTrue(join(leader, second), "the co-Alpha could not join");
            kill(leader, outsider);
            var pack = net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.of(second).pack();
            helper.assertTrue(pack.get(0).rank().equals("alpha:2") && pack.get(0).base().equals("alpha") && pack.get(0).sparks() == 2, "the leader's row is " + pack.get(0) + " state " + state(leader).sparks() + " index " + PackIndex.get(helper.getLevel()).sparksOf(leader.getUUID()) + " leader=" + leader.getUUID());
            helper.assertTrue(pack.get(1).rank().equals("coalpha") && pack.get(1).sparks() == 1, "the co-Alpha's row is " + pack.get(1));
        } finally {
            clean(helper, leader, second, outsider);
        }
        helper.succeed();
    }

    // ---- the absent leader ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderAwayForSevenDaysLosesTheLeadToTheStrongestSecondAlpha(GameTestHelper helper) {
        UUID goneId = UUID.randomUUID();
        ServerPlayer weak = alpha(helper, 2, 1.5);
        ServerPlayer strong = alpha(helper, 9, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, goneId, 1, 3.5);
        online(weak, strong, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            // the leader is somebody who is not online, with a pack
            index.join(goneId, weak.getUUID());
            index.join(goneId, strong.getUUID());
            index.join(goneId, beta.getUUID());
            index.setStrength(weak.getUUID(), 1002);
            index.setStrength(strong.getUUID(), 1009);
            index.setLastSeen(goneId, now - 6 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(goneId), "a leader away for 6 days was deposed");

            index.setLastSeen(goneId, now - 8 * PackAbsence.DAY_MILLIS);
            SpeciesConfig.overrideAbsentLeaderDays(0);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(goneId), "the switch at 0 did not turn it off");
            SpeciesConfig.overrideAbsentLeaderDays(null);

            helper.assertTrue(PackAbsence.check(server, now) == 1, "the absent leader was not deposed");
            helper.assertTrue(index.isLeader(strong.getUUID()), "the strongest second Alpha does not lead: " + index.leaderOf(strong.getUUID()));
            helper.assertTrue(index.alphaOf(goneId).map(strong.getUUID()::equals).orElse(false), "the old leader is not a member of the new pack");
            helper.assertTrue(strong.getUUID().equals(state(beta).packAlpha()), "the Beta did not follow");
            helper.assertTrue(index.noticeCount(goneId) == 1 && !index.isFormerAlpha(goneId), "the old leader was not told, or lost the spark");
            helper.assertTrue(PackAbsence.check(server, now) == 0, "it deposed again");
        } finally {
            index.leave(goneId);
            index.takeNotices(goneId);
            clean(helper, weak, strong, beta);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLeaderWithNoRecordedTimeOrNoSecondAlphaOrWhoIsOnlineIsLeftAlone(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        UUID away = UUID.randomUUID();
        UUID mate = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        online(leader);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            // an online leader, however old their last time is
            index.join(leader.getUUID(), mate);
            index.join(leader.getUUID(), second);
            index.setStrength(second, 1003);
            index.setLastSeen(leader.getUUID(), now - 30 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(leader.getUUID()), "an online leader was deposed");

            // a leader nobody recorded a time for: the time starts now, nobody is deposed
            index.join(away, UUID.randomUUID());
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.lastSeen(away).isPresent() && index.isLeader(away), "an unrecorded leader was deposed or no time was started");
            // away a long time, but no second Alpha: nothing to hand the pack to
            index.setLastSeen(away, now - 20 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(away), "a pack with no second Alpha changed hands");
        } finally {
            index.leave(mate);
            index.leave(second);
            for (UUID id : index.betasOf(away)) {
                index.leave(id);
            }
            clean(helper, leader);
        }
        helper.succeed();
    }

    // ---- the absent leader, round 3 ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAbsentLeaderIsOnlyReplacedBySomeoneWhoIsHere(GameTestHelper helper) {
        ServerPlayer gone = alpha(helper, 6, 1.5);
        ServerPlayer away = alpha(helper, 9, 2.5);
        ServerPlayer here = alpha(helper, 2, 3.5);
        online(here); // the leader and the stronger Alpha are both away
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            index.join(gone.getUUID(), away.getUUID());
            index.join(gone.getUUID(), here.getUUID());
            index.setStrength(away.getUUID(), 1009);
            index.setStrength(here.getUUID(), 1002);
            index.setLastSeen(gone.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            // the stronger Alpha was seen 8 days ago too: not here, and no record at all counts as not here
            index.setLastSeen(away.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 1 && index.isLeader(here.getUUID()), "the successor is not the one who is here: " + index.leaderOf(here.getUUID()));
            PackIndex.get(helper.getLevel()).leave(away.getUUID());
        } finally {
            index.leave(gone.getUUID());
            clean(helper, gone, away, here);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoAbsentAlphasNeverPassTheLeadBackAndForth(GameTestHelper helper) {
        ServerPlayer a = alpha(helper, 6, 1.5);
        ServerPlayer b = alpha(helper, 9, 2.5);
        online(); // nobody is here
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            index.join(a.getUUID(), b.getUUID());
            index.setStrength(b.getUUID(), 1009);
            index.setLastSeen(a.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(a.getUUID()), "the lead moved to somebody with no record");
            index.setLastSeen(b.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(a.getUUID()), "the lead moved to somebody who is away as well");
            // b was seen yesterday: the lead goes to b, and b away for a week does not send it back to a (a was away longer)
            index.setLastSeen(b.getUUID(), now - PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 1 && index.isLeader(b.getUUID()), "the lead did not go to the one seen yesterday");
            long later = now + 8 * PackAbsence.DAY_MILLIS;
            helper.assertTrue(PackAbsence.check(server, later) == 0 && index.isLeader(b.getUUID()), "the lead was passed back to an Alpha who is away too");
        } finally {
            index.leave(a.getUUID());
            index.leave(b.getUUID());
            clean(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLeadMovesAtExactlySevenDaysAndKeepsTheOldLeadersRankAndSparks(GameTestHelper helper) {
        ServerPlayer gone = alpha(helper, 6, 1.5);
        TalkingDuelist next = talkingAlpha(helper, 3, 2.5);
        ServerPlayer mate = wolf(helper, Rank.BETA, gone.getUUID(), 1, 3.5);
        online(next, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.setSparks(gone, 2);
            index.join(gone.getUUID(), next.getUUID());
            index.join(gone.getUUID(), mate.getUUID());
            index.setStrength(next.getUUID(), 1003);
            index.setLastSeen(gone.getUUID(), now - 7 * PackAbsence.DAY_MILLIS + 1);
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(gone.getUUID()), "7 days minus a millisecond was enough");
            index.setLastSeen(gone.getUUID(), now - 7 * PackAbsence.DAY_MILLIS);
            helper.assertTrue(PackAbsence.check(server, now) == 1 && index.isLeader(next.getUUID()), "exactly 7 days was not enough");
            helper.assertTrue(state(gone).rank() == Rank.ALPHA && state(gone).sparks() == 2, "the old leader lost their rank or sparks: " + state(gone));
            helper.assertTrue(next.received("supernaturalbloodlines.pack.absent_lead_now") && !next.received("supernaturalbloodlines.pack.absent_leader"),
                    "the new leader was not told they lead, or was told about themselves in the third person: " + next.keys());
            helper.assertTrue(index.noticeCount(gone.getUUID()) == 1, "the old leader has no notice for their next login");
        } finally {
            index.leave(gone.getUUID());
            index.takeNotices(gone.getUUID());
            clean(helper, gone, next, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyOnlinePackMemberIsRenewedEveryHourAndTheAliveTimeIsWrittenOncePerMinute(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer mate = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        online(leader, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            helper.assertTrue(index.inPack(leader.getUUID()), "the pack was not indexed");
            index.setLastSeen(leader.getUUID(), now - 2 * PackAbsence.REFRESH_MILLIS);
            index.setLastSeen(mate.getUUID(), now - 2 * PackAbsence.REFRESH_MILLIS);
            PackAbsence.check(server, now);
            helper.assertTrue(index.lastSeen(leader.getUUID()).orElse(0L) == now, "a crash left an online leader looking away: " + index.lastSeen(leader.getUUID()));
            helper.assertTrue(index.lastSeen(mate.getUUID()).orElse(0L) == now, "a crash left an online member looking away: " + index.lastSeen(mate.getUUID()));

            // "the server is running" is written at most once a minute
            index.setAliveAt(0);
            PackAbsence.check(server, now);
            helper.assertTrue(index.aliveAt() == now, "the alive time was not written");
            PackAbsence.check(server, now + 30_000L);
            helper.assertTrue(index.aliveAt() == now, "it was written twice within a minute");
            PackAbsence.check(server, now + PackAbsence.ALIVE_EVERY_MILLIS);
            helper.assertTrue(index.aliveAt() == now + PackAbsence.ALIVE_EVERY_MILLIS, "it was not written after a minute");
        } finally {
            clean(helper, leader, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRestartForgivesOnlyTheTimeTheServerWasOffAndNeverEraseAnAbsence(GameTestHelper helper) {
        ServerPlayer gone = alpha(helper, 6, 1.5);
        ServerPlayer next = alpha(helper, 3, 2.5);
        online(next);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        try {
            index.join(gone.getUUID(), next.getUUID());
            index.setStrength(next.getUUID(), 1003);
            // 8 days away, and the server was off for the last 3 of them: 5 days of absence are left, not 8 and not 0
            index.setLastSeen(gone.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            index.setAliveAt(now - 3 * PackAbsence.DAY_MILLIS);
            PackAbsence.onStarted(server, now);
            helper.assertTrue(index.lastSeen(gone.getUUID()).orElse(0L) == now - 5 * PackAbsence.DAY_MILLIS, "downtime was not forgiven exactly: " + (now - index.lastSeen(gone.getUUID()).orElse(0L)));
            helper.assertTrue(index.aliveAt() == now, "the start did not record that the server is running");
            helper.assertTrue(PackAbsence.check(server, now) == 0 && index.isLeader(gone.getUUID()), "downtime counted as an absence");

            // a restart a minute after the last sign of life must not save somebody who was away for 8 days
            index.setLastSeen(gone.getUUID(), now - 8 * PackAbsence.DAY_MILLIS);
            index.setAliveAt(now - 60_000L);
            PackAbsence.onStarted(server, now);
            helper.assertTrue(PackAbsence.check(server, now) == 1 && index.isLeader(next.getUUID()), "a quick restart wiped an 8 day absence");
        } finally {
            index.leave(gone.getUUID());
            index.takeNotices(gone.getUUID());
            clean(helper, gone, next);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWorldThatNeverRecordedItIsNotShifted(GameTestHelper helper) {
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        UUID some = UUID.randomUUID();
        try {
            index.setLastSeen(some, now - 8 * PackAbsence.DAY_MILLIS);
            index.setAliveAt(0);
            PackAbsence.onStarted(server, now);
            helper.assertTrue(index.lastSeen(some).orElse(0L) == now - 8 * PackAbsence.DAY_MILLIS, "an old world was shifted by a gap nobody knows");
            helper.assertTrue(index.aliveAt() == now, "the start did not begin recording");
        } finally {
            index.setLastSeen(some, 0);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void lastSeenIsOnlyKeptForPackMembers(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer stranger = human(helper, 2.5);
        online(leader, stranger);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(leader.getUUID(), UUID.randomUUID());
            PackIndex.login(leader);
            PackIndex.login(stranger);
            helper.assertTrue(index.lastSeen(leader.getUUID()).isPresent(), "a pack member's time was not kept");
            helper.assertTrue(index.lastSeen(stranger.getUUID()).isEmpty(), "the time of somebody in no pack was kept");
        } finally {
            for (UUID id : index.betasOf(leader.getUUID())) {
                index.leave(id);
            }
            clean(helper, leader, stranger);
        }
        helper.succeed();
    }

    // ---- a True Alpha who leaves ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaWhoLeavesHandsThePackToTheirSecondAlphaAndDissolvesItOnlyWithNone(GameTestHelper helper) {
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, null, 5, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer beta = wolf(helper, Rank.BETA, truth.getUUID(), 1, 3.5);
        online(truth, second, beta);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            index.join(truth.getUUID(), second.getUUID());
            VampirismAPI.factionPlayerHandler(truth).leaveFaction(false);
            helper.assertTrue(index.isLeader(second.getUUID()) && index.alphaOf(beta.getUUID()).map(second.getUUID()::equals).orElse(false), "the second Alpha did not take the pack");
            helper.assertTrue(second.getUUID().equals(state(beta).packAlpha()) && state(beta).rank() == Rank.BETA, "the Beta did not follow: " + state(beta));
            helper.assertTrue(state(truth).rank() == Rank.TRUE_ALPHA && index.isFormerAlpha(truth.getUUID()), "the True Alpha lost the spark, or no tombstone");
        } finally {
            clean(helper, truth, second, beta);
        }
        helper.succeed();
    }

    // ---- a kill inside the pack ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillInsideThePackMakesNoBlueEyesAndNoGeneAwakening(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer killer = wolf(helper, Rank.BETA, leader.getUUID(), 1, 2.5);
        ServerPlayer mate = human(helper, 3.5);
        ServerPlayer stranger = human(helper, 4.5);
        ServerPlayer carrier = human(helper, 5.5);
        ServerPlayer carrierMate = human(helper, 6.5);
        ServerPlayer carrier2 = human(helper, 7.5);
        online(leader, killer, mate, stranger, carrier, carrierMate, carrier2);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, mate) && join(leader, carrier) && join(leader, carrierMate), "the Packmates could not join");
            kill(killer, mate);
            helper.assertTrue(!state(killer).killedInnocent(), "a kill inside the pack turned the killer's eyes blue");
            kill(killer, stranger);
            helper.assertTrue(state(killer).killedInnocent(), "the control kill of a stranger no longer turns the eyes blue");

            // a human with the latent gene: a kill inside the pack awakens nothing, a kill outside it does
            BloodlineAccess.set(carrier, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().latentGene(true).geneRolled(true).build()).build());
            BloodlineAccess.set(carrier2, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().latentGene(true).geneRolled(true).build()).build());
            kill(carrier, carrierMate);
            helper.assertTrue(state(carrier).lineage() == Lineage.NONE, "a kill inside the pack awakened the gene");
            kill(carrier2, stranger);
            helper.assertTrue(state(carrier2).lineage() != Lineage.NONE, "the control kill no longer awakens the gene");
        } finally {
            clean(helper, leader, killer, mate, stranger, carrier, carrierMate, carrier2);
        }
        helper.succeed();
    }

    // ---- leaving a pack just before a kill ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void someoneWhoLeftThePackAMomentAgoStillCountsAsItsMemberForAKill(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        long[] clock = {1_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackCommand.leave(second) == null && !index.inPack(second.getUUID()), "the second Alpha could not leave");
            kill(second, leader);
            helper.assertTrue(state(leader).rank() == Rank.ALPHA && state(second).sparks() == 1 && !state(second).stoleSpark(), "leaving just before the kill gained the spark");

            clock[0] += 11 * 60_000L;
            kill(second, leader);
            helper.assertTrue(state(leader).rank() == Rank.OMEGA && state(second).sparks() == 2, "after the grace the kill no longer stole: " + state(leader).rank() + " " + state(second).sparks());
        } finally {
            PackIndex.overrideClock(null);
            clean(helper, leader, second);
        }
        // and with the grace at 0 the kill steals at once
        ServerPlayer leader2 = alpha(helper, 6, 1.5);
        ServerPlayer second2 = alpha(helper, 3, 2.5);
        online(leader2, second2);
        SpeciesConfig.overrideLeftPackGrace(0);
        try {
            helper.assertTrue(join(leader2, second2) && PackCommand.leave(second2) == null, "the second pack could not be built");
            kill(second2, leader2);
            helper.assertTrue(state(leader2).rank() == Rank.OMEGA, "the switch at 0 did not turn the grace off");
        } finally {
            SpeciesConfig.overrideLeftPackGrace(null);
            clean(helper, leader2, second2);
        }
        helper.succeed();
    }

    // ---- what a stolen spark says ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aStealTellsTheKillerTheCountAndTheCapAndTheVictimWhoTookIt(GameTestHelper helper) {
        TalkingDuelist killer = talkingAlpha(helper, 5, 1.5);
        TalkingDuelist victim = talkingAlpha(helper, 5, 2.5);
        ServerPlayer away = alpha(helper, 5, 3.5); // not online: told at the next login
        online(killer, victim);
        PackIndex index = PackIndex.get(helper.getLevel());
        SpeciesConfig.overrideMaxStackedSparks(2);
        try {
            kill(killer, victim);
            helper.assertTrue(killer.received("supernaturalbloodlines.pack.spark_gained") && !killer.received("supernaturalbloodlines.pack.spark_capped"), "the killer was not told the count: " + killer.keys());
            helper.assertTrue(victim.received("supernaturalbloodlines.pack.spark_lost"), "the victim was not told who took it");
            kill(killer, away);
            helper.assertTrue(killer.received("supernaturalbloodlines.pack.spark_capped"), "the cap was not mentioned");
            helper.assertTrue(index.takeNotices(away.getUUID()).stream().anyMatch(n -> n[0].equals("supernaturalbloodlines.pack.spark_lost")), "an absent victim was not left a notice");
        } finally {
            SpeciesConfig.overrideMaxStackedSparks(null);
            clean(helper, killer, victim, away);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCureOrAHealTellsTheAlphaTheirStackedSparksAreGone(GameTestHelper helper) {
        TalkingDuelist stacked = talkingAlpha(helper, 5, 1.5);
        TalkingDuelist healer = talkingAlpha(helper, 5, 2.5);
        ServerPlayer patient = human(helper, 3.5);
        online(stacked, healer, patient);
        try {
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.setSparks(stacked, 3);
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess.setSparks(healer, 2);
            VampirismAPI.factionPlayerHandler(stacked).leaveFaction(false);
            helper.assertTrue(stacked.received("supernaturalbloodlines.pack.spark_reset"), "the cure said nothing about the stacked sparks: " + stacked.keys());
            helper.assertTrue(PackCommand.heal(healer, patient) == null && healer.received("supernaturalbloodlines.pack.spark_reset"), "the heal said nothing about the stacked sparks");
        } finally {
            clean(helper, stacked, healer, patient);
        }
        helper.succeed();
    }

    // ---- the grace, and what is said when it makes a kill nothing ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKillerWhoseKillIsIgnoredByTheGraceIsToldAndAPlainInPackKillIsNotCommented(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        TalkingDuelist second = talkingAlpha(helper, 3, 2.5);
        TalkingDuelist other = talkingAlpha(helper, 3, 3.5);
        online(leader, second, other);
        try {
            helper.assertTrue(join(leader, second) && join(leader, other), "the pack could not be built");
            kill(other, leader); // inside the pack, no grace needed: nothing is said
            helper.assertTrue(!other.received("supernaturalbloodlines.pack.grace_no_spark"), "an ordinary in-pack kill was commented");
            helper.assertTrue(PackCommand.leave(second) == null, "the second Alpha could not leave");
            kill(second, leader);
            helper.assertTrue(second.received("supernaturalbloodlines.pack.grace_no_spark_you") && !second.received("supernaturalbloodlines.pack.grace_no_spark"), "the one who left a moment ago was not told, in the first person, why there is no spark: " + second.keys());
            helper.assertTrue(state(leader).rank() == Rank.ALPHA, "leaving first gained the spark");
        } finally {
            clean(helper, leader, second, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKickedMemberGetsNoGraceAndTheLeaderCanBeRobbedAtOnce(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        online(leader, second);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackMembers.remove(helper.getLevel().getServer(), second.getUUID(), "supernaturalbloodlines.pack.kicked", "Leader"), "the kick failed");
            kill(second, leader);
            helper.assertTrue(state(leader).rank() == Rank.OMEGA && state(second).sparks() == 2, "a kicked member got a grace: " + state(leader).rank() + " " + state(second).sparks());
        } finally {
            clean(helper, leader, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingAndJoiningAFriendsPackDoesNotFreeAKillOnTheOldOne(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer friend = alpha(helper, 4, 3.5);
        online(leader, second, friend);
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackCommand.leave(second) == null && join(friend, second), "the second Alpha could not move to the friend's pack");
            kill(second, leader);
            helper.assertTrue(state(leader).rank() == Rank.ALPHA && state(second).sparks() == 1, "joining another pack freed the kill on the old one");
        } finally {
            clean(helper, leader, second, friend);
        }
        helper.succeed();
    }

    // ---- True Alpha victims, and the cure line ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTrueAlphaVictimIsToldOnlyAboutTheSparksStackedOnTheirs(GameTestHelper helper) {
        TalkingDuelist killer = talkingAlpha(helper, 5, 1.5);
        TalkingDuelist stacked = talkingAlpha(helper, 5, 2.5);
        TalkingDuelist plain = talkingAlpha(helper, 5, 3.5);
        online(killer, stacked, plain);
        try {
            for (TalkingDuelist t : new TalkingDuelist[] {stacked, plain}) {
                BloodlineAccess.set(t, BloodlineAccess.get(t).toBuilder().wolf(state(t).toBuilder().rank(Rank.TRUE_ALPHA).build()).build());
            }
            PackAccess.setSparks(stacked, 2);
            kill(killer, stacked);
            helper.assertTrue(stacked.received("supernaturalbloodlines.pack.spark_lost_stacked") && !stacked.received("supernaturalbloodlines.pack.spark_lost"),
                    "the True Alpha with a stacked spark was told the wrong thing: " + stacked.keys());
            kill(killer, plain);
            helper.assertTrue(!plain.received("supernaturalbloodlines.pack.spark_lost") && !plain.received("supernaturalbloodlines.pack.spark_lost_stacked"),
                    "a True Alpha with nothing stacked was told a spark was taken: " + plain.keys());
        } finally {
            clean(helper, killer, stacked, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCureTellsTheAlphaTheirStackedSparksAreGoneExactlyOnce(GameTestHelper helper) {
        TalkingDuelist stacked = talkingAlpha(helper, 5, 1.5);
        online(stacked);
        try {
            PackAccess.setSparks(stacked, 3);
            // leaving the faction with the sparks still on the wolf layer: the listener and passOn both know them
            VampirismAPI.factionPlayerHandler(stacked).leaveFaction(false);
            helper.assertTrue(stacked.count("supernaturalbloodlines.pack.spark_reset") == 1, "the cure said it " + stacked.count("supernaturalbloodlines.pack.spark_reset") + " times: " + stacked.keys());
        } finally {
            clean(helper, stacked);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCuresOwnPathClearsTheLayerFirstAndStillTellsExactlyOnce(GameTestHelper helper) {
        TalkingDuelist stacked = talkingAlpha(helper, 5, 1.5);
        online(stacked);
        try {
            PackAccess.setSparks(stacked, 3);
            // what the Cure does: the wolf layer is cleared first, the faction is left after
            BloodlineAccess.set(stacked, BloodlineAccess.get(stacked).toBuilder().wolf(WolfState.NONE).build());
            VampirismAPI.factionPlayerHandler(stacked).leaveFaction(false);
            helper.assertTrue(stacked.count("supernaturalbloodlines.pack.spark_reset") == 1, "the cure said it " + stacked.count("supernaturalbloodlines.pack.spark_reset") + " times: " + stacked.keys());
        } finally {
            clean(helper, stacked);
        }
        helper.succeed();
    }

    // ---- /pack leave, driven for real, for a werewolf Beta (round 5) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWerewolfBetaWhoRunsPackLeaveStillGetsTheGraceAndTheOldPartyIsSynced(GameTestHelper helper) {
        TalkingDuelist leader = talkingAlpha(helper, 6, 1.5);
        TalkingDuelist beta = talkingBeta(helper, leader.getUUID(), 2.5);
        ServerPlayer mate = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        online(leader, beta, mate);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyBackend fake = new net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyBackend() {
            final java.util.Map<UUID, UUID> ids = new java.util.HashMap<>();
            final java.util.Map<UUID, java.util.Set<UUID>> parties = new java.util.HashMap<>();

            @Override
            public Found find(net.minecraft.server.MinecraftServer server, UUID owner, UUID id) {
                return id.equals(ids.get(owner)) ? new Found(true, new net.ofatech.supernaturalbloodlines.core.bloodline.PartySyncRules.Party(owner, new java.util.HashSet<>(parties.get(owner)))) : new Found(true, null);
            }

            @Override
            public java.util.Optional<UUID> create(net.minecraft.server.MinecraftServer server, UUID owner) {
                ids.put(owner, UUID.randomUUID());
                parties.put(owner, new java.util.HashSet<>(java.util.Set.of(owner)));
                return java.util.Optional.of(ids.get(owner));
            }

            @Override
            public boolean add(net.minecraft.server.MinecraftServer server, UUID owner, UUID member) {
                return parties.get(owner).add(member) || true;
            }

            @Override
            public boolean remove(net.minecraft.server.MinecraftServer server, UUID owner, UUID member) {
                parties.get(owner).remove(member);
                return true;
            }
        };
        net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink.setBackend(fake);
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(index.alphaOf(beta.getUUID()).map(leader.getUUID()::equals).orElse(false), "the Beta was not in the pack");
            net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink.sync(server, leader.getUUID(), true);
            helper.assertTrue(index.mirrorOwner(beta.getUUID()).isPresent(), "the pack was not mirrored to start with");

            helper.assertTrue(PackCommand.leave(beta) == null, "the Beta could not leave");
            helper.assertTrue(!index.inPack(beta.getUUID()), "the Beta is still in the pack");
            // the party of the pack they LEFT was synced: the Beta is out of it
            helper.assertTrue(index.mirrorOwner(beta.getUUID()).isEmpty(), "the old pack's party was not synced: the leaver is still mirrored");
            helper.assertTrue(index.leftRecently(beta.getUUID(), 10), "no grace was recorded for the werewolf Beta who left");

            kill(beta, leader);
            helper.assertTrue(state(leader).rank() == Rank.ALPHA && state(leader).sparks() == 1 && !state(beta).stoleSpark(), "leaving as a werewolf Beta just before the kill gained the spark: " + state(leader).rank());
            helper.assertTrue(!state(beta).killedInnocent(), "the kill turned the killer's eyes blue: leaving first made it an innocent kill");
            helper.assertTrue(PackIndex.samePack(beta, leader), "the leaver does not count as one pack with the one they left");
            helper.assertTrue(beta.received("supernaturalbloodlines.pack.grace_no_spark_you"), "the leaver was not told why there is no spark: " + beta.keys());
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink.setBackend(null);
            clean(helper, leader, beta, mate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGraceFollowsAPackThatChangesHandsByTransfer(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer leaver = wolf(helper, Rank.BETA, leader.getUUID(), 1, 3.5);
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null, 1, 4.5);
        online(leader, second, leaver, thief);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the second Alpha could not join");
            helper.assertTrue(PackCommand.leave(leaver) == null, "the Beta could not leave");
            kill(thief, leader); // stolen from outside: the pack goes to the second Alpha with transfer
            helper.assertTrue(index.isLeader(second.getUUID()), "the second Alpha does not lead");
            helper.assertTrue(index.killsInPack(leaver.getUUID(), second.getUUID(), 10), "the grace was lost when the pack changed hands");
            kill(leaver, second);
            helper.assertTrue(state(second).rank() == Rank.ALPHA, "a kill on the pack they just left, now under its new leader, stole the spark");
        } finally {
            clean(helper, leader, second, leaver, thief);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingThePartyIsLeavingByChoiceAndKeepsTheGrace(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        UUID member = UUID.randomUUID();
        online(leader);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            index.join(leader.getUUID(), member);
            helper.assertTrue(PackMembers.remove(server, member, "supernaturalbloodlines.pack.left_party", "", true), "the removal failed");
            helper.assertTrue(index.leftRecently(member, 10) && index.killsInPack(member, leader.getUUID(), 10), "leaving the party is not leaving by choice");

            index.join(leader.getUUID(), member);
            index.takeNotices(member);
            helper.assertTrue(PackMembers.remove(server, member, "supernaturalbloodlines.pack.kicked", "Leader"), "the kick failed");
            helper.assertTrue(index.killsInPack(member, leader.getUUID(), 10) == index.killsInPack(member, leader.getUUID(), 10) && !index.samePackNow(member, leader.getUUID()), "a kick");
        } finally {
            index.leave(member);
            index.takeNotices(member);
            clean(helper, leader);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCrashTimeIsWrittenEvenWithTheAbsenceCheckSwitchedOffAndAClockThatRanBackwards(GameTestHelper helper) {
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        long now = System.currentTimeMillis();
        SpeciesConfig.overrideAbsentLeaderDays(0);
        try {
            index.setAliveAt(0);
            PackAbsence.check(server, now);
            helper.assertTrue(index.aliveAt() == now, "the alive time was not written with the check switched off");
            // the clock went back (a time in the future was stored)
            index.setAliveAt(now + 10 * PackAbsence.DAY_MILLIS);
            PackAbsence.check(server, now);
            helper.assertTrue(index.aliveAt() == now, "a time in the future stayed: the next start would be shifted by a negative gap and then not at all");
        } finally {
            SpeciesConfig.overrideAbsentLeaderDays(null);
            index.setAliveAt(now);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void queryingTheGraceChangesNothingAndOldLeavesArePrunedByTheTick(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        UUID member = UUID.randomUUID();
        online(leader);
        long[] clock = {1_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        PackIndex index = PackIndex.get(helper.getLevel());
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        try {
            index.join(leader.getUUID(), member);
            index.leaveVoluntarily(member);
            clock[0] += 2 * PackAbsence.DAY_MILLIS;
            helper.assertTrue(!index.killsInPack(member, leader.getUUID(), 10), "a leave from two days ago still counts");
            helper.assertTrue(index.hasLeft(member), "asking removed the entry: a query must not change anything");
            PackAbsence.check(server, clock[0]);
            helper.assertTrue(!index.hasLeft(member), "the tick did not prune an old leave");
        } finally {
            PackIndex.overrideClock(null);
            clean(helper, leader);
        }
        helper.succeed();
    }
}
