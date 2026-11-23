package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import com.google.gson.JsonParser;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.event.ActionEvent;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.GroupClock;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.OverlapTable;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SideEvent;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraUse;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelCatalogue;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelGate;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/**
 * Rule C on the server: no stacking and one shared cooldown on every path (our slot, the wheel, Vampirism's own toggle, Tensura's own packets), and one entry in the catalogue. The
 * tests use their own small table (a Vampirism lasting action as the main side against a Tensura skill or an action of ours), so a change to the shipped table never breaks them.
 * Needs Vampirism and Tensura; skips cleanly without them.
 */
@PrefixGameTestTemplate(false)
public final class WheelOverlapGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String CHANT = "tensura/tensura:chant_annulment";
    private static final String COERCION = "tensura/tensura:coercion";
    private static final String TEST_ACTION = "bloodline/" + NS + ":wheel_test_action";

    private WheelOverlapGameTests() {
    }

    private static OverlapTable table(String mainVampirism, String... others) {
        StringBuilder members = new StringBuilder("\"vampirism/" + mainVampirism + "\"");
        for (String o : others) {
            members.append(",\"").append(o).append("\"");
        }
        String json = "{\"format\":1,\"groups\":[{\"id\":\"g\",\"research\":\"T\",\"confidence\":\"same\",\"main\":0,\"members\":[" + members + "]}]}";
        return OverlapTable.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    private static PowerRef ref(String key) {
        return PowerRef.parse(key).orElseThrow();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.parse(path);
    }

    private static final List<String> LASTING = List.of("vampirism:regen", "vampirism:vampire_rage", "vampirism:vampire_invisibility", "vampirism:bat");

    /** Unlocks a lasting action of the vampire and checks it can be used now. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceLocation lasting(GameTestHelper helper, ServerPlayer p, int skip) {
        IFactionPlayer fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        int seen = 0;
        for (String s : LASTING) {
            IAction action = FactionActions.byId(id(s));
            if (action == null) {
                continue;
            }
            ((IActionHandler) fp.getActionHandler()).unlockActions(List.of(action));
            if (action.canUse(fp) == IAction.PERM.ALLOWED && action.getCooldown(fp) > 0) {
                if (seen++ == skip) {
                    return id(s);
                }
            }
        }
        helper.fail("setup: no usable lasting Vampirism action #" + skip);
        throw new IllegalStateException();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IActionHandler handler(ServerPlayer p) {
        return (IActionHandler) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getActionHandler();
    }

    /** Vampirism's own key: its toggle, nothing of ours in front of it. */
    private static void theirKey(ServerPlayer p, ResourceLocation action) {
        FactionActions.toggle(p, action, new IAction.ActivationContext() {
            @Override
            public Optional<net.minecraft.core.BlockPos> targetBlock() {
                return Optional.empty();
            }

            @Override
            public Optional<net.minecraft.world.entity.Entity> targetEntity() {
                return Optional.empty();
            }
        });
        WheelGate.settle(p);
    }

    private static int mainCooldown(ServerPlayer p, ResourceLocation action) {
        return FactionActions.byId(action) == null ? 0 : ((IAction) FactionActions.byId(action)).getCooldown(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow());
    }

    private static ServerPlayer vampire(GameTestHelper helper, OverlapTable t) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        WheelGate.setTableForTest(t);
        WheelGate.resetForTest(p);
        return p;
    }

    private static void done(GameTestHelper helper, ServerPlayer p) {
        WheelGate.setTableForTest(null);
        WheelGate.resetForTest(p);
        helper.succeed();
    }

    private static boolean chantOn(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(id("tensura:chant_annulment")).map(ManasSkillInstance::isToggled).orElse(false);
    }

    private static void failWith(GameTestHelper helper, ServerPlayer p, String message) {
        WheelGate.setTableForTest(null);
        WheelGate.resetForTest(p);
        helper.fail(message);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void secondSideIsRefusedWhileTheFirstIsOn(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), CHANT));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:chant_annulment"));
        // the Tensura side is on: Vampirism's own key is cancelled by its event (before the action was ever used, so no cooldown of its own is in the way)
        if (WheelService.use(p, ref(CHANT), 0, -1, false) != null || !chantOn(p)) {
            failWith(helper, p, "setup: the Tensura side did not start with nothing else on");
            return;
        }
        WheelGate.settle(p);
        theirKey(p, x);
        if (ActionGameTests.active(p, x)) {
            failWith(helper, p, "Vampirism's own key started the main side while the Tensura side is on (its event was not cancelled)");
            return;
        }
        // from the wheel too
        String viaWheel = WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, x), 0, -1, false);
        if (viaWheel == null || !viaWheel.endsWith("group_active") || ActionGameTests.active(p, x)) {
            failWith(helper, p, "the wheel started the main side while the Tensura side is on: " + viaWheel);
            return;
        }
        // Tensura side off, main side on: the other way round, from the wheel and from Tensura's own toggle
        RequestAbilityModeChangePacket.toggleAbilityPacket(id("tensura:chant_annulment")).toggleAbility(p);
        WheelGate.settle(p);
        if (chantOn(p)) {
            failWith(helper, p, "setup: the Tensura side did not turn off");
            return;
        }
        WheelGate.resetForTest(p);
        theirKey(p, x);
        if (!ActionGameTests.active(p, x)) {
            failWith(helper, p, "setup: the main side did not start with nothing else on");
            return;
        }
        String wheel = WheelService.use(p, ref(CHANT), 0, -1, false);
        if (wheel == null || !wheel.endsWith("group_active") || chantOn(p)) {
            failWith(helper, p, "the wheel started the second side while the first is on: " + wheel + " on=" + chantOn(p));
            return;
        }
        RequestAbilityModeChangePacket.toggleAbilityPacket(id("tensura:chant_annulment")).toggleAbility(p);
        if (chantOn(p)) {
            failWith(helper, p, "Tensura's own toggle started the second side while the first is on");
            return;
        }
        done(helper, p);
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void firstSideOffStartsTheGroupCooldownOnTheSecond(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), CHANT));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:chant_annulment"));
        theirKey(p, x);
        helper.assertTrue(WheelGate.clock(p).cooldownLeft("g", net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p)) == 0, "a lasting side started a cooldown when it went on");
        theirKey(p, x); // off
        int left = WheelGate.clock(p).cooldownLeft("g", net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p));
        int main = mainCooldown(p, x);
        if (main <= 0 || left != main) {
            failWith(helper, p, "the group cooldown after the side ended is " + left + ", the main side's own is " + main);
            return;
        }
        String refusal = WheelService.use(p, ref(CHANT), 0, -1, false);
        if (refusal == null || !refusal.endsWith("group_cooldown") || chantOn(p)) {
            failWith(helper, p, "the other side was not refused during the shared cooldown: " + refusal);
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lastingEndCooldownIsRaisedToTheGroupFloor(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation a = lasting(helper, p, 0);
        ResourceLocation b = lasting(helper, p, 1);
        // the main side is the one with the longer cooldown, the side that is used has the shorter one
        boolean aLonger = mainCooldown(p, a) >= mainCooldown(p, b);
        ResourceLocation main = aLonger ? a : b;
        ResourceLocation side = aLonger ? b : a;
        if (mainCooldown(p, main) <= mainCooldown(p, side)) {
            failWith(helper, p, "setup: two actions with the same cooldown");
            return;
        }
        WheelGate.setTableForTest(table(main.toString(), "vampirism/" + side));
        WheelGate.resetForTest(p);
        // the side is on; it ends with a cooldown of its own that is shorter than the main side's: the event is raised to the main side's
        theirKey(p, side);
        IAction action = FactionActions.byId(side);
        IFactionPlayer<?> fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        ActionEvent.ActionDeactivatedEvent ended = new ActionEvent.ActionDeactivatedEvent(fp, action, 0, 5);
        NeoForge.EVENT_BUS.post(ended);
        if (ended.getCooldown() != mainCooldown(p, main)) {
            failWith(helper, p, "the cooldown the ending action starts is " + ended.getCooldown() + ", the main side's own is " + mainCooldown(p, main));
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void stackGuardOffAllowsBoth(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), CHANT));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:chant_annulment"));
        HudConfig.WHEEL_STACK_GUARD_ENABLED.set(false);
        try {
            theirKey(p, x);
            String refusal = WheelService.use(p, ref(CHANT), 0, -1, false);
            if (refusal != null || !chantOn(p) || !ActionGameTests.active(p, x)) {
                failWith(helper, p, "with the guard off both sides should run: " + refusal);
                return;
            }
            helper.assertTrue(WheelGate.clock(p).isIdle(), "the guard is off but the clock was written");
        } finally {
            HudConfig.WHEEL_STACK_GUARD_ENABLED.set(true);
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void dedupHidesTheNonMainEntryInTheCatalogue(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), COERCION));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:coercion"));
        List<WheelEntry> list = WheelCatalogue.build(p);
        Optional<WheelEntry> main = list.stream().filter(e -> e.ref().equals(new PowerRef(PowerSource.VAMPIRISM, x))).findFirst();
        boolean coercionShown = list.stream().anyMatch(e -> e.ref().equals(ref(COERCION)));
        if (main.isEmpty() || coercionShown || !main.get().groupId().equals("g")) {
            failWith(helper, p, "main shown=" + main.isPresent() + " group=" + main.map(WheelEntry::groupId).orElse("-") + " coercion shown=" + coercionShown);
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void dedupOffListsBoth(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), COERCION));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:coercion"));
        HudConfig.WHEEL_DEDUP_ENABLED.set(false);
        try {
            List<WheelEntry> list = WheelCatalogue.build(p);
            boolean both = list.stream().anyMatch(e -> e.ref().equals(new PowerRef(PowerSource.VAMPIRISM, x))) && list.stream().anyMatch(e -> e.ref().equals(ref(COERCION)));
            if (!both || list.stream().anyMatch(e -> !e.groupId().isEmpty())) {
                failWith(helper, p, "dedup off must list both sides and carry no group: both=" + both);
                return;
            }
        } finally {
            HudConfig.WHEEL_DEDUP_ENABLED.set(true);
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSideThatIsOnStaysInTheCatalogueSoItCanBeTurnedOff(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), CHANT));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:chant_annulment"));
        helper.assertTrue(WheelService.use(p, ref(CHANT), 0, -1, false) == null && chantOn(p), "setup: the Tensura side did not start");
        WheelGate.settle(p);
        List<WheelEntry> list = WheelCatalogue.build(p);
        boolean chantShown = list.stream().anyMatch(e -> e.ref().equals(ref(CHANT)));
        boolean mainShown = list.stream().anyMatch(e -> e.ref().equals(new PowerRef(PowerSource.VAMPIRISM, x)));
        if (!chantShown || mainShown) {
            failWith(helper, p, "while the Tensura side is on it is the one shown: chant=" + chantShown + " main=" + mainShown);
            return;
        }
        // and using it again turns it off (a group never stops an "off")
        String off = WheelService.use(p, ref(CHANT), 0, -1, false);
        if (off != null || chantOn(p)) {
            failWith(helper, p, "the side that is on could not be turned off: " + off);
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void slotAndWheelShareTheSameGate(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelCatalogueGameTests.TestAction ours = new WheelCatalogueGameTests.TestAction();
        ExtraActions.install(ours);
        try {
            WheelGate.setTableForTest(table(x.toString(), TEST_ACTION));
            WheelGate.resetForTest(p);
            LoadoutService.set(p, 4, ours.id());
            theirKey(p, x);
            String slot = ActionService.trigger(p, 4, -1, false);
            String wheel = WheelService.use(p, new PowerRef(PowerSource.BLOODLINE, ours.id()), 0, -1, false);
            if (slot == null || !slot.endsWith("group_active") || !slot.equals(wheel) || ours.runs != 0) {
                failWith(helper, p, "slot " + slot + ", wheel " + wheel + ", runs " + ours.runs);
                return;
            }
            // off, and the action of ours is free to run; it then puts the group on the main side's cooldown
            theirKey(p, x);
            WheelGate.resetForTest(p);
            if (ActionService.trigger(p, 4, -1, false) != null || ours.runs != 1) {
                failWith(helper, p, "the action of ours did not run when nothing was on");
                return;
            }
            int left = WheelGate.clock(p).cooldownLeft("g", net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p));
            if (left != mainCooldown(p, x)) {
                failWith(helper, p, "after our action ran the group cooldown is " + left + ", the main side's own is " + mainCooldown(p, x));
                return;
            }
            String again = WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, x), 0, -1, false);
            if (again == null || !again.endsWith("group_cooldown")) {
                failWith(helper, p, "the main side was not refused in the shared cooldown: " + again);
                return;
            }
        } finally {
            ExtraActions.remove(ours.id());
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraSideIsRefusedFromAnyKey(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), COERCION));
        WheelGate.resetForTest(p);
        ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
        theirKey(p, x);
        // Tensura's own key: ManasCore's press, in its own slot, no wheel anywhere
        boolean started = SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0);
        if (started || coercion.onCoolDown(0)) {
            failWith(helper, p, "Tensura's own press ran the second side while the first is on");
            return;
        }
        theirKey(p, x); // off: the shared cooldown runs, the slot press is still refused
        if (SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0)) {
            failWith(helper, p, "Tensura's own press ran the second side in the shared cooldown");
            return;
        }
        // with nothing on and no cooldown it runs, and puts the group on the main side's cooldown
        WheelGate.resetForTest(p);
        if (!SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0)) {
            failWith(helper, p, "setup: the press was refused with nothing on");
            return;
        }
        WheelGate.settle(p);
        if (WheelGate.clock(p).cooldownLeft("g", net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p)) != mainCooldown(p, x)) {
            failWith(helper, p, "the Tensura press left the group cooldown at " + WheelGate.clock(p).cooldownLeft("g", net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p)));
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRefusedTensuraPressLeavesNoGroupCooldown(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), COERCION));
        WheelGate.resetForTest(p);
        ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
        // in no slot: Tensura itself refuses a press with key 0, so the group must not start a cooldown for a power that did not run
        boolean started = SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0);
        WheelGate.settle(p);
        if (started || !WheelGate.clock(p).isIdle()) {
            failWith(helper, p, "a press Tensura refused left the clock at " + WheelGate.clock(p) + " (started " + started + ")");
            return;
        }
        // in its slot but on its own cooldown: ManasCore refuses after the event, so only a check of ours keeps the clock clean
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
        coercion.setCoolDown(10, 0);
        started = SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0);
        WheelGate.settle(p);
        if (started || !WheelGate.clock(p).isIdle()) {
            failWith(helper, p, "a press refused for its own cooldown left the clock at " + WheelGate.clock(p) + " (started " + started + ")");
            return;
        }
        done(helper, p);
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void playerDiesWhileLastingStartsOneCooldown(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(table(x.toString(), CHANT));
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:chant_annulment"));
        // the Tensura side is on in the clock but is not on any more (its mod ended it with no event): a death, a logout or a dimension change ends here
        WheelGate.apply(p, ref(CHANT), SideEvent.ON, true, 0);
        helper.assertTrue(WheelGate.clock(p).activeSide("g").isPresent(), "setup: the side is not on in the clock");
        WheelGate.endAll(p);
        long end = WheelGate.clock(p).cooldownEnd("g");
        if (WheelGate.clock(p).activeSide("g").isPresent() || end == 0) {
            failWith(helper, p, "the side that ended with the player left the group locked or without a cooldown: " + WheelGate.clock(p));
            return;
        }
        WheelGate.endAll(p);
        WheelGate.reconcile(p);
        if (WheelGate.clock(p).cooldownEnd("g") != end) {
            failWith(helper, p, "a second end started a second cooldown: " + end + " then " + WheelGate.clock(p).cooldownEnd("g"));
            return;
        }
        done(helper, p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void clockSurvivesRelog(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        WheelGate.resetForTest(p);
        WheelGate.apply(p, ref("vampirism/vampirism:regen"), SideEvent.ON, true, 0);
        WheelGate.setTableForTest(table("vampirism:regen", CHANT));
        WheelGate.resetForTest(p);
        WheelGate.apply(p, ref("vampirism/vampirism:regen"), SideEvent.ON, true, 0);
        WheelGate.apply(p, ref(CHANT), SideEvent.FIRED, false, 0);
        GroupClock before = WheelGate.clock(p);
        // what a relog does: the attachment goes through its codec
        Tag saved = GroupClock.CODEC.encodeStart(NbtOps.INSTANCE, before).getOrThrow();
        GroupClock after = GroupClock.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(before.equals(after) && after.activeSide("g").isPresent(), "the clock changed through its codec: " + before + " then " + after);
        done(helper, p);
    }

    /** The Tensura chain is also collapsed by the table (the highest owned tier stays), on top of Tensura's own kinds file. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void productionTableCollapsesTheRegenerationChain(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        WheelGate.setTableForTest(null);
        WheelGate.resetForTest(p);
        WheelTestKit.learn(p, id("tensura:self_regeneration"));
        WheelTestKit.learn(p, id("tensura:ultraspeed_regeneration"));
        List<WheelEntry> list = WheelCatalogue.build(p);
        helper.assertTrue(list.stream().noneMatch(e -> e.ref().id().getPath().equals("self_regeneration")), "the lowest tier is listed");
        helper.assertTrue(list.stream().anyMatch(e -> e.ref().id().getPath().equals("ultraspeed_regeneration")), "the highest tier is not listed");
        done(helper, p);
    }

    /** A held skill that leaves its slot while it is held (the preset changed, the slot was cleared) is not cut off after a second: the hold bracket is open for every wheel hold. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSlottedSkillHeldFromTheWheelSurvivesLeavingItsSlot(GameTestHelper helper) {
        ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("tensura:gravity_flight"));
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 1, inst.getSkill(), 0);
        helper.assertTrue(TensuraUse.press(p, id("tensura:gravity_flight"), 0) == null, "the press was refused");
        WheelTestKit.tick(p, 5);
        helper.assertTrue(io.github.manasmods.manascore.skill.impl.TickingSkill.isTickingSkill(p, inst.getSkill(), 0), "setup: the held skill stopped by itself");
        TensuraStorages.getAbilityFrom(p).removeSkillFromPresets(inst.getSkill());
        WheelTestKit.tick(p, 60);
        helper.assertTrue(io.github.manasmods.manascore.skill.impl.TickingSkill.isTickingSkill(p, inst.getSkill(), 0), "a held skill that left its slot was cut off");
        TensuraUse.release(p, id("tensura:gravity_flight"), 0);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillSlottedInSlotTwoHeldFromTheWheelRunsSixtyTicks(GameTestHelper helper) {
        ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(helper);
        ManasSkillInstance inst = WheelTestKit.learn(p, id("tensura:gravity_flight"));
        TensuraStorages.getAbilityFrom(p).setActivePreset(0);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 2, inst.getSkill(), 0);
        helper.assertTrue(TensuraUse.press(p, id("tensura:gravity_flight"), 0) == null, "the press was refused");
        WheelTestKit.tick(p, 60);
        helper.assertTrue(io.github.manasmods.manascore.skill.impl.TickingSkill.isTickingSkill(p, inst.getSkill(), 0), "a slotted held skill was cut off within 60 ticks");
        TensuraUse.release(p, id("tensura:gravity_flight"), 0);
        helper.succeed();
    }
}
