package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.FactionActions;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;

/** What the server lets a player put in a slot when Vampirism is there: only what the player's own faction has unlocked. */
@PrefixGameTestTemplate(false)
public final class LoadoutVampirismGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private LoadoutVampirismGameTests() {
    }

    /** A vampire action that has nothing to do with the player's skills: found by registry order, so it is whatever Vampirism has first. */
    static ResourceLocation anActionOf(de.teamlapen.vampirism.api.entity.factions.IPlayableFaction<?> faction) {
        for (IAction<?> a : VampirismRegistries.ACTION.get()) {
            if (a.matchesFaction(faction) && FactionActions.idOf(a) != null) {
                return FactionActions.idOf(a);
            }
        }
        throw new IllegalStateException("no action of " + faction.getID());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void unlock(ServerPlayer p, ResourceLocation id) {
        IFactionPlayer<?> fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        ((IActionHandler) fp.getActionHandler()).unlockActions(List.of(FactionActions.byId(id)));
    }

    static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 4), "could not make a vampire");
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNewVampireGetsBiteInSlotOne(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        LoadoutService.offerDefaults(p);
        Loadout l = LoadoutService.get(p);
        helper.assertTrue(l.get(0).equals(Optional.of(Loadout.BITE)), "slot 1 is " + l.get(0));
        for (int i = 1; i < Loadout.SLOTS; i++) {
            helper.assertTrue(l.get(i).isEmpty(), "slot " + (i + 1) + " is " + l.get(i));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteIsNeverPutInASlotThePlayerSetOrBackAfterTheyClearIt(GameTestHelper helper) {
        ServerPlayer set = TestPlayers.createMortal(helper);
        ResourceLocation action = anActionOf(VReference.VAMPIRE_FACTION);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(set).setFactionAndLevel(VReference.VAMPIRE_FACTION, 4), "could not make a vampire");
        unlock(set, action);
        helper.assertTrue(LoadoutService.set(set, 0, action) == null, "the player could not set slot 1");
        LoadoutService.offerDefaults(set);
        helper.assertTrue(LoadoutService.get(set).get(0).equals(Optional.of(action)), "a slot the player set was overwritten: " + LoadoutService.get(set).get(0));
        ServerPlayer cleared = vampire(helper);
        LoadoutService.offerDefaults(cleared);
        helper.assertTrue(LoadoutService.set(cleared, 0, null) == null, "clearing slot 1 was refused");
        LoadoutService.offerDefaults(cleared);
        helper.assertTrue(LoadoutService.get(cleared).get(0).isEmpty(), "a cleared Bite was put back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteArrivesWhenAHumanBecomesAVampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        LoadoutService.offerDefaults(p);
        helper.assertTrue(LoadoutService.get(p).get(0).isEmpty(), "a human had Bite");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        LoadoutService.offerDefaults(p);
        helper.assertTrue(LoadoutService.get(p).get(0).equals(Optional.of(Loadout.BITE)), "the new vampire has " + LoadoutService.get(p).get(0));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void setLoadoutAcceptsUnlocked(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = anActionOf(VReference.VAMPIRE_FACTION);
        unlock(p, action);
        helper.assertTrue(LoadoutService.set(p, 2, action) == null, "an unlocked action was refused");
        helper.assertTrue(LoadoutService.get(p).get(2).equals(Optional.of(action)), "the slot is " + LoadoutService.get(p).get(2));
        helper.assertTrue(LoadoutService.set(p, 4, action) == null, "a duplicate was refused");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void setLoadoutRefusesLockedOrForeign(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation locked = anActionOf(VReference.VAMPIRE_FACTION);
        helper.assertTrue(!FactionActions.unlocked(p, locked), "the test action is already unlocked");
        String refusal = LoadoutService.set(p, 1, locked);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a locked action: " + refusal);
        String unknown = LoadoutService.set(p, 1, ResourceLocation.parse("vampirism:no_such_action"));
        helper.assertTrue(unknown != null && unknown.endsWith("unknown_action"), "an unknown action: " + unknown);
        if (ModList.get().isLoaded("werewolves")) {
            ResourceLocation wolf = anActionOf(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION);
            String foreign = LoadoutService.set(p, 1, wolf);
            helper.assertTrue(foreign != null && foreign.endsWith("not_unlocked"), "a werewolf action for a vampire: " + foreign);
        }
        helper.assertTrue(LoadoutService.get(p).get(1).isEmpty(), "a refused set changed the loadout");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anActionOfAnotherFactionIsNotAssignableEvenIfUnlockedElsewhere(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = anActionOf(VReference.VAMPIRE_FACTION);
        unlock(p, action);
        LoadoutService.offerDefaults(p);
        // the player is cured: no faction. The list is kept, but nothing new can be put in
        helper.assertTrue(LoadoutService.set(p, 2, action) == null, "setup: the unlocked action was refused");
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(LoadoutService.get(p).get(2).equals(Optional.of(action)), "a faction change cleared the loadout: " + LoadoutService.get(p));
        String refusal = LoadoutService.set(p, 3, action);
        helper.assertTrue(refusal != null && refusal.endsWith("not_unlocked"), "a cured player got an action: " + refusal);
        helper.assertTrue(LoadoutService.get(p).get(0).equals(Optional.of(Loadout.BITE)), "a faction change touched Bite");
        helper.succeed();
    }

    /** A cured vampire keeps the list; its slots show greyed later (the client), nothing is cleared here. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void loadoutNotClearedOnFactionChange(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ResourceLocation action = anActionOf(VReference.VAMPIRE_FACTION);
        unlock(p, action);
        LoadoutService.set(p, 5, action);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(LoadoutService.get(p).get(5).equals(Optional.of(action)), "the faction change cleared slot 6: " + LoadoutService.get(p));
        helper.succeed();
    }
}
