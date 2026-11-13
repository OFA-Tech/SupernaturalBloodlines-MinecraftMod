package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl.Control;

/** The Moon Ring (R96). Links Werewolves, so it is registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class MoonRingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private MoonRingGameTests() {
    }

    private static ServerPlayer werewolf(GameTestHelper helper, int level) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "could not make a werewolf");
        return p;
    }

    private static void wear(ServerPlayer p, UUID boundTo) {
        p.getInventory().items.set(5, RingGameTests.ring(ArtifactItems.MOON_RING.get(), boundTo));
    }

    /** Posts the form action the way Werewolves does; true when a listener cancelled it. */
    private static boolean shiftCancelled(ServerPlayer p, boolean insideOwnTick) {
        return shiftCancelled(p, ModActions.BEAST_FORM.get(), insideOwnTick);
    }

    private static boolean shiftCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        if (insideOwnTick) {
            ForcedShift.enterTick(p);
        }
        try {
            var event = new ActionEvent.ActionActivatedEvent(
                    VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), cast(action), 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IAction cast(IAction<?> a) {
        return a;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRingPreventsForcedShift(GameTestHelper helper) {
        ServerPlayer plain = werewolf(helper, 1);
        helper.assertTrue(shiftCancelled(plain, true) == false, "control: a werewolf without a ring was spared the forced shift");

        ServerPlayer wearer = werewolf(helper, 1);
        wear(wearer, wearer.getUUID());
        helper.assertTrue(shiftCancelled(wearer, true), "the forced shift was not cancelled for the Moon Ring's owner");

        ServerPlayer foreign = werewolf(helper, 1);
        wear(foreign, wearer.getUUID());
        helper.assertTrue(!shiftCancelled(foreign, true), "a ring bound to someone else stopped the forced shift");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRingAllowsHalfFormsAtWill(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper, 1);
        helper.assertTrue(MoonControl.of(p) == Control.NONE, "control: moon control without a ring");
        wear(p, p.getUUID());
        helper.assertTrue(MoonControl.of(p) == Control.HALF, "expected half control, got " + MoonControl.of(p));
        helper.assertTrue(!shiftCancelled(p, ModActions.SURVIVAL_FORM.get(), false), "a voluntary half form was cancelled");
        helper.assertTrue(!shiftCancelled(p, ModActions.HUMAN_FORM.get(), false), "a voluntary human form was cancelled");
        helper.assertTrue(shiftCancelled(p, ModActions.BEAST_FORM.get(), false), "a half-control wolf shifted into the beast at will");

        ServerPlayer foreign = werewolf(helper, 1);
        wear(foreign, p.getUUID());
        helper.assertTrue(MoonControl.of(foreign) == Control.NONE, "a ring bound to someone else gave control");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonRingAllowsFullFormsAtLevel8(GameTestHelper helper) {
        ServerPlayer low = werewolf(helper, 7);
        wear(low, low.getUUID());
        helper.assertTrue(MoonControl.of(low) == Control.HALF, "level 7 should only have half control, got " + MoonControl.of(low));

        ServerPlayer high = werewolf(helper, 8);
        helper.assertTrue(MoonControl.of(high) == Control.NONE, "control: level 8 without a ring has control");
        wear(high, high.getUUID());
        helper.assertTrue(MoonControl.of(high) == Control.FULL, "level 8 should have full control, got " + MoonControl.of(high));
        helper.assertTrue(!shiftCancelled(high, false), "a level 8 ring wearer could not shift into the beast at will");
        helper.assertTrue(shiftCancelled(low, ModActions.BEAST_FORM.get(), false), "a level 7 ring wearer shifted into the beast at will");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfTheMoonBindsTarget(GameTestHelper helper) {
        ServerPlayer target = werewolf(helper, 1);
        RiteTestSupport.place(helper, target);
        try {
            RiteTestSupport.start(helper, "ward_of_the_moon", target, List.of(new ItemStack(ArtifactItems.MOON_RING.get())));
            List<ItemStack> rings = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(RiteTestSupport.centre(helper)).inflate(1.5)).stream()
                    .map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(s -> s.is(ArtifactItems.MOON_RING.get())).toList();
            helper.assertTrue(rings.size() == 1, "expected one ring, got " + rings.size());
            helper.assertTrue(target.getUUID().equals(rings.get(0).get(ArtifactItems.BOUND_TO.get())), "the ring is not bound to the player on the circle");
        } finally {
            RiteTestSupport.remove(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfTheMoonRefusesAVampire(GameTestHelper helper) {
        ServerPlayer target = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(target).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1), "no vampire");
        RiteTestSupport.place(helper, target);
        try {
            RiteTestSupport.start(helper, "ward_of_the_moon", target, List.of(new ItemStack(ArtifactItems.MOON_RING.get())));
            int bound = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(RiteTestSupport.centre(helper)).inflate(1.5)).stream()
                    .map(net.minecraft.world.entity.item.ItemEntity::getItem)
                    .filter(s -> s.is(ArtifactItems.MOON_RING.get()) && s.get(ArtifactItems.BOUND_TO.get()) != null).toList().size();
            helper.assertTrue(bound == 0, "a vampire got a bound Moon Ring");
        } finally {
            RiteTestSupport.remove(helper, target);
        }
        helper.succeed();
    }
}
