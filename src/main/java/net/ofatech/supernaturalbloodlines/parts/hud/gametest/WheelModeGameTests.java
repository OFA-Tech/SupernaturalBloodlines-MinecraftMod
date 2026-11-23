package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import com.mojang.serialization.Codec;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.IAbility;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetPowerModePayload;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraModes;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelModes;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;

/** Mode changes from the wheel: a slotted skill goes through Tensura's own slot mode, a skill in no slot through our own memory. Needs Tensura. */
@PrefixGameTestTemplate(false)
public final class WheelModeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation MULTI = ResourceLocation.fromNamespaceAndPath("tensura", "gravity_field");

    private WheelModeGameTests() {
    }

    private static PowerRef ref(ResourceLocation id) {
        return new PowerRef(PowerSource.TENSURA, id);
    }

    /** A skill with several modes whose class does not override {@code nextMode} (Tensura's base answer is always mode 0), if there is one. */
    private static ResourceLocation baseNextModeSkill() {
        for (ManasSkill s : SkillAPI.getSkillRegistry()) {
            if (s instanceof TensuraSkill) {
                ResourceLocation id = SkillAPI.getSkillRegistry().getId(s);
                ManasSkillInstance probe = s.createDefaultInstance();
                if (probe.getModes() > 1) {
                    boolean overrides = false;
                    for (Class<?> c = s.getClass(); c != null && c != TensuraSkill.class; c = c.getSuperclass()) {
                        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                            overrides |= m.getName().equals("nextMode");
                        }
                    }
                    if (!overrides) {
                        return id;
                    }
                }
            }
        }
        return null;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void slottedSkillModeChangeUsesTensurasOwnSlotMode(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, MULTI);
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        ab.setActivePreset(0);
        ab.setAbilitySlot(0, 0, inst.getSkill(), 0);
        ab.setAbilitySlot(1, 2, inst.getSkill(), 0);
        int before = TensuraModes.current(p, inst);
        String refusal = WheelService.changeMode(p, ref(MULTI), 1);
        helper.assertTrue(refusal == null, "the mode change was refused: " + refusal);
        int first = ab.getAbilitySlot(0, 0).getMode();
        int second = ab.getAbilitySlot(1, 2).getMode();
        helper.assertTrue(first != before, "the active slot's mode did not move: " + first);
        helper.assertTrue(first == second, "the second slot holding it disagrees: " + first + " vs " + second);
        helper.assertTrue(WheelModes.get(p, MULTI) == 0, "our own memory was used for a skill that is in a slot");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unslottedModeIsStoredAndSurvivesRelog(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, MULTI);
        helper.assertTrue(WheelService.changeMode(p, ref(MULTI), 1) == null, "the mode change was refused");
        int now = TensuraModes.current(p, inst);
        helper.assertTrue(now != 0 && WheelModes.get(p, MULTI) == now, "stored " + WheelModes.get(p, MULTI) + " current " + now);
        // what a relog does: the attachment goes through its codec
        Codec<Map<ResourceLocation, Integer>> codec = Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT);
        Tag saved = codec.encodeStart(NbtOps.INSTANCE, p.getData(net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments.WHEEL_MODES)).getOrThrow();
        Map<ResourceLocation, Integer> loaded = codec.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.getOrDefault(MULTI, -1) == now, "the mode was not saved: " + loaded);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void modeIndexOutOfRangeIsClamped(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, MULTI);
        helper.assertTrue(WheelModes.set(p, MULTI, 99, 3) == 2, "a mode over the end");
        helper.assertTrue(WheelModes.set(p, MULTI, -5, 3) == 0, "a negative mode");
        helper.assertTrue(WheelModes.set(p, MULTI, 1, 0) == 0, "no modes at all");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noUnlockedModeSaysSoAndChangesNothing(GameTestHelper helper) {
        ResourceLocation base = baseNextModeSkill();
        if (base == null) {
            helper.succeed(); // every multi-mode skill of this Tensura overrides nextMode: nothing to test
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, base);
        // Tensura's base nextMode always answers mode 0: from mode 0 there is no other mode
        String refusal = WheelService.changeMode(p, ref(base), 1);
        helper.assertTrue(refusal != null && refusal.endsWith("no_mode"), base + ": " + refusal);
        helper.assertTrue(WheelModes.get(p, base) == 0, "a mode was stored for " + base);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillWithOneModeHasNoModeToChange(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, ResourceLocation.fromNamespaceAndPath("tensura", "coercion"));
        String refusal = WheelService.changeMode(p, ref(ResourceLocation.fromNamespaceAndPath("tensura", "coercion")), 1);
        helper.assertTrue(refusal != null && refusal.endsWith("no_modes"), "coercion: " + refusal);
        String missing = WheelService.changeMode(p, ref(ResourceLocation.fromNamespaceAndPath("tensura", "corrosion")), 1);
        helper.assertTrue(missing != null && missing.endsWith("not_unlocked"), "an unlearned skill: " + missing);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deltaZeroOrHugeIsTreatedAsItsSign(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, MULTI);
        WheelModes.set(p, MULTI, 1, 3);
        int middle = TensuraModes.current(p, inst);
        helper.assertTrue(WheelService.changeMode(p, ref(MULTI), 1) == null, "+1 refused");
        int up = TensuraModes.current(p, inst);
        WheelModes.set(p, MULTI, 1, 3);
        helper.assertTrue(WheelService.changeMode(p, ref(MULTI), Integer.MAX_VALUE) == null, "huge positive refused");
        helper.assertTrue(TensuraModes.current(p, inst) == up, "a huge delta is not +1");
        WheelModes.set(p, MULTI, 1, 3);
        helper.assertTrue(WheelService.changeMode(p, ref(MULTI), 0) == null, "zero refused");
        helper.assertTrue(TensuraModes.current(p, inst) == up, "zero is not treated as +1");
        WheelModes.set(p, MULTI, 1, 3);
        WheelService.changeMode(p, ref(MULTI), -1);
        int down = TensuraModes.current(p, inst);
        WheelModes.set(p, MULTI, 1, 3);
        WheelService.changeMode(p, ref(MULTI), Integer.MIN_VALUE);
        helper.assertTrue(TensuraModes.current(p, inst) == down, "a huge negative delta is not -1");
        helper.assertTrue(up != middle || down != middle, "setup: the mode never moved");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aModeChangeIsRateLimitedAndNeedsThePowerLooked(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, MULTI);
        HudNet.forgetRateLimit(p);
        int limited = 0;
        for (int i = 0; i < 14; i++) {
            if (HudNet.RATE_LIMITED.equals(HudNet.handleMode(p, new SetPowerModePayload(ref(MULTI).key(), 1)))) {
                limited++;
            }
        }
        helper.assertTrue(limited == 4, "mode changes dropped: " + limited);
        helper.succeed();
    }
}
