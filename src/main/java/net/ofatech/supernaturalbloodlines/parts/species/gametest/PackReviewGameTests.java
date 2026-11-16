package net.ofatech.supernaturalbloodlines.parts.species.gametest;

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
            helper.assertTrue(join(leader, second) && join(rival, taken), "the packs could not be built");
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
    public static void anAlphaWhoRunsWithAPackDoesNotStealFromOutsideIt(GameTestHelper helper) {
        ServerPlayer leader = alpha(helper, 6, 1.5);
        ServerPlayer second = alpha(helper, 3, 2.5);
        ServerPlayer outsider = alpha(helper, 5, 3.5);
        online(leader, second, outsider);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(leader, second), "the co-Alpha could not join");
            kill(second, outsider);
            helper.assertTrue(state(outsider).rank() == Rank.ALPHA && !state(second).stoleSpark(), "the spark moved: " + state(outsider) + " / " + state(second));
            helper.assertTrue(index.alphaOf(second.getUUID()).isPresent(), "the co-Alpha left the pack");
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
}
