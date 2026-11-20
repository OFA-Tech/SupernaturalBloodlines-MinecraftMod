package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.TriggerActionPayload;

/** What a Supernatural-mode slot key does on the server (13b Task 3): the action comes from the player's loadout, and Bite is one of the actions. Needs Vampirism. */
@PrefixGameTestTemplate(false)
public final class ActionGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ActionGameTests() {
    }

    /** A lasting action of the faction that the player can use right now, unlocked for the test (found by registry order, so it is whatever works first). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static ResourceLocation usableLasting(ServerPlayer p, IPlayableFaction<?> faction) {
        IFactionPlayer fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        for (Object o : VampirismRegistries.ACTION.get()) {
            IAction action = (IAction) o;
            ResourceLocation id = FactionActions.idOf(action);
            // werewolves:human_form turns a werewolf back into a human: it is "usable" for a human-form player but its toggle refuses
            if (id != null && !id.getPath().equals("human_form") && action instanceof ILastingAction && action.matchesFaction(faction)) {
                ((IActionHandler) fp.getActionHandler()).unlockActions(List.of(action));
                if (action.canUse(fp) == IAction.PERM.ALLOWED) {
                    return id;
                }
            }
        }
        throw new IllegalStateException("no usable lasting action for " + faction.getID());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean active(ServerPlayer p, ResourceLocation id) {
        return ((IActionHandler) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getActionHandler()).isActionActive(id);
    }

    static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = BiteGameTests.vampire(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 4), "could not make a level 4 vampire");
        LoadoutService.offerBite(p); // what the once-a-second check does for a new vampire
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void triggerTogglesUnlockedAction(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        LoadoutService.set(p, 2, action);
        helper.assertTrue(ActionService.trigger(p, 2, -1, false) == null, "the trigger was refused");
        helper.assertTrue(active(p, action), action + " is not active after the trigger");
        // the second press, after the cooldown it left, would be refused; deactivating is Vampirism's own toggle, which does not ask the cooldown
        helper.assertTrue(ActionService.trigger(p, 2, -1, true) == null, "a release is not a refusal");
        helper.assertTrue(active(p, action), "a release toggled the action");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void secondTriggerTurnsALastingActionOff(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        LoadoutService.set(p, 0, action);
        ActionService.trigger(p, 0, -1, false);
        helper.assertTrue(active(p, action), "not active after the first trigger");
        String second = ActionService.trigger(p, 0, -1, false);
        helper.assertTrue(second == null && !active(p, action), "the second trigger did not switch it off: " + second);
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void triggerRespectsCooldown(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        LoadoutService.set(p, 1, action);
        ActionService.trigger(p, 1, -1, false);
        ActionService.trigger(p, 1, -1, false); // off: now on cooldown
        IActionHandler handler = (IActionHandler) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getActionHandler();
        helper.assertTrue(handler.isActionOnCooldown(FactionActions.byId(action)), "setup: the action is not on cooldown");
        String refusal = ActionService.trigger(p, 1, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("cooldown"), "a trigger during the cooldown: " + refusal);
        helper.assertTrue(!active(p, action), "the cooldown did not stop it");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void triggerLockedRefused(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        // the player's own list names an action that is then taken away (a respec, a faction change): the server checks again at trigger time
        LoadoutService.set(p, 4, action);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        String refusal = ActionService.trigger(p, 4, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a trigger with no faction: " + refusal);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void triggerUsesLoadoutNotPacket(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        LoadoutService.set(p, 2, action);
        LoadoutService.set(p, 0, null);
        HudNet.forgetRateLimit(p);
        // the payload names only a slot: slot 1 is empty, so nothing happens, whatever else the loadout holds
        String refusal = HudNet.handleTrigger(p, new TriggerActionPayload(0, -1, false));
        helper.assertTrue(refusal != null && refusal.endsWith("empty_slot"), "an empty slot: " + refusal);
        helper.assertTrue(!active(p, action), "another slot's action ran");
        String badSlot = HudNet.handleTrigger(p, new TriggerActionPayload(7, -1, false));
        helper.assertTrue(badSlot != null && badSlot.endsWith("bad_slot"), "a slot out of range: " + badSlot);
        helper.succeed();
    }

    /** A release counts only for a press the server accepted: a stray release must not end a feeding that something else started. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aReleaseWithNothingHeldDoesNothing(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        int mine = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        helper.assertTrue(BiteService.press(p, v.getId()).ok(), "setup: the bite was refused"); // started without ActionService: no press of slot 1 is held
        helper.assertTrue(!ActionService.held(p, 0), "setup: a press that ActionService never saw is held");
        helper.assertTrue(ActionService.trigger(p, 0, -1, true) == null, "a release with nothing held was refused");
        for (int i = 0; i < 40 && VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() <= mine; i++) {
            Feeding.tick(p);
        }
        helper.assertTrue(VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() > mine, "the stray release ended the feeding");
        BiteService.release(p);
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aReleaseAfterAnAcceptedPressEndsTheFeeding(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        int mine = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        helper.assertTrue(ActionService.trigger(p, 0, v.getId(), false) == null, "the press was refused");
        helper.assertTrue(ActionService.held(p, 0), "an accepted press is not noted as held");
        helper.assertTrue(ActionService.trigger(p, 0, -1, true) == null, "the release was refused");
        helper.assertTrue(!ActionService.held(p, 0), "the release left the press held");
        for (int i = 0; i < 10; i++) {
            Feeding.tick(p);
        }
        helper.assertTrue(VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() <= mine, "the feeding went on after the release");
        v.discard();
        helper.succeed();
    }

    /** A fake or mock player, or a client without our channel, cannot take a payload: the sync is skipped instead of throwing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLoadoutIsNotSentToAPlayerWithoutTheChannel(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        helper.assertTrue(!LoadoutService.canReceive(p), "a test player claims to have the channel");
        try {
            LoadoutService.sync(p);
            LoadoutService.set(p, 3, Loadout.BITE); // sets and syncs
        } catch (RuntimeException e) {
            helper.fail("sending to a player without the channel threw: " + e);
            return;
        }
        helper.assertTrue(LoadoutService.get(p).get(3).equals(java.util.Optional.of(Loadout.BITE)), "the change was lost with the sync");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forgettingAPlayerClearsTheirHeldPresses(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(ActionService.trigger(p, 0, v.getId(), false) == null, "the press was refused");
        helper.assertTrue(ActionService.held(p, 0), "setup: not held");
        ActionService.forget(p);
        helper.assertTrue(!ActionService.held(p, 0), "a logged out player is still holding a key");
        BiteService.release(p);
        v.discard();
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSleepingPlayerCannotTriggerAnAction(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = usableLasting(p, VReference.VAMPIRE_FACTION);
        LoadoutService.set(p, 2, action);
        p.setPose(net.minecraft.world.entity.Pose.SLEEPING);
        p.setSleepingPos(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)));
        helper.assertTrue(p.isSleeping(), "setup: the player is not asleep");
        String refusal = ActionService.trigger(p, 2, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("cannot_act") && !active(p, action), "a sleeping player triggered it: " + refusal);
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteFiresFromItsSlot(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        int villagerBlood = BiteGameTests.blood(v);
        int mine = VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
        helper.assertTrue(LoadoutService.get(p).get(0).equals(java.util.Optional.of(Loadout.BITE)), "Bite is not in slot 1 by default");
        helper.assertTrue(ActionService.trigger(p, 0, v.getId(), false) == null, "Bite from slot 1 was refused");
        for (int i = 0; i < 40 && VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() <= mine; i++) {
            Feeding.tick(p);
        }
        helper.assertTrue(ActionService.trigger(p, 0, -1, true) == null, "the release was refused");
        helper.assertTrue(BiteGameTests.blood(v) < villagerBlood && VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel() > mine,
                "the feeding did not run: villager " + BiteGameTests.blood(v) + ", vampire " + VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel());
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void movedBiteFiresFromTheNewSlotOnly(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        LoadoutService.set(p, 3, Loadout.BITE);
        LoadoutService.set(p, 0, null);
        String old = ActionService.trigger(p, 0, v.getId(), false);
        helper.assertTrue(old != null && old.endsWith("empty_slot") && v.getHealth() == v.getMaxHealth(), "slot 1 still bites: " + old);
        helper.assertTrue(ActionService.trigger(p, 3, v.getId(), false) == null, "Bite from slot 4 was refused");
        helper.assertTrue(BiteService.cooldownLeft(p) > 0, "slot 4 did not bite");
        v.discard();
        helper.succeed();
    }

    /** Every press counts against the rate limit, a refused one too; a release never does. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyTriggerPressCountsAgainstTheRateLimit(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        LoadoutService.set(p, 0, null);
        HudNet.forgetRateLimit(p);
        int limit = net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.rateLimitPerSecond();
        for (int i = 0; i < limit; i++) {
            String r = HudNet.handleTrigger(p, new TriggerActionPayload(0, -1, false)); // empty slot: refused each time
            helper.assertTrue(r != null && r.endsWith("empty_slot"), "press " + i + ": " + r);
        }
        helper.assertTrue(HudNet.RATE_LIMITED.equals(HudNet.handleTrigger(p, new TriggerActionPayload(0, -1, false))), "a press over the limit was not dropped");
        helper.assertTrue(HudNet.handleTrigger(p, new TriggerActionPayload(0, -1, true)) == null, "a release was dropped");
        helper.succeed();
    }
}
