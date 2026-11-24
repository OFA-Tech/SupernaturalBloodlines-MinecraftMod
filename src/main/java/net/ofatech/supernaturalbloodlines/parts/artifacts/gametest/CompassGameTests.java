package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.DoppelgangerRoll;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.CompassMode;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.shared.species.DoppelgangerIndex;

/**
 * Plan 12b, task 4: the Vampire Compass in Doppelgänger mode. The tests install their own {@link DoppelgangerIndex} source (the
 * doppelgängers are marked through the core attachment), so they do not depend on the species part being on.
 */
@PrefixGameTestTemplate(false)
public final class CompassGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final BloodlineState GENE = BloodlineState.NONE.toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build();

    private CompassGameTests() {
    }

    private static ItemStack compass(CompassMode mode) {
        ItemStack stack = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        if (mode != null) {
            stack.set(ArtifactItems.COMPASS_MODE.get(), mode);
        }
        return stack;
    }

    private static ServerPlayer holder(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        return p;
    }

    private static void join(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().addNewPlayer(p);
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** The doppelgänger villagers this test made: the index source lists only these, so villagers of tests running beside it never count. */
    private static final List<Villager> OWN = new ArrayList<>();

    private static Villager doppelganger(GameTestHelper helper, BlockPos at) {
        Villager v = helper.spawn(EntityType.VILLAGER, at);
        v.setData(ModAttachments.DOPPELGANGER_VILLAGER, new DoppelgangerRoll(true, true));
        OWN.add(v);
        return v;
    }

    private static Villager plain(GameTestHelper helper, BlockPos at) {
        Villager v = helper.spawn(EntityType.VILLAGER, at);
        v.setData(ModAttachments.DOPPELGANGER_VILLAGER, new DoppelgangerRoll(false, true));
        return v;
    }

    /** Runs {@code body} with an index that lists the doppelgänger villagers made by this test ({@link #OWN}) that are still in the level. */
    private static void withIndex(GameTestHelper helper, Runnable body) {
        DoppelgangerIndex.Source before = DoppelgangerIndex.current();
        OWN.clear();
        DoppelgangerIndex.set(level -> OWN.stream().filter(v -> !v.isRemoved() && v.level() == level).toList());
        try {
            body.run();
        } finally {
            DoppelgangerIndex.set(before);
            OWN.clear();
        }
    }

    /** The gene player nearest to {@code from} in the level, whoever made them: the compass sees every player, so the test compares against this. */
    private static ServerPlayer nearestGenePlayer(GameTestHelper helper, ServerPlayer from) {
        ServerPlayer best = null;
        for (ServerPlayer p : helper.getLevel().players()) {
            if (p != from && p.isAlive() && !p.isCreative() && !p.isSpectator() && BloodlineAccess.get(p).doppelgangerGene()
                    && (best == null || p.distanceToSqr(from) < best.distanceToSqr(from))) {
                best = p;
            }
        }
        return best;
    }

    private static Optional<GlobalPos> target(ItemStack compass) {
        LodestoneTracker t = compass.get(DataComponents.LODESTONE_TRACKER);
        return t == null ? null : t.target();
    }

    private static boolean pointsAt(ItemStack compass, Entity e) {
        Optional<GlobalPos> t = target(compass);
        return t != null && t.isPresent() && t.get().pos().equals(e.blockPosition());
    }

    private static void update(GameTestHelper helper, ItemStack compass, ServerPlayer p) {
        VampireCompassItem.update(compass, helper.getLevel(), p);
    }

    @SuppressWarnings("unchecked")
    private static Mob vampireMob(GameTestHelper helper, BlockPos at) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), at);
    }

    // ---- the toggle ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sneakUseTogglesMode(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ItemStack stack = compass(null);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        try {
            helper.assertTrue(VampireCompassItem.mode(stack) == CompassMode.VAMPIRES, "a new compass is not in vampire mode");

            p.setShiftKeyDown(false);
            var plainUse = stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(plainUse.getResult() == InteractionResult.PASS && VampireCompassItem.mode(stack) == CompassMode.VAMPIRES,
                    "a plain use changed the mode");

            p.setShiftKeyDown(true);
            withIndex(helper, () -> {
                Villager near = doppelganger(helper, new BlockPos(4, 1, 1));
                var use = stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
                helper.assertTrue(use.getResult().consumesAction() && VampireCompassItem.mode(stack) == CompassMode.DOPPELGANGERS,
                        "sneak + use did not switch to doppelgängers");
                // the toggle itself finds nothing: the needle spins until the next periodic update
                helper.assertTrue(target(stack) != null && target(stack).isEmpty(), "the toggle searched for a target: " + target(stack));
                update(helper, stack, p);
                helper.assertTrue(pointsAt(stack, near), "the next update did not find the villager");
            });
            stack.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
            helper.assertTrue(VampireCompassItem.mode(stack) == CompassMode.VAMPIRES, "the second sneak + use did not switch back");

            // the tooltip shows the mode
            for (CompassMode m : CompassMode.values()) {
                ItemStack s = compass(m);
                List<Component> lines = new ArrayList<>();
                s.getItem().appendHoverText(s, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
                helper.assertTrue(lines.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        && t.getArgs().length == 1 && t.getArgs()[0] instanceof Component arg
                        && arg.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents a && a.getKey().equals(m.langKey())),
                        "the tooltip does not show " + m);
            }
        } finally {
            p.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    // ---- villagers ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pointsAtNearestDoppelgangerVillager(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        withIndex(helper, () -> {
            Villager far = doppelganger(helper, new BlockPos(13, 1, 13));
            Villager near = doppelganger(helper, new BlockPos(6, 1, 1));
            plain(helper, new BlockPos(3, 1, 1));
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);

            update(helper, stack, p);
            helper.assertTrue(pointsAt(stack, near), "points at " + target(stack) + ", the nearest doppelgänger is at " + near.blockPosition());
            helper.assertTrue(!stack.get(DataComponents.LODESTONE_TRACKER).tracked(), "it would look for a lodestone");

            near.discard();
            update(helper, stack, p);
            helper.assertTrue(pointsAt(stack, far), "did not move on to the next doppelgänger: " + target(stack));
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ignoresVillagerOutOfRange(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        helper.assertTrue(ArtifactsConfig.compassDoppelgangerRange() == 512, "the default range is " + ArtifactsConfig.compassDoppelgangerRange());
        withIndex(helper, () -> {
            Villager v = doppelganger(helper, new BlockPos(11, 1, 1)); // 10 blocks from the holder
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);
            try {
                ArtifactsConfig.overrideCompassDoppelgangerRange(9);
                update(helper, stack, p);
                helper.assertTrue(target(stack) != null && target(stack).isEmpty(), "a villager beyond the range was found: " + target(stack));
                ArtifactsConfig.overrideCompassDoppelgangerRange(11);
                update(helper, stack, p);
                helper.assertTrue(pointsAt(stack, v), "a villager inside the range was not found");
            } finally {
                ArtifactsConfig.overrideCompassDoppelgangerRange(null);
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUnloadedVillagerIsNotFound(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        withIndex(helper, () -> {
            Villager v = doppelganger(helper, new BlockPos(6, 1, 1));
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);
            update(helper, stack, p);
            helper.assertTrue(pointsAt(stack, v), "setup: not found");
            v.discard();
            update(helper, stack, p);
            helper.assertTrue(target(stack).isEmpty(), "a removed villager is still the target");
        });
        helper.succeed();
    }

    // ---- players ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pointsAtGenePlayerAnywhereInDimension(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ServerPlayer carrier = holder(helper);
        ServerPlayer plainPlayer = holder(helper);
        carrier.setPos(p.position().add(600, 0, 0)); // beyond the villager range
        plainPlayer.setPos(p.position().add(5, 0, 0));
        join(helper, p);
        join(helper, carrier);
        join(helper, plainPlayer);
        try {
            BloodlineAccess.set(carrier, GENE);
            BloodlineAccess.set(plainPlayer, BloodlineState.NONE);
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);
            withIndex(helper, () -> {
                update(helper, stack, p);
                ServerPlayer expected = nearestGenePlayer(helper, p);
                helper.assertTrue(expected != null && BloodlineAccess.get(carrier).doppelgangerGene(), "setup: the carrier is not a gene player in the level");
                helper.assertTrue(pointsAt(stack, expected), "the nearest gene player is not the target: " + target(stack) + ", expected " + expected.blockPosition()
                        + (expected == carrier ? "" : " (a gene player of another test was nearer than the one 600 blocks away)"));
                helper.assertTrue(carrier.distanceToSqr(p) > 512.0 * 512.0, "setup: the carrier is inside the villager range");
                // a nearer doppelgänger villager takes over, a farther one does not
                Villager near = doppelganger(helper, new BlockPos(6, 1, 1));
                update(helper, stack, p);
                helper.assertTrue(pointsAt(stack, near) || nearestGenePlayer(helper, p) != null && nearestGenePlayer(helper, p).distanceToSqr(p) < near.distanceToSqr(p),
                        "a nearer villager did not win over the far player");
            });
            // the holder's own gene is not a target. Only players count here: the villager above is still in the level, and the real index lists a
            // villager the species part rolled as a doppelgänger when it joined (a lucky roll) and the villagers of tests beside this one, so the real
            // index would make the answer depend on chance; this test's own index lists nothing
            BloodlineAccess.set(p, GENE);
            BloodlineAccess.set(carrier, BloodlineState.NONE);
            withIndex(helper, () -> {
                ItemStack own = compass(CompassMode.DOPPELGANGERS);
                update(helper, own, p);
                ServerPlayer other = nearestGenePlayer(helper, p); // gene players of tests running beside this one
                helper.assertTrue(other == null ? target(own) != null && target(own).isEmpty() : pointsAt(own, other),
                        "the holder points at themself or at the wrong player: " + target(own));
            });
        } finally {
            leave(helper, p, carrier, plainPlayer);
            BloodlineAccess.set(p, BloodlineState.NONE);
            BloodlineAccess.set(carrier, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void targetGetsNoMessage(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        RecordingPlayer target = RecordingPlayer.create(helper);
        target.setPos(p.position().add(5, 0, 0));
        join(helper, p);
        join(helper, target);
        try {
            BloodlineAccess.set(target, GENE);
            BloodlineState before = BloodlineAccess.get(target);
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);
            update(helper, stack, p);
            update(helper, stack, p);
            helper.assertTrue(pointsAt(stack, target), "setup: the gene player was not found");
            helper.assertTrue(!target.received("supernaturalbloodlines.gene.revealed.doppelganger.yes")
                    && !target.received("supernaturalbloodlines.gene.revelation") && !target.receivedOnActionBar("supernaturalbloodlines.compass.switched"),
                    "the target was told something");
            helper.assertTrue(BloodlineAccess.get(target).equals(before), "the target's state changed: " + BloodlineAccess.get(target));
            helper.assertTrue(BloodlineAccess.get(target).revealedGenes().isEmpty(), "the target's gene was revealed");
            helper.assertTrue(!target.isCurrentlyGlowing() && target.getActiveEffects().isEmpty(), "the target glows or has an effect");
        } finally {
            leave(helper, p, target);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- the old mode ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireModeUnchanged(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ServerPlayer carrier = holder(helper);
        carrier.setPos(p.position().add(4, 0, 0));
        join(helper, carrier);
        try {
            BloodlineAccess.set(carrier, GENE);
            withIndex(helper, () -> {
                doppelganger(helper, new BlockPos(3, 1, 1));
                Mob vampire = vampireMob(helper, new BlockPos(9, 1, 1));
                for (ItemStack stack : List.of(compass(null), compass(CompassMode.VAMPIRES))) {
                    update(helper, stack, p);
                    helper.assertTrue(pointsAt(stack, vampire), "vampire mode does not point at the vampire: " + target(stack));
                }
                // and the doppelgänger mode does not point at a vampire
                ItemStack other = compass(CompassMode.DOPPELGANGERS);
                update(helper, other, p);
                helper.assertTrue(!pointsAt(other, vampire), "doppelgänger mode points at a vampire");
            });
        } finally {
            leave(helper, carrier);
            BloodlineAccess.set(carrier, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutASpeciesIndexNothingIsFound(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        DoppelgangerIndex.Source before = DoppelgangerIndex.current();
        DoppelgangerIndex.set(null);
        try {
            doppelganger(helper, new BlockPos(3, 1, 1));
            ItemStack stack = compass(CompassMode.DOPPELGANGERS);
            update(helper, stack, p);
            helper.assertTrue(target(stack) != null && target(stack).isEmpty(), "found a villager with no index installed: " + target(stack));
        } finally {
            DoppelgangerIndex.set(before);
        }
        helper.succeed();
    }

    // ---- privacy: the needle only knows a target while the compass is held ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void trackerIsClearedWhenTheCompassIsNoLongerHeld(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        withIndex(helper, () -> {
            Villager near = doppelganger(helper, new BlockPos(5, 1, 1));
            Mob vampire = vampireMob(helper, new BlockPos(7, 1, 1));
            // the locator mode has its own privacy checks in LocatorGameTests (it needs an attuned compass and a person to find)
            for (CompassMode mode : List.of(CompassMode.VAMPIRES, CompassMode.DOPPELGANGERS)) {
                Entity expected = mode == CompassMode.VAMPIRES ? vampire : near;
                ItemStack stack = compass(mode);
                p.getInventory().clearContent();
                p.setItemInHand(InteractionHand.MAIN_HAND, stack);

                // held in the main hand: updated on the beat
                p.tickCount = 40;
                stack.inventoryTick(helper.getLevel(), p, 0, true);
                helper.assertTrue(pointsAt(stack, expected), mode + ": a held compass has no target: " + target(stack));

                // put away in the inventory: cleared at once, not on the next beat
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                p.getInventory().setItem(5, stack);
                p.tickCount = 41;
                stack.inventoryTick(helper.getLevel(), p, 5, false);
                helper.assertTrue(target(stack) != null && target(stack).isEmpty(), mode + ": a compass in the inventory still points at " + target(stack));
                p.tickCount = 60;
                stack.inventoryTick(helper.getLevel(), p, 5, false);
                helper.assertTrue(target(stack).isEmpty(), mode + ": a compass in the inventory was updated");

                // the off hand counts as held
                p.getInventory().setItem(5, ItemStack.EMPTY);
                p.setItemInHand(InteractionHand.OFF_HAND, stack);
                p.tickCount = 80;
                stack.inventoryTick(helper.getLevel(), p, 40, false);
                helper.assertTrue(pointsAt(stack, expected), mode + ": an off hand compass has no target: " + target(stack));
                p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

                // dropped: cleared as the item entity joins
                stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(helper.getLevel().dimension(), expected.blockPosition())), false));
                ItemEntity drop = new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), stack.copy());
                helper.getLevel().addFreshEntity(drop);
                helper.assertTrue(target(drop.getItem()) != null && target(drop.getItem()).isEmpty(), mode + ": a dropped compass still points at " + target(drop.getItem()));
                drop.discard();

                // put into a container: cleared when the player closes it
                ItemStack stored = stack.copy();
                var container = new net.minecraft.world.SimpleContainer(27);
                container.setItem(3, stored);
                var menu = net.minecraft.world.inventory.ChestMenu.threeRows(0, p.getInventory(), container);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close(p, menu));
                helper.assertTrue(target(stored) != null && target(stored).isEmpty(), mode + ": a compass in a closed container still points at " + target(stored));
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCompassWithNoTargetGetsNoComponentWhenNotHeld(GameTestHelper helper) {
        ServerPlayer p = holder(helper);
        ItemStack stack = compass(null);
        p.getInventory().setItem(5, stack);
        p.tickCount = 40;
        stack.inventoryTick(helper.getLevel(), p, 5, false);
        helper.assertTrue(stack.get(DataComponents.LODESTONE_TRACKER) == null, "an idle compass in the inventory got a tracker");
        helper.succeed();
    }}
