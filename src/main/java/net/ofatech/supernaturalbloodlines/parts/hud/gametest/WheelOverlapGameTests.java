package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import com.google.gson.JsonParser;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.event.ActionEvent;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import io.github.manasmods.tensura.race.vampire.VampireRace;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.VampirismVision;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks;
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

    /** A group of any id, members named by their full key (the first is the main side). */
    private static OverlapTable tableOf(String groupId, String... members) {
        StringBuilder list = new StringBuilder();
        for (String m : members) {
            list.append(list.length() == 0 ? "" : ",").append('"').append(m).append('"');
        }
        String json = "{\"format\":1,\"groups\":[{\"id\":\"" + groupId + "\",\"research\":\"T\",\"confidence\":\"same\",\"main\":0,\"members\":[" + list + "]}]}";
        return OverlapTable.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    /** Runs a test and puts the shared test seams back whatever happens in it (a failed assertion throws), so the next test never sees this one's table or clock. */
    private static void guarded(GameTestHelper helper, java.util.function.Consumer<GameTestHelper> body) {
        try {
            body.accept(helper);
        } finally {
            WheelGate.setTableForTest(null);
            WheelClocks.setNowForTest(null);
            HudConfig.WHEEL_STACK_GUARD_ENABLED.set(true);
            HudConfig.WHEEL_DEDUP_ENABLED.set(true);
        }
    }

    private static final String SELF_REGEN = "tensura/tensura:self_regeneration";
    private static final String ULTRA_REGEN = "tensura/tensura:ultraspeed_regeneration";

    /** Switches a Tensura switch on the way its skill screen does, and counts it at the end of the tick. */
    private static void switchOn(ServerPlayer p, String skill) {
        RequestAbilityModeChangePacket.toggleAbilityPacket(id(skill)).toggleAbility(p);
        WheelGate.settle(p);
    }

    private static void tickPlayer(ServerPlayer p, int ticks) {
        for (int i = 0; i < ticks; i++) {
            p.tickCount++;
            WheelTestKit.tick(p);
            // a player the tests make is a fake one, which the listener leaves alone: the gate's own tick
            WheelGate.tick(p);
        }
    }

    private static WheelEntry entryOf(ServerPlayer p, PowerRef ref) {
        return WheelCatalogue.build(p).stream().filter(e -> e.ref().equals(ref)).findFirst().orElseThrow(() -> new IllegalStateException("not listed: " + ref));
    }

    /** A higher tier of a chain that is on is on for its group: the clock saves it as the first tier, so the check must look at the tier the player owns. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHigherRegenerationTierThatIsOnKeepsVampireRegenOutPastTheCheck(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation regen = id("vampirism:regen");
            unlock(p, regen);
            WheelGate.setTableForTest(null);
            WheelGate.resetForTest(p);
            ManasSkillInstance ultra = WheelTestKit.learn(p, id("tensura:ultraspeed_regeneration"));
            switchOn(p, "tensura:ultraspeed_regeneration");
            h.assertTrue(ultra.isToggled(), "setup: the tier did not switch on");
            if (!activeOf(p, "regeneration").equals(Optional.of(ref(SELF_REGEN).key()))) {
                failWith(h, p, "the tier that is on is not the active side: " + WheelGate.clock(p));
                return;
            }
            tickPlayer(p, 20);
            if (!ultra.isToggled() || !activeOf(p, "regeneration").equals(Optional.of(ref(SELF_REGEN).key()))) {
                failWith(h, p, "the check ended a tier that is still on: on=" + ultra.isToggled() + " " + WheelGate.clock(p));
                return;
            }
            Optional<LockReason> refusal = WheelGate.check(p, new PowerRef(PowerSource.VAMPIRISM, regen));
            theirKey(p, regen);
            if (refusal.isEmpty() || refusal.get() != LockReason.GROUP_ACTIVE || ActionGameTests.active(p, regen)) {
                failWith(h, p, "vampire regen was not kept out while ultraspeed regeneration is on: " + refusal + " active=" + ActionGameTests.active(p, regen));
                return;
            }
            done(h, p);
        });
    }

    /** The tier was switched on while the guard was off: the check finds it by the tier the player owns. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHigherTierThatWasOnBeforeTheGuardIsFoundByTheCheck(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            unlock(p, id("vampirism:regen"));
            WheelGate.setTableForTest(null);
            WheelGate.resetForTest(p);
            ManasSkillInstance ultra = WheelTestKit.learn(p, id("tensura:ultraspeed_regeneration"));
            HudConfig.WHEEL_STACK_GUARD_ENABLED.set(false);
            try {
                switchOn(p, "tensura:ultraspeed_regeneration");
            } finally {
                HudConfig.WHEEL_STACK_GUARD_ENABLED.set(true);
            }
            h.assertTrue(ultra.isToggled() && WheelGate.clock(p).isIdle(), "setup: the tier is not on, or the clock saw it");
            WheelGate.reconcile(p);
            if (!activeOf(p, "regeneration").equals(Optional.of(ref(SELF_REGEN).key()))) {
                failWith(h, p, "the tier that is on was not found: " + WheelGate.clock(p));
                return;
            }
            WheelGate.reconcile(p);
            if (activeOf(p, "regeneration").isEmpty()) {
                failWith(h, p, "the second check ended it: " + WheelGate.clock(p));
                return;
            }
            done(h, p);
        });
    }

    /** Vampirism's vision is the vampire's: for a hunter it is not an owned main side, so the group takes the cooldown of the side that was used. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void visionIsOwnedByTheVampireFactionOnly(GameTestHelper helper) {
        guarded(helper, h -> {
            int[] left = new int[2];
            for (int who = 0; who < 2; who++) {
                ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(h);
                h.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(who == 0 ? de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION
                        : de.teamlapen.vampirism.api.VReference.HUNTER_FACTION, 1), "setup: could not set the faction");
                WheelGate.setTableForTest(tableOf("g", "vampirism/vampirism:vision", COERCION));
                WheelGate.resetForTest(p);
                ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
                TensuraStorages.getAbilityFrom(p).setActivePreset(0);
                TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
                h.assertTrue(SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0), "setup: the press was refused");
                coercion.setCoolDown(10, 0);
                WheelGate.settle(p);
                left[who] = groupLeft(p, "g");
            }
            if (left[0] != 0 || left[1] != 200) {
                failWith(h, ActionGameTests.vampire(h), "the group cooldown is " + left[0] + " for a vampire (vision has none, so none) and " + left[1] + " for a hunter (the side used has 200)");
                return;
            }
            helper.succeed();
        });
    }

    /** Rule C: the side that is on stays the shown entry, and the entry says what else it covers, so nothing vanishes without a hint. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theShownEntryNamesTheSidesItCovers(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation regen = id("vampirism:regen");
            unlock(p, regen);
            WheelGate.setTableForTest(null);
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:ultraspeed_regeneration"));
            PowerRef vampireRegen = new PowerRef(PowerSource.VAMPIRISM, regen);
            PowerRef ultra = ref(ULTRA_REGEN);
            // the names of the two entries, from the list with nothing hidden
            HudConfig.WHEEL_DEDUP_ENABLED.set(false);
            String vampireName;
            String ultraName;
            try {
                vampireName = entryOf(p, vampireRegen).name().getString();
                ultraName = entryOf(p, ultra).name().getString();
            } finally {
                HudConfig.WHEEL_DEDUP_ENABLED.set(true);
            }
            // nothing on: the main side stays and says it covers the Tensura tier
            WheelEntry main = entryOf(p, vampireRegen);
            String expected = "Also covers: Tensura " + ultraName;
            if (!main.coverNote().getString().equals(expected) || WheelCatalogue.build(p).stream().anyMatch(e -> e.ref().equals(ultra))) {
                failWith(h, p, "the main side says '" + main.coverNote().getString() + "', expected '" + expected + "'");
                return;
            }
            // the Tensura tier is on: it is the one shown, and says the other side is locked while it is on
            switchOn(p, "tensura:ultraspeed_regeneration");
            WheelEntry shown = entryOf(p, ultra);
            expected = "Also covers: Vampirism " + vampireName + ", locked while this is on";
            if (!shown.coverNote().getString().equals(expected) || WheelCatalogue.build(p).stream().anyMatch(e -> e.ref().equals(vampireRegen))) {
                failWith(h, p, "the side that is on says '" + shown.coverNote().getString() + "', expected '" + expected + "'");
                return;
            }
            done(h, p);
        });
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
        guarded(helper, WheelOverlapGameTests::secondSideIsRefusedWhileTheFirstIsOnBody);
    }

    private static void secondSideIsRefusedWhileTheFirstIsOnBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::firstSideOffStartsTheGroupCooldownOnTheSecondBody);
    }

    private static void firstSideOffStartsTheGroupCooldownOnTheSecondBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::lastingEndCooldownIsRaisedToTheGroupFloorBody);
    }

    private static void lastingEndCooldownIsRaisedToTheGroupFloorBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::stackGuardOffAllowsBothBody);
    }

    private static void stackGuardOffAllowsBothBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::dedupHidesTheNonMainEntryInTheCatalogueBody);
    }

    private static void dedupHidesTheNonMainEntryInTheCatalogueBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::dedupOffListsBothBody);
    }

    private static void dedupOffListsBothBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::theSideThatIsOnStaysInTheCatalogueSoItCanBeTurnedOffBody);
    }

    private static void theSideThatIsOnStaysInTheCatalogueSoItCanBeTurnedOffBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::slotAndWheelShareTheSameGateBody);
    }

    private static void slotAndWheelShareTheSameGateBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::tensuraSideIsRefusedFromAnyKeyBody);
    }

    private static void tensuraSideIsRefusedFromAnyKeyBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::aRefusedTensuraPressLeavesNoGroupCooldownBody);
    }

    private static void aRefusedTensuraPressLeavesNoGroupCooldownBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::playerDiesWhileLastingStartsOneCooldownBody);
    }

    private static void playerDiesWhileLastingStartsOneCooldownBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::clockSurvivesRelogBody);
    }

    private static void clockSurvivesRelogBody(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation x = lasting(helper, p, 0);
        WheelGate.setTableForTest(tableOf("g", "vampirism/" + x, CHANT, COERCION));
        WheelGate.resetForTest(p);
        WheelGate.apply(p, ref("vampirism/" + x), SideEvent.ON, true, 0);
        WheelGate.apply(p, ref(CHANT), SideEvent.FIRED, false, 0);
        GroupClock before = WheelGate.clock(p);
        helper.assertTrue(before.activeSide("g").isPresent() && before.cooldownEnd("g") > 0 && before.cooldownTotal("g") > 0, "setup: the clock is not running: " + before);
        // what a relog does: the player is written out with its attachments and read back, the clock having been lost in between
        CompoundTag saved = p.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.toString().contains("wheel_clock"), "the clock is not part of what is saved with the player");
        WheelGate.resetForTest(p);
        helper.assertTrue(WheelGate.clock(p).isIdle(), "setup: the clock was not cleared");
        p.load(saved);
        GroupClock after = WheelGate.clock(p);
        helper.assertTrue(before.equals(after) && after.activeSide("g").isPresent(), "the clock changed through the player's save: " + before + " then " + after);
        done(helper, p);
    }

    /** The Tensura chain is also collapsed by the table (the highest owned tier stays), on top of Tensura's own kinds file. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void productionTableCollapsesTheRegenerationChain(GameTestHelper helper) {
        guarded(helper, WheelOverlapGameTests::productionTableCollapsesTheRegenerationChainBody);
    }

    private static void productionTableCollapsesTheRegenerationChainBody(GameTestHelper helper) {
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
        guarded(helper, WheelOverlapGameTests::aSlottedSkillHeldFromTheWheelSurvivesLeavingItsSlotBody);
    }

    private static void aSlottedSkillHeldFromTheWheelSurvivesLeavingItsSlotBody(GameTestHelper helper) {
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

    private static final String FLIGHT = "tensura/tensura:gravity_flight";
    private static final String BAT = "vampirism/vampirism:bat";
    private static final String RACE_ABILITY = "tensura/tensura:race_ability";
    private static final String WEREWOLF_RAGE = "werewolves/werewolves:rage";

    private static int groupLeft(ServerPlayer p, String group) {
        return WheelGate.clock(p).cooldownLeft(group, net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p));
    }

    private static Optional<String> activeOf(ServerPlayer p, String group) {
        return WheelGate.clock(p).activeSide(group);
    }

    /** Unlocks one Vampirism action of the vampire. @return its own cooldown */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int unlock(ServerPlayer p, ResourceLocation action) {
        IAction a = FactionActions.byId(action);
        IFactionPlayer fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        ((IActionHandler) fp.getActionHandler()).unlockActions(List.of(a));
        return a.getCooldown(fp);
    }

    /** Tensura's vampire race ability the way ManasCore runs it: the activation event first, then the race's own code, then the end of the tick. */
    private static dev.architectury.event.EventResult useRaceAbility(ServerPlayer p) {
        ManasRaceInstance inst = RaceAPI.getRaceFrom(p).getRace().orElseThrow();
        dev.architectury.event.EventResult result = RaceEvents.ACTIVATE_ABILITY.invoker().activateAbility(inst, p);
        if (!result.isFalse()) {
            ((VampireRace) inst.getRace()).onActivateAbility(inst, p);
        }
        WheelGate.settle(p);
        return result;
    }

    private static boolean batsMode(ServerPlayer p) {
        return p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.BATS_MODE));
    }

    /** Bypass 1 (main side not owned): the shared cooldown was 0 because the main side belongs to a faction the player is not in. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMainSideThePlayerLacksStillStartsTheSharedCooldown(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            // the main side is a werewolf action: a vampire does not have it, so the first side they do have speaks for the group
            WheelGate.setTableForTest(tableOf("g", WEREWOLF_RAGE, "vampirism/" + x));
            WheelGate.resetForTest(p);
            theirKey(p, x);
            theirKey(p, x); // off
            int own = mainCooldown(p, x);
            if (own <= 0 || groupLeft(p, "g") != own) {
                failWith(h, p, "the group cooldown is " + groupLeft(p, "g") + ", the side that was used has " + own);
                return;
            }
            Optional<LockReason> werewolf = WheelGate.check(p, ref(WEREWOLF_RAGE));
            if (werewolf.isEmpty() || werewolf.get() != LockReason.GROUP_COOLDOWN) {
                failWith(h, p, "the other side was not held back by the shared cooldown: " + werewolf);
                return;
            }
            done(h, p);
        });
    }

    /** Bypass 2: a held Tensura skill is on while it is held, so no other side starts, and its end starts the cooldown. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHeldTensuraSkillBlocksTheOtherSideUntilItIsReleased(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(h);
            // the flight and, in its place for Ascension's angel wings (an addon the CI run does not have), another Tensura skill of the same group
            WheelGate.setTableForTest(tableOf("g", FLIGHT, COERCION));
            WheelGate.resetForTest(p);
            ManasSkillInstance flight = WheelTestKit.learn(p, id("tensura:gravity_flight"));
            ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
            TensuraStorages.getAbilityFrom(p).setActivePreset(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 1, flight.getSkill(), 0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
            h.assertTrue(TensuraUse.press(p, id("tensura:gravity_flight"), 0) == null, "setup: the press was refused");
            WheelGate.settle(p);
            h.assertTrue(io.github.manasmods.manascore.skill.impl.TickingSkill.isTickingSkill(p, flight.getSkill(), 0), "setup: the held skill is not held");
            if (!activeOf(p, "g").equals(Optional.of(ref(FLIGHT).key()))) {
                failWith(h, p, "a held skill is not on in the clock: " + WheelGate.clock(p));
                return;
            }
            if (SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0)) {
                failWith(h, p, "a second side started while the flight is held");
                return;
            }
            TensuraUse.release(p, id("tensura:gravity_flight"), 0);
            WheelGate.settle(p);
            if (activeOf(p, "g").isPresent()) {
                failWith(h, p, "the flight was released but is still on in the clock: " + WheelGate.clock(p));
                return;
            }
            if (!SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0)) {
                failWith(h, p, "the other side stayed refused after the flight ended (it has no cooldown)");
                return;
            }
            SkillAPI.getSkillsFrom(p).handleSkillRelease(coercion, 0, 0, false);
            done(h, p);
        });
    }

    /** Bypass 3: Bats Mode is the bat form; the bat of Vampirism stays refused while it lasts, even after the cooldown the old "fired" record would have run out. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void batsModeBlocksTheBatEvenAfterTheCooldownAndItsEndStartsOne(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = tensuraVampire(h);
            ResourceLocation bat = id("vampirism:bat");
            int batCooldown = unlock(p, bat);
            h.assertTrue(batCooldown > 0, "setup: the bat has no cooldown");
            WheelGate.setTableForTest(tableOf("bat_form", BAT, RACE_ABILITY));
            WheelGate.resetForTest(p);
            if (useRaceAbility(p).isFalse() || !batsMode(p)) {
                failWith(h, p, "setup: Bats Mode did not start");
                return;
            }
            if (!activeOf(p, "bat_form").equals(Optional.of(ref(RACE_ABILITY).key()))) {
                failWith(h, p, "Bats Mode is not on in the clock: " + WheelGate.clock(p));
                return;
            }
            // the ten-tick check sees the effect still there and keeps the side on
            WheelGate.reconcile(p);
            if (!activeOf(p, "bat_form").equals(Optional.of(ref(RACE_ABILITY).key()))) {
                failWith(h, p, "the check ended Bats Mode while the effect lasts: " + WheelGate.clock(p));
                return;
            }
            // the time the bat's own cooldown would take passes: Bats Mode is still on, the bat is still out
            long later = net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.now(p) + 100_000L;
            net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.setNowForTest(() -> later);
            Optional<LockReason> refusal = WheelGate.check(p, ref(BAT));
            if (refusal.isEmpty() || refusal.get() != LockReason.GROUP_ACTIVE) {
                failWith(h, p, "the bat was not refused while Bats Mode lasts: " + refusal);
                return;
            }
            net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.setNowForTest(null);
            theirKey(p, bat);
            if (ActionGameTests.active(p, bat)) {
                failWith(h, p, "Vampirism's own key started the bat while Bats Mode lasts");
                return;
            }
            // the ability again ends Bats Mode: the bat's cooldown is the group's, and both sides wait
            if (useRaceAbility(p).isFalse() || batsMode(p)) {
                failWith(h, p, "the second press did not end Bats Mode");
                return;
            }
            if (activeOf(p, "bat_form").isPresent() || groupLeft(p, "bat_form") != batCooldown) {
                failWith(h, p, "the end of Bats Mode left " + WheelGate.clock(p) + ", the bat's own cooldown is " + batCooldown);
                return;
            }
            if (!useRaceAbility(p).isFalse() || batsMode(p)) {
                failWith(h, p, "the ability was not refused during the shared cooldown");
                return;
            }
            // another race's ability is not a bat form: the same group in the same cooldown does not stop it
            h.assertTrue(WheelGate.groupable(p, ref(RACE_ABILITY)), "the vampire race's ability is not part of the groups");
            h.assertTrue(RaceAPI.getRaceFrom(p).setRace(id("tensura:human"), false), "setup: could not change the race");
            ManasRaceInstance human = RaceAPI.getRaceFrom(p).getRace().orElseThrow();
            if (RaceEvents.ACTIVATE_ABILITY.invoker().activateAbility(human, p).isFalse()) {
                failWith(h, p, "a race that is not the vampire's was stopped by the bat form group");
                return;
            }
            if (WheelGate.groupable(p, ref(RACE_ABILITY)) || !WheelGate.groupable(p, ref(FLIGHT))) {
                failWith(h, p, "the ability of a race that is not the vampire's is part of the groups, or a skill is not");
                return;
            }
            done(h, p);
        });
    }

    /** A press that drains is the bite, not the bat form: only the bite group counts it, and Bats Mode is not on. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDrainOfTheRaceAbilityCountsInTheBiteGroupOnly(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = tensuraVampire(h);
            WheelGate.setTableForTest(OverlapTable.parse(JsonParser.parseString("{\"format\":1,\"groups\":["
                    + "{\"id\":\"bat_form\",\"research\":\"T\",\"confidence\":\"close\",\"main\":0,\"members\":[\"" + BAT + "\",\"" + RACE_ABILITY + "\"]},"
                    + "{\"id\":\"bite\",\"research\":\"T\",\"confidence\":\"close\",\"main\":0,\"members\":[\"vampirism/vampirism:regen\",\"" + RACE_ABILITY + "\"]}]}").getAsJsonObject()));
            WheelGate.resetForTest(p);
            unlock(p, id("vampirism:regen"));
            net.minecraft.world.entity.PathfinderMob victim = BiteGameTests.villager(h, 2.5);
            // hurt, so Tensura's own test for a feed ("hurt or food not full") says yes
            p.setHealth(10f);
            lookEast(p);
            useRaceAbility(p);
            boolean drained = victim.getHealth() < victim.getMaxHealth();
            victim.discard();
            if (!drained || batsMode(p)) {
                failWith(h, p, "setup: the press did not drain (drained " + drained + ", bats " + batsMode(p) + ")");
                return;
            }
            if (activeOf(p, "bat_form").isPresent() || groupLeft(p, "bat_form") != 0) {
                failWith(h, p, "a drain was counted in the bat form group: " + WheelGate.clock(p));
                return;
            }
            if (groupLeft(p, "bite") <= 0) {
                failWith(h, p, "a drain started no cooldown in the bite group: " + WheelGate.clock(p));
                return;
            }
            done(h, p);
        });
    }

    private static ServerPlayer tensuraVampire(GameTestHelper helper) {
        ServerPlayer p = BiteGameTests.vampire(helper);
        helper.assertTrue(RaceAPI.getRaceFrom(p).setRace(id("tensura:vampire"), false), "could not give the player Tensura's vampire race");
        lookEast(p);
        return p;
    }

    private static void lookEast(ServerPlayer p) {
        p.setYRot(-90f);
        p.setYHeadRot(-90f);
        p.setXRot(0f);
    }

    /** Same tick: whatever another side did this tick (an on, or an instant that starts a cooldown) keeps this side out, though neither is in the clock yet. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoSidesInTheSameTickAreNotBothStarted(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            WheelGate.setTableForTest(table(x.toString(), CHANT, COERCION));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:chant_annulment"));
            ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
            TensuraStorages.getAbilityFrom(p).setActivePreset(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
            // an "on" this tick (no settle between the two presses)
            FactionActions.toggle(p, x, new IAction.ActivationContext() {
                @Override
                public Optional<net.minecraft.core.BlockPos> targetBlock() {
                    return Optional.empty();
                }

                @Override
                public Optional<net.minecraft.world.entity.Entity> targetEntity() {
                    return Optional.empty();
                }
            });
            String second = WheelService.use(p, ref(CHANT), 0, -1, false);
            if (second == null || !second.endsWith("group_active") || chantOn(p)) {
                failWith(h, p, "a second side started in the same tick as an 'on': " + second + " on=" + chantOn(p));
                return;
            }
            WheelGate.resetForTest(p);
            // an instant side fired this tick: its cooldown is not in the clock yet, the other side is refused all the same
            if (!SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0)) {
                failWith(h, p, "setup: the instant side did not start");
                return;
            }
            Optional<LockReason> other = WheelGate.check(p, new PowerRef(PowerSource.VAMPIRISM, x));
            if (other.isEmpty()) {
                failWith(h, p, "a side started in the same tick as another that was counted as fired or held");
                return;
            }
            done(h, p);
        });
    }

    /** The HUD or the guard was off, or Vampirism's own vision key (it has no event) was used: the check every ten ticks finds what is on and makes it the active side. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void whatIsOnButWasNeverRecordedIsFoundByTheTenTickCheck(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            WheelGate.setTableForTest(tableOf("g", "vampirism/vampirism:vision", CHANT));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:chant_annulment"));
            var visions = VampirismAPI.vampireVisionRegistry().getVisions();
            h.assertTrue(!visions.isEmpty(), "setup: Vampirism has no vision");
            VampirismAPI.vampirePlayer(p).unlockVision(visions.get(0));
            VampirismAPI.vampirePlayer(p).activateVision(visions.get(0));
            h.assertTrue(VampirismVision.active(p), "setup: the vision is not on");
            if (!WheelGate.clock(p).isIdle()) {
                failWith(h, p, "setup: the clock knew about a vision that had no event");
                return;
            }
            p.tickCount = 9;
            WheelGate.tick(p);
            if (!WheelGate.clock(p).isIdle()) {
                failWith(h, p, "the check ran on a tick that is not a multiple of ten");
                return;
            }
            p.tickCount = 10;
            WheelGate.tick(p);
            if (!activeOf(p, "g").equals(Optional.of(ref("vampirism/vampirism:vision").key()))) {
                failWith(h, p, "the vision that is on was not found by the check: " + WheelGate.clock(p));
                return;
            }
            String refused = WheelService.use(p, ref(CHANT), 0, -1, false);
            if (refused == null || !refused.endsWith("group_active") || chantOn(p)) {
                failWith(h, p, "the other side started while the vision is on: " + refused);
                return;
            }
            // the vision goes off with no event: the check ends it
            VampirismAPI.vampirePlayer(p).activateVision(null);
            p.tickCount = 20;
            WheelGate.tick(p);
            if (activeOf(p, "g").isPresent()) {
                failWith(h, p, "a vision that went off is still on in the clock: " + WheelGate.clock(p));
                return;
            }
            done(h, p);
        });
    }

    /** The guard setting turned on while something was already on (the runtime toggle): the next check picks it up. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void turningTheGuardOnWhileASideIsOnMakesItTheActiveSide(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            WheelGate.setTableForTest(table(x.toString(), CHANT));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:chant_annulment"));
            HudConfig.WHEEL_STACK_GUARD_ENABLED.set(false);
            try {
                theirKey(p, x);
            } finally {
                HudConfig.WHEEL_STACK_GUARD_ENABLED.set(true);
            }
            h.assertTrue(ActionGameTests.active(p, x) && WheelGate.clock(p).isIdle(), "setup: the side is not on, or the clock saw it");
            WheelGate.reconcile(p);
            if (!activeOf(p, "g").equals(Optional.of(ref("vampirism/" + x).key()))) {
                failWith(h, p, "the side that was on is not the active side: " + WheelGate.clock(p));
                return;
            }
            String refused = WheelService.use(p, ref(CHANT), 0, -1, false);
            if (refused == null || !refused.endsWith("group_active")) {
                failWith(h, p, "the other side started while the first is on: " + refused);
                return;
            }
            done(h, p);
        });
    }

    /** The main side is a Tensura skill that was not the one pressed: its cooldown cannot be read, so the group gets the cooldown of the side that was (never 0 while it has one). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTensuraMainThatWasNotPressedGivesWayToTheSideThatWas(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(h);
            WheelGate.setTableForTest(tableOf("g", FLIGHT, COERCION));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:gravity_flight"));
            ManasSkillInstance coercion = WheelTestKit.learn(p, id("tensura:coercion"));
            TensuraStorages.getAbilityFrom(p).setActivePreset(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, coercion.getSkill(), 0);
            h.assertTrue(SkillAPI.getSkillsFrom(p).startHoldSkill(coercion, 0, 0), "setup: the press was refused");
            // the skill's own cooldown, as its code sets it right after the press (Tensura counts whole seconds)
            coercion.setCoolDown(10, 0);
            WheelGate.settle(p);
            int own = 200;
            if (groupLeft(p, "g") != own) {
                failWith(h, p, "the group cooldown is " + groupLeft(p, "g") + ", the side that was pressed has " + own);
                return;
            }
            done(h, p);
        });
    }

    /** The catalogue shows what the gate knows: the other side is locked while a side is on, and carries the group's cooldown (and its length) once it ended. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCatalogueLocksTheOtherSideAndCarriesTheGroupCooldown(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            WheelGate.setTableForTest(table(x.toString(), CHANT));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:chant_annulment"));
            HudConfig.WHEEL_DEDUP_ENABLED.set(false);
            try {
                theirKey(p, x);
                WheelEntry chant = WheelCatalogue.build(p).stream().filter(e -> e.ref().equals(ref(CHANT))).findFirst().orElseThrow();
                if (chant.state().lock() != LockReason.GROUP_ACTIVE) {
                    failWith(h, p, "the other side is not locked while the first is on: " + chant.state().lock());
                    return;
                }
                theirKey(p, x); // off
                chant = WheelCatalogue.build(p).stream().filter(e -> e.ref().equals(ref(CHANT))).findFirst().orElseThrow();
                int main = mainCooldown(p, x);
                if (chant.state().lock() == LockReason.GROUP_ACTIVE || chant.state().cooldownEnd() != WheelGate.clock(p).cooldownEnd("g") || chant.state().cooldownTotal() != main) {
                    failWith(h, p, "the other side shows lock " + chant.state().lock() + ", end " + chant.state().cooldownEnd() + " (clock " + WheelGate.clock(p).cooldownEnd("g")
                            + "), length " + chant.state().cooldownTotal() + " (the main side's " + main + ")");
                    return;
                }
            } finally {
                HudConfig.WHEEL_DEDUP_ENABLED.set(true);
            }
            done(h, p);
        });
    }

    /** A skill with several modes is not the same power as a whole one: it is never hidden, locked or counted (Gravity Field has three modes). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillWithModesIsNeverPartOfAGroup(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            String fieldKey = "tensura/tensura:gravity_field";
            WheelGate.setTableForTest(table(x.toString(), fieldKey));
            WheelGate.resetForTest(p);
            ManasSkillInstance field = WheelTestKit.learn(p, id("tensura:gravity_field"));
            h.assertTrue(field.getModes() > 1, "setup: the skill has no modes");
            theirKey(p, x);
            List<WheelEntry> list = WheelCatalogue.build(p);
            Optional<WheelEntry> shown = list.stream().filter(e -> e.ref().equals(ref(fieldKey))).findFirst();
            if (shown.isEmpty() || shown.get().state().lock() == LockReason.GROUP_ACTIVE || !shown.get().groupId().isEmpty()) {
                failWith(h, p, "a skill with modes was treated as part of the group: " + shown.map(e -> e.state().lock() + " " + e.groupId()).orElse("hidden"));
                return;
            }
            // and the gate lets its press through while the group is held by the other side
            TensuraStorages.getAbilityFrom(p).setActivePreset(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, field.getSkill(), 0);
            var result = SkillEvents.ACTIVATE_SKILL.invoker().activateSkill(Changeable.of(field), p, 0, 0);
            if (result.isFalse()) {
                failWith(h, p, "the press of a skill with modes was refused by a group");
                return;
            }
            done(h, p);
        });
    }

    /** A switch counts as off only once Tensura really switched it off: an event whose switch Tensura then refused leaves it on. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aToggleOffCountsOnlyOnceTensuraAppliedIt(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            ResourceLocation x = lasting(h, p, 0);
            WheelGate.setTableForTest(table(x.toString(), CHANT));
            WheelGate.resetForTest(p);
            ManasSkillInstance chant = WheelTestKit.learn(p, id("tensura:chant_annulment"));
            if (WheelService.use(p, ref(CHANT), 0, -1, false) != null || !chantOn(p)) {
                failWith(h, p, "setup: the switch did not turn on");
                return;
            }
            WheelGate.settle(p);
            // Tensura's event for a switch-off that then does not happen (another mod refuses it)
            SkillEvents.TOGGLE_SKILL.invoker().toggleSkill(Changeable.of(chant), p);
            WheelGate.settle(p);
            if (!chantOn(p) || activeOf(p, "g").isEmpty()) {
                failWith(h, p, "a switch-off that Tensura did not apply was counted: on=" + chantOn(p) + " " + WheelGate.clock(p));
                return;
            }
            RequestAbilityModeChangePacket.toggleAbilityPacket(id("tensura:chant_annulment")).toggleAbility(p);
            WheelGate.settle(p);
            if (chantOn(p) || activeOf(p, "g").isPresent()) {
                failWith(h, p, "a real switch-off was not counted: on=" + chantOn(p) + " " + WheelGate.clock(p));
                return;
            }
            done(h, p);
        });
    }

    /** The wheel's own vision cycle is recorded at once (no event of Vampirism's), and the cycle that turns it off ends the side. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theWheelsVisionCycleIsRecordedAsOnAndOff(GameTestHelper helper) {
        guarded(helper, h -> {
            ServerPlayer p = ActionGameTests.vampire(h);
            WheelGate.setTableForTest(tableOf("g", "vampirism/vampirism:vision", CHANT));
            WheelGate.resetForTest(p);
            WheelTestKit.learn(p, id("tensura:chant_annulment"));
            var visions = VampirismAPI.vampireVisionRegistry().getVisions();
            VampirismAPI.vampirePlayer(p).unlockVision(visions.get(0));
            PowerRef vision = new PowerRef(PowerSource.VAMPIRISM, VampirismVision.ID);
            String first = WheelService.use(p, vision, 0, -1, false);
            if (first != null || !VampirismVision.active(p) || !activeOf(p, "g").equals(Optional.of(vision.key()))) {
                failWith(h, p, "the vision cycle was not recorded as on: " + first + " " + WheelGate.clock(p));
                return;
            }
            String refused = WheelService.use(p, ref(CHANT), 0, -1, false);
            if (refused == null || !refused.endsWith("group_active")) {
                failWith(h, p, "the other side started while the vision is on: " + refused);
                return;
            }
            // cycle until it is off again (a vampire with one vision: the next press)
            for (int i = 0; i < 4 && VampirismVision.active(p); i++) {
                WheelService.use(p, vision, 0, -1, false);
            }
            if (VampirismVision.active(p) || activeOf(p, "g").isPresent()) {
                failWith(h, p, "the vision is off but the clock says " + WheelGate.clock(p));
                return;
            }
            done(h, p);
        });
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillSlottedInSlotTwoHeldFromTheWheelRunsSixtyTicks(GameTestHelper helper) {
        guarded(helper, WheelOverlapGameTests::aSkillSlottedInSlotTwoHeldFromTheWheelRunsSixtyTicksBody);
    }

    private static void aSkillSlottedInSlotTwoHeldFromTheWheelRunsSixtyTicksBody(GameTestHelper helper) {
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
