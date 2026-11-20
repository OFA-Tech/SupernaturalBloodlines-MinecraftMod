package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;

/** R110 rows 1 and 4: the Werewolves bite. Needs Werewolves. */
@PrefixGameTestTemplate(false)
public final class BiteWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BiteWerewolfGameTests() {
    }

    static ServerPlayer wolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        BiteGameTests.place(helper, p, 1.5);
        return p;
    }

    /** {@code WerewolfPlayer.switchForm} is public, but its class needs the Vampirism full jar to compile against. */
    static void switchForm(ServerPlayer p, WerewolfForm form) {
        Object wolf = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        try {
            MethodHandles.publicLookup().findVirtual(wolf.getClass(), "switchForm", MethodType.methodType(void.class, WerewolfForm.class)).invoke(wolf, form);
        } catch (Throwable e) {
            throw new IllegalStateException("cannot switch the form", e);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNewWerewolfGetsBiteInSlotOne(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService.offerBite(p);
        var slot = net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService.get(p).get(0);
        helper.assertTrue(slot.equals(java.util.Optional.of(net.ofatech.supernaturalbloodlines.core.hud.Loadout.BITE)), "slot 1 is " + slot);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wolfFormBiteUsesWerewolves(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        switchForm(p, WerewolfForm.BEAST);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(BiteService.actionFor(p) == BiteAction.WEREWOLF_BITE, "a beast's bite is " + BiteService.actionFor(p));
        float health = v.getHealth();
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(r.ok(), "the werewolf bite was refused: " + r.refusal());
        helper.assertTrue(v.getHealth() < health, "Werewolves' bite did not hurt the villager");
        switchForm(p, WerewolfForm.NONE);
        v.discard();
        helper.succeed();
    }

    /** R110 row T never matches for a werewolf (the turning would only end quietly), so row 4 offers the werewolf bite. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfInTurningNotOffered(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(9000).build());
        try {
            helper.assertTrue(BiteService.actionFor(p) == BiteAction.WEREWOLF_BITE, "a werewolf in the turning bites with " + BiteService.actionFor(p));
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** The half form is transformed (Werewolves' own canBite() says so), so it is a wolf form: row 1, not row 4. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void halfFormTakesRowOne(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        switchForm(p, WerewolfForm.HUMAN);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(BiteService.actionFor(p) == BiteAction.WEREWOLF_BITE, "a werewolf in the half form bites with " + BiteService.actionFor(p));
        float health = v.getHealth();
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(r.ok() && v.getHealth() < health, "the half form's bite: " + r);
        switchForm(p, WerewolfForm.NONE);
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfInHumanFormIsRefusedByWerewolves(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(BiteService.actionFor(p) == BiteAction.WEREWOLF_BITE, "row 4: a werewolf in human form still gets the werewolf bite: " + BiteService.actionFor(p));
        BiteService.Result r = BiteService.press(p, v.getId());
        helper.assertTrue(!r.ok() && r.refusal().endsWith("cannot_bite") && v.getHealth() == v.getMaxHealth(), "an untransformed werewolf bit: " + r);
        v.discard();
        helper.succeed();
    }
}
