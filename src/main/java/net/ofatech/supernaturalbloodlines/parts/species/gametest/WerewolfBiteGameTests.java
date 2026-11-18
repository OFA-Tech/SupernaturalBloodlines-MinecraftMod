package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaBite;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;

/** Plan 9, task 2: Alpha-only turning, the pending bite, the pack index and {@code /pack}. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class WerewolfBiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Holder<MobEffect> LUPUS =
            BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("werewolves:lupus_sanguinem")).orElseThrow();

    private WerewolfBiteGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, UUID packAlpha) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).packAlpha(packAlpha).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(WerewolfFaction.join(p), "the test wolf could not join the werewolves");
        return p;
    }

    private static void clean(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.removeEffect(LUPUS);
            p.removeData(SpeciesRegistries.PENDING_ALPHA);
            p.removeData(SpeciesRegistries.PENDING_WILD);
        }
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

    private static void bite(ServerPlayer attacker, ServerPlayer victim) {
        hit(victim, attacker.damageSources().playerAttack(attacker));
    }

    private static boolean werewolf(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION;
    }

    private static WolfState wolfOf(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    private static Map<UUID, ServerPlayer> lookup(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        return map;
    }

    // ---- who may turn someone ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void betaBiteDoesNotInfect(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, UUID.randomUUID());
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        ServerPlayer plain = wolf(helper, Rank.NONE, null);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer commanded = TestPlayers.createDuelist(helper);
        try {
            for (ServerPlayer biter : List.of(beta, omega, plain)) {
                bite(biter, victim);
                helper.assertFalse(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "a " + wolfOf(biter).rank() + " bite infected");
                helper.assertFalse(victim.hasEffect(LUPUS), "the sanguinem effect is on the victim of a " + wolfOf(biter).rank());
            }
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "a pending Alpha was recorded");
            helper.assertFalse(victim.getData(SpeciesRegistries.PENDING_WILD), "a wild bite was recorded");

            // a one-argument addEffect with no biter at all (a potion, a command) is allowed and counts as wild
            helper.assertTrue(commanded.addEffect(new MobEffectInstance(LUPUS, 600)), "an unattributed effect was refused");
            helper.assertTrue(commanded.getData(SpeciesRegistries.PENDING_WILD), "an unattributed effect is not wild");
        } finally {
            clean(beta, omega, plain, victim, commanded);
        }
        helper.succeed();
    }

    /** 13d M8: only_alphas_turn=false gives back Werewolves' own rule: a Beta's bite infects (and records no pack). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void betaBiteInfectsWhenOnlyAlphasTurnIsOff(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, UUID.randomUUID());
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideOnlyAlphasTurn(false);
        try {
            bite(beta, victim);
            helper.assertTrue(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "a Beta bite was refused with only_alphas_turn off");
            helper.assertTrue(victim.hasEffect(LUPUS), "the sanguinem effect is not on the victim");
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "a pending Alpha was recorded for a Beta");
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideOnlyAlphasTurn(null);
            clean(beta, victim);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaBiteInfectsAndJoinsPack(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer trueAlpha = wolf(helper, Rank.TRUE_ALPHA, null);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer second = TestPlayers.createDuelist(helper);
        SpeciesConfig.overrideAlphaBiteDeathChance(0.0);
        AlphaBite.useRoll(() -> 0.5);
        try {
            bite(alpha, victim);
            helper.assertTrue(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "an Alpha bite was refused");
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(alpha.getUUID()), "the Alpha was not remembered");
            helper.assertTrue(wolfOf(victim).lineage() == Lineage.NONE && !werewolf(victim), "the victim turned before the effect ran out");

            // the effect runs out: Werewolves joins the faction
            helper.assertTrue(WerewolfFaction.join(victim), "the victim could not join the werewolves");
            WolfState w = wolfOf(victim);
            helper.assertTrue(w.lineage() == Lineage.BITTEN && w.rank() == Rank.BETA && alpha.getUUID().equals(w.packAlpha()), "wolf layer is " + w);
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(alpha.getUUID().equals(index.alphaOf(victim.getUUID()).orElse(null)), "the index has no pack for the bitten");
            helper.assertTrue(index.betasOf(alpha.getUUID()).contains(victim.getUUID()), "the Alpha's pack lacks the bitten");
            helper.assertTrue(alpha.getUUID().equals(BloodlineAccess.get(victim).sire()), "the Alpha is not the sire: " + BloodlineAccess.get(victim).sire());
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "the pending Alpha was not cleared");
            helper.assertTrue(victim.isAlive(), "the victim died");

            // a True Alpha turns people too
            bite(trueAlpha, second);
            helper.assertTrue(second.addEffect(new MobEffectInstance(LUPUS, 600)), "a True Alpha bite was refused");
            helper.assertTrue(second.getData(SpeciesRegistries.PENDING_ALPHA).equals(trueAlpha.getUUID()), "the True Alpha was not remembered");
        } finally {
            AlphaBite.resetRoll();
            SpeciesConfig.overrideAlphaBiteDeathChance(null);
            clean(alpha, trueAlpha, victim, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaBiteCanKill(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        SpeciesConfig.overrideAlphaBiteDeathChance(1.0);
        try {
            bite(alpha, victim);
            helper.assertTrue(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "an Alpha bite was refused");
            WerewolfFaction.join(victim);
            // the rejection waits until Vampirism's join has returned
            helper.assertTrue(victim.isAlive() && werewolf(victim), "the rejection ran inside the faction event");
        } catch (RuntimeException e) {
            SpeciesConfig.overrideAlphaBiteDeathChance(null);
            clean(alpha, victim);
            throw e;
        }
        helper.runAfterDelay(2, () -> {
            try {
                helper.assertFalse(victim.isAlive(), "the victim survived a chance of 1");
                DamageSource last = victim.getLastDamageSource();
                helper.assertTrue(last != null && last.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                        ResourceLocation.fromNamespaceAndPath(NS, "rejected_bite"))), "died of " + last);
                helper.assertFalse(werewolf(victim), "the dead stayed in the werewolf faction");
                helper.assertTrue(wolfOf(victim).lineage() == Lineage.NONE && wolfOf(victim).packAlpha() == null, "wolf layer is " + wolfOf(victim));
                helper.assertTrue(PackIndex.get(helper.getLevel()).alphaOf(victim.getUUID()).isEmpty(), "the dead is in a pack");
                helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "the pending Alpha was not cleared");
            } finally {
                SpeciesConfig.overrideAlphaBiteDeathChance(null);
                clean(alpha, victim);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wildBiteMakesOmega(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer commanded = TestPlayers.createDuelist(helper);
        Mob beast = (Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("werewolves:werewolf_beast")), 1, 1, 1);
        // the wild bite is never a death roll
        SpeciesConfig.overrideAlphaBiteDeathChance(1.0);
        try {
            hit(victim, victim.damageSources().mobAttack(beast));
            helper.assertTrue(victim.addEffect(new MobEffectInstance(LUPUS, 600)), "a wild bite was refused");
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_WILD), "the wild bite was not remembered");
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "an Alpha was recorded for a wild bite");

            helper.assertTrue(WerewolfFaction.join(victim), "the victim could not join the werewolves");
            WolfState w = wolfOf(victim);
            helper.assertTrue(victim.isAlive(), "the victim died");
            helper.assertTrue(w.lineage() == Lineage.BITTEN && w.rank() == Rank.OMEGA && w.packAlpha() == null, "wolf layer is " + w);
            helper.assertTrue(PackIndex.get(helper.getLevel()).alphaOf(victim.getUUID()).isEmpty(), "an Omega is in a pack");
            helper.assertTrue(BloodlineAccess.get(victim).sire() == null, "a wild bite recorded a sire");
            helper.assertFalse(victim.getData(SpeciesRegistries.PENDING_WILD), "the wild bite was not cleared");

            // a join with nothing pending (a rite, a command) is not a bite
            WerewolfFaction.join(commanded);
            helper.assertTrue(wolfOf(commanded).lineage() == Lineage.NONE, "a join with no bite gave a lineage");
        } finally {
            SpeciesConfig.overrideAlphaBiteDeathChance(null);
            beast.discard();
            clean(victim, commanded);
        }
        helper.succeed();
    }

    // ---- packs ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void inviteAcceptLeave(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        lookup(alpha, omega, human);
        try {
            helper.assertTrue(PackCommand.invite(omega, alpha) != null, "an Omega invited");
            helper.assertTrue(PackCommand.invite(alpha, human) == null, "an Alpha could not invite a human as a Packmate (plan 14)");
            helper.assertTrue(PackCommand.invite(alpha, alpha) != null, "an Alpha invited themselves");
            helper.assertTrue(PackCommand.invite(alpha, omega) == null, "the invitation was refused");
            String token = PackCommand.requests().tokenOf(alpha.getUUID(), omega.getUUID()).orElse(null);
            helper.assertTrue(token != null, "no token was made");

            helper.assertTrue(PackCommand.accept(human, token) != null, "someone else used the token");
            helper.assertTrue(PackCommand.accept(omega, "nonsense") != null, "a made-up token worked");
            helper.assertTrue(wolfOf(omega).rank() == Rank.OMEGA, "the Omega joined without accepting");

            helper.assertTrue(PackCommand.accept(omega, token) == null, "the accept was refused");
            WolfState w = wolfOf(omega);
            helper.assertTrue(w.rank() == Rank.BETA && alpha.getUUID().equals(w.packAlpha()), "wolf layer is " + w);
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(alpha.getUUID().equals(index.alphaOf(omega.getUUID()).orElse(null)), "the index has no pack");
            helper.assertTrue(PackCommand.accept(omega, token) != null, "a token worked twice");

            helper.assertTrue(PackCommand.leave(alpha) != null, "an Alpha left their pack");
            helper.assertTrue(PackCommand.leave(omega) == null, "leaving was refused");
            w = wolfOf(omega);
            helper.assertTrue(w.rank() == Rank.OMEGA && w.packAlpha() == null, "wolf layer is " + w);
            helper.assertTrue(index.alphaOf(omega.getUUID()).isEmpty() && index.betasOf(alpha.getUUID()).isEmpty(), "the index still has the pack");
            helper.assertTrue(PackCommand.leave(omega) != null, "leaving twice worked");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, omega, human);
        }
        helper.succeed();
    }

    /**
     * P2: the hub's Accept button ran "pack accept" with no token, which was an incomplete command, so nobody ever joined. The command now
     * answers the newest open invitation, and the hub's snapshot lists that invitation with its token for the Requests card.
     */
    private static boolean joinPack(ServerPlayer leader, ServerPlayer member) {
        return PackCommand.invite(leader, member) == null && PackCommand.accept(member, PackCommand.requests().tokenOf(leader.getUUID(), member.getUUID()).orElseThrow()) == null;
    }

    /** Hub IA 3.7: an NPC in the observer's own line shows its name and species and nothing else, and is marked as no player; a stranger is not. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anNpcInYourLineShowsOnlyItsNameAndSpecies(GameTestHelper helper) {
        ServerPlayer me = wolf(helper, Rank.ALPHA, null);
        ServerPlayer stranger = TestPlayers.createDuelist(helper);
        lookup(me, stranger);
        java.util.UUID thrall = java.util.UUID.randomUUID();
        var sirelines = net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex.get(helper.getLevel());
        try {
            sirelines.add(me.getUUID(), thrall, "vampirism:advanced_vampire", "Thrall");
            var profile = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(me, thrall);
            helper.assertTrue(profile.npc() && profile.facts().keySet().equals(java.util.EnumSet.of(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.NAME,
                    net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES)), "an NPC of the line was told " + profile.facts().keySet() + " npc=" + profile.npc());
            helper.assertTrue(profile.facts().get(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES).equals(List.of("advanced_vampire")), "the species is " + profile.facts());
            var other = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(stranger, thrall);
            helper.assertTrue(!other.npc() && other.facts().isEmpty(), "a stranger was told about the NPC: " + other.facts());
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(me, stranger.getUUID()).npc(), "a player was marked as an NPC");
        } finally {
            sirelines.remove(thrall);
            SireCall.overridePlayerLookup(null);
        }
        helper.succeed();
    }

    /** An invitation whose inviter is only offline stays open (nothing taken), and answers once they are back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anInvitationFromAnOfflineInviterIsKept(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member);
        try {
            helper.assertTrue(PackCommand.invite(alpha, member) == null, "the invitation was not made");
            String token = PackCommand.requests().tokenOf(alpha.getUUID(), member.getUUID()).orElseThrow();
            // the inviter logs out through the real event, and the lookup stops finding them
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(alpha));
            lookup(member);
            helper.assertTrue(PackCommand.requests().tokenOf(alpha.getUUID(), member.getUUID()).isPresent(), "the inviter's logout voided the invitation they sent");
            var refused = PackCommand.accept(member, token);
            helper.assertTrue(refused != null && refused.getString().contains("offline"), "an offline inviter was answered " + refused);
            helper.assertTrue(PackCommand.requests().tokenOf(alpha.getUUID(), member.getUUID()).isPresent(), "the token was used up by an answer that did nothing");
            lookup(alpha, member);
            helper.assertTrue(PackCommand.accept(member, token) == null, "the kept invitation could not be accepted when the inviter was back");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, member);
        }
        helper.succeed();
    }

    /** An invitation is void when the INVITEE logs out (the real event): the token is gone, and so is an alliance request sent to them. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anInviteesLogoutVoidsWhatWasSentToThem(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member);
        try {
            helper.assertTrue(PackCommand.invite(alpha, member) == null, "the invitation was not made");
            helper.assertTrue(PackCommand.requests().tokenOf(alpha.getUUID(), member.getUUID()).isPresent(), "no token to start with");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(member));
            helper.assertTrue(PackCommand.requests().tokenOf(alpha.getUUID(), member.getUUID()).isEmpty(), "the invitee's logout left the invitation open");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, member);
        }
        helper.succeed();
    }

    /** A packmate sees the line root only as far as that player has discovered it: when the index moves the root to another Original, the new one stays hidden. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPacksProfileNeverShowsARootThePlayerHasNotDiscovered(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member);
        var line = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.LINE;
        java.util.UUID first = net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.values()[0].uuid();
        java.util.UUID second = net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.values()[1].uuid();
        var index = net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex.get(helper.getLevel().getServer());
        net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideOriginalVisibility(false);
        try {
            helper.assertTrue(joinPack(alpha, member), "the pack could not be built");
            index.setRoot(member.getUUID(), first, true);
            // the member discovers the root of their own line: the pack learns it
            BloodlineAccess.set(member, BloodlineAccess.get(member).toBuilder().knownLine(first).build());
            String firstName = net.ofatech.supernaturalbloodlines.parts.species.line.Lines.rootName(helper.getLevel().getServer(), first).getString();
            String secondName = net.ofatech.supernaturalbloodlines.parts.species.line.Lines.rootName(helper.getLevel().getServer(), second).getString();
            var shown = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts().get(line);
            helper.assertTrue(List.of(firstName).equals(shown), "the discovered root was shown as " + shown);
            // the root moves on (the Original fell, the line was reassigned): what the member has not discovered must not reach the pack
            index.setRoot(member.getUUID(), second, true);
            var after = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts().get(line);
            helper.assertTrue(after == null || !after.contains(secondName), "an undiscovered root was shown to the pack: " + after);
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideOriginalVisibility(null);
            index.remove(member.getUUID());
            SireCall.overridePlayerLookup(null);
            clean(alpha, member);
        }
        helper.succeed();
    }

    /** A packmate's profile of a pack-mate carries the days in the pack (from the pack index's join time) and the Alpha rank's bonus (the numbers the rank really sets). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackmatesProfileCarriesDaysInThePackAndTheBonus(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member);
        long[] now = {1_000_000_000_000L};
        net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.overrideClock(() -> now[0]);
        try {
            helper.assertTrue(joinPack(alpha, member), "the pack could not be built");
            now[0] += 3 * 86_400_000L + 1000;
            var ofAlpha = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(member, alpha.getUUID()).facts();
            var ofMember = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts();
            var days = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.PACK_DAYS;
            var bonus = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.BONUS;
            helper.assertTrue(List.of("3").equals(ofMember.get(days)), "the member's days in the pack were " + ofMember.get(days));
            helper.assertTrue(List.of("3").equals(ofAlpha.get(days)), "the pack's age was " + ofAlpha.get(days));
            // literals and the modifiers the rank really sets, not the function under test
            var attack = alpha.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifier(net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects.MODIFIER_ID);
            var health = alpha.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getModifier(net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects.MODIFIER_ID);
            helper.assertTrue(attack != null && health != null && attack.amount() == 0.2 && health.amount() == 4.0, "the rank sets " + attack + " / " + health);
            helper.assertTrue(List.of("+20% attack, +4 max health").equals(ofAlpha.get(bonus)), "the Alpha's bonus was " + ofAlpha.get(bonus));
            helper.assertTrue(!ofMember.containsKey(bonus), "an Omega was shown a bonus: " + ofMember.get(bonus));
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.overrideClock(System::currentTimeMillis);
            SireCall.overridePlayerLookup(null);
            clean(alpha, member);
        }
        helper.succeed();
    }

    /** An NPC Original the observer has not discovered answers like a stranger: no facts and no NPC flag (the flag alone would name the Original). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUndiscoveredNpcOriginalAnswersLikeAStranger(GameTestHelper helper) {
        ServerPlayer me = wolf(helper, Rank.ALPHA, null);
        lookup(me);
        java.util.UUID original = net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.values()[0].uuid();
        var sirelines = net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex.get(helper.getLevel());
        try {
            sirelines.add(original, me.getUUID(), "minecraft:player", "Me");
            var before = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(me, original);
            helper.assertTrue(!before.npc() && before.facts().isEmpty(), "an undiscovered Original was told: npc=" + before.npc() + " " + before.facts());
        } finally {
            sirelines.remove(me.getUUID());
            SireCall.overridePlayerLookup(null);
            clean(me);
        }
        helper.succeed();
    }

    /** The sire fact never names an NPC Original or a creature (the hub's own Bloodline page shows those as "?"), and a pack-mate's genes are only the ones that player has discovered. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aProfileNeverNamesACreatureSireOrUndiscoveredGenes(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member);
        var sirelines = net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex.get(helper.getLevel());
        java.util.UUID original = net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal.values()[0].uuid();
        var sire = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SIRE;
        var genes = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.GENES;
        try {
            helper.assertTrue(joinPack(alpha, member), "the pack could not be built");
            sirelines.add(original, member.getUUID(), "minecraft:player", "Member");
            var ofMember = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts();
            helper.assertTrue(!ofMember.containsKey(sire), "an NPC Original sire was named: " + ofMember.get(sire));
            // the member's werewolf gene is rolled (wolf() sets it) but undiscovered: a packmate learns it only through a discovery, and then only what was discovered
            net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(member, net.ofatech.supernaturalbloodlines.core.BloodlineAccess.get(member).toBuilder()
                    .revealedGenes(java.util.Set.of(net.ofatech.supernaturalbloodlines.core.bloodline.Genes.WEREWOLF)).build());
            var shown = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts().get(genes);
            helper.assertTrue(shown != null && shown.contains("doppelganger:unknown") && shown.stream().anyMatch(g -> g.startsWith("werewolf:") && !g.endsWith(":unknown")), "the genes shown were " + shown);
        } finally {
            sirelines.remove(member.getUUID());
            SireCall.overridePlayerLookup(null);
            clean(alpha, member);
        }
        helper.succeed();
    }

    /**
     * Hub IA 3.7: what an observer may learn of a person comes from the server's own indexes. A stranger gets no fields at all, a pack-mate
     * the pack's facts, and a secret (genes) only once somebody has discovered it; the pack's pool teaches a newcomer, and leaving keeps it.
     */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNonMemberCannotLearnAMembersSpecies(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer member = wolf(helper, Rank.OMEGA, null);
        ServerPlayer stranger = TestPlayers.createDuelist(helper);
        ServerPlayer newcomer = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, member, stranger, newcomer);
        var species = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES;
        var genes = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.GENES;
        var pack = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.PACK;
        try {
            helper.assertTrue(joinPack(alpha, member), "the pack could not be built");
            var forStranger = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(stranger, member.getUUID());
            helper.assertTrue(forStranger.facts().isEmpty() && !forStranger.opView(), "a stranger was told " + forStranger.facts());
            var forAlpha = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID());
            helper.assertTrue(forAlpha.facts().containsKey(species) && forAlpha.facts().containsKey(pack), "a pack-mate was told only " + forAlpha.facts().keySet());
            helper.assertTrue(!forAlpha.facts().containsKey(genes), "a gene nobody discovered was shown to the pack");

            // the member discovers their own gene: the pack knows it at once, the stranger still knows nothing
            BloodlineAccess.set(member, BloodlineAccess.get(member).toBuilder().revealedGenes(java.util.Set.of(net.ofatech.supernaturalbloodlines.core.bloodline.Genes.WEREWOLF)).build());
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(alpha, member.getUUID()).facts().containsKey(genes), "the pack did not learn a discovery");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(stranger, member.getUUID()).facts().isEmpty(), "a discovery reached a stranger");

            // a newcomer receives the pool, and leaving the pack keeps what they know but not the live facts
            helper.assertTrue(joinPack(alpha, newcomer), "the newcomer could not join");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(newcomer, member.getUUID()).facts().containsKey(genes), "the newcomer was not given the pool");
            helper.assertTrue(PackCommand.leave(newcomer) == null, "the newcomer could not leave");
            var after = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(newcomer, member.getUUID());
            helper.assertTrue(after.facts().containsKey(genes) && !after.facts().containsKey(pack) && !after.facts().containsKey(species), "after leaving they know " + after.facts().keySet());
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, member, stranger, newcomer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hubAcceptRunsTheTokenCommand(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, omega);
        try {
            helper.assertTrue(PackCommand.invite(alpha, omega) == null, "the invitation was refused");
            String token = PackCommand.requests().tokenOf(alpha.getUUID(), omega.getUUID()).orElseThrow();
            var pending = net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(omega);
            helper.assertTrue(pending.size() == 1 && pending.get(0).kind() == net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.Kind.PACK_INVITE
                    && pending.get(0).token().equals(token) && pending.get(0).ticksLeft() == net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests.TTL_TICKS,
                    "the Requests card data is " + pending);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(alpha).isEmpty(), "the inviter was shown the invitation");

            helper.assertTrue(dispatch(omega, "pack accept") == 1, "pack accept with no token failed");
            w(helper, omega, alpha);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests.of(omega).isEmpty(), "the answered invitation is still listed");
            helper.assertTrue(dispatch(omega, "pack accept") == 0, "a second accept with nothing open worked");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, omega);
        }
        helper.succeed();
    }

    /** Runs a typed command as {@code p} with permission 0; an unknown or incomplete command is an error, not a quiet 0. */
    private static int dispatch(ServerPlayer p, String command) {
        net.minecraft.commands.CommandSource sink = new net.minecraft.commands.CommandSource() {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component c) {
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        var stack = new net.minecraft.commands.CommandSourceStack(sink, p.position(), p.getRotationVector(), (net.minecraft.server.level.ServerLevel) p.level(), 0,
                p.getGameProfile().getName(), p.getDisplayName(), p.server, p);
        try {
            return p.server.getCommands().getDispatcher().execute(command, stack);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError("\"" + command + "\" is not a command: " + e.getMessage());
        }
    }

    /** The member actions of the profile page name a person by UUID: the commands take one where they take a name. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void memberActionsTakeAUuid(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, omega);
        try {
            helper.assertTrue(PackCommand.invite(alpha, omega) == null && PackCommand.accept(omega, PackCommand.requests().tokenOf(alpha.getUUID(), omega.getUUID()).orElseThrow()) == null,
                    "the control invitation failed");
            helper.assertTrue(dispatch(alpha, "pack kick " + omega.getUUID()) == 1, "kicking by UUID failed");
            helper.assertTrue(PackIndex.get(helper.getLevel()).alphaOf(omega.getUUID()).isEmpty(), "the kicked member is still in the pack");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, omega);
        }
        helper.succeed();
    }

    private static void w(GameTestHelper helper, ServerPlayer member, ServerPlayer alpha) {
        helper.assertTrue(alpha.getUUID().equals(PackIndex.get(helper.getLevel()).alphaOf(member.getUUID()).orElse(null)), "the accepted invitation did not put them in the pack");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void secondInviteReplacesTheFirst(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        lookup(alpha, omega);
        try {
            helper.assertTrue(PackCommand.invite(alpha, omega) == null, "the first invitation was refused");
            String first = PackCommand.requests().tokenOf(alpha.getUUID(), omega.getUUID()).orElseThrow();
            helper.assertTrue(PackCommand.invite(alpha, omega) == null, "the second invitation was refused");
            String second = PackCommand.requests().tokenOf(alpha.getUUID(), omega.getUUID()).orElseThrow();
            helper.assertTrue(!first.equals(second), "the same token was reused");
            helper.assertTrue(PackCommand.accept(omega, first) != null, "the first invitation still worked after a second one");
            helper.assertTrue(PackCommand.accept(omega, second) == null, "the second invitation was refused");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, omega);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void secondBiteDoesNotSwitchPack(GameTestHelper helper) {
        ServerPlayer first = wolf(helper, Rank.ALPHA, null);
        ServerPlayer second = wolf(helper, Rank.ALPHA, null);
        ServerPlayer beta = wolf(helper, Rank.BETA, first.getUUID());
        ServerPlayer omega = wolf(helper, Rank.OMEGA, null);
        lookup(first, second, beta, omega);
        SpeciesConfig.overrideAlphaBiteDeathChance(0.0);
        AlphaBite.useRoll(() -> 0.5);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(first.getUUID().equals(index.alphaOf(beta.getUUID()).orElse(null)), "the listener did not index the Beta");

            // the second Alpha bites; nothing may be pending for someone who already is a werewolf
            bite(second, beta);
            beta.addEffect(new MobEffectInstance(LUPUS, 600));
            helper.assertTrue(beta.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "a pending Alpha was recorded for a werewolf");
            // and even a stale pending Alpha does nothing when the Beta joins the werewolves again
            beta.setData(SpeciesRegistries.PENDING_ALPHA, second.getUUID());
            VampirismAPI.factionPlayerHandler(beta).leaveFaction(false);
            WerewolfFaction.join(beta);
            WolfState w = wolfOf(beta);
            helper.assertTrue(w.rank() == Rank.BETA && first.getUUID().equals(w.packAlpha()), "the pack switched: " + w);
            helper.assertTrue(first.getUUID().equals(index.alphaOf(beta.getUUID()).orElse(null)), "the index switched");
            helper.assertTrue(beta.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID), "the stale pending Alpha stayed");

            // an invitation from the second Alpha cannot be accepted until /pack leave
            helper.assertTrue(PackCommand.invite(second, beta) != null, "an invitation to a Beta with a pack was made");
            helper.assertTrue(PackCommand.invite(second, omega) == null, "the invitation to the Omega was refused");
            String stale = PackCommand.requests().tokenOf(second.getUUID(), omega.getUUID()).orElseThrow();
            helper.assertTrue(PackCommand.invite(first, omega) == null, "the first invitation was refused");
            String mine = PackCommand.requests().tokenOf(first.getUUID(), omega.getUUID()).orElseThrow();
            helper.assertTrue(PackCommand.accept(omega, mine) == null, "the Omega could not join the first pack");
            helper.assertTrue(PackCommand.accept(omega, stale) != null, "a Beta accepted a second pack");
            helper.assertTrue(first.getUUID().equals(wolfOf(omega).packAlpha()), "the Beta switched packs: " + wolfOf(omega));
            helper.assertTrue(first.getUUID().equals(index.alphaOf(omega.getUUID()).orElse(null)), "the index switched packs");
        } finally {
            AlphaBite.resetRoll();
            SpeciesConfig.overrideAlphaBiteDeathChance(null);
            SireCall.overridePlayerLookup(null);
            clean(first, second, beta, omega);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void emissaryAndAnchorAreStored(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer beta = wolf(helper, Rank.BETA, alpha.getUUID());
        ServerPlayer witch = TestPlayers.createDuelist(helper);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            ServerPlayer rival = wolf(helper, Rank.ALPHA, null);
            lookup(alpha, beta, witch, human, rival);
            helper.assertTrue(PackCommand.emissary(beta, witch) != null, "a Beta named an emissary");
            // only someone who is in the pack already can be named: invite them first (they join as a Packmate)
            helper.assertTrue(PackCommand.emissary(alpha, witch) != null && index.emissaryOf(alpha.getUUID()).isEmpty(), "a player outside the pack was named");
            helper.assertTrue(!index.inPack(witch.getUUID()), "naming an Emissary pulled them into the pack");
            helper.assertTrue(PackCommand.invite(alpha, witch) == null
                    && PackCommand.accept(witch, PackCommand.requests().tokenOf(alpha.getUUID(), witch.getUUID()).orElseThrow()) == null, "the witch could not join");
            helper.assertTrue(PackCommand.emissary(alpha, human) != null, "a non-witch became the emissary");
            for (Species s : List.of(Species.SIPHONER, Species.HERETIC, Species.TRIBRID)) {
                BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(s).build());
                helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "a " + s + " was refused as emissary");
            }
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "the witch was refused");
            helper.assertTrue(witch.getUUID().equals(index.emissaryOf(alpha.getUUID()).orElse(null)), "the emissary was not stored");
            try {
                // a witch serves one Alpha: a second Alpha cannot name them (they are no member of that pack)
                helper.assertTrue(PackCommand.emissary(rival, witch) != null, "a second Alpha took another Alpha's emissary");
                helper.assertTrue(alpha.getUUID().equals(index.emissaryAlpha(witch.getUUID()).orElse(null)), "the emissary changed hands");
                helper.assertTrue(PackCommand.emissary(alpha, null) == null && index.emissaryOf(alpha.getUUID()).isEmpty(), "the emissary was not released");

                // an Emissary who leaves the pack is nobody's Emissary, and cannot be named again until they are back in it
                helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "the first Alpha could not name them again");
                helper.assertTrue(PackCommand.leave(witch) == null, "the witch could not leave the pack");
                helper.assertTrue(index.emissaryOf(alpha.getUUID()).isEmpty(), "an Emissary who left the pack is still named");
                helper.assertTrue(PackCommand.emissary(alpha, witch) != null, "named an Emissary who is not in the pack any more");
                helper.assertTrue(PackCommand.invite(rival, witch) == null
                        && PackCommand.accept(witch, PackCommand.requests().tokenOf(rival.getUUID(), witch.getUUID()).orElseThrow()) == null, "the witch could not join the rival's pack");
                helper.assertTrue(PackCommand.emissary(rival, witch) == null, "a free witch was refused");
                helper.assertTrue(PackCommand.emissary(rival, null) == null, "the rival could not let go");
                helper.assertTrue(PackCommand.leave(witch) == null, "the witch could not leave the rival's pack");
            } finally {
                index.setEmissary(rival.getUUID(), null);
                clean(rival);
            }
            helper.assertTrue(PackCommand.emissary(alpha, null) == null && index.emissaryOf(alpha.getUUID()).isEmpty(), "the emissary was not cleared");

            helper.assertTrue(PackCommand.anchor(human, alpha) != null, "a human set an anchor");
            helper.assertTrue(PackCommand.anchor(beta, beta) != null, "someone anchored themselves");
            helper.assertTrue(PackCommand.anchor(beta, human) == null, "the anchor was refused");
            helper.assertTrue(human.getUUID().equals(index.anchorOf(beta.getUUID()).orElse(null)), "the anchor was not stored");
            helper.assertTrue(PackCommand.anchor(beta, null) == null && index.anchorOf(beta.getUUID()).isEmpty(), "the anchor was not cleared");
        } finally {
            SireCall.overridePlayerLookup(null);
            clean(alpha, beta, witch, human);
        }
        helper.succeed();
    }
}
