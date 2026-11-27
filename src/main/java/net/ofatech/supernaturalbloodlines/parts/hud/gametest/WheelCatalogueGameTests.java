package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.UsePowerPayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelCataloguePayload;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.PowerProvider;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.VampirismVision;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelCatalogue;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelSync;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/** The wheel's catalogue of our powers, the factions' (Vampirism, Werewolves) and Vampirism's Vision, and the push of it to a viewer. Needs Vampirism. */
@PrefixGameTestTemplate(false)
public final class WheelCatalogueGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation TEST_ID = ResourceLocation.fromNamespaceAndPath(NS, "wheel_test_action");

    private WheelCatalogueGameTests() {
    }

    /** An action of ours that a test can switch on and off and give a cooldown. */
    static class TestAction implements ExtraActions.Action {
        volatile boolean available = true;
        volatile boolean aimed;
        volatile int left;

        @Override
        public ResourceLocation id() {
            return TEST_ID;
        }

        @Override
        public Component name() {
            return Component.literal("Wheel test");
        }

        @Override
        public Component note() {
            return Component.literal("test");
        }

        @Override
        public ResourceLocation icon() {
            return ResourceLocation.fromNamespaceAndPath(NS, "textures/gui/hud/unknown.png");
        }

        @Override
        public boolean available(Player p) {
            return available;
        }

        volatile int runs;

        @Nullable
        @Override
        public Component trigger(ServerPlayer p) {
            runs++;
            return null;
        }

        @Override
        public boolean aimed() {
            return aimed;
        }

        @Override
        public int cooldownTicks(ServerPlayer p) {
            return 100;
        }

        @Override
        public int cooldownLeft(ServerPlayer p) {
            return left;
        }
    }

    static Optional<WheelEntry> find(ServerPlayer p, PowerSource source, ResourceLocation id) {
        return WheelCatalogue.build(p).stream().filter(e -> e.ref().equals(new PowerRef(source, id))).findFirst();
    }

    static boolean has(ServerPlayer p, PowerSource source, ResourceLocation id) {
        return find(p, source, id).isPresent();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void oursListsBiteOnlyForABiter(GameTestHelper helper) {
        ServerPlayer vampire = ActionGameTests.vampire(helper);
        Optional<WheelEntry> bite = find(vampire, PowerSource.BLOODLINE, Loadout.BITE);
        helper.assertTrue(bite.isPresent(), "a vampire has no Bite entry");
        helper.assertTrue(bite.get().kind() == PowerKind.HELD, "Bite is " + bite.get().kind());
        ServerPlayer human = TestPlayers.create(helper);
        helper.assertTrue(!has(human, PowerSource.BLOODLINE, Loadout.BITE), "a human has a Bite entry");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaRoarListedOnlyWhenAvailable(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        TestAction action = new TestAction();
        ExtraActions.install(action);
        try {
            helper.assertTrue(has(p, PowerSource.BLOODLINE, TEST_ID), "an available action of ours is not listed");
            action.available = false;
            helper.assertTrue(!has(p, PowerSource.BLOODLINE, TEST_ID), "an unavailable action is listed");
        } finally {
            ExtraActions.remove(TEST_ID);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ripHeartAndNeckSnapAreAimed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        // the real ones live in the species part: check what is installed, and the wheel's reading of an aimed action
        for (ResourceLocation id : List.of(ExtraActions.RIP_HEART, ExtraActions.NECK_SNAP)) {
            ExtraActions.get(id).ifPresent(a -> helper.assertTrue(a.aimed(), id + " is not aimed"));
        }
        ExtraActions.get(ExtraActions.ALPHA_ROAR).ifPresent(a -> helper.assertTrue(!a.aimed(), "the Alpha Roar fires at once"));
        TestAction action = new TestAction();
        action.aimed = true;
        ExtraActions.install(action);
        try {
            helper.assertTrue(find(p, PowerSource.BLOODLINE, TEST_ID).orElseThrow().kind() == PowerKind.AIMED, "an aimed action is not AIMED");
            action.aimed = false;
            helper.assertTrue(find(p, PowerSource.BLOODLINE, TEST_ID).orElseThrow().kind() == PowerKind.INSTANT, "a plain action is not INSTANT");
        } finally {
            ExtraActions.remove(TEST_ID);
        }
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void unlockedVampirismActionsListedLockedOnesNot(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation unlocked = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        helper.assertTrue(has(p, PowerSource.VAMPIRISM, unlocked), unlocked + " is unlocked but not listed");
        IFactionPlayer fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        IActionHandler handler = (IActionHandler) fp.getActionHandler();
        int checked = 0;
        for (Object o : VampirismRegistries.ACTION.get()) {
            IAction a = (IAction) o;
            ResourceLocation id = FactionActions.idOf(a);
            if (id != null && a.matchesFaction(VReference.VAMPIRE_FACTION) && !handler.isActionUnlocked(a)) {
                helper.assertTrue(!has(p, PowerSource.VAMPIRISM, id), id + " is not unlocked but listed");
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "setup: the vampire has every action unlocked");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lastingActionIsToggleWithOnState(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation id = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        WheelEntry before = find(p, PowerSource.VAMPIRISM, id).orElseThrow();
        helper.assertTrue(before.kind() == PowerKind.TOGGLE && !before.state().on(), "before: " + before.kind() + " on=" + before.state().on());
        helper.assertTrue(FactionActions.toggleFor(p, id, -1) == null, "the toggle was refused");
        WheelEntry after = find(p, PowerSource.VAMPIRISM, id).orElseThrow();
        helper.assertTrue(after.state().on(), "the entry is not on after the toggle");
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void teleportIsAimed(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation id = ResourceLocation.parse("vampirism:teleport");
        IAction action = FactionActions.byId(id);
        helper.assertTrue(action != null, "Vampirism has no teleport action");
        ((IActionHandler) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getActionHandler()).unlockActions(List.of(action));
        Optional<WheelEntry> e = find(p, PowerSource.VAMPIRISM, id);
        helper.assertTrue(e.isPresent() && e.get().kind() == PowerKind.AIMED, "teleport: " + e.map(WheelEntry::kind));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThrowingProviderIsSkippedNotFatal(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        PowerProvider bad = player -> {
            throw new IllegalStateException("a provider that breaks");
        };
        WheelCatalogue.install(bad);
        try {
            helper.assertTrue(has(p, PowerSource.BLOODLINE, Loadout.BITE), "the other providers vanished with the broken one");
            helper.assertTrue(WheelCatalogue.build(p).size() > 0, "an empty list");
        } finally {
            WheelCatalogue.remove(bad);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noPowersGivesAnEmptyListNotAnError(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.create(helper);
        List<WheelEntry> list = WheelCatalogue.build(human);
        helper.assertTrue(list.stream().noneMatch(e -> e.ref().source() == PowerSource.BLOODLINE || e.ref().source() == PowerSource.VAMPIRISM
                || e.ref().source() == PowerSource.WEREWOLVES), "a human has powers of ours or the factions': " + list.size());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void visionEntryIsListedOnlyForAVampirismVampire(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer vampire = ActionGameTests.vampire(helper);
        unlockAVision(vampire);
        helper.assertTrue(has(vampire, PowerSource.VAMPIRISM, VampirismVision.ID), "a vampire has no Vision entry");
        ServerPlayer human = TestPlayers.create(helper);
        helper.assertTrue(!has(human, PowerSource.VAMPIRISM, VampirismVision.ID), "a human has a Vision entry");
        helper.succeed();
    }

    static void unlockAVision(ServerPlayer p) throws ReflectiveOperationException {
        var vision = VampirismAPI.vampireVisionRegistry().getVisions().get(0);
        VampirismAPI.vampirePlayer(p).getClass().getMethod("unlockVision", de.teamlapen.vampirism.api.entity.player.vampire.IVampireVision.class)
                .invoke(VampirismAPI.vampirePlayer(p), vision);
    }

    /** Plan 23 Task 6: the Vision entry needs a vision the vampire has unlocked (a skill gives it); without one there is nothing to cycle. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWithNoVisionSkillHasNoVisionEntry(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer p = ActionGameTests.vampire(helper);
        for (var v : VampirismAPI.vampireVisionRegistry().getVisions()) {
            VampirismAPI.vampirePlayer(p).getClass().getMethod("unUnlockVision", de.teamlapen.vampirism.api.entity.player.vampire.IVampireVision.class)
                    .invoke(VampirismAPI.vampirePlayer(p), v);
        }
        helper.assertTrue(!VampirismVision.unlocked(p) && !has(p, PowerSource.VAMPIRISM, VampirismVision.ID), "a vampire with no vision has a Vision entry");
        unlockAVision(p);
        helper.assertTrue(VampirismVision.unlocked(p) && has(p, PowerSource.VAMPIRISM, VampirismVision.ID), "a vampire with a vision has no Vision entry");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void visionUseCallsSwitchVisionAndChangesTheCaption(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer p = ActionGameTests.vampire(helper);
        var vision = VampirismAPI.vampireVisionRegistry().getVisions().get(0);
        VampirismAPI.vampirePlayer(p).getClass().getMethod("unlockVision", de.teamlapen.vampirism.api.entity.player.vampire.IVampireVision.class)
                .invoke(VampirismAPI.vampirePlayer(p), vision);
        WheelEntry before = find(p, PowerSource.VAMPIRISM, VampirismVision.ID).orElseThrow();
        helper.assertTrue(!before.state().on() && before.modeName().getString().isEmpty(), "a vampire with no vision on shows one");
        helper.assertTrue(WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, VampirismVision.ID), 0, -1, false) == null, "the Vision use was refused");
        WheelEntry after = find(p, PowerSource.VAMPIRISM, VampirismVision.ID).orElseThrow();
        helper.assertTrue(after.state().on() && !after.modeName().getString().isEmpty(), "the caption did not change: " + after.modeName().getString());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void visionHandleMissingMeansNoEntryAndOneWarning(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer p = ActionGameTests.vampire(helper);
        unlockAVision(p);
        VampirismVision.forceMissingForTest(true);
        try {
            helper.assertTrue(!has(p, PowerSource.VAMPIRISM, VampirismVision.ID), "the Vision entry is listed without a handle");
            find(p, PowerSource.VAMPIRISM, VampirismVision.ID);
            find(p, PowerSource.VAMPIRISM, VampirismVision.ID);
            helper.assertTrue(VampirismVision.warnings() == 1, "warnings: " + VampirismVision.warnings());
        } finally {
            VampirismVision.forceMissingForTest(false);
        }
        helper.assertTrue(has(p, PowerSource.VAMPIRISM, VampirismVision.ID), "the entry did not come back with the handle");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerWhoCannotActSeesEveryPowerLocked(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        helper.assertTrue(find(p, PowerSource.BLOODLINE, Loadout.BITE).orElseThrow().state().lock() == LockReason.NONE, "setup: Bite is locked");
        Predicate blocker = new Predicate();
        ExtraActions.addBlocker(blocker);
        try {
            helper.assertTrue(find(p, PowerSource.BLOODLINE, Loadout.BITE).orElseThrow().state().lock() == LockReason.CANNOT_ACT, "a downed player's Bite is not locked");
        } finally {
            ExtraActions.removeBlocker(blocker);
        }
        helper.succeed();
    }

    private static final class Predicate implements java.util.function.Predicate<ServerPlayer> {
        @Override
        public boolean test(ServerPlayer p) {
            return true;
        }
    }

    // ---- T8: open, push on change, settings ----

    private static List<WheelCataloguePayload> capture() {
        List<WheelCataloguePayload> sent = new ArrayList<>();
        WheelSync.setSink((p, payload) -> sent.add(payload));
        return sent;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void openSendsOneCatalogueAtOnce(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        List<WheelCataloguePayload> sent = capture();
        try {
            WheelSync.open(p);
            helper.assertTrue(sent.size() == 1, "payloads on open: " + sent.size());
            helper.assertTrue(sent.get(0).entries().stream().anyMatch(e -> e.ref().id().equals(Loadout.BITE)), "the catalogue has no Bite");
        } finally {
            WheelSync.resetSink();
            WheelSync.close(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void idleViewerGetsNothingForFortyTicks(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        List<WheelCataloguePayload> sent = capture();
        try {
            WheelSync.open(p);
            for (int i = 1; i <= 40; i++) {
                p.tickCount = i;
                WheelSync.tick(p);
            }
            helper.assertTrue(sent.size() == 1, "payloads after 40 idle ticks: " + sent.size());
        } finally {
            WheelSync.resetSink();
            WheelSync.close(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCooldownChangeSendsExactlyOneUpdate(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        TestAction action = new TestAction();
        ExtraActions.install(action);
        List<WheelCataloguePayload> sent = capture();
        long[] now = {1000};
        WheelClocks.setNowForTest(() -> now[0]);
        try {
            WheelSync.open(p);
            // the cooldown starts: one update, with a stable end and the full length
            action.left = 60;
            p.tickCount = 5;
            WheelSync.tick(p);
            helper.assertTrue(sent.size() == 2, "payloads after the cooldown began: " + sent.size());
            helper.assertTrue(sent.get(1).revision() > sent.get(0).revision(), "the revision did not grow");
            WheelEntry e = sent.get(1).entries().stream().filter(x -> x.ref().id().equals(TEST_ID)).findFirst().orElseThrow();
            helper.assertTrue(e.state().cooldownEnd() == 1060 && e.state().cooldownTotal() == 100, "end " + e.state().cooldownEnd() + " total " + e.state().cooldownTotal());
            // it runs down: the game time moves and the ticks left fall, and nothing is sent
            for (int t = 10; t < 60; t += 5) {
                now[0] = 1000 + t;
                action.left = 60 - t;
                p.tickCount = t;
                WheelSync.tick(p);
            }
            helper.assertTrue(sent.size() == 2, "a running cooldown was sent again: " + sent.size() + " payloads");
            // it ends: exactly one more
            now[0] = 1060;
            action.left = 0;
            p.tickCount = 60;
            WheelSync.tick(p);
            helper.assertTrue(sent.size() == 3, "payloads after the cooldown ended: " + sent.size());
            helper.assertTrue(sent.get(2).entries().stream().filter(x -> x.ref().id().equals(TEST_ID)).findFirst().orElseThrow().state().cooldownEnd() == 0, "the end stayed");
        } finally {
            WheelClocks.setNowForTest(null);
            WheelSync.resetSink();
            WheelSync.close(p);
            ExtraActions.remove(TEST_ID);
        }
        helper.succeed();
    }

    /** The server only checks an open wheel every {@code wheel_catalogue_interval_ticks} (5): of ticks 1 to 40 only the multiples of 5 build. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyEveryFifthTickBuildsForAViewer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        TestAction action = new TestAction();
        ExtraActions.install(action);
        List<WheelCataloguePayload> sent = capture();
        try {
            WheelSync.open(p);
            int before = WheelCatalogue.builds();
            for (int i = 1; i <= 40; i++) {
                action.available = i % 2 == 0; // every build differs from the one before
                p.tickCount = i;
                int builtBefore = WheelCatalogue.builds();
                WheelSync.tick(p);
                boolean built = WheelCatalogue.builds() != builtBefore;
                helper.assertTrue(built == (i % 5 == 0), "tick " + i + (built ? " built" : " did not build"));
            }
            helper.assertTrue(WheelCatalogue.builds() - before == 8, "builds in 40 ticks: " + (WheelCatalogue.builds() - before));
            helper.assertTrue(sent.size() == 9, "payloads: " + sent.size());
        } finally {
            WheelSync.resetSink();
            WheelSync.close(p);
            ExtraActions.remove(TEST_ID);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void closedPlayerIsNeverBuilt(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        int before = WheelCatalogue.builds();
        for (int i = 1; i <= 40; i++) {
            p.tickCount = i;
            WheelSync.tick(p);
        }
        helper.assertTrue(WheelCatalogue.builds() == before, "the catalogue was built for a closed wheel");
        WheelSync.open(p);
        WheelSync.close(p);
        int afterClose = WheelCatalogue.builds();
        WheelSync.tick(p);
        helper.assertTrue(WheelCatalogue.builds() == afterClose, "the catalogue was built after the wheel closed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void logoutRemovesTheViewer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<WheelCataloguePayload> sent = capture();
        try {
            WheelSync.open(p);
            helper.assertTrue(WheelSync.viewing(p), "setup: not viewing");
            WheelService.forget(p);
            helper.assertTrue(!WheelSync.viewing(p), "still a viewer after forget (logout, death, dimension change)");
            int n = sent.size();
            WheelSync.tick(p);
            helper.assertTrue(sent.size() == n, "an update went to a player who left");
        } finally {
            WheelSync.resetSink();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wheelDisabledSendsAnEmptyListAndRefusesUse(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        List<WheelCataloguePayload> sent = capture();
        HudConfig.WHEEL_ENABLED.set(false);
        try {
            WheelSync.open(p);
            helper.assertTrue(sent.size() == 1 && sent.get(0).entries().isEmpty(), "a disabled wheel sent powers");
            HudNet.forgetRateLimit(p);
            String refusal = HudNet.handleUse(p, new UsePowerPayload(new PowerRef(PowerSource.BLOODLINE, Loadout.BITE).key(), 0, -1, false));
            helper.assertTrue(refusal != null && refusal.endsWith("wheel_disabled"), "use with the wheel off: " + refusal);
        } finally {
            HudConfig.WHEEL_ENABLED.set(true);
            WheelSync.resetSink();
            WheelSync.close(p);
        }
        helper.succeed();
    }
}
