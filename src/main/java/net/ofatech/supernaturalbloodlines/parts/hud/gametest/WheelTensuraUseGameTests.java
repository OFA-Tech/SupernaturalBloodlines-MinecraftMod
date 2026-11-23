package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.UsePowerPayload;
import net.ofatech.supernaturalbloodlines.parts.hud.tensura.WheelActivation;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraProvider;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraUse;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;

/**
 * The spike of plan W1 task 6 and the Tensura half of task 7: can the wheel start, hold and end a Tensura skill, spell or art that is in no preset slot, with nothing parked in a
 * hidden slot? Every test ticks the player as the game does ({@link WheelTestKit#tick}: NeoForge's pre event, ManasCore's own living tick, the post event). Needs Tensura.
 */
@PrefixGameTestTemplate(false)
public final class WheelTensuraUseGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final java.util.UUID OWNER = java.util.UUID.nameUUIDFromBytes("wheel-test".getBytes());

    private WheelTensuraUseGameTests() {
    }

    private static ResourceLocation id(String path) {
        return WheelTensuraGameTests.id(path);
    }

    private static ManasSkill skill(String path) {
        return SkillAPI.getSkillRegistry().get(id(path));
    }

    private static PowerRef ref(String path) {
        return new PowerRef(PowerSource.TENSURA, id(path));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedInstantSkillActivates(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("coercion"));
        SkillStorage st = SkillAPI.getSkillsFrom(p);
        helper.assertTrue(!TensuraStorages.getAbilityFrom(p).isAbilityInPresets(inst.getSkill()), "setup: the skill is in a preset");
        helper.assertTrue(!st.startHoldSkill(inst, 0, 0), "the control: Tensura accepted a skill that is in no slot, so the test proves nothing");
        String refusal = TensuraUse.press(p, id("coercion"), 0);
        helper.assertTrue(refusal == null, "the wheel's press was refused: " + refusal);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void scopeDoesNotLeakToOtherSkills(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance a = WheelTestKit.learn(p, id("coercion"));
        ManasSkillInstance b = WheelTestKit.learn(p, id("corrosion"));
        SkillStorage st = SkillAPI.getSkillsFrom(p);
        boolean other = WheelActivation.scope(p.getUUID(), a.getSkill(), 0, () -> st.startHoldSkill(b, 0, 0));
        helper.assertTrue(!other, "inside a scope for coercion, corrosion was accepted");
        helper.assertTrue(!st.startHoldSkill(a, 0, 0), "after the scope closed, coercion was still accepted");
        helper.assertTrue(WheelActivation.scope(p.getUUID(), a.getSkill(), 0, () -> st.startHoldSkill(a, 0, 0)), "inside its own scope coercion was refused");
        helper.assertTrue(!WheelActivation.allows(p.getUUID(), a.getSkill(), 0, 0), "the scope leaked");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void scopeIsRestoredAfterAnException(GameTestHelper helper) {
        ManasSkill s = skill("coercion");
        try {
            WheelActivation.scope(OWNER, s, 0, () -> {
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            // the point is what happens next
        }
        helper.assertTrue(!WheelActivation.allows(OWNER, s, 0, 0), "the scope stayed open after an exception");
        helper.assertTrue(WheelActivation.scope(OWNER, s, 0, () -> WheelActivation.scope(OWNER, skill("corrosion"), 1, () -> WheelActivation.allows(OWNER, s, 0, 0))), "an outer scope was lost by a nested one");
        helper.assertTrue(!WheelActivation.allows(OWNER, s, 0, 0), "nested scopes leaked");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aScopeForOneModeDoesNotAllowAnother(GameTestHelper helper) {
        ManasSkill s = skill("gravity_field");
        helper.assertTrue(WheelActivation.scope(OWNER, s, 1, () -> WheelActivation.allows(OWNER, s, 0, 1) && WheelActivation.allows(OWNER, s, 0, -1) && !WheelActivation.allows(OWNER, s, 0, 2)), "mode rule");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mixinOffLocksUnslotted(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("coercion"));
        HudConfig.NEXT_PRESET_ACTIVE.set(false);
        try {
            String refusal = TensuraUse.press(p, id("coercion"), 0);
            helper.assertTrue(refusal != null && refusal.equals("supernaturalbloodlines.wheel.refused.tensura"), "press with the hook off: " + refusal);
            helper.assertTrue(!inst.onCoolDown(0) && !inst.isToggled(), "something ran");
            helper.assertTrue(WheelCatalogueTensura.lock(p, id("coercion")) == net.ofatech.supernaturalbloodlines.core.wheel.LockReason.NEEDS_SLOT, "the entry is not locked NEEDS_SLOT");
        } finally {
            HudConfig.NEXT_PRESET_ACTIVE.set(true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedConfigOffLocksUnslotted(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("coercion"));
        HudConfig.WHEEL_UNSLOTTED_TENSURA.set(false);
        try {
            String refusal = TensuraUse.press(p, id("coercion"), 0);
            helper.assertTrue(refusal != null && refusal.endsWith("refused.tensura"), "press with wheel_unslotted_tensura off: " + refusal);
        } finally {
            HudConfig.WHEEL_UNSLOTTED_TENSURA.set(true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedToggleSurvivesFortyTicks(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("chant_annulment"));
        helper.assertTrue(TensuraUse.press(p, id("chant_annulment"), 0) == null, "the toggle was refused");
        helper.assertTrue(inst.isToggled(), "the toggle did not switch on");
        WheelTestKit.tick(p, 40);
        helper.assertTrue(inst.isToggled(), "the toggle was switched off by Tensura within 40 ticks");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPassiveSwitchFlipsThroughTensurasOwnToggle(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("self_regeneration"));
        helper.assertTrue(TensuraUse.press(p, id("self_regeneration"), 0) == null && inst.isToggled(), "the switch did not turn on");
        WheelTestKit.tick(p, 40);
        helper.assertTrue(inst.isToggled(), "the switch did not survive 40 ticks");
        helper.assertTrue(TensuraUse.press(p, id("self_regeneration"), 0) == null && !inst.isToggled(), "the second press did not turn it off");
        String resist = (WheelTestKit.learn(p, id("cold_resistance")) != null) ? TensuraUse.press(p, id("cold_resistance"), 0) : null;
        helper.assertTrue(resist != null, "a resistance was switched from the wheel");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heldSkillPressAndReleaseAreSymmetric(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        ManasSkill s = skill("gravity_flight");
        helper.assertTrue(TensuraUse.press(p, id("gravity_flight"), 0) == null, "the press was refused");
        helper.assertTrue(TickingSkill.isTickingSkill(p, s, 0), "the skill is not held after the press");
        TensuraUse.release(p, id("gravity_flight"), 0);
        helper.assertTrue(!TickingSkill.isTickingSkill(p, s, 0), "the release (key 0, as the press) did not end the hold");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedHeldSkillRunsPastTwentyTicks(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        ManasSkill s = skill("gravity_flight");
        helper.assertTrue(TensuraUse.press(p, id("gravity_flight"), 0) == null, "the press was refused");
        WheelTestKit.tick(p, 60);
        helper.assertTrue(TickingSkill.isTickingSkill(p, s, 0), "a held skill in no slot was cut off within 60 ticks");
        TensuraUse.release(p, id("gravity_flight"), 0);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutTheBracketAHeldSkillIsCutOff(GameTestHelper helper) {
        // the control of the bracket: with the player-tick scope removed the same skill dies at about 20 ticks, so the bracket is what keeps it alive
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        ManasSkill s = skill("gravity_flight");
        helper.assertTrue(TensuraUse.press(p, id("gravity_flight"), 0) == null, "the press was refused");
        WheelActivation.stopAll(p);
        for (int i = 0; i < 60; i++) {
            io.github.manasmods.manascore.skill.api.EntityEvents.LIVING_POST_TICK.invoker().tick(p);
        }
        helper.assertTrue(!TickingSkill.isTickingSkill(p, s, 0), "the skill ran 60 ticks with no bracket: the bracket is not what keeps it alive");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedLongSpellCompletes(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("magic_wall"));
        ManasSkill s = skill("magic_wall");
        helper.assertTrue(TensuraUse.press(p, id("magic_wall"), 0) == null, "the spell was refused");
        WheelTestKit.tick(p, 60);
        helper.assertTrue(TickingSkill.isTickingSkill(p, s, 0), "a spell with a cast time was cut off within 60 ticks");
        TensuraUse.release(p, id("magic_wall"), 0);
        helper.assertTrue(!TickingSkill.isTickingSkill(p, s, 0), "released early the spell is still held");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heldBracketEndsOnReleaseDeathAndLogout(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        ManasSkill s = skill("gravity_flight");
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, false) == null, "the press was refused");
        helper.assertTrue(WheelActivation.isHolding(p, s), "the bracket did not open");
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, true) == null, "the release answered");
        helper.assertTrue(!WheelActivation.isHolding(p, s), "the bracket stayed after the release");
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, false) == null, "the second press was refused");
        WheelService.forget(p); // death, logout and a dimension change all end here
        helper.assertTrue(!WheelActivation.isHolding(p, s), "the bracket stayed after forget");
        helper.assertTrue(!TickingSkill.isTickingSkill(p, s, 0), "the skill is still held after forget");
        helper.assertTrue(!WheelService.heldTensura(p, ref("gravity_flight")), "the service still thinks it is held");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void raceAbilityStartsAndReleasesWithoutASlot(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(p).setRace(id("harpy"), false);
        String refusal = WheelService.use(p, new PowerRef(PowerSource.TENSURA, TensuraProvider.RACE_ABILITY), 0, -1, false);
        helper.assertTrue(refusal == null, "the race ability was refused: " + refusal);
        helper.assertTrue(RaceAPI.getRaceFrom(p).hasHeldAbility(), "the race ability is not held");
        WheelService.use(p, new PowerRef(PowerSource.TENSURA, TensuraProvider.RACE_ABILITY), 0, -1, true);
        helper.assertTrue(!RaceAPI.getRaceFrom(p).hasHeldAbility(), "the release did not end the race ability");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forgetReleasesAHeldTensuraSkill(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, false) == null, "the press was refused");
        WheelService.forget(p);
        helper.assertTrue(!TickingSkill.isTickingSkill(p, skill("gravity_flight"), 0), "a held Tensura skill survived forget");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTensuraUseOfASkillThePlayerLacksIsRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        String refusal = WheelService.use(p, ref("coercion"), 0, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "an unlearned skill: " + refusal);
        WheelTestKit.learn(p, id("coercion"));
        helper.assertTrue(WheelService.use(p, ref("coercion"), 0, -1, false) == null, "after learning it, the use was refused");
        SkillAPI.getSkillsFrom(p).forgetSkill(skill("coercion"));
        String stale = WheelService.use(p, ref("coercion"), 0, -1, false);
        helper.assertTrue(stale != null && stale.endsWith("not_unlocked"), "a skill forgotten while the wheel was open: " + stale);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aForgedModeIsRefusedAndNothingRuns(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("gravity_field"));
        for (int bad : new int[] {-1, 3, 99, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            String refusal = WheelService.use(p, ref("gravity_field"), bad, -1, false);
            helper.assertTrue(refusal != null && refusal.endsWith("bad_mode"), "mode " + bad + ": " + refusal);
        }
        helper.assertTrue(!inst.onCoolDown(0) && !inst.onCoolDown(1) && !inst.onCoolDown(2), "a forged mode started something");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTensuraPressFloodIsDropped(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("coercion"));
        HudNet.forgetRateLimit(p);
        int dropped = 0;
        for (int i = 0; i < 14; i++) {
            if (HudNet.RATE_LIMITED.equals(HudNet.handleUse(p, new UsePowerPayload(ref("coercion").key(), 0, -1, false)))) {
                dropped++;
            }
        }
        helper.assertTrue(dropped == 4, "dropped " + dropped);
        helper.succeed();
    }

    /** The wheel passes key number 0 and its own mode: a skill slotted in slot 2, or slotted in another mode, must still start. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void slottedSkillInSlotTwoActivatesFromTheWheel(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("coercion"));
        SkillStorage st = SkillAPI.getSkillsFrom(p);
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 1, inst.getSkill(), 0);
        helper.assertTrue(!TensuraProvider.unslotted(p, inst.getSkill()), "setup: the skill is not slotted");
        helper.assertTrue(!st.startHoldSkill(inst, 0, 0), "the control: Tensura accepted key 0 for a skill in slot 2, so the test proves nothing");
        String refusal = TensuraUse.press(p, id("coercion"), 0);
        helper.assertTrue(refusal == null, "a skill in slot 2 was refused from the wheel: " + refusal);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillSlottedInAnotherModeActivatesFromTheWheel(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("gravity_field"));
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 2, inst.getSkill(), 0);
        helper.assertTrue(!SkillAPI.getSkillsFrom(p).startHoldSkill(inst, 0, 2), "the control: Tensura accepted another mode of the slotted skill");
        String refusal = TensuraUse.press(p, id("gravity_field"), 2);
        helper.assertTrue(refusal == null, "mode 2 of a skill slotted in mode 0 was refused from the wheel: " + refusal);
        helper.succeed();
    }

    /** The scope is the first player's: a second player who has the same skill does not get it "in the slot". */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherPlayerWithTheSameSkillIsNotLetThrough(GameTestHelper helper) {
        ServerPlayer one = TestPlayers.create(helper);
        ServerPlayer two = TestPlayers.create(helper);
        ManasSkillInstance a = WheelTestKit.learn(one, id("coercion"));
        ManasSkillInstance b = WheelTestKit.learn(two, id("coercion"));
        io.github.manasmods.tensura.storage.ability.IAbility abOne = TensuraStorages.getAbilityFrom(one);
        io.github.manasmods.tensura.storage.ability.IAbility abTwo = TensuraStorages.getAbilityFrom(two);
        boolean[] seen = new boolean[4];
        WheelActivation.scope(one.getUUID(), a.getSkill(), 0, () -> {
            seen[0] = abOne.isAbilityInActivePreset(0, a.getSkill(), 0);
            seen[1] = abTwo.isAbilityInActivePreset(0, b.getSkill(), 0);
            seen[2] = abTwo.isAbilityInActivePreset(b.getSkill());
            seen[3] = SkillAPI.getSkillsFrom(two).startHoldSkill(b, 0, 0);
            return null;
        });
        helper.assertTrue(seen[0], "the scoped player's own check was refused");
        helper.assertTrue(!seen[1] && !seen[2] && !seen[3], "a second player with the same skill got it in the slot: " + java.util.Arrays.toString(seen));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHoldBracketIsOnlyOpenAroundItsOwnPlayersTick(GameTestHelper helper) {
        ServerPlayer one = TestPlayers.create(helper);
        ServerPlayer two = TestPlayers.create(helper);
        WheelTestKit.learn(one, id("gravity_flight"));
        ManasSkillInstance b = WheelTestKit.learn(two, id("gravity_flight"));
        helper.assertTrue(TensuraUse.press(one, id("gravity_flight"), 0) == null, "the press was refused");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(one));
        boolean own = WheelActivation.allows(one.getUUID(), b.getSkill(), 0, 0);
        boolean other = WheelActivation.allows(two.getUUID(), b.getSkill(), 0, 0);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(one));
        helper.assertTrue(own, "the bracket is not open for the player who holds the skill");
        helper.assertTrue(!other, "the bracket of one player is open for another who has the same skill");
        TensuraUse.release(one, id("gravity_flight"), 0);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondPressOfAHeldPowerIsRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("gravity_flight"));
        ManasSkill s = skill("gravity_flight");
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, false) == null, "the first press was refused");
        String second = WheelService.use(p, ref("gravity_flight"), 0, -1, false);
        helper.assertTrue(second != null && second.endsWith("already_held"), "a second press of a held power: " + second);
        // the release handle is still the first press's: one release ends it
        WheelService.use(p, ref("gravity_flight"), 0, -1, true);
        helper.assertTrue(!TickingSkill.isTickingSkill(p, s, 0), "one release did not end the hold after a refused second press");
        helper.assertTrue(WheelService.use(p, ref("gravity_flight"), 0, -1, false) == null, "the power could not be used again after its release");
        WheelService.use(p, ref("gravity_flight"), 0, -1, true);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownTheWheelShowsIsTheFullOneAndDoesNotMove(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("coercion"));
        long[] now = {2000};
        net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.setNowForTest(() -> now[0]);
        try {
            net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.of(p).notePress(ref("coercion").key(), 2000, 400);
            inst.setCoolDown(20, 0);
            helper.assertTrue(inst.onCoolDown(0) && inst.getCoolDown(0) == 20, "setup: cooldown " + inst.getCoolDown(0));
            var first = WheelTensuraGameTests.entry(p, id("coercion")).orElseThrow().state();
            helper.assertTrue(first.cooldownEnd() == 2400 && first.cooldownTotal() == 400, "end " + first.cooldownEnd() + " total " + first.cooldownTotal());
            now[0] = 2100;
            inst.setCoolDown(15, 0);
            var later = WheelTensuraGameTests.entry(p, id("coercion")).orElseThrow().state();
            helper.assertTrue(later.equals(first), "a running cooldown changed the entry: " + first + " then " + later);
            helper.assertTrue(later.cooldownLeft(2100) == 300, "left " + later.cooldownLeft(2100));
        } finally {
            net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.setNowForTest(null);
        }
        helper.succeed();
    }

    /** A catalogue lock, read the way the wheel reads it. */
    private static final class WheelCatalogueTensura {
        static net.ofatech.supernaturalbloodlines.core.wheel.LockReason lock(ServerPlayer p, ResourceLocation id) {
            return WheelTensuraGameTests.entry(p, id).orElseThrow().state().lock();
        }
    }
}
