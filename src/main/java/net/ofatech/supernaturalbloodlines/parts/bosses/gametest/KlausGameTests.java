package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.werewolves.api.WReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Klaus (plan 17c): when the director sends him, the warning, the shift and its hitbox, the venom bite, Klaus's Blood, and that nothing he does can start
 * line death by a mob's hand. The moon, the mods, the villagers and the ground are pinned, so a neighbouring test cannot change the answer.
 */
@PrefixGameTestTemplate(false)
public final class KlausGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String UNKNOWN = "supernaturalbloodlines.boss.notice.klaus.unknown";
    private static final String HINT = "supernaturalbloodlines.boss.notice.klaus.hint";
    private static final String LOST_SCENT = "supernaturalbloodlines.boss.klaus.lost_scent";
    private static final String COUNTDOWN = "supernaturalbloodlines.boss.klaus.countdown";
    private static final String DAWN = "supernaturalbloodlines.boss.klaus.dawn";
    private static final long NIGHT = 24000L * 20 + 15000;
    private static final long MORNING = 24000L * 20 + 6000;

    private KlausGameTests() {
    }

    private static boolean werewolves() {
        return ModList.get().isLoaded("werewolves");
    }

    /** A player with the doppelganger gene, standing in the test. */
    private static LineTestPlayer carrier(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build());
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        return p;
    }

    private static LineTestPlayer stranger(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().doppelgangerGene(false).doppelgangerRolled(true).build());
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        return p;
    }

    /** A full moon night, the mods there, a roll that passes, the ground fixed, and these players the only ones in the overworld. */
    private static void night(ServerPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMoon(l -> true);
        BossDirector.useMods(b -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useVillagers(l -> List.of());
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    /** Game ticks for an entity the level is not ticking: its own count goes up one a tick, as the level does, so what it does every ten ticks happens every ten. */
    private static void run(KlausEntity k, int ticks) {
        for (int i = 0; i < ticks && !k.isRemoved(); i++) {
            k.tickCount++;
            k.tick();
        }
    }

    private static KlausEntity klausOf(BossTestRig rig) {
        return (KlausEntity) BossDirector.bossEntity(rig.server, BossId.KLAUS);
    }

    // ---- the director ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerDoppelgangerIsWarnedThenKlausArrivesAfterTheWarning(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came before the warning ran out");
            helper.assertTrue(p.count(UNKNOWN) == 2, "the warning was not on both the action bar and in chat, with the seconds: " + p.heard());
            helper.assertTrue(p.received(HINT), "no hint that mountain ash holds him: " + p.heard());
            helper.assertTrue(p.chat() == 2, "the warning and the hint are the two chat lines, got " + p.chat());
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt > 0 && rig.index.state(BossId.KLAUS) == BossState.UNSEEN, "nothing is waiting in the index");
            BossClock.offsetForTests((BossesConfig.klausWarningSeconds() - 1) * 20L);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came a second early");
            BossClock.offsetForTests(BossesConfig.klausWarningSeconds() * 20L + 5);
            BossDirector.check(rig.server);
            KlausEntity k = klausOf(rig);
            helper.assertTrue(k != null && k.isAlive(), "he did not arrive when the warning ran out");
            helper.assertTrue(p.getUUID().equals(k.prey()) && k.form() == BossForm.HUMAN, "he does not hunt the player the director picked, or came as a wolf");
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.ACTIVE && rig.index.of(BossId.KLAUS).appearances == 1 && rig.index.of(BossId.KLAUS).arriveAt == -1,
                    "the index says " + rig.index.state(BossId.KLAUS));
            double d = Math.sqrt(k.distanceToSqr(p));
            helper.assertTrue(d >= 4 && d <= 45, "he came from " + d + " blocks away");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVillagerDoppelgangerIsNotWarnedAndKlausComesAtOnce(GameTestHelper helper) {
        LineTestPlayer watcher = stranger(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(watcher);
            Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
            BossDirector.useVillagers(l -> List.of(v));
            BossDirector.check(rig.server);
            KlausEntity k = klausOf(rig);
            helper.assertTrue(k != null, "he did not come for a doppelganger villager");
            helper.assertTrue(v.getUUID().equals(k.prey()), "he does not hunt the villager");
            helper.assertTrue(watcher.received() == 0, "a player was warned about a villager: " + watcher.heard());
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1, "a villager's arrival was put off");
        } finally {
            forget(helper, watcher);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVillagerFarFromEveryPlayerIsLeftAlone(GameTestHelper helper) {
        LineTestPlayer watcher = stranger(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(watcher);
            Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
            watcher.setPos(v.getX() + KlausRules.VILLAGER_RANGE + 1, v.getY(), v.getZ());
            BossDirector.useVillagers(l -> List.of(v));
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came for a villager nobody is near");
            helper.assertTrue(rig.index.of(BossId.KLAUS).lastRollDay == -1, "a roll was spent with nobody to hunt");
            watcher.setPos(v.getX() + KlausRules.VILLAGER_RANGE - 1, v.getY(), v.getZ());
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) != null, "he did not come for a villager a player is near");
        } finally {
            forget(helper, watcher);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withHuntsPlayersOffOnlyVillagersAreHunted(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("klaus_hunts_players", false);
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null && rig.index.of(BossId.KLAUS).arriveAt == -1, "he hunted a player with klaus_hunts_players off");
            helper.assertTrue(p.received() == 0, "the player was warned: " + p.heard());
            Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
            BossDirector.useVillagers(l -> List.of(v));
            BossDirector.check(rig.server);
            KlausEntity k = klausOf(rig);
            helper.assertTrue(k != null && v.getUUID().equals(k.prey()), "he did not come for the villager");
        } finally {
            BossesConfig.override("klaus_hunts_players", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withAZeroWarningAPlayerIsNotMadeToWait(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("klaus_warning_seconds", 0);
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) != null, "he did not come at once with a zero warning");
            helper.assertTrue(p.received("supernaturalbloodlines.boss.notice.klaus.unknown_now"), "the player was not told he is already here: " + p.heard());
        } finally {
            BossesConfig.override("klaus_warning_seconds", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whenThereIsNoGroundThatFarHeComesCloserAndWithNoGroundAtAllNotAtAll(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("klaus_warning_seconds", 0);
            night(p);
            int[] asked = {0};
            BossDirector.useGround((level, from, min, max) -> {
                if (min < 24) {
                    asked[0] = max;
                }
                return min >= 24 ? java.util.Optional.empty() : java.util.Optional.of(from.offset(8, 0, 8));
            });
            BossDirector.check(rig.server);
            KlausEntity k = klausOf(rig);
            helper.assertTrue(k != null, "he did not come when only closer ground was there");
            helper.assertTrue(asked[0] == 24, "the closer ground asked for was up to " + asked[0]);
            k.discard();
            rig.index.forget(BossId.KLAUS);
            BossDirector.useGround((level, from, min, max) -> java.util.Optional.empty());
            helper.getLevel().setDayTime(helper.getLevel().getDayTime() + 24000L);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came where there is no ground");
        } finally {
            BossesConfig.override("klaus_warning_seconds", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutAFullMoonAWerewolfModADoppelgangerOrTheSwitchHeStaysAway(GameTestHelper helper) {
        LineTestPlayer gene = carrier(helper);
        LineTestPlayer none = stranger(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(gene);
            BossDirector.useMoon(l -> false);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null && rig.index.of(BossId.KLAUS).arriveAt == -1, "he came without a full moon");
            helper.assertTrue(rig.index.of(BossId.KLAUS).lastRollDay == -1, "a roll was spent without a full moon");
            BossDirector.useMoon(l -> true);
            BossDirector.useMods(b -> false);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null && rig.index.of(BossId.KLAUS).arriveAt == -1, "he came without Werewolves");
            BossDirector.useMods(b -> true);
            BossesConfig.override("klaus_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null && rig.index.of(BossId.KLAUS).arriveAt == -1, "he came with his switch off");
            BossesConfig.override("klaus_enabled", null);
            night(none);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null && rig.index.of(BossId.KLAUS).arriveAt == -1, "he came with no doppelganger in the world");
            helper.assertTrue(rig.index.of(BossId.KLAUS).lastRollDay == -1, "a roll was spent with nobody to hunt");
        } finally {
            BossesConfig.override("klaus_enabled", null);
            forget(helper, gene, none);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutWerewolvesInTheGameTheRealCheckKeepsHimOut(GameTestHelper helper) {
        LineTestPlayer gene = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(gene);
            BossDirector.useMods(null);
            helper.assertTrue(BossDirector.modsPresent(BossId.KLAUS) == werewolves(), "the director's idea of Werewolves is not the game's");
            helper.assertTrue(BossDirector.modsPresent(BossId.MIKAEL), "Mikael needs nothing");
            BossDirector.check(rig.server);
            boolean came = klausOf(rig) != null || rig.index.of(BossId.KLAUS).arriveAt >= 0;
            helper.assertTrue(came == werewolves(), "Werewolves is " + werewolves() + " and he " + (came ? "came" : "stayed away"));
        } finally {
            forget(helper, gene);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFailedRollSpendsTheNightAndTheNextFullMoonRollsAgain(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("klaus_warning_seconds", 0);
            night(p);
            BossDirector.useRoll(() -> 0.99);
            helper.getLevel().setDayTime(24000L * 40 + 15000);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came on a roll above the chance");
            BossDirector.useRoll(() -> 0.0);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "a second roll was taken the same night");
            helper.getLevel().setDayTime(24000L * 48 + 15000);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) != null, "he did not come the next full moon");
        } finally {
            BossesConfig.override("klaus_warning_seconds", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWarnedArrivalIsDroppedWhenHeIsSwitchedOffMeanwhile(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt > 0, "control: no arrival waits");
            BossesConfig.override("klaus_enabled", false);
            BossClock.offsetForTests(BossesConfig.klausWarningSeconds() * 20L + 5);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came although he was switched off while the warning ran");
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1, "the arrival is still waiting");
        } finally {
            BossesConfig.override("klaus_enabled", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWarnedArrivalIsDroppedWhenTheMoonSetsMeanwhile(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt > 0, "control: no arrival waits");
            BossDirector.useMoon(l -> false);
            BossClock.offsetForTests(BossesConfig.klausWarningSeconds() * 20L + 5);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came although the full moon night was over while the warning ran");
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1, "the arrival is still waiting");
            helper.assertTrue(p.count(KlausEntity.LOST_SCENT) == 1, "the target was not told the wolf lost the scent: " + p.heard());
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKlausWhoFallsMidShiftDoesNotSwapUntilHeGetsUp(GameTestHelper helper) {
        List<String> seen = new java.util.ArrayList<>();
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            KlausEntity.observeSwap(e -> seen.add(e.form() + "@" + e.shiftTicks()));
            k.startShift();
            for (int i = 1; i < ShiftRules.SWAP_TICK; i++) {
                k.tick();
            }
            int at = k.shiftTicks();
            k.desiccate();
            for (int i = 0; i < 40; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.SHIFTING && k.shiftTicks() == at, "a fallen Klaus went on shifting: " + k.form() + " at " + k.shiftTicks());
            helper.assertTrue(seen.isEmpty() && k.getBbHeight() == net.ofatech.supernaturalbloodlines.core.bloodline.LyingBody.HEIGHT, "a fallen Klaus swapped his body (the box is the lying one, R184): " + seen + " " + k.getBbHeight());
            k.recover();
            for (int i = 0; i < 4; i++) {
                k.tick();
            }
            helper.assertTrue(seen.size() == 1, "he did not swap once he got up: " + seen);
        } finally {
            KlausEntity.observeSwap(null);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anArrivalForSomeoneWhoLeftIsDropped(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt > 0, "control: no arrival waits");
            SireCall.overridePlayerLookup(id -> null); // logged off
            BossClock.offsetForTests(BossesConfig.klausWarningSeconds() * 20L + 5);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) == null, "he came for somebody who left");
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1 && rig.index.of(BossId.KLAUS).target == null, "the dropped arrival is still written down");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- the shift ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heShiftsAtHalfHealthStaysTheSameEntityAndTheHitboxFollows(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            UUID id = k.getUUID();
            helper.assertTrue(k.form() == BossForm.HUMAN && Math.abs(k.getBbHeight() - 1.95F) < 1e-4, "control: not a man of 1.95 blocks: " + k.getBbHeight());
            double base = k.getAttributeValue(Attributes.ATTACK_DAMAGE);
            k.setHealth(k.getMaxHealth() * 0.51F);
            for (int i = 0; i < 12; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.HUMAN, "he shifted above half health");
            k.setHealth(k.getMaxHealth() * 0.5F);
            for (int i = 0; i < 12; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.SHIFTING, "he did not start to shift at half health: " + k.form());
            for (int i = 0; i < ShiftRules.SHIFT_TICKS + 5; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.BEAST, "the shift did not end: " + k.form());
            helper.assertTrue(Math.abs(k.getBbHeight() - 1.5F) < 1e-4 && Math.abs(k.getBbWidth() - 0.9F) < 1e-4, "the hitbox is " + k.getBbWidth() + " x " + k.getBbHeight());
            helper.assertTrue(k.getUUID().equals(id) && k.isAlive() && rig.index.of(BossId.KLAUS).entity.equals(id), "he is not the same entity");
            helper.assertTrue(Math.abs(k.getAttributeValue(Attributes.ATTACK_DAMAGE) - base * (1 + ShiftRules.BEAST_DAMAGE_BONUS)) < 1e-6, "the wolf does not bite harder: " + k.getAttributeValue(Attributes.ATTACK_DAMAGE));
            k.setHealth(k.getMaxHealth());
            for (int i = 0; i < 30; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.BEAST, "he turned back");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStandsStillWhileHeShifts(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            k.startShift();
            k.setDeltaMovement(0.4, 0, 0.4);
            double x = k.getX();
            k.tick();
            helper.assertTrue(k.form() == BossForm.SHIFTING && k.shiftTicks() == 1, "the shift did not run: " + k.form() + " " + k.shiftTicks());
            helper.assertTrue(Math.abs(k.getDeltaMovement().x) < 1e-9 && Math.abs(k.getDeltaMovement().z) < 1e-9, "he kept walking while he shifted: " + k.getDeltaMovement());
            helper.assertTrue(Math.abs(k.getX() - x) < 0.05, "he moved " + (k.getX() - x) + " blocks while he shifted");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heShiftsAgainstAWerewolfAtFullHealth(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        ServerPlayer wolf = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(wolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            k.setTarget(wolf);
            for (int i = 0; i < 12; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() != BossForm.HUMAN, "he did not shift against a werewolf: " + k.form());
            KlausEntity other = BossDirector.spawnKlaus(helper.getLevel(), null, helper.absolutePos(new BlockPos(5, 1, 5))).orElseThrow();
            ServerPlayer mortal = TestPlayers.createDuelist(helper);
            other.setTarget(mortal);
            for (int i = 0; i < 12; i++) {
                other.tick();
            }
            helper.assertTrue(other.form() == BossForm.HUMAN, "he shifted against a mortal at full health");
            other.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisFormAndShiftSurviveASave(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            double base = k.getAttributeValue(Attributes.ATTACK_DAMAGE);
            k.setForm(BossForm.BEAST);
            k.lookForm(BossForm.BEAST, 33);
            UUID prey = UUID.randomUUID();
            k.hunt(prey);
            helper.getLevel().setDayTime(NIGHT);
            run(k, 30);
            int since = k.sinceHit();
            int ticks = k.shiftTicks();
            helper.assertTrue(since > 0 && !k.isRemoved() && ticks > 33, "control: no count to save: " + since + ", " + ticks);
            CompoundTag tag = new CompoundTag();
            k.saveWithoutId(tag);
            KlausEntity again = KlausEntity.create(helper.getLevel());
            again.load(tag);
            helper.assertTrue(again.form() == BossForm.BEAST, "the form was forgotten: " + again.form());
            helper.assertTrue(Math.abs(again.getBbHeight() - 1.5F) < 1e-4, "the hitbox was not the wolf's after a load: " + again.getBbHeight());
            helper.assertTrue(prey.equals(again.prey()), "the prey was forgotten");
            helper.assertTrue(again.shiftTicks() == ticks, "the ticks of the shift were forgotten: " + again.shiftTicks());
            helper.assertTrue(again.sinceHit() == since, "the time since his last blow was forgotten: " + again.sinceHit() + " of " + since);
            again.setUUID(UUID.randomUUID()); // the saved one is still in the level: a second entity with its uuid is refused
            helper.assertTrue(helper.getLevel().addFreshEntity(again), "the loaded Klaus was not added to the level");
            helper.assertTrue(Math.abs(again.getAttributeValue(Attributes.ATTACK_DAMAGE) - base * (1 + ShiftRules.BEAST_DAMAGE_BONUS)) < 1e-6,
                    "the wolf lost its bite through a load: " + again.getAttributeValue(Attributes.ATTACK_DAMAGE));
            again.discard();
        }
        helper.succeed();
    }

    // ---- the bite ----

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheWolfsBiteIsVenomAndOnlyOnAVampire(GameTestHelper helper) {
        ServerPlayer victim = vampire(helper);
        ServerPlayer second = vampire(helper);
        ServerPlayer mortal = TestPlayers.createDuelist(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            k.doHurtTarget(victim);
            helper.assertTrue(!victim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "the man's blow was venom");
            k.setForm(BossForm.BEAST);
            k.doHurtTarget(mortal);
            helper.assertTrue(!mortal.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a mortal was poisoned");
            k.doHurtTarget(second);
            MobEffectInstance venom = second.getEffect(SpeciesRegistries.HYBRID_VENOM);
            helper.assertTrue(venom != null && venom.getDuration() > Venom.DURATION_TICKS - 100, "the wolf's bite was not venom: " + venom);
            second.invulnerableTime = 0;
            second.setHealth(second.getMaxHealth());
            second.removeEffect(SpeciesRegistries.HYBRID_VENOM);
            second.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, 500));
            k.doHurtTarget(second);
            helper.assertTrue(second.getEffect(SpeciesRegistries.HYBRID_VENOM).getDuration() <= 500, "a second bite renewed the venom");
        }
        helper.succeed();
    }

    // ---- Klaus's Blood ----

    private static boolean drink(GameTestHelper helper, ServerPlayer p, ItemStack bottle) {
        p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        boolean started = bottle.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction();
        if (started) {
            p.setItemInHand(InteractionHand.MAIN_HAND, bottle.finishUsingItem(helper.getLevel(), p));
        }
        return started;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void klausBloodStartsTheTransitionUnderBothSettings(GameTestHelper helper) {
        if (!werewolves()) {
            helper.succeed();
            return;
        }
        for (boolean anyOriginal : new boolean[] {false, true}) {
            ServerPlayer p = TestPlayers.createMortal(helper);
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
            SpeciesConfig.overrideTransitionBloodAnyOriginal(anyOriginal);
            try {
                helper.assertTrue(drink(helper, p, new ItemStack(BossItems.KLAUS_BLOOD.get())), "the drink did not start, any_original=" + anyOriginal);
                helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not used up");
                helper.assertTrue(p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && BossTestRig.KLAUS.equals(p.getData(SpeciesRegistries.BLOOD_SOURCE)),
                        "the drinker is not marked with Klaus's blood, any_original=" + anyOriginal);
                p.hurt(helper.getLevel().damageSources().generic(), 1000f);
                BloodlineState s = BloodlineAccess.get(p);
                helper.assertTrue(s.inTransition() && BossTestRig.KLAUS.equals(s.pendingSire()), "no transition in Klaus's line, any_original=" + anyOriginal + ": " + s);
            } finally {
                SpeciesConfig.overrideTransitionBloodAnyOriginal(null);
                BloodlineAccess.set(p, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void klausBloodCuresVenomAndIsNotASiresBottle(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        p.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, Venom.DURATION_TICKS));
        ItemStack blood = new ItemStack(BossItems.KLAUS_BLOOD.get());
        helper.assertTrue(blood.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "cannot drink it while poisoned");
        p.setItemInHand(InteractionHand.MAIN_HAND, blood.finishUsingItem(helper.getLevel(), p));
        helper.assertTrue(!p.hasEffect(SpeciesRegistries.HYBRID_VENOM), "Klaus's Blood did not cure the venom");
        helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not given back");
        helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem.sourceOf(new ItemStack(BossItems.KLAUS_BLOOD.get())) == null, "it is a Sire's Blood bottle");
        helper.succeed();
    }

    // ---- his death, his line, and a mob's hand ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWhiteOakDeathOfAFallenKlausKillsHisLineAndRewardsOnce(GameTestHelper helper) {
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(child.getUUID(), BossTestRig.KLAUS, true);
            night(child);
            KlausEntity k = rig.spawnKlaus();
            k.desiccate();
            helper.assertTrue(rig.dropsNear(k, BossItems.KLAUS_BLOOD.get()) == 1, "he did not drop Klaus's Blood when brought down: " + rig.dropsNear(k, BossItems.KLAUS_BLOOD.get()));
            helper.assertTrue(!child.isDeadOrDying(), "control: the line died when he only fell");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(k, rig.whiteOakBlow()));
            helper.assertTrue(child.isDeadOrDying(), "a hybrid in Klaus's line did not die with him");
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.DEAD, "the index says " + rig.index.state(BossId.KLAUS));
            helper.assertTrue(rig.dropsNear(k, BossItems.KLAUS_BLOOD.get()) == 1, "the reward was paid twice");
        } finally {
            forget(helper, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKlausWhoDiesOfAnythingButWhiteOakTakesNoLineWithHim(GameTestHelper helper) {
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(child.getUUID(), BossTestRig.KLAUS, true);
            night(child);
            KlausEntity k = rig.spawnKlaus();
            k.desiccate();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(k, helper.getLevel().damageSources().genericKill()));
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.DEAD, "control: the index says " + rig.index.state(BossId.KLAUS));
            helper.assertTrue(!child.isDeadOrDying(), "a /kill on a fallen Klaus killed his line");
        } finally {
            forget(helper, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobsWhiteOakHitNeverCountsEvenKlausWithAStake(GameTestHelper helper) {
        LineTestPlayer original = LineTestPlayer.create(helper);
        LineTestPlayer child = LineTestPlayer.create(helper);
        BloodlineAccess.set(original, BloodlineAccess.get(original).toBuilder().species(Species.ORIGINAL).build());
        BloodlineAccess.set(child, BloodlineAccess.get(child).toBuilder().species(Species.HYBRID).build());
        try (BossTestRig rig = new BossTestRig(helper)) {
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder.record(original, child);
            night(original, child);
            KlausEntity k = rig.spawnKlaus();
            k.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BossItems.MIKAELS_STAKE.get()));
            BossesConfig.override("mikael_stakes_originals", true);
            helper.assertTrue(!OriginalRules.isWhiteOakKill(k.damageSources().mobAttack(k)), "Klaus's hit with a white oak stake counts as a white oak kill");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, k.damageSources().mobAttack(k)));
            helper.assertTrue(!child.isDeadOrDying(), "Klaus's hit killed an Original's line");
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, rig.whiteOakBlow()));
            helper.assertTrue(child.isDeadOrDying(), "control: a player's white oak blow did not kill the line");
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
            forget(helper, original, child);
        }
        helper.succeed();
    }

    // ---- the hunt ends ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKlausWhosePreyIsGoneWithdrawsAndMayComeAgain(GameTestHelper helper) {
        LineTestPlayer later = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            helper.getLevel().setDayTime(NIGHT);
            KlausEntity k = rig.spawnKlaus();
            k.setNoAi(true);
            k.hunt(UUID.randomUUID());
            run(k, KlausRules.WITHDRAW_AFTER + 40);
            helper.assertTrue(k.isRemoved(), "a Klaus whose prey is gone is still standing");
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.LOST, "the index says " + rig.index.state(BossId.KLAUS));
            BossesConfig.override("klaus_warning_seconds", 0);
            night(later);
            BossDirector.check(rig.server);
            helper.assertTrue(klausOf(rig) != null, "he could not come again");
        } finally {
            BossesConfig.override("klaus_warning_seconds", null);
            forget(helper, later);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aKlausWithPreyHereStaysAndOneWhoIsDesiccatedNeverWithdraws(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            helper.getLevel().setDayTime(NIGHT);
            Villager prey = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
            KlausEntity k = rig.spawnKlaus();
            k.setNoAi(true);
            k.hunt(prey.getUUID());
            run(k, KlausRules.WITHDRAW_AFTER + 40);
            helper.assertTrue(!k.isRemoved(), "he withdrew with his prey right there");
            k.hunt(UUID.randomUUID());
            k.desiccate();
            run(k, KlausRules.WITHDRAW_AFTER + 40);
            helper.assertTrue(!k.isRemoved(), "a fallen Original withdrew");
        }
        helper.succeed();
    }

    // ---- the hunt tells the target what they are (R165) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void beingHuntedRevealsTheGeneToTheTargetAndTheHowlIsForThemAlone(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        LineTestPlayer bystander = stranger(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().revealedGenes(java.util.Set.of()).build());
            BloodlineAccess.set(bystander, BloodlineAccess.get(bystander).toBuilder().revealedGenes(java.util.Set.of()).build());
            helper.assertTrue(!BloodlineAccess.get(p).revealedGenes().contains(Genes.DOPPELGANGER), "control: the gene was known already");
            night(p, bystander);
            BossDirector.useWorld(l -> List.of(p, bystander), null);
            BossDirector.check(rig.server);
            helper.assertTrue(BloodlineAccess.get(p).revealedGenes().contains(Genes.DOPPELGANGER), "the hunted player was not told, in the book, what they are");
            helper.assertTrue(p.received(UNKNOWN), "no message: " + p.heard());
            helper.assertTrue(p.notified().contains("minecraft:entity.wolf.howl"), "the howl did not reach the one he hunts: " + p.notified());
            helper.assertTrue(bystander.received() == 0 && bystander.notified().isEmpty(), "somebody who is not hunted was told or heard: " + bystander.heard());
            helper.assertTrue(!BloodlineAccess.get(bystander).revealedGenes().contains(Genes.DOPPELGANGER), "a bystander learned a gene");
        } finally {
            forget(helper, p, bystander);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theZeroWarningHuntRevealsTheGeneToo(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("klaus_warning_seconds", 0);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().revealedGenes(java.util.Set.of()).build());
            night(p);
            BossDirector.check(rig.server);
            helper.assertTrue(BloodlineAccess.get(p).revealedGenes().contains(Genes.DOPPELGANGER), "the gene was not revealed when he came at once");
            helper.assertTrue(p.notified().contains("minecraft:entity.wolf.howl"), "no howl for the target: " + p.notified());
        } finally {
            BossesConfig.override("klaus_warning_seconds", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- the warning counts down, and says when the hunt is off ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWarningRemindsAtThirtyAndTenSecondsOnceEach(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            long total = BossesConfig.klausWarningSeconds() * 20L;
            BossClock.offsetForTests(total - 40 * 20L);
            BossDirector.check(rig.server);
            helper.assertTrue(p.count(COUNTDOWN) == 0, "a reminder at 40 seconds");
            BossClock.offsetForTests(total - 29 * 20L);
            BossDirector.check(rig.server);
            BossDirector.check(rig.server);
            helper.assertTrue(p.count(COUNTDOWN) == 1, "the 30 s reminder was shown " + p.count(COUNTDOWN) + " times");
            helper.assertTrue(p.chat() == 2, "a reminder went to chat: " + p.chat());
            BossClock.offsetForTests(total - 9 * 20L);
            BossDirector.check(rig.server);
            BossDirector.check(rig.server);
            helper.assertTrue(p.count(COUNTDOWN) == 2 && p.heard().contains("[" + COUNTDOWN + "]10"), "the 10 s reminder: " + p.heard());
            helper.assertTrue(klausOf(rig) == null, "he came before the time was up");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHuntIsCalledOffAndTheTargetToldWheneverTheTargetStopsBeingOne(GameTestHelper helper) {
        String[] causes = {"died", "player hunt switched off", "peaceful", "spectator"};
        for (String cause : causes) {
            LineTestPlayer p = carrier(helper);
            try (BossTestRig rig = new BossTestRig(helper)) {
                night(p);
                BossDirector.check(rig.server);
                helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt > 0, cause + ": control: no arrival waits");
                switch (cause) {
                    case "died" -> p.setHealth(0.0F);
                    case "player hunt switched off" -> BossesConfig.override("klaus_hunts_players", false);
                    case "peaceful" -> BossDirector.useDifficulty(l -> net.minecraft.world.Difficulty.PEACEFUL);
                    default -> p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                }
                BossClock.offsetForTests(BossesConfig.klausWarningSeconds() * 20L + 5);
                BossDirector.check(rig.server);
                helper.assertTrue(klausOf(rig) == null, cause + ": he came anyway");
                helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1 && rig.index.of(BossId.KLAUS).target == null, cause + ": the arrival is still written down");
                helper.assertTrue(p.count(LOST_SCENT) == 1, cause + ": the target was not told the wolf lost the scent: " + p.heard());
            } finally {
                BossesConfig.override("klaus_hunts_players", null);
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHuntIsCalledOffAtOnceNotWhenTheTimeIsUp(GameTestHelper helper) {
        LineTestPlayer p = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(p);
            BossDirector.check(rig.server);
            BossesConfig.override("klaus_hunts_players", false);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.KLAUS).arriveAt == -1, "the arrival waited for its time although the target is no longer one");
            helper.assertTrue(p.count(LOST_SCENT) == 1, "no message: " + p.heard());
        } finally {
            BossesConfig.override("klaus_hunts_players", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- the hunt that goes nowhere ends ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void atDawnHeWithdrawsAndMayComeAgain(GameTestHelper helper) {
        LineTestPlayer prey = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(prey);
            helper.getLevel().setDayTime(NIGHT);
            KlausEntity k = rig.spawnKlaus();
            k.setNoAi(true);
            k.hunt(prey.getUUID());
            run(k, 40);
            helper.assertTrue(!k.isRemoved(), "control: he left in the middle of the night");
            helper.getLevel().setDayTime(MORNING);
            run(k, 12);
            helper.assertTrue(k.isRemoved(), "he is still here at dawn");
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.LOST, "the index says " + rig.index.state(BossId.KLAUS));
            helper.assertTrue(prey.count(DAWN) == 1, "the hunted player was not told: " + prey.heard());
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHuntWithoutAHitEndsAfterFourMinutesAndAHitStartsTheCountAgain(GameTestHelper helper) {
        LineTestPlayer prey = carrier(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(prey);
            helper.getLevel().setDayTime(NIGHT);
            KlausEntity k = rig.spawnKlaus();
            k.setNoAi(true);
            k.hunt(prey.getUUID());
            k.setPos(prey.getX() + 1, prey.getY(), prey.getZ());
            k.setInvulnerable(true); // thousands of ticks in a test structure: he must not suffocate in its floor
            run(k, KlausRules.GIVE_UP_AFTER - 400);
            helper.assertTrue(!k.isRemoved(), "he gave up too soon, sinceHit " + k.sinceHit());
            k.doHurtTarget(prey);
            run(k, KlausRules.GIVE_UP_AFTER - 400);
            helper.assertTrue(!k.isRemoved(), "a blow on his prey did not start the count again");
            run(k, 600);
            helper.assertTrue(k.isRemoved(), "he hunts for ever without a hit, sinceHit " + k.sinceHit());
            helper.assertTrue(rig.index.state(BossId.KLAUS) == BossState.LOST, "the index says " + rig.index.state(BossId.KLAUS));
            helper.assertTrue(prey.count(LOST_SCENT) == 1, "the hunted player was not told: " + prey.heard());
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowOnSomeoneElseDoesNotCountAsAHitOnHisPrey(GameTestHelper helper) {
        LineTestPlayer prey = carrier(helper);
        Villager other = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
        try (BossTestRig rig = new BossTestRig(helper)) {
            night(prey);
            helper.getLevel().setDayTime(NIGHT);
            KlausEntity k = rig.spawnKlaus();
            k.setNoAi(true);
            k.hunt(prey.getUUID());
            k.setPos(prey.getX() + 1, prey.getY(), prey.getZ());
            k.setInvulnerable(true); // thousands of ticks in a test structure: he must not suffocate in its floor
            run(k, KlausRules.GIVE_UP_AFTER - 400);
            k.doHurtTarget(other);
            run(k, 800);
            helper.assertTrue(k.isRemoved(), "a blow on a bystander kept the hunt alive");
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    // ---- a desiccated Klaus stays a man ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedKlausNeverShiftsEvenAgainstAWerewolf(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            k.desiccate();
            helper.assertTrue(k.getHealth() / k.getMaxHealth() <= ShiftRules.SHIFT_AT_HEALTH, "control: he is not at half health");
            for (int i = 0; i < 30; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.HUMAN && k.shiftTicks() == 0, "a fallen Klaus shifted: " + k.form());
            helper.assertTrue(k.isDesiccated() && k.isNoAi(), "control: he got up");
            k.recover();
            for (int i = 0; i < 12; i++) {
                k.tick();
            }
            helper.assertTrue(k.form() == BossForm.HUMAN, "full health after returning, and he shifted: " + k.form());
        }
        helper.succeed();
    }

    // ---- the swap ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBodyIsSwappedOnOneTickWithTheHitboxSmokeHowlAndShake(GameTestHelper helper) {
        List<String> seen = new java.util.ArrayList<>();
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            KlausEntity.observeSwap(e -> seen.add(e.form() + "@" + e.shiftTicks() + " h=" + e.getBbHeight()));
            k.startShift();
            for (int i = 1; i < ShiftRules.SWAP_TICK; i++) {
                k.tick();
                helper.assertTrue(Math.abs(k.getBbHeight() - 1.95F) < 1e-4, "the hitbox changed before the swap, at tick " + k.shiftTicks() + ": " + k.getBbHeight());
            }
            helper.assertTrue(seen.isEmpty(), "the swap happened early: " + seen);
            k.tick();
            helper.assertTrue(k.shiftTicks() == ShiftRules.SWAP_TICK && Math.abs(k.getBbHeight() - 1.5F) < 1e-4, "the hitbox is not the wolf's on the swap tick: " + k.getBbHeight() + " at " + k.shiftTicks());
            helper.assertTrue(seen.size() == 1 && seen.get(0).startsWith("SHIFTING@" + ShiftRules.SWAP_TICK) && seen.get(0).endsWith("h=1.5"), "the swap effects ran " + seen);
            for (int i = 0; i < ShiftRules.END_TICK; i++) {
                k.tick();
            }
            helper.assertTrue(seen.size() == 1 && k.form() == BossForm.BEAST, "the swap ran again, or the shift did not end: " + seen + " " + k.form());
        } finally {
            KlausEntity.observeSwap(null);
        }
        helper.succeed();
    }

    // ---- the animation he plays (the client reads this; the server can ask the same question) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDesiccatedWolfPlaysAndHoldsTheDeathAnimation(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            k.setForm(BossForm.BEAST);
            helper.assertTrue(!KlausRules.animation(k.isDeadOrDying(), k.isDesiccated(), false, false, 0.0).equals("death"), "control: a standing wolf plays death");
            k.desiccate();
            helper.assertTrue(KlausRules.animation(k.isDeadOrDying(), k.isDesiccated(), false, false, 0.0).equals("death"), "a desiccated wolf does not play death");
        }
        helper.succeed();
    }

    // ---- Klaus's Blood: its tooltip, and when his line is gone ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void klausBloodSaysWhatItDoes(GameTestHelper helper) {
        ItemStack blood = new ItemStack(BossItems.KLAUS_BLOOD.get());
        List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        blood.getItem().appendHoverText(blood, net.minecraft.world.item.Item.TooltipContext.of(helper.getLevel()), lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        helper.assertTrue(lines.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("item.supernaturalbloodlines.klaus_blood.tooltip")),
                "no tooltip: " + lines);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withHisLineDeadKlausBloodStillCuresButTurnsNobody(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).markDead(BossTestRig.KLAUS);
            p.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, Venom.DURATION_TICKS));
            ItemStack blood = new ItemStack(BossItems.KLAUS_BLOOD.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, blood.finishUsingItem(helper.getLevel(), p));
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.HYBRID_VENOM), "it did not cure the venom");
            helper.assertTrue(!p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && !p.hasData(SpeciesRegistries.BLOOD_SOURCE), "it marked the drinker for a sire who is gone");
            helper.assertTrue(p.received("item.supernaturalbloodlines.klaus_blood.dead_line"), "the player was not told: " + p.heard());
            helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not given back");
        } finally {
            LineIndex.get(helper.getLevel().getServer()).unmarkDead(BossTestRig.KLAUS);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- the mountain ash claim, as it really is ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyAPackEmissarysLineHoldsKlausMikaelElijahRebekahKolAndFinnAPlainOneDoesNot(GameTestHelper helper) {
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "mountain_ash_line"))) {
            helper.succeed(); // the artifacts part is off: there is no line to test
            return;
        }
        ServerPlayer vampire = vampire(helper);
        try (BossTestRig rig = new BossTestRig(helper)) {
            KlausEntity k = rig.spawnKlaus();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity m = rig.spawn();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity elijah = rig.spawnElijah();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity rebekah = rig.spawnRebekah();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity kol = rig.spawnKol();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity finn = rig.spawnFinn();
            BlockPos at = new BlockPos(6, 1, 6);
            helper.setBlock(at, net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh.LINE.get().defaultBlockState());
            BlockPos abs = helper.absolutePos(at);
            var level = helper.getLevel();
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(vampire, level, abs), "control: a plain line does not hold a vampire");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(k, level, abs), "a plain line holds Klaus: the hint can say so");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(m, level, abs), "a plain line holds Mikael: the hint can say so");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(elijah, level, abs), "a plain line holds Elijah");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(rebekah, level, abs), "a plain line holds Rebekah");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(kol, level, abs), "a plain line holds Kol");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(finn, level, abs), "a plain line holds Finn");
            ((net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity) level.getBlockEntity(abs)).setEmissary(true);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(k, level, abs), "an Emissary's line does not hold Klaus");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(m, level, abs), "an Emissary's line does not hold Mikael");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(elijah, level, abs), "an Emissary's line does not hold Elijah");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(rebekah, level, abs), "an Emissary's line does not hold Rebekah");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(kol, level, abs), "an Emissary's line does not hold Kol");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlock.blocks(finn, level, abs), "an Emissary's line does not hold Finn");
        }
        helper.succeed();
    }
    // ---- the operator ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandSpawnsKlausOnlyWhereHisModsAreThere(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            BossDirector.useMods(b -> false);
            try {
                helper.assertTrue(commands.getDispatcher().execute("supernaturalbloodlines boss klaus spawn", source) == 0, "the spawn command was not refused without Werewolves");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException bad) {
                throw new AssertionError(bad);
            }
            helper.assertTrue(klausOf(rig) == null, "the spawn command made him without Werewolves");
            BossDirector.useMods(b -> true);
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss klaus spawn");
            KlausEntity k = klausOf(rig);
            helper.assertTrue(k != null && k.isAlive(), "the spawn command made no Klaus");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss klaus desiccate");
            helper.assertTrue(k.isDesiccated(), "the desiccate command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss klaus return");
            helper.assertTrue(!k.isDesiccated() && rig.index.state(BossId.KLAUS) == BossState.ACTIVE, "the return command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss klaus reset");
            helper.assertTrue(!k.isAlive() && rig.index.state(BossId.KLAUS) == BossState.UNSEEN, "the reset left him or his record");
        }
        helper.succeed();
    }
}
