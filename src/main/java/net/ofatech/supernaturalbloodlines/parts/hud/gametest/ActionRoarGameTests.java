package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** (Own batch: the Roar's cooldowns and permits are global, so these must not run beside tests that roar.) The Alpha Roar as a HUD action (it has no key of its own): who gets it, where it is put, and that the slot still asks the species part's own checks. Needs the species part and Werewolves. */
@PrefixGameTestTemplate(false)
public final class ActionRoarGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ActionRoarGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).protectPath(false).betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 4), "the test wolf could not join the werewolves");
        return p;
    }

    /** Leaves nothing behind for the tests that run after: the Roar's cooldowns and permits, the pack index, the effects. */
    private static void clean(ServerPlayer p) {
        net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.reset();
        net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex index = net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.get(p.serverLevel());
        index.leave(p.getUUID());
        index.takeDissolved(p.getUUID());
        p.removeAllEffects();
        if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        }
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void theRoarIsAnActionOfTheSpeciesPart(GameTestHelper helper) {
        helper.assertTrue(ExtraActions.get(ExtraActions.ALPHA_ROAR).isPresent(), "the species part did not install the Roar");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void anAlphaGetsBiteInSlotOneAndTheRoarInTheNextEmptySlot(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        LoadoutService.offerDefaults(p);
        Loadout l = LoadoutService.get(p);
        helper.assertTrue(l.get(0).equals(Optional.of(Loadout.BITE)), "slot 1 is " + l.get(0));
        helper.assertTrue(l.get(1).equals(Optional.of(ExtraActions.ALPHA_ROAR)), "slot 2 is " + l.get(1));
        for (int i = 2; i < Loadout.SLOTS; i++) {
            helper.assertTrue(l.get(i).isEmpty(), "slot " + (i + 1) + " is " + l.get(i));
        }
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void theRoarNeverOverwritesASlotThePlayerSet(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.TRUE_ALPHA);
        helper.assertTrue(LoadoutService.set(p, 0, Loadout.BITE) == null && LoadoutService.set(p, 1, Loadout.BITE) == null, "setup: the player could not set slots 1 and 2");
        LoadoutService.offerDefaults(p);
        Loadout l = LoadoutService.get(p);
        helper.assertTrue(l.get(0).equals(Optional.of(Loadout.BITE)) && l.get(1).equals(Optional.of(Loadout.BITE)), "a slot the player set was overwritten: " + l);
        helper.assertTrue(l.get(2).equals(Optional.of(ExtraActions.ALPHA_ROAR)), "the Roar is not in the first empty slot: " + l);
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void aFullLoadoutTakesNothingAndTheRoarIsNotPutBackAfterTheyClearIt(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        for (int i = 0; i < Loadout.SLOTS; i++) {
            helper.assertTrue(LoadoutService.set(p, i, Loadout.BITE) == null, "setup: slot " + (i + 1) + " was refused");
        }
        LoadoutService.offerDefaults(p);
        helper.assertTrue(!LoadoutService.get(p).contains(ExtraActions.ALPHA_ROAR), "the Roar was put over a slot: " + LoadoutService.get(p));
        // not spent while there was no room: it goes in when a slot is free, and then it is never put back after a clear
        LoadoutService.set(p, 3, null);
        LoadoutService.offerDefaults(p);
        helper.assertTrue(LoadoutService.get(p).get(3).equals(Optional.of(ExtraActions.ALPHA_ROAR)), "the Roar did not take the free slot: " + LoadoutService.get(p));
        LoadoutService.set(p, 3, null);
        LoadoutService.offerDefaults(p);
        helper.assertTrue(LoadoutService.get(p).get(3).isEmpty(), "a spent Roar came back: " + LoadoutService.get(p));
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void aBetaOrAHumanGetsNoRoarAndCannotAssignIt(GameTestHelper helper) {
        ServerPlayer beta = wolf(helper, Rank.BETA);
        LoadoutService.offerDefaults(beta);
        helper.assertTrue(!LoadoutService.get(beta).contains(ExtraActions.ALPHA_ROAR), "a Beta was given the Roar: " + LoadoutService.get(beta));
        String refusal = LoadoutService.set(beta, 2, ExtraActions.ALPHA_ROAR);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a Beta assigned the Roar: " + refusal);
        ServerPlayer human = TestPlayers.createMortal(helper);
        LoadoutService.offerDefaults(human);
        helper.assertTrue(!LoadoutService.get(human).contains(ExtraActions.ALPHA_ROAR), "a human was given the Roar");
        helper.assertTrue(LoadoutService.set(human, 2, ExtraActions.ALPHA_ROAR) != null, "a human assigned the Roar");
        clean(beta);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void aSlotWithTheRoarRoarsOnceThenAsksTheCooldown(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        helper.assertTrue(LoadoutService.set(p, 4, ExtraActions.ALPHA_ROAR) == null, "the Alpha could not assign the Roar");
        helper.assertTrue(ActionService.trigger(p, 4, -1, false) == null, "the Roar from its slot was refused");
        String again = ActionService.trigger(p, 4, -1, false);
        helper.assertTrue(again != null && again.endsWith("roar_cooldown"), "the second Roar was not on cooldown: " + again);
        clean(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void aDemotedAlphaCannotRoarFromTheSlot(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        LoadoutService.set(p, 4, ExtraActions.ALPHA_ROAR);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.BETA).geneRolled(true).protectPath(false).betaSince(1).build()).build());
        String refusal = ActionService.trigger(p, 4, -1, false);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a Beta roared from a kept slot: " + refusal);
        clean(p);
        helper.succeed();
    }

    /** Every path that roars (a slot, the Pack button, {@code /pack roar}) tells the slot its cooldown: the species part reports it, not the slot's caller. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void aRoarFromAnyPathReportsItsCooldownToTheSlot(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        java.util.List<Integer> heard = new java.util.ArrayList<>();
        ExtraActions.CooldownListener l = (who, id, ticks) -> {
            if (who == p && id.equals(ExtraActions.ALPHA_ROAR)) {
                heard.add(ticks);
            }
        };
        ExtraActions.onCooldown(l);
        try {
            net.minecraft.network.chat.Component refusal = net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.roar(p); // what /pack roar and the Pack button call
            helper.assertTrue(refusal == null, "the roar was refused: " + refusal);
            helper.assertTrue(heard.size() == 1 && heard.get(0) > 0, "the cooldown was not reported once: " + heard);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.cooldownLeft(p) > 0, "no cooldown left to send on login");
        } finally {
            ExtraActions.removeCooldownListener(l);
            clean(p);
        }
        helper.succeed();
    }

    /** The Roar key's payload counts against the same limit as the HUD's ({@code 10} a second): the eleventh in a second is dropped, a refused one counts too. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "hud_roar")
    public static void theRoarPayloadIsRateLimitedLikeTheHudsPayloads(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.ALPHA);
        net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.forgetPayloadLimit(p);
        int limit = net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.PAYLOADS_PER_SECOND;
        helper.assertTrue(limit == net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.rateLimitPerSecond(), "the limits differ: " + limit);
        for (int i = 0; i < limit; i++) {
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.payloadAllowed(p), "payload " + i + " was dropped");
        }
        helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.payloadAllowed(p), "the payload over the limit was not dropped");
        net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.handlePayload(p);
        helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.cooldownLeft(p) == 0, "a dropped payload still roared");
        net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar.forgetPayloadLimit(p);
        clean(p);
        helper.succeed();
    }
}
