package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload;
import net.ofatech.supernaturalbloodlines.core.network.EyeColorPayload.Color;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaBite;
import net.ofatech.supernaturalbloodlines.parts.species.pack.DefenderKills;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;

/** Plan 9, task 3: the spark (stolen, inherited, given up), the True Alpha, rank modifiers and eye colours. Werewolves only. */
@PrefixGameTestTemplate(false)
public final class WerewolfSparkGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long NOW = 2_000_000L;

    private WerewolfSparkGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, UUID packAlpha, int level) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).packAlpha(packAlpha)
                .betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        return p;
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, UUID packAlpha) {
        return wolf(helper, rank, packAlpha, 1);
    }

    private static void clean(ServerPlayer... players) {
        SireCall.overridePlayerLookup(null);
        for (ServerPlayer p : players) {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.removeData(SpeciesRegistries.PENDING_ALPHA);
            p.removeData(SpeciesRegistries.PENDING_WILD);
        }
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
    }

    private static WolfState state(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    private static void kill(ServerPlayer killer, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, killer.damageSources().playerAttack(killer)));
    }

    private static void killedBy(DamageSource source, LivingEntity victim) {
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(victim, source));
    }

    private static boolean isIn(PackIndex index, UUID alpha, ServerPlayer beta) {
        return alpha.equals(index.alphaOf(beta.getUUID()).orElse(null)) && index.betasOf(alpha).contains(beta.getUUID());
    }

    // ---- stolen ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killingAlphaStealsSpark(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer beta = wolf(helper, Rank.BETA, alpha.getUUID());
        ServerPlayer offlineBeta = wolf(helper, Rank.BETA, alpha.getUUID());
        ServerPlayer thief = wolf(helper, Rank.OMEGA, null);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        ServerPlayer other = wolf(helper, Rank.ALPHA, null);
        online(alpha, beta, thief, human, other);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            // ordinary deaths keep rank and spark: a mob, a human, no killer at all
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            killedBy(zombie.damageSources().mobAttack(zombie), alpha);
            kill(human, alpha);
            killedBy(alpha.damageSources().fall(), alpha);
            kill(alpha, alpha);
            helper.assertTrue(state(alpha).rank() == Rank.ALPHA && index.betasOf(alpha.getUUID()).size() == 2, "an ordinary death cost the spark: " + state(alpha));

            kill(thief, alpha);
            WolfState t = state(thief);
            helper.assertTrue(t.rank() == Rank.ALPHA && t.stoleSpark() && t.packAlpha() == null, "the thief is " + t);
            WolfState old = state(alpha);
            helper.assertTrue(old.rank() == Rank.OMEGA && old.packAlpha() == null, "the old Alpha is " + old);
            helper.assertTrue(state(beta).packAlpha().equals(thief.getUUID()), "the online Beta was not moved: " + state(beta));
            helper.assertTrue(isIn(index, thief.getUUID(), beta) && isIn(index, thief.getUUID(), offlineBeta), "the index did not move the pack");
            helper.assertTrue(index.betasOf(alpha.getUUID()).isEmpty() && index.alphaOf(alpha.getUUID()).isEmpty(), "the old Alpha still has a pack");
            helper.assertTrue(index.isFormerAlpha(alpha.getUUID()) && !index.isFormerAlpha(thief.getUUID()), "no tombstone for the old Alpha");

            // the Beta that was offline is moved when it logs in
            PackIndex.login(offlineBeta);
            helper.assertTrue(thief.getUUID().equals(state(offlineBeta).packAlpha()) && isIn(index, thief.getUUID(), offlineBeta),
                    "the offline Beta went back to the old Alpha: " + state(offlineBeta));

            // an Alpha who already leads a pack keeps it and adopts the victim's Betas
            kill(other, thief);
            helper.assertTrue(state(other).rank() == Rank.ALPHA && state(other).stoleSpark(), "the second Alpha is " + state(other));
            helper.assertTrue(isIn(index, other.getUUID(), beta) && isIn(index, other.getUUID(), offlineBeta), "the merged pack is missing a Beta");
            helper.assertTrue(state(thief).rank() == Rank.OMEGA, "the thief kept the spark: " + state(thief));
        } finally {
            clean(alpha, beta, offlineBeta, thief, human, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void betaKillingOwnAlphaTakesPack(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.TRUE_ALPHA, null);
        ServerPlayer killer = wolf(helper, Rank.BETA, alpha.getUUID());
        ServerPlayer second = wolf(helper, Rank.BETA, alpha.getUUID());
        online(alpha, killer, second);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(isIn(index, alpha.getUUID(), killer) && isIn(index, alpha.getUUID(), second), "the pack was not indexed");
            kill(killer, alpha);
            WolfState k = state(killer);
            helper.assertTrue(k.rank() == Rank.ALPHA && k.packAlpha() == null && k.stoleSpark(), "the killer is " + k);
            helper.assertTrue(index.alphaOf(killer.getUUID()).isEmpty(), "the new Alpha is still a Beta in the index");
            helper.assertTrue(isIn(index, killer.getUUID(), second) && killer.getUUID().equals(state(second).packAlpha()),
                    "the rest of the pack did not follow: " + state(second));
            WolfState old = state(alpha);
            helper.assertTrue(old.rank() == Rank.OMEGA && old.packAlpha() == null, "the old Alpha is " + old);
            helper.assertTrue(index.betasOf(alpha.getUUID()).isEmpty() && index.alphaOf(alpha.getUUID()).isEmpty(), "the old Alpha is still in a pack");
            // the Beta days were kept: a True Alpha candidate does not start over
            helper.assertTrue(state(second).betaSince() == 1, "a Beta's days were reset: " + state(second));
        } finally {
            clean(alpha, killer, second);
        }
        helper.succeed();
    }

    // ---- inherited ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curedAlphaPassesSpark(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer weak = wolf(helper, Rank.BETA, alpha.getUUID(), 2);
        ServerPlayer strong = wolf(helper, Rank.BETA, alpha.getUUID(), 5);
        online(alpha, weak, strong);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            VampirismAPI.factionPlayerHandler(alpha).leaveFaction(false);
            WolfState s = state(strong);
            helper.assertTrue(s.rank() == Rank.ALPHA && s.packAlpha() == null && !s.stoleSpark(), "the strongest Beta is " + s);
            helper.assertTrue(state(weak).rank() == Rank.BETA && strong.getUUID().equals(state(weak).packAlpha()), "the weaker Beta is " + state(weak));
            helper.assertTrue(isIn(index, strong.getUUID(), weak), "the pack was not moved in the index");
            helper.assertTrue(state(alpha).rank() == Rank.OMEGA && state(alpha).packAlpha() == null, "the cured Alpha is " + state(alpha));
            helper.assertTrue(index.betasOf(alpha.getUUID()).isEmpty(), "the cured Alpha still has a pack");
        } finally {
            clean(alpha, weak, strong);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaWithNoOnlineBetaDissolvesThePack(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer away = wolf(helper, Rank.BETA, alpha.getUUID(), 9);
        online(alpha);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            VampirismAPI.factionPlayerHandler(alpha).leaveFaction(false);
            helper.assertTrue(state(alpha).rank() == Rank.OMEGA, "the cured Alpha is " + state(alpha));
            helper.assertTrue(index.betasOf(alpha.getUUID()).isEmpty() && index.alphaOf(away.getUUID()).isEmpty(), "the pack was not dissolved");
            helper.assertTrue(state(away).rank() == Rank.BETA, "the offline Beta was changed before they logged in");
            PackIndex.login(away);
            WolfState w = state(away);
            helper.assertTrue(w.rank() == Rank.OMEGA && w.packAlpha() == null && w.lineage() == Lineage.BITTEN, "the Beta is " + w);
            helper.assertTrue(index.alphaOf(away.getUUID()).isEmpty(), "the Beta went back to the index");
            PackIndex.login(away);
            helper.assertTrue(state(away).rank() == Rank.OMEGA, "a second login changed the Beta");
        } finally {
            clean(alpha, away);
        }
        helper.succeed();
    }

    // ---- given up ----

    private static Holder<MobEffect> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElseThrow();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void healGivesUpSpark(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer heir = wolf(helper, Rank.BETA, alpha.getUUID(), 4);
        ServerPlayer second = wolf(helper, Rank.BETA, alpha.getUUID(), 1);
        ServerPlayer patient = TestPlayers.createDuelist(helper);
        ServerPlayer far = TestPlayers.createDuelist(helper);
        patient.setPos(alpha.position().add(3, 0, 0));
        far.setPos(alpha.position().add(5, 0, 0));
        online(alpha, heir, second, patient, far);
        try {
            PackIndex index = PackIndex.get(helper.getLevel());
            patient.setHealth(1f);
            List<String> sick = List.of("supernaturalbloodlines:hybrid_venom", "werewolves:wolfsbane", "supernaturalbloodlines:wolfsbane_northern_blue",
                    "supernaturalbloodlines:wolfsbane_yellow", "supernaturalbloodlines:wolf_lichen");
            List<String> present = new ArrayList<>();
            for (String id : sick) {
                if (BuiltInRegistries.MOB_EFFECT.containsKey(ResourceLocation.parse(id))
                        && patient.addEffect(new MobEffectInstance(effect(id), 6000)) && far.addEffect(new MobEffectInstance(effect(id), 6000))) {
                    present.add(id);
                }
            }
            helper.assertTrue(present.contains("werewolves:wolfsbane") && present.contains("supernaturalbloodlines:hybrid_venom"), "the test effects could not be applied: " + present);

            helper.assertTrue(PackCommand.heal(heir, patient) != null, "a Beta gave up a spark they do not have");
            helper.assertTrue(PackCommand.heal(alpha, far) != null, "the heal reached 5 blocks");
            helper.assertTrue(PackCommand.heal(alpha, alpha) != null, "an Alpha healed themselves");
            helper.assertTrue(state(alpha).rank() == Rank.ALPHA && patient.getHealth() == 1f, "a refused heal changed something");

            Component error = PackCommand.heal(alpha, patient);
            helper.assertTrue(error == null, "the heal was refused: " + error);
            helper.assertTrue(patient.getHealth() == patient.getMaxHealth(), "not fully healed: " + patient.getHealth());
            for (String id : present) {
                helper.assertFalse(patient.hasEffect(effect(id)), id + " was not removed");
                helper.assertTrue(far.hasEffect(effect(id)), id + " was removed from someone who was not healed");
            }
            helper.assertTrue(state(heir).rank() == Rank.ALPHA && state(heir).packAlpha() == null && !state(heir).stoleSpark(), "the heir is " + state(heir));
            WolfState giver = state(alpha);
            helper.assertTrue(giver.rank() == Rank.BETA && heir.getUUID().equals(giver.packAlpha()), "the giver is " + giver);
            helper.assertTrue(isIn(index, heir.getUUID(), alpha) && isIn(index, heir.getUUID(), second), "the pack did not follow the spark");
            helper.assertTrue(heir.getUUID().equals(state(second).packAlpha()), "the other Beta is " + state(second));
        } finally {
            clean(alpha, heir, second, patient, far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lonelyAlphaGivingUpTheSparkBecomesOmega(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.TRUE_ALPHA, null);
        ServerPlayer patient = TestPlayers.createDuelist(helper);
        patient.setPos(alpha.position());
        online(alpha, patient);
        try {
            helper.assertTrue(PackCommand.heal(alpha, patient) == null, "the heal was refused");
            helper.assertTrue(state(alpha).rank() == Rank.OMEGA && state(alpha).packAlpha() == null, "the giver is " + state(alpha));
        } finally {
            clean(alpha, patient);
        }
        helper.succeed();
    }

    // ---- innocent kills and eyes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void villagerKillTurnsEyesBlue(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA, UUID.randomUUID());
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        ServerPlayer attacker = wolf(helper, Rank.OMEGA, null);
        ServerPlayer peaceful = TestPlayers.createDuelist(helper);
        List<String> sent = new ArrayList<>();
        EyeColorPayload.overrideSpy((to, payload) -> sent.add(to.getUUID() + ":" + payload.playerId() + ":" + payload.color()));
        try {
            helper.assertTrue(EyeColorPayload.Color.of(state(beta)) == Color.GOLD && EyeColorPayload.Color.of(state(alpha)) == Color.RED, "the starting eyes are wrong");
            kill(beta, helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1)));
            helper.assertFalse(state(beta).killedInnocent(), "a zombie was an innocent");
            helper.assertTrue(sent.isEmpty(), "eye colours were sent without a change: " + sent);

            Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
            kill(beta, villager);
            helper.assertTrue(state(beta).killedInnocent() && EyeColorPayload.Color.of(state(beta)) == Color.BLUE, "the eyes are " + EyeColorPayload.Color.of(state(beta)));
            helper.assertTrue(sent.contains(beta.getUUID() + ":" + beta.getId() + ":BLUE"), "the new colour was not sent: " + sent);

            // an Alpha keeps red eyes, but it still counts against the True Alpha
            kill(alpha, helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1)));
            helper.assertTrue(state(alpha).killedInnocent() && EyeColorPayload.Color.of(state(alpha)) == Color.RED, "the Alpha's eyes are " + EyeColorPayload.Color.of(state(alpha)));

            // a player who was not attacking the killer is innocent, one who was is not
            kill(attacker, peaceful);
            helper.assertTrue(state(attacker).killedInnocent(), "killing a peaceful player was not innocent");
            ServerPlayer duel = wolf(helper, Rank.OMEGA, null);
            try {
                human.setLastHurtMob(duel);
                kill(duel, human);
                helper.assertFalse(state(duel).killedInnocent(), "a player who attacked first was an innocent");
            } finally {
                clean(duel);
            }

            // a human does not get blue eyes, the rules ignore them
            ServerPlayer plain = TestPlayers.createDuelist(helper);
            kill(plain, helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1)));
            helper.assertTrue(state(plain).equals(WolfState.NONE), "a human changed: " + state(plain));
        } finally {
            EyeColorPayload.overrideSpy(null);
            clean(beta, alpha, human, attacker, peaceful);
        }
        helper.succeed();
    }

    // ---- defender kills and the True Alpha ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void defenderKillsCountOnlyWhenDefending(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer killer = wolf(helper, Rank.BETA, alpha.getUUID());
        ServerPlayer stranger = wolf(helper, Rank.OMEGA, null);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        online(alpha, killer, stranger, human);
        try {
            Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            kill(killer, z);
            helper.assertTrue(state(killer).defenderKills() == 0, "a mob with no target counted");
            z.setTarget(killer);
            kill(killer, z);
            helper.assertTrue(state(killer).defenderKills() == 0, "defending yourself counted");
            z.setTarget(stranger);
            kill(killer, z);
            helper.assertTrue(state(killer).defenderKills() == 0, "defending a stranger counted");
            z.setTarget(human);
            kill(killer, z);
            helper.assertTrue(state(killer).defenderKills() == 0, "defending a human counted");

            z.setTarget(alpha);
            kill(killer, z);
            helper.assertTrue(state(killer).defenderKills() == 1, "defending the Alpha did not count");
            Zombie villagerGuard = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            villagerGuard.setTarget(helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1)));
            kill(killer, villagerGuard);
            helper.assertTrue(state(killer).defenderKills() == 2, "defending a villager did not count");

            Zombie packmate = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            packmate.setTarget(killer);
            kill(alpha, packmate);
            helper.assertTrue(state(alpha).defenderKills() == 1, "the Alpha defending a Beta did not count");

            // a human killer, a passive animal and a stranger's villager do not count
            Zombie ignored = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            ignored.setTarget(alpha);
            kill(human, ignored);
            Zombie pig = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
            pig.setTarget(alpha);
            killedBy(pig.damageSources().generic(), pig);
            helper.assertTrue(state(human).equals(WolfState.NONE), "a human got a defender kill");
        } finally {
            clean(alpha, killer, stranger, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void trueAlphaAwakensWhenEligible(GameTestHelper helper) {
        int max = WerewolfFaction.faction().getHighestReachableLevel();
        ServerPlayer candidate = wolf(helper, Rank.BETA, UUID.randomUUID(), max);
        ServerPlayer lowLevel = wolf(helper, Rank.BETA, UUID.randomUUID(), max - 1);
        ServerPlayer thief = wolf(helper, Rank.BETA, UUID.randomUUID(), max);
        ServerPlayer blue = wolf(helper, Rank.BETA, UUID.randomUUID(), max);
        ServerPlayer rookie = wolf(helper, Rank.BETA, UUID.randomUUID(), max);
        ServerPlayer few = wolf(helper, Rank.BETA, UUID.randomUUID(), max);
        UUID old = UUID.randomUUID();
        try {
            WolfState ready = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.BETA).packAlpha(old).geneRolled(true)
                    .betaSince(NOW - 30L * 24000).defenderKills(300).build();
            Map<ServerPlayer, WolfState> setup = new HashMap<>();
            setup.put(candidate, ready);
            setup.put(lowLevel, ready);
            setup.put(thief, ready.toBuilder().stoleSpark(true).build());
            setup.put(blue, ready.toBuilder().killedInnocent(true).build());
            setup.put(rookie, ready.toBuilder().betaSince(NOW - 30L * 24000 + 1).build());
            setup.put(few, ready.toBuilder().defenderKills(299).build());
            setup.forEach((p, w) -> BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w).build()));

            for (ServerPlayer p : List.of(lowLevel, thief, blue, rookie, few)) {
                helper.assertFalse(DefenderKills.checkTrueAlpha(p, NOW), "an ineligible wolf was refused with true: " + state(p));
                helper.assertTrue(state(p).rank() == Rank.BETA, "an ineligible wolf became " + state(p).rank());
            }
            helper.assertTrue(DefenderKills.checkTrueAlpha(candidate, NOW), "the eligible wolf did not awaken");
            WolfState t = state(candidate);
            helper.assertTrue(t.rank() == Rank.TRUE_ALPHA && t.packAlpha() == null, "the True Alpha is " + t);
            helper.assertTrue(PackIndex.get(helper.getLevel()).alphaOf(candidate.getUUID()).isEmpty(), "the True Alpha is still in a pack");
            helper.assertFalse(DefenderKills.checkTrueAlpha(candidate, NOW), "an existing True Alpha was awarded again");
            // there is no cap: a second one awakens too
            BloodlineAccess.set(lowLevel, BloodlineAccess.get(lowLevel).toBuilder().wolf(ready).build());
            VampirismAPI.factionPlayerHandler(lowLevel).setFactionAndLevel(WReference.WEREWOLF_FACTION, max);
            helper.assertTrue(DefenderKills.checkTrueAlpha(lowLevel, NOW), "a second True Alpha was refused");
        } finally {
            clean(candidate, lowLevel, thief, blue, rookie, few);
        }
        helper.succeed();
    }

    // ---- ranks ----

    private static double modifier(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance a = p.getAttribute(attribute);
        var m = a == null ? null : a.getModifier(RankEffects.MODIFIER_ID);
        return m == null ? 0 : m.amount();
    }

    private static void assertRank(GameTestHelper helper, ServerPlayer p, double attack, double speed, double health) {
        helper.assertTrue(Math.abs(modifier(p, Attributes.ATTACK_DAMAGE) - attack) < 1e-9, "attack modifier " + modifier(p, Attributes.ATTACK_DAMAGE) + " for " + state(p).rank() + ", expected " + attack);
        helper.assertTrue(Math.abs(modifier(p, Attributes.MOVEMENT_SPEED) - speed) < 1e-9, "speed modifier " + modifier(p, Attributes.MOVEMENT_SPEED) + " for " + state(p).rank() + ", expected " + speed);
        helper.assertTrue(Math.abs(modifier(p, Attributes.MAX_HEALTH) - health) < 1e-9, "health modifier " + modifier(p, Attributes.MAX_HEALTH) + " for " + state(p).rank() + ", expected " + health);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rankModifiersApplied(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.OMEGA, null);
        try {
            assertRank(helper, p, -0.10, -0.10, 0);
            WolfState w = state(p);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().rank(Rank.BETA).packAlpha(UUID.randomUUID()).build()).build());
            assertRank(helper, p, 0, 0, 0);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().rank(Rank.ALPHA).build()).build());
            assertRank(helper, p, 0.20, 0, 4);
            helper.assertTrue(p.getMaxHealth() == 24f, "max health is " + p.getMaxHealth());
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().wolf(w.toBuilder().rank(Rank.TRUE_ALPHA).build()).build());
            assertRank(helper, p, 0.22, 0, 4.4);

            // a login (or any later sync) neither stacks nor loses them
            RankEffects.sync(p);
            RankEffects.sync(p);
            assertRank(helper, p, 0.22, 0, 4.4);

            // out of the faction, a cured wolf has no perks and no penalties
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            assertRank(helper, p, 0, 0, 0);
            helper.assertTrue(p.getHealth() <= p.getMaxHealth(), "health above the maximum after losing the perk");
            // (leaving the faction cost the spark, so they are an Omega when they come back)
            helper.assertTrue(state(p).rank() == Rank.OMEGA, "the cured True Alpha is " + state(p).rank());
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1);
            assertRank(helper, p, -0.10, -0.10, 0);
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    // ---- born wolves ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean forcedShiftCancelled(ServerPlayer p) {
        ForcedShift.enterTick(p);
        try {
            var event = new ActionEvent.ActionActivatedEvent(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(),
                    (IAction) ModActions.BEAST_FORM.get(), 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bornWolfIsSparedHalfTheForcedShifts(GameTestHelper helper) {
        ServerPlayer born = wolf(helper, Rank.OMEGA, null);
        ServerPlayer bitten = wolf(helper, Rank.OMEGA, null);
        BloodlineAccess.set(born, BloodlineAccess.get(born).toBuilder().wolf(state(born).toBuilder().lineage(Lineage.BORN).build()).build());
        long before = helper.getLevel().getDayTime();
        try {
            int sparedNights = 0;
            int shiftedNights = 0;
            for (long day = 100; day < 140; day++) {
                helper.getLevel().setDayTime(day * 24000 + 15000);
                boolean expected = MoonControl.bornSparedOn(born.getUUID(), day);
                helper.assertTrue(forcedShiftCancelled(born) == expected, "night " + day + ": the born wolf was " + (expected ? "not spared" : "spared"));
                helper.assertFalse(forcedShiftCancelled(bitten), "a bitten wolf was spared on night " + day);
                sparedNights += expected ? 1 : 0;
                shiftedNights += expected ? 0 : 1;
            }
            helper.assertTrue(sparedNights > 0 && shiftedNights > 0, "spared " + sparedNights + " nights, shifted " + shiftedNights);
        } finally {
            helper.getLevel().setDayTime(before);
            clean(born, bitten);
        }
        helper.succeed();
    }

    // ---- the pending bite ----

    private static final Holder<MobEffect> LUPUS = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("werewolves:lupus_sanguinem")).orElseThrow();

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pendingAlphaWhoLostTheSparkMakesOmega(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, null);
        ServerPlayer victim = TestPlayers.createDuelist(helper);
        ServerPlayer offline = TestPlayers.createDuelist(helper);
        ServerPlayer stillAlpha = TestPlayers.createDuelist(helper);
        online(alpha, victim, offline, stillAlpha);
        SpeciesConfig.overrideAlphaBiteDeathChance(0.0);
        AlphaBite.useRoll(() -> 0.5);
        try {
            net.minecraft.server.MinecraftServer server = victim.getServer();
            boolean pvp = server.isPvpAllowed();
            server.setPvpAllowed(true);
            victim.invulnerableTime = 0;
            victim.hurt(alpha.damageSources().playerAttack(alpha), 1f);
            server.setPvpAllowed(pvp);
            victim.addEffect(new MobEffectInstance(LUPUS, 600));
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(alpha.getUUID()), "the Alpha was not remembered");
            // the Alpha loses the spark before the effect runs out
            BloodlineAccess.set(alpha, BloodlineAccess.get(alpha).toBuilder().wolf(state(alpha).toBuilder().rank(Rank.OMEGA).build()).build());
            helper.assertTrue(WerewolfFaction.join(victim), "the victim could not join the werewolves");
            WolfState w = state(victim);
            helper.assertTrue(w.lineage() == Lineage.BITTEN && w.rank() == Rank.OMEGA && w.packAlpha() == null, "the victim is " + w);
            helper.assertTrue(alpha.getUUID().equals(BloodlineAccess.get(victim).sire()), "the sire was not recorded: " + BloodlineAccess.get(victim).sire());
            helper.assertTrue(victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID) && victim.isAlive(), "pending Alpha left over or the victim died");

            // an offline Alpha who lost the spark (the tombstone): the same
            UUID gone = UUID.randomUUID();
            PackIndex.get(helper.getLevel()).markFormerAlpha(gone);
            offline.setData(SpeciesRegistries.PENDING_ALPHA, gone);
            helper.assertTrue(WerewolfFaction.join(offline), "could not join");
            helper.assertTrue(state(offline).rank() == Rank.OMEGA && state(offline).packAlpha() == null, "the offline-Alpha bite gave " + state(offline));

            // an offline Alpha with no tombstone still counts
            UUID asleep = UUID.randomUUID();
            stillAlpha.setData(SpeciesRegistries.PENDING_ALPHA, asleep);
            helper.assertTrue(WerewolfFaction.join(stillAlpha), "could not join");
            helper.assertTrue(state(stillAlpha).rank() == Rank.BETA && asleep.equals(state(stillAlpha).packAlpha()), "the bite of a sleeping Alpha gave " + state(stillAlpha));
        } finally {
            AlphaBite.resetRoll();
            SpeciesConfig.overrideAlphaBiteDeathChance(null);
            victim.removeEffect(LUPUS);
            clean(alpha, victim, offline, stillAlpha);
        }
        helper.succeed();
    }
}
