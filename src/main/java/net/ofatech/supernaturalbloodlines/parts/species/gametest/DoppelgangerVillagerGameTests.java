package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerVillagers;

/** Plan 12b, task 2: the doppelgänger villager's roll (once, kept through a reload) and the per-level index. Runs without Werewolves. */
@PrefixGameTestTemplate(false)
public final class DoppelgangerVillagerGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private DoppelgangerVillagerGameTests() {
    }

    private static Villager villager(GameTestHelper helper) {
        return helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
    }

    private static Villager spawnWith(GameTestHelper helper, double chance) {
        SpeciesConfig.overrideDoppelgangerVillagerChance(chance);
        try {
            return villager(helper);
        } finally {
            SpeciesConfig.overrideDoppelgangerVillagerChance(null);
        }
    }

    /** Saves {@code v}, removes it and loads the saved copy back into the level, the way a chunk reload does. */
    private static Villager reload(GameTestHelper helper, Villager v) {
        CompoundTag tag = new CompoundTag();
        helper.assertTrue(v.save(tag), "the villager did not save");
        v.discard();
        Entity copy = EntityType.create(tag, helper.getLevel()).orElseThrow();
        helper.assertTrue(copy instanceof Villager, "the copy is a " + copy);
        helper.getLevel().addFreshEntity(copy);
        return (Villager) copy;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void spawnedVillagerIsADoppelgangerAtChanceOne(GameTestHelper helper) {
        Villager v = spawnWith(helper, 1.0);
        try {
            helper.assertTrue(DoppelgangerVillagers.isDoppelganger(v), "a villager spawned at chance 1.0 is not a doppelgänger");
        } finally {
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void spawnedVillagerIsNeverOneAtChanceZero(GameTestHelper helper) {
        Villager v = spawnWith(helper, 0.0);
        try {
            helper.assertTrue(!DoppelgangerVillagers.isDoppelganger(v), "a villager spawned at chance 0.0 is a doppelgänger");
            helper.assertTrue(v.getData(ModAttachments.DOPPELGANGER_VILLAGER).rolled(), "the roll was not recorded");
            Entity trader = helper.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 1));
            helper.assertTrue(!trader.hasData(ModAttachments.DOPPELGANGER_VILLAGER), "a wandering trader was rolled");
            trader.discard();
        } finally {
            v.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rollIsBelowTheChanceNotEqual(GameTestHelper helper) {
        SpeciesConfig.overrideDoppelgangerVillagerChance(0.005);
        Villager under = null;
        Villager edge = null;
        try {
            DoppelgangerVillagers.useRoll(() -> 0.0049);
            under = villager(helper);
            DoppelgangerVillagers.useRoll(() -> 0.005);
            edge = villager(helper);
            helper.assertTrue(DoppelgangerVillagers.isDoppelganger(under), "a roll under the chance did not make a doppelgänger");
            helper.assertTrue(!DoppelgangerVillagers.isDoppelganger(edge), "a roll equal to the chance made a doppelgänger");
        } finally {
            DoppelgangerVillagers.resetRoll();
            SpeciesConfig.overrideDoppelgangerVillagerChance(null);
            if (under != null) {
                under.discard();
            }
            if (edge != null) {
                edge.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void reloadedVillagerKeepsItsRollAndIsNeverRerolled(GameTestHelper helper) {
        Villager yes = spawnWith(helper, 1.0);
        Villager no = spawnWith(helper, 0.0);
        Villager yes2 = null;
        Villager no2 = null;
        try {
            // the chances are flipped while they are away: a reroll would flip them
            SpeciesConfig.overrideDoppelgangerVillagerChance(0.0);
            yes2 = reload(helper, yes);
            SpeciesConfig.overrideDoppelgangerVillagerChance(1.0);
            no2 = reload(helper, no);
            helper.assertTrue(DoppelgangerVillagers.isDoppelganger(yes2), "a reloaded doppelgänger lost its roll");
            helper.assertTrue(!DoppelgangerVillagers.isDoppelganger(no2), "a reloaded plain villager was rerolled into a doppelgänger");
        } finally {
            SpeciesConfig.overrideDoppelgangerVillagerChance(null);
            if (yes2 != null) {
                yes2.discard();
            }
            if (no2 != null) {
                no2.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void villagerWithoutARollIsRolledOnceWhenItLoads(GameTestHelper helper) {
        // a villager saved before the update (or placed by worldgen) has no roll
        Villager old = spawnWith(helper, 0.0);
        old.removeData(ModAttachments.DOPPELGANGER_VILLAGER);
        Villager first = null;
        Villager second = null;
        try {
            SpeciesConfig.overrideDoppelgangerVillagerChance(1.0);
            first = reload(helper, old);
            helper.assertTrue(DoppelgangerVillagers.isDoppelganger(first), "an unrolled villager was not rolled on load");
            SpeciesConfig.overrideDoppelgangerVillagerChance(0.0);
            second = reload(helper, first);
            helper.assertTrue(DoppelgangerVillagers.isDoppelganger(second), "the roll was lost on the next load");
        } finally {
            SpeciesConfig.overrideDoppelgangerVillagerChance(null);
            if (first != null) {
                first.discard();
            }
            if (second != null) {
                second.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void indexAddsOnJoinAndRemovesOnLeave(GameTestHelper helper) {
        Villager doppel = spawnWith(helper, 1.0);
        Villager plain = spawnWith(helper, 0.0);
        try {
            var loaded = DoppelgangerVillagers.loaded(helper.getLevel());
            helper.assertTrue(loaded.contains(doppel), "a loaded doppelgänger is not in the index");
            helper.assertTrue(!loaded.contains(plain), "a plain villager is in the index");

            doppel.discard();
            helper.assertTrue(!DoppelgangerVillagers.loaded(helper.getLevel()).contains(doppel), "an unloaded doppelgänger stayed in the index");

            // and it comes back when it loads again
            Villager back = reload(helper, spawnWith(helper, 1.0));
            helper.assertTrue(DoppelgangerVillagers.loaded(helper.getLevel()).contains(back), "a reloaded doppelgänger is not in the index");
            back.discard();
            helper.assertTrue(!DoppelgangerVillagers.loaded(helper.getLevel()).contains(back), "the index kept a removed villager");
        } finally {
            doppel.discard();
            plain.discard();
        }
        helper.succeed();
    }
}
