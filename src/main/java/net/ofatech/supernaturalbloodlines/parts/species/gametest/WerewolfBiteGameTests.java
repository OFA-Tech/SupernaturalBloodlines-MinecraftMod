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
            helper.assertTrue(PackCommand.invite(alpha, human) != null, "an Alpha invited a human");
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
            helper.assertTrue(PackCommand.emissary(beta, witch) != null, "a Beta named an emissary");
            helper.assertTrue(PackCommand.emissary(alpha, human) != null, "a non-witch became the emissary");
            for (Species s : List.of(Species.SIPHONER, Species.HERETIC, Species.TRIBRID)) {
                BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(s).build());
                helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "a " + s + " was refused as emissary");
            }
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "the witch was refused");
            helper.assertTrue(witch.getUUID().equals(index.emissaryOf(alpha.getUUID()).orElse(null)), "the emissary was not stored");
            // a witch serves one Alpha: a second Alpha cannot take them until the first lets go
            ServerPlayer rival = wolf(helper, Rank.ALPHA, null);
            try {
                helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "the first Alpha lost their emissary");
                helper.assertTrue(PackCommand.emissary(rival, witch) != null, "a second Alpha took another Alpha's emissary");
                helper.assertTrue(alpha.getUUID().equals(index.emissaryAlpha(witch.getUUID()).orElse(null)), "the emissary changed hands");
                helper.assertTrue(PackCommand.emissary(alpha, null) == null && index.emissaryOf(alpha.getUUID()).isEmpty(), "the emissary was not released");
                helper.assertTrue(PackCommand.emissary(rival, witch) == null, "a free witch was refused");
                helper.assertTrue(PackCommand.emissary(rival, null) == null, "the rival could not let go");
                helper.assertTrue(PackCommand.emissary(alpha, witch) == null, "the first Alpha could not take them back");
            } finally {
                index.setEmissary(rival.getUUID(), null);
                clean(rival);
            }            helper.assertTrue(PackCommand.emissary(alpha, null) == null && index.emissaryOf(alpha.getUUID()).isEmpty(), "the emissary was not cleared");

            helper.assertTrue(PackCommand.anchor(human, alpha) != null, "a human set an anchor");
            helper.assertTrue(PackCommand.anchor(beta, beta) != null, "someone anchored themselves");
            helper.assertTrue(PackCommand.anchor(beta, human) == null, "the anchor was refused");
            helper.assertTrue(human.getUUID().equals(index.anchorOf(beta.getUUID()).orElse(null)), "the anchor was not stored");
            helper.assertTrue(PackCommand.anchor(beta, null) == null && index.anchorOf(beta.getUUID()).isEmpty(), "the anchor was not cleared");
        } finally {
            clean(alpha, beta, witch, human);
        }
        helper.succeed();
    }
}
