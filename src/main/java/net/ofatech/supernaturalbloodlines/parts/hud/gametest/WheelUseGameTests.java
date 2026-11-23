package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.UsePowerPayload;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** What the server does with a power asked for from the wheel: the same refusals as the slot or key would give, and nothing for a forged or stale request. Needs Vampirism. */
@PrefixGameTestTemplate(false)
public final class WheelUseGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final PowerRef BITE = new PowerRef(PowerSource.BLOODLINE, Loadout.BITE);

    private WheelUseGameTests() {
    }

    private static final class Down implements Predicate<ServerPlayer> {
        @Override
        public boolean test(ServerPlayer p) {
            return true;
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void downedPlayerCannotUseFromWheel(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation action = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        Down down = new Down();
        ExtraActions.addBlocker(down);
        try {
            String bite = WheelService.use(p, BITE, 0, -1, false);
            helper.assertTrue(bite != null && bite.endsWith("cannot_act"), "Bite from the wheel while downed: " + bite);
            String faction = WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, action), 0, -1, false);
            helper.assertTrue(faction != null && faction.endsWith("cannot_act"), "a faction action while downed: " + faction);
            helper.assertTrue(!ActionGameTests.active(p, action), "the action ran");
        } finally {
            ExtraActions.removeBlocker(down);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snappedAndDyingCannotUse(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation action = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        PowerRef ref = new PowerRef(PowerSource.VAMPIRISM, action);
        p.setGameMode(GameType.SPECTATOR);
        String spectator = WheelService.use(p, ref, 0, -1, false);
        helper.assertTrue(spectator != null && spectator.endsWith("cannot_act"), "a spectator: " + spectator);
        p.setGameMode(GameType.SURVIVAL);
        p.setHealth(0f);
        String dead = WheelService.use(p, ref, 0, -1, false);
        helper.assertTrue(dead != null && dead.endsWith("cannot_act"), "a dead player: " + dead);
        helper.assertTrue(!ActionGameTests.active(p, action), "the action ran");
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void forgedUseOfALockedActionRefused(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        IFactionPlayer fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        IActionHandler handler = (IActionHandler) fp.getActionHandler();
        ResourceLocation locked = null;
        for (Object o : VampirismRegistries.ACTION.get()) {
            IAction a = (IAction) o;
            ResourceLocation id = FactionActions.idOf(a);
            if (id != null && a instanceof de.teamlapen.vampirism.api.entity.player.actions.ILastingAction && a.matchesFaction(VReference.VAMPIRE_FACTION)
                    && !handler.isActionUnlocked(a)) {
                locked = id;
                break;
            }
        }
        helper.assertTrue(locked != null, "setup: no locked vampire action");
        String refusal = WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, locked), 0, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a forged use of " + locked + ": " + refusal);
        helper.assertTrue(!ActionGameTests.active(p, locked), "the locked action ran");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wheelRefusalsEqualSlotRefusals(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation action = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        PowerRef ref = new PowerRef(PowerSource.VAMPIRISM, action);
        LoadoutService.set(p, 3, action);
        // a cooldown: on then off leaves one
        helper.assertTrue(ActionService.triggerById(p, action, -1, false) == null, "setup: the first use was refused");
        helper.assertTrue(ActionService.triggerById(p, action, -1, false) == null, "setup: the second use was refused");
        String slot = ActionService.trigger(p, 3, -1, false);
        String wheel = WheelService.use(p, ref, 0, -1, false);
        helper.assertTrue(slot != null && slot.equals(wheel), "cooldown: slot " + slot + " wheel " + wheel);
        // downed
        Down down = new Down();
        ExtraActions.addBlocker(down);
        try {
            helper.assertTrue(java.util.Objects.equals(ActionService.trigger(p, 3, -1, false), WheelService.use(p, ref, 0, -1, false)), "downed refusals differ");
        } finally {
            ExtraActions.removeBlocker(down);
        }
        // no target for a bite
        String slotBite = ActionService.trigger(p, 0, -1, false);
        String wheelBite = WheelService.use(p, BITE, 0, -1, false);
        helper.assertTrue(java.util.Objects.equals(slotBite, wheelBite), "bite with no target: slot " + slotBite + " wheel " + wheelBite);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void releaseWithNothingHeldIsIgnored(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        helper.assertTrue(WheelService.use(p, BITE, 0, -1, true) == null, "a release with nothing held answered");
        helper.assertTrue(!ActionService.heldId(p, Loadout.BITE), "a release started something");
        helper.assertTrue(HudNet.handleUse(p, new UsePowerPayload("nonsense", 0, -1, true)) == null, "a forged release answered");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forgetReleasesAHeldBite(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        int mine = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        String refusal = WheelService.use(p, BITE, 0, v.getId(), false);
        helper.assertTrue(refusal == null, "the bite from the wheel was refused: " + refusal);
        helper.assertTrue(ActionService.heldId(p, Loadout.BITE), "the bite is not held");
        for (int i = 0; i < 40 && VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() <= mine; i++) {
            Feeding.tick(p);
        }
        int drunk = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        helper.assertTrue(drunk > mine, "setup: the feeding never started");
        WheelService.forget(p);
        helper.assertTrue(!ActionService.heldId(p, Loadout.BITE), "the bite is still held after forget");
        // ending a feeding lets what was already taken settle (a few ticks); after that nothing more may come
        for (int i = 0; i < 20; i++) {
            Feeding.tick(p);
        }
        int settled = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        for (int i = 0; i < 40; i++) {
            Feeding.tick(p);
        }
        helper.assertTrue(VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() == settled, "the feeding went on after forget: " + settled + " to "
                + VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel());
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unknownRefAndWrongSourceAreRefused(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        ResourceLocation action = ActionGameTests.usableLasting(p, VReference.VAMPIRE_FACTION);
        String[] refusals = {
                WheelService.use(p, new PowerRef(PowerSource.BLOODLINE, ResourceLocation.parse("supernaturalbloodlines:nothing")), 0, -1, false),
                // a real faction action, named as ours: must not run
                WheelService.use(p, new PowerRef(PowerSource.BLOODLINE, action), 0, -1, false),
                // our Bite, named as Vampirism's
                WheelService.use(p, new PowerRef(PowerSource.VAMPIRISM, Loadout.BITE), 0, -1, false),
                // a Vampirism action named as Werewolves'
                WheelService.use(p, new PowerRef(PowerSource.WEREWOLVES, action), 0, -1, false),
                // not run by the server at all
                WheelService.use(p, new PowerRef(PowerSource.INFINITY, ResourceLocation.parse("infinity:soul")), 0, -1, false),
                WheelService.use(p, new PowerRef(PowerSource.GREAT_SAGE, ResourceLocation.parse("greatsage:scan")), 0, -1, false),
        };
        for (int i = 0; i < refusals.length; i++) {
            helper.assertTrue(refusals[i] != null, "forged use #" + i + " was accepted");
        }
        helper.assertTrue(!ActionGameTests.active(p, action), "a forged use ran the action");
        helper.assertTrue(refusals[4].endsWith("not_server_power") && refusals[5].endsWith("not_server_power"), "not_server_power: " + refusals[4]);
        helper.succeed();
    }

    /** The refusal is the same whoever the target is: nothing names what the target was. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void noRefusalNamesTheTarget(GameTestHelper helper) {
        ServerPlayer p = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(helper);
        ServerPlayer other = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.create(helper);
        PathfinderMob villager = BiteGameTests.villager(helper, 2.5);
        WheelCatalogueGameTests.TestAction action = new WheelCatalogueGameTests.TestAction() {
            @Override
            public Component trigger(ServerPlayer player, net.minecraft.world.entity.LivingEntity target) {
                return Component.translatable("supernaturalbloodlines.hud.action.disallowed");
            }
        };
        action.aimed = true;
        ExtraActions.install(action);
        try {
            PowerRef ref = new PowerRef(PowerSource.BLOODLINE, action.id());
            String atVillager = WheelService.use(p, ref, 0, villager.getId(), false);
            String atPlayer = WheelService.use(p, ref, 0, other.getId(), false);
            String atNothing = WheelService.use(p, ref, 0, -1, false);
            helper.assertTrue(atVillager != null && atVillager.equals(atPlayer) && atPlayer.equals(atNothing), "refusals differ by target: " + atVillager + ", " + atPlayer + ", " + atNothing);
        } finally {
            ExtraActions.remove(action.id());
            villager.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void usePowerFloodIsDropped(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        HudNet.forgetRateLimit(p);
        UsePowerPayload press = new UsePowerPayload(BITE.key(), 0, -1, false);
        AtomicInteger limited = new AtomicInteger();
        for (int i = 0; i < 15; i++) {
            if (HudNet.RATE_LIMITED.equals(HudNet.handleUse(p, press))) {
                limited.incrementAndGet();
            }
        }
        helper.assertTrue(limited.get() == 5, "presses dropped: " + limited.get() + " (limit " + net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.rateLimitPerSecond() + " a second)");
        helper.assertTrue(!HudNet.RATE_LIMITED.equals(HudNet.handleUse(p, new UsePowerPayload(BITE.key(), 0, -1, true))), "a release was rate limited");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aForgedModeChangeOfAnActionIsRefused(GameTestHelper helper) {
        ServerPlayer p = ActionGameTests.vampire(helper);
        HudNet.forgetRateLimit(p);
        String refusal = HudNet.handleMode(p, new net.ofatech.supernaturalbloodlines.parts.hud.net.SetPowerModePayload(BITE.key(), 1));
        helper.assertTrue(refusal != null && refusal.endsWith("no_modes"), "a mode change of Bite: " + refusal);
        String junk = HudNet.handleMode(p, new net.ofatech.supernaturalbloodlines.parts.hud.net.SetPowerModePayload("???", 1));
        helper.assertTrue(junk != null && junk.endsWith("unknown_power"), "a mode change of junk: " + junk);
        helper.succeed();
    }
}
