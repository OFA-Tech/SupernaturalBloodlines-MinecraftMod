package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/** Phase 12c Task 2, the werewolf skills of the species part: each compares the same player with and without the skill. */
@PrefixGameTestTemplate(false)
public final class WerewolfSkillRuleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private WerewolfSkillRuleGameTests() {}

    private static ServerPlayer werewolf(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).turnedAt(1).build());
        return p;
    }

    private static void own(ServerPlayer p, String... skills) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonCalmSparesAQuarterOfTheNights(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        int plain = 0;
        int calm = 0;
        try {
            for (int day = 0; day < 400; day++) {
                level.setDayTime(day * 24000L + 18000);
                plain += ForcedShift.moonCalmSpares(p) ? 1 : 0;
            }
            own(p, "S-WOL-2");
            for (int day = 0; day < 400; day++) {
                level.setDayTime(day * 24000L + 18000);
                calm += ForcedShift.moonCalmSpares(p) ? 1 : 0;
            }
        } finally {
            level.setDayTime(saved);
        }
        helper.assertTrue(plain == 0, plain + " nights were spared without the skill");
        helper.assertTrue(calm > 60 && calm < 140, calm + " of 400 nights spared with the skill (about a quarter)");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void packHeartNeedsTwoBetas(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
        helper.assertTrue(PackBonus.betasNeeded(p) == 3, "base " + PackBonus.betasNeeded(p));
        own(p, "S-WOL-5");
        helper.assertTrue(PackBonus.betasNeeded(p) == 2, "with the skill " + PackBonus.betasNeeded(p));
        helper.assertTrue(PackBonus.bonusFor(2, 2, PackBonus.betasNeeded(p)) > 0 && PackBonus.bonusFor(2, 2) == 0, "two Betas do not give the bonus");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void roarsEchoShortensTheCooldown(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, Species.NONE);
        helper.assertTrue(AlphaRoar.cooldown(p) == 1200, "base " + AlphaRoar.cooldown(p));
        own(p, "S-WOL-6");
        helper.assertTrue(AlphaRoar.cooldown(p) == 800, "with the skill " + AlphaRoar.cooldown(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void venomFangSlowsTheVictim(GameTestHelper helper) {
        ServerPlayer wolf = werewolf(helper, Species.HYBRID);
        ServerPlayer first = vampire(helper);
        bite(wolf, first);
        helper.assertTrue(first.hasEffect(SpeciesRegistries.HYBRID_VENOM) && !first.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "venom, no slowness without the skill");
        own(wolf, "S-HYB-2");
        ServerPlayer second = vampire(helper);
        bite(wolf, second);
        var slow = second.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(second.hasEffect(SpeciesRegistries.HYBRID_VENOM) && slow != null && slow.getDuration() > 90 && slow.getDuration() <= 100 && slow.getAmplifier() == 0,
                "slowness with the skill: " + slow);
        helper.succeed();
    }

    /** A hybrid with Predator's Thirst still poisons the vampire it bites: the venom is a rule of the hit, not of the drink. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridBiterStillPoisons(GameTestHelper helper) {
        ServerPlayer wolf = werewolf(helper, Species.HYBRID);
        own(wolf, "S-VAM-6");
        ServerPlayer victim = vampire(helper);
        bite(wolf, victim);
        helper.assertTrue(victim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "the hybrid's bite no longer poisons");
        helper.succeed();
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static void bite(ServerPlayer attacker, LivingEntity victim) {
        net.minecraft.server.MinecraftServer server = victim.getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            victim.invulnerableTime = 0;
            victim.hurt(attacker.damageSources().playerAttack(attacker), 4f);
        } finally {
            server.setPvpAllowed(pvp);
        }
    }
}
