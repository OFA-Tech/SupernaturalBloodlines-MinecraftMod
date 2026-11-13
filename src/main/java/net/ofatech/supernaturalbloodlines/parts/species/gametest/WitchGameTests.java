package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.UUID;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.favouriteless.enchanted.common.enchanted.altar.AltarHelper;
import net.favouriteless.enchanted.common.init.EBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.rite.AncestralCallingRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.witch.Siphoning;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.PartBlocks;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/**
 * Witches and siphoners. The altar tests run in a batch of their own, high up: a witch anywhere near an altar changes its
 * range and the cost of every rite on it, so none of them may share the world with another test's witch.
 */
@PrefixGameTestTemplate(false)
public final class WitchGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private WitchGameTests() {}

    // ---- helpers ----

    private static BloodlineState state(Species s, int siphoned) {
        return BloodlineState.NONE.toBuilder().species(s).witchLayer(s == Species.WITCH).siphonedPower(siphoned).build();
    }

    private static void set(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static double power(AltarBlockEntity altar) {
        try {
            Field f = AltarBlockEntity.class.getDeclaredField("power");
            f.setAccessible(true);
            return f.getDouble(altar);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AltarBlockEntity altar(GameTestHelper helper, BlockPos base, double power) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 2; z++) {
                level.setBlock(base.offset(x, 0, z), EBlocks.ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        helper.assertTrue(AltarHelper.tryFormAltar(level, base.offset(2, 0, 1)), "the altar did not form");
        AltarBlockEntity altar = (AltarBlockEntity) level.getBlockEntity(base);
        helper.assertTrue(altar != null, "no altar block entity at " + base);
        altar.firstTick();
        set(altar, "power", power);
        return altar;
    }

    /** A gold chalk with a floor, cast by {@code caster}, linked to the altars (none = no altar at all). */
    private static GoldChalkBlockEntity chalk(GameTestHelper helper, BlockPos pos, UUID caster, BlockPos... altars) {
        ServerLevel level = helper.getLevel();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pos, EBlocks.GOLDEN_CHALK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        GoldChalkBlockEntity chalk = (GoldChalkBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(chalk != null, "no gold chalk block entity at " + pos);
        set(chalk, "caster", caster);
        for (BlockPos a : altars) {
            chalk.getPosHolder().add(a);
        }
        return chalk;
    }

    private static void put(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        p.setPos(Vec3.atBottomCenterOf(at).add(0, 1, 0));
        helper.getLevel().addNewPlayer(p);
    }

    private static RecordingPlayer player(Species s, int siphoned, GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineAccess.set(p, state(s, siphoned));
        return p;
    }

    // ---- Rite of Ancestral Calling ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ancestralCallingMakesWitchOrSiphoner(GameTestHelper helper) {
        helper.assertTrue(type(helper, "ancestral_calling") != null, "the ancestral_calling rite did not load");
        // the roll: a seeded random gives about the configured share of siphoners, and 0 / 1 are certain
        RandomSource random = RandomSource.create(42);
        int siphoners = 0;
        for (int i = 0; i < 1000; i++) {
            siphoners += AncestralCallingRiteFactory.pick(random, 0.25) == BloodlineEvent.ANCESTRAL_CALLING_SIPHONER ? 1 : 0;
        }
        helper.assertTrue(siphoners > 200 && siphoners < 300, "25% should give about 250 siphoners in 1000, not " + siphoners);
        helper.assertTrue(AncestralCallingRiteFactory.pick(random, 0.0) == BloodlineEvent.ANCESTRAL_CALLING_WITCH, "chance 0 still gave a siphoner");
        helper.assertTrue(AncestralCallingRiteFactory.pick(random, 1.0) == BloodlineEvent.ANCESTRAL_CALLING_SIPHONER, "chance 1 still gave a witch");
        helper.assertTrue(SpeciesConfig.siphonerChance() == SpeciesConfig.DEFAULT_SIPHONER_CHANCE, "default siphoner_chance is " + SpeciesConfig.siphonerChance());

        // the rite itself, with the roll forced both ways
        try {
            for (boolean siphoner : new boolean[] {true, false}) {
                SpeciesConfig.overrideSiphonerChance(siphoner ? 1.0 : 0.0);
                RecordingPlayer caster = RecordingPlayer.create(helper);
                place(helper, caster);
                try {
                    start(helper, "ancestral_calling", caster, java.util.List.of());
                    BloodlineState s = BloodlineAccess.get(caster);
                    helper.assertTrue(s.species() == (siphoner ? Species.SIPHONER : Species.WITCH), "species is " + s.species());
                    helper.assertTrue(s.witchLayer() == !siphoner, "witch layer is " + s.witchLayer());
                } finally {
                    remove(helper, caster);
                    BloodlineAccess.set(caster, BloodlineState.NONE);
                }
            }
            // a vampire is refused and told why
            RecordingPlayer vampire = RecordingPlayer.create(helper);
            VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            place(helper, vampire);
            try {
                start(helper, "ancestral_calling", vampire, java.util.List.of());
                helper.assertTrue(vampire.receivedOnActionBar("supernaturalbloodlines.refusal.vampire_no_magic"), "the vampire was not told");
                helper.assertTrue(BloodlineAccess.get(vampire).species() == Species.NONE, "a vampire became " + BloodlineAccess.get(vampire).species());
            } finally {
                remove(helper, vampire);
                VampirismAPI.factionPlayerHandler(vampire).leaveFaction(false);
            }
        } finally {
            SpeciesConfig.overrideSiphonerChance(null);
        }
        helper.succeed();
    }

    // ---- witch bonus ----

    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_cost")
    public static void witchCheapensRite(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, witch.getUUID(), base);
        put(helper, witch, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 4250, "a witch pays 750 of 1000, altar left with " + power(altar));
            // far from the altar the bonus is gone
            witch.setPos(Vec3.atBottomCenterOf(base).add(500, 0, 0));
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 3250, "far from the altar the full 1000 is due, altar left with " + power(altar));
            // someone who is no witch pays in full
            witch.setPos(Vec3.atBottomCenterOf(chalkPos));
            BloodlineAccess.set(witch, BloodlineState.NONE);
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 2250, "a plain player pays the full 1000, altar left with " + power(altar));
            helper.assertTrue(WitchAltarBonus.hasBonus(Species.TRIBRID) && WitchAltarBonus.hasBonus(Species.HERETIC), "tribrid and heretic keep the bonus");
            helper.assertTrue(!WitchAltarBonus.hasBonus(Species.SIPHONER), "a siphoner is no witch");
        } finally {
            remove(helper, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_range")
    public static void witchWidensAltarRange(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 0);
        BlockPos east = base.offset(40, 0, 0);
        BlockPos south = base.offset(0, 0, 40);
        helper.assertTrue(!altar.posWithinRange(east) && !altar.posWithinRange(south), "40 blocks away is in range without a witch");
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        put(helper, witch, base.offset(1, 0, 0));
        try {
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), base, 32) == 48, "range is " + WitchAltarBonus.rangeFor(helper.getLevel(), base, 32));
            helper.assertTrue(altar.posWithinRange(east) || altar.posWithinRange(south), "40 blocks away is out of range with a witch at the altar");
            SpeciesConfig.overrideWitchBonus(false);
            helper.assertTrue(!altar.posWithinRange(east) && !altar.posWithinRange(south), "the range grew with witch_bonus_enabled off");
        } finally {
            SpeciesConfig.overrideWitchBonus(null);
            remove(helper, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_off")
    public static void witchBonusDisabledWhenMixinMissing(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, witch.getUUID(), base);
        put(helper, witch, chalkPos);
        SpeciesConfig.overrideWitchBonus(false);
        try {
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 4000, "with the bonus off a witch pays 1000, altar left with " + power(altar));
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), base, 32) == 32, "the range grew with the bonus off");
        } finally {
            SpeciesConfig.overrideWitchBonus(null);
            remove(helper, witch);
        }
        helper.succeed();
    }

    // ---- siphoned power pays ----

    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_cost")
    public static void siphonerPaysFromSiphonedPower(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer siphoner = player(Species.SIPHONER, 500, helper);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, siphoner.getUUID(), base);
        put(helper, siphoner, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(300), "the charge failed");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 200 && power(altar) == 5000,
                    "siphoned " + BloodlineAccess.get(siphoner).siphonedPower() + ", altar " + power(altar));
            helper.assertTrue(chalk.tryConsumePower(300), "the charge failed");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 0 && power(altar) == 4900,
                    "the altar pays what siphoned power cannot: siphoned " + BloodlineAccess.get(siphoner).siphonedPower() + ", altar " + power(altar));
            // an altar that cannot pay the rest costs the siphoner nothing
            BloodlineAccess.set(siphoner, state(Species.SIPHONER, 100));
            set(altar, "power", 10.0);
            helper.assertTrue(!chalk.tryConsumePower(300), "a poor altar paid 200");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 100, "siphoned power was spent on a charge that failed: " + BloodlineAccess.get(siphoner).siphonedPower());
        } finally {
            remove(helper, siphoner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_alone")
    public static void siphonerNeedsNoAltarWhenSiphonedPowerCoversIt(GameTestHelper helper) {
        BlockPos chalkPos = helper.absolutePos(new BlockPos(1, 200, 1));
        RecordingPlayer siphoner = player(Species.SIPHONER, 400, helper);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, siphoner.getUUID());
        put(helper, siphoner, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(300), "siphoned power alone did not pay");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 100, "siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
            helper.assertTrue(!chalk.tryConsumePower(300), "100 siphoned power paid 300 with no altar");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 100, "a failed charge cost " + (100 - BloodlineAccess.get(siphoner).siphonedPower()));
        } finally {
            remove(helper, siphoner);
        }
        helper.succeed();
    }

    /** The start cost of a rite, through the chalk's real tick: no altar at all, 1000 siphoned power, a 1000 power rite. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_start", timeoutTicks = 100)
    public static void siphonerStartsRiteWithoutAltar(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer siphoner = player(Species.SIPHONER, 1000, helper);
        GoldChalkBlockEntity chalk = chalk(helper, centre(helper), siphoner.getUUID());
        place(helper, siphoner);
        set(chalk, "type", type(helper, "blood_offering"));
        set(chalk, "itemsToConsume", new ArrayList<>());
        set(chalk, "entitiesToConsume", new ArrayList<>());
        set(chalk, "isInitialising", true);
        helper.succeedWhen(() -> {
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 0, "siphoned power is " + BloodlineAccess.get(siphoner).siphonedPower());
            helper.assertTrue(dropped(helper, "supernaturalbloodlines:blood_stone") == 1, "the rite did not run");
            remove(helper, siphoner);
        });
    }

    // ---- siphoning ----

    private static PlayerInteractEvent.RightClickBlock click(ServerPlayer p, BlockPos pos) {
        var e = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
        NeoForge.EVENT_BUS.post(e);
        return e;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonerDrainsBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos plain = helper.absolutePos(new BlockPos(2, 1, 0));
        level.setBlock(stone, PartBlocks.BLOOD_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(plain, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(Siphoning.hasAltarData(level, level.getBlockState(stone)), "blood stone holds no altar power");
        helper.assertTrue(!Siphoning.hasAltarData(level, level.getBlockState(plain)), "plain stone holds altar power");

        RecordingPlayer siphoner = player(Species.SIPHONER, 0, helper);
        siphoner.setShiftKeyDown(true);
        try {
            helper.assertTrue(click(siphoner, stone).isCanceled(), "sneaking on altar power did not take the click");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == Siphoning.GAIN, "siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
            click(siphoner, stone);
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == Siphoning.GAIN, "the same block gave power twice");
            helper.assertTrue(siphoner.receivedOnActionBar(Siphoning.COOLING), "no cooldown message");
            helper.assertTrue(!click(siphoner, plain).isCanceled(), "a block without altar power took the click");
            // standing up, or holding something, is an ordinary click
            siphoner.setShiftKeyDown(false);
            level.setBlock(stone.above(), PartBlocks.BLOOD_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(!click(siphoner, stone.above()).isCanceled(), "a standing player siphoned");
            // not a siphoner
            RecordingPlayer human = player(Species.NONE, 0, helper);
            human.setShiftKeyDown(true);
            helper.assertTrue(!click(human, stone.above()).isCanceled(), "a human siphoned");
        } finally {
            BloodlineAccess.set(siphoner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonerDrainsCreatureAndStopsAtTheCap(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        RecordingPlayer siphoner = player(Species.SIPHONER, Siphoning.CAP - 20, helper);
        try {
            float before = zombie.getHealth();
            helper.assertTrue(Siphoning.siphonEntity(siphoner, zombie), "the zombie gave nothing");
            helper.assertTrue(before - zombie.getHealth() == Siphoning.ENTITY_DAMAGE, "the zombie lost " + (before - zombie.getHealth()));
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == Siphoning.CAP, "siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
            zombie.invulnerableTime = 0;
            helper.assertTrue(!Siphoning.siphonEntity(siphoner, zombie), "took power past the cap");
            helper.assertTrue(zombie.getHealth() == before - Siphoning.ENTITY_DAMAGE, "a full siphoner still hurt the zombie");
            helper.assertTrue(siphoner.receivedOnActionBar(Siphoning.FULL), "no 'full' message");
        } finally {
            BloodlineAccess.set(siphoner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- joining the vampires ----

    private static void joinsVampires(GameTestHelper helper, Species before, Species after) {
        ServerPlayer p = player(before, 0, helper);
        try {
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.species() == after, before + " joined the vampires and is " + s.species());
            helper.assertTrue(!s.witchLayer(), "the witch layer stayed");
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchJoiningVampireLosesMagic(GameTestHelper helper) {
        joinsVampires(helper, Species.WITCH, Species.NONE);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonerJoiningVampireBecomesHeretic(GameTestHelper helper) {
        joinsVampires(helper, Species.SIPHONER, Species.HERETIC);
        helper.succeed();
    }
}
