package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.component.EDataComponents;
import net.favouriteless.enchanted.common.items.component.EntityRefData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.CompassMode;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.VampireCompassItem;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.LocatorNotices;
import net.ofatech.supernaturalbloodlines.parts.artifacts.rite.LocatorRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;
import net.ofatech.supernaturalbloodlines.shared.species.LocatorTargets;

/**
 * The locator (R315 to R319, R325): the rite through its real files, what the compass does with it, and that every reason somebody cannot be found looks the
 * same. These need the species part (the seal, the blood, the settings); without it the locator does not exist and each test ends at once.
 */
@PrefixGameTestTemplate(false)
public final class LocatorGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private LocatorGameTests() {
    }

    private static TalkingDuelist person(GameTestHelper helper, double dx, double dz) {
        TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        p.setPos(Vec3.atBottomCenterOf(RiteTestSupport.centre(helper)).add(dx, 1, dz));
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    private static void leave(GameTestHelper helper, TalkingDuelist... people) {
        for (TalkingDuelist p : people) {
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        for (String key : List.of("locator_enabled", "locator_minutes", "locator_range", "locator_blur", "locator_tell_target")) {
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator(key, null);
        }
        LocatorNotices.reset();
        RiteTestSupport.clearDropped(helper);
    }

    private static ItemStack taglock(UUID owner) {
        ItemStack taglock = new ItemStack(EItems.TAGLOCK_FILLED.get());
        taglock.set(EDataComponents.ENTITY_REF.get(), EntityRefData.of(owner, "owner"));
        return taglock;
    }

    private static List<ItemStack> with(ItemStack source) {
        return List.of(new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get()), source);
    }

    /** The compasses lying at the circle. */
    private static List<ItemStack> compasses(GameTestHelper helper) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(RiteTestSupport.centre(helper)).inflate(1.5))) {
            if (e.getItem().is(ArtifactItems.VAMPIRE_COMPASS.get())) {
                out.add(e.getItem());
            }
        }
        return out;
    }

    private static ItemStack attuned(UUID target, long until) {
        ItemStack compass = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
        compass.set(ArtifactItems.LOCATOR_SEAL.get(), LocatorTargets.seal(target).orElseThrow());
        compass.set(ArtifactItems.LOCATOR_UNTIL.get(), until);
        compass.set(ArtifactItems.COMPASS_MODE.get(), CompassMode.LOCATOR);
        return compass;
    }

    private static LodestoneTracker aim(GameTestHelper helper, ItemStack compass, ServerPlayer holder) {
        VampireCompassItem.update(compass, helper.getLevel(), holder);
        return compass.getOrDefault(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
    }

    // ---- the rite ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aTaglockAttunesACompassToItsOwnerAndTheTargetIsToldOnce(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist witch = person(helper, 3, 0);
        TalkingDuelist target = person(helper, 20, 20);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            RiteTestSupport.start(helper, "rite_of_the_locator", witch, with(taglock(target.getUUID())));
            List<ItemStack> out = compasses(helper);
            helper.assertTrue(out.size() == 1, "the compass did not come back: " + out.size());
            ItemStack compass = out.get(0);
            helper.assertTrue(VampireCompassItem.attuned(compass), "it is not attuned");
            helper.assertTrue(LocatorTargets.open(compass.get(ArtifactItems.LOCATOR_SEAL.get())).equals(Optional.of(target.getUUID())), "it is attuned to somebody else");
            helper.assertFalse(compass.get(ArtifactItems.LOCATOR_SEAL.get()).contains(target.getUUID().toString()), "the target's id is in the clear on the stack");
            helper.assertTrue(compass.get(ArtifactItems.LOCATOR_UNTIL.get()) > helper.getLevel().getGameTime() + 1000, "the attunement is not about 30 minutes long");
            helper.assertTrue(target.count(LocatorRiteFactory.TOLD) == 1 && witch.count(LocatorRiteFactory.TOLD) == 0, "the target was not told exactly once, or the caster was");
            helper.assertTrue(target.received() == 1, "the target heard more than the one line: " + target.keys());
        } finally {
            leave(helper, witch, target);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aBloodBottleAttunesAndNeverNamesAnyone(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist witch = person(helper, 3, 0);
        TalkingDuelist source = person(helper, 20, 20);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            ItemStack bottle = new ItemStack(RiteTestSupport.item(NS + ":original_blood"));
            net.ofatech.supernaturalbloodlines.parts.species.item.BloodStamp.write(bottle, new net.ofatech.supernaturalbloodlines.core.blood.BloodSource(source.getUUID(), "Mikael", false,
                    net.ofatech.supernaturalbloodlines.core.blood.BloodKind.ORIGINAL));
            RiteTestSupport.start(helper, "rite_of_the_locator_original_blood", witch, with(bottle));
            helper.assertTrue(compasses(helper).size() == 1 && LocatorTargets.open(compasses(helper).get(0).get(ArtifactItems.LOCATOR_SEAL.get())).equals(Optional.of(source.getUUID())),
                    "the bottle did not attune it to whoever it was taken from");
            helper.assertTrue(witch.received() == 0 && witch.countOnActionBar(LocatorRiteFactory.WONT_ANSWER) == 0, "the caster was told something: " + witch.keys());
            helper.assertTrue(source.count(LocatorRiteFactory.TOLD) == 1, "the source was not told");
            helper.assertFalse(compasses(helper).get(0).toString().contains("Mikael"), "a name is on the compass");
        } finally {
            leave(helper, witch, source);
        }
        helper.succeed();
    }

    /** One rite that must fail: the same one line, no compass, the ingredients spent. */
    private static void fails(GameTestHelper helper, TalkingDuelist caster, String rite, List<ItemStack> items, String why) {
        int told = caster.countOnActionBar(LocatorRiteFactory.WONT_ANSWER);
        RiteTestSupport.clearDropped(helper);
        RiteTestSupport.start(helper, rite, caster, items);
        helper.assertTrue(compasses(helper).isEmpty(), why + ": a compass came out");
        helper.assertTrue(caster.countOnActionBar(LocatorRiteFactory.WONT_ANSWER) == told + 1, why + ": the one line did not come exactly once");
        helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:iron_ingot") == 0, why + ": ingredients came back, so this failure costs differently");
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void everyFailureIsOneMessageAtOneCostAndTheTargetChangesNothing(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist witch = person(helper, 3, 0);
        TalkingDuelist human = person(helper, 20, 20);
        TalkingDuelist other = person(helper, 20, -20);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            // the target is never looked at: a human, a vampire (a different species), somebody who is not here at all, and a villager's id all attune alike
            for (UUID target : new UUID[] {human.getUUID(), UUID.randomUUID(), new UUID(0, 1)}) {
                RiteTestSupport.clearDropped(helper);
                RiteTestSupport.start(helper, "rite_of_the_locator", witch, with(taglock(target)));
                helper.assertTrue(compasses(helper).size() == 1, "a target of this kind was refused");
                helper.assertTrue(witch.countOnActionBar(LocatorRiteFactory.WONT_ANSWER) == 0, "a target of this kind got the refusal line");
            }
            BloodlineAccess.set(other, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            RiteTestSupport.clearDropped(helper);
            RiteTestSupport.start(helper, "rite_of_the_locator", witch, with(taglock(other.getUUID())));
            helper.assertTrue(compasses(helper).size() == 1, "an Original's taglock was refused");
            helper.assertTrue(witch.countOnActionBar(LocatorRiteFactory.WONT_ANSWER) == 0, "an Original's taglock got the refusal line");

            fails(helper, witch, "rite_of_the_locator_hybrid_blood", with(new ItemStack(RiteTestSupport.item(NS + ":hybrid_blood"))), "a bottle with no blood in it");
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_enabled", false);
            fails(helper, witch, "rite_of_the_locator", with(taglock(human.getUUID())), "the switch off");
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_enabled", null);
            BloodlineAccess.set(witch, BloodlineState.NONE);
            fails(helper, witch, "rite_of_the_locator", with(taglock(human.getUUID())), "a caster who is not a witch type");
        } finally {
            leave(helper, witch, human, other);
        }
        helper.succeed();
    }

    // ---- the compass ----

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theNeedleSpinsTheSameForEveryReasonAndNeverPointsAtTheExactSpot(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist holder = person(helper, 0, 0);
        TalkingDuelist target = person(helper, 600, 0);
        try {
            long now = helper.getLevel().getGameTime();
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_range", 0);
            // control: findable, a blurred point, an answer in hundreds
            ItemStack found = attuned(target.getUUID(), now + 5000);
            LodestoneTracker aimed = aim(helper, found, holder);
            helper.assertTrue(aimed.target().isPresent(), "control: a findable target gave no needle");
            double dx = aimed.target().get().pos().getX() - target.getX();
            double dz = aimed.target().get().pos().getZ() - target.getZ();
            helper.assertTrue(Math.abs(dx) <= 32 && Math.abs(dz) <= 32, "the point is not around the target: " + dx + ", " + dz);
            helper.assertTrue(aimed.target().get().pos().getX() != (int) Math.floor(target.getX()) || aimed.target().get().pos().getZ() != (int) Math.floor(target.getZ()),
                    "the needle points at the exact spot");
            // standing still inside the square does not move the needle, and a step to the next square does
            target.setPos(target.position().add(1, 0, 1));
            ItemStack again = attuned(target.getUUID(), now + 5000);
            helper.assertTrue(aim(helper, again, holder).target().get().pos().equals(aimed.target().get().pos()) || (int) Math.floor(target.getX() / 32) != (int) Math.floor((target.getX() - 1) / 32),
                    "a step inside one square moved the needle");
            target.setPos(target.position().add(-1, 0, -1));
            helper.assertTrue(found.getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1) == 600 || found.getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1) == 500 || found.getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1) == 700,
                    "the distance is not in hundreds: " + found.getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1));
            // every reason it cannot be found is the same empty needle and the same -1
            List<String> reasons = new ArrayList<>();
            List<ItemStack> spinning = new ArrayList<>();
            spinning.add(attuned(UUID.randomUUID(), now + 5000)); reasons.add("offline");
            spinning.add(attuned(target.getUUID(), now - 1)); reasons.add("spent");
            ItemStack unsealed = attuned(target.getUUID(), now + 5000);
            unsealed.set(ArtifactItems.LOCATOR_SEAL.get(), "not a seal of this server"); spinning.add(unsealed); reasons.add("a seal that does not open");
            for (int i = 0; i < spinning.size(); i++) {
                LodestoneTracker t = aim(helper, spinning.get(i), holder);
                helper.assertTrue(t.target().isEmpty() && spinning.get(i).getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1) == -1, reasons.get(i) + ": the needle did not spin");
            }
            ItemStack outOfRange = attuned(target.getUUID(), now + 5000);
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_range", 100);
            helper.assertTrue(aim(helper, outOfRange, holder).target().isEmpty(), "out of range: the needle did not spin");
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_range", 0);
            net.ofatech.supernaturalbloodlines.core.boss.CloakAccess.set(target, now + 5000);
            helper.assertTrue(aim(helper, attuned(target.getUUID(), now + 5000), holder).target().isEmpty(), "a cloaked target: the needle did not spin");
            net.ofatech.supernaturalbloodlines.core.boss.CloakAccess.set(target, net.ofatech.supernaturalbloodlines.core.boss.Cloak.NONE);
            helper.assertTrue(aim(helper, attuned(target.getUUID(), now + 5000), holder).target().isPresent(), "control: an uncloaked target is found again");
            target.setGameMode(GameType.CREATIVE);
            helper.assertTrue(aim(helper, attuned(target.getUUID(), now + 5000), holder).target().isEmpty(), "a creative target: the needle did not spin");
            target.setGameMode(GameType.SURVIVAL);
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_enabled", false);
            helper.assertTrue(aim(helper, attuned(target.getUUID(), now + 5000), holder).target().isEmpty(), "locator_enabled = false: the needle did not spin");
        } finally {
            leave(helper, holder, target);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void aPutAwayCompassKeepsTheAttunementAndLosesTheNeedle(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist holder = person(helper, 0, 0);
        TalkingDuelist target = person(helper, 40, 0);
        try {
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_range", 0);
            ItemStack compass = attuned(target.getUUID(), helper.getLevel().getGameTime() + 5000);
            helper.assertTrue(aim(helper, compass, holder).target().isPresent(), "control: no needle");
            VampireCompassItem.clearTarget(compass);
            helper.assertTrue(compass.get(DataComponents.LODESTONE_TRACKER).target().isEmpty(), "the needle stayed");
            helper.assertTrue(compass.getOrDefault(ArtifactItems.LOCATOR_NEAR.get(), -1) == -1, "the distance stayed");
            helper.assertTrue(VampireCompassItem.attuned(compass) && compass.get(ArtifactItems.LOCATOR_UNTIL.get()) != null, "the attunement was lost");
        } finally {
            leave(helper, holder, target);
        }
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void theEndOfTheSearchIsToldOnceAndOnlyWhenItIsOver(GameTestHelper helper) {
        if (!LocatorTargets.installed()) {
            helper.succeed();
            return;
        }
        TalkingDuelist witch = person(helper, 3, 0);
        TalkingDuelist target = person(helper, 20, 20);
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            RiteTestSupport.start(helper, "rite_of_the_locator", witch, with(taglock(target.getUUID())));
            LocatorNotices.sweep(helper.getLevel().getServer());
            helper.assertTrue(target.count(LocatorRiteFactory.ENDED) == 0, "told it ended while it lasts");
            net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig.overrideLocator("locator_tell_target", false);
            TalkingDuelist silent = person(helper, 20, -20);
            RiteTestSupport.start(helper, "rite_of_the_locator", witch, with(taglock(silent.getUUID())));
            helper.assertTrue(silent.received() == 0, "locator_tell_target = false still told them");
            leave(helper, silent);
        } finally {
            leave(helper, witch, target);
        }
        helper.succeed();
    }
}
