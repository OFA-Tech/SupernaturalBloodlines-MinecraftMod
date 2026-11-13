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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
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

    static void set(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static double power(AltarBlockEntity altar) {
        try {
            Field f = AltarBlockEntity.class.getDeclaredField("power");
            f.setAccessible(true);
            return f.getDouble(altar);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static AltarBlockEntity altar(GameTestHelper helper, BlockPos base, double power) {
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
    static GoldChalkBlockEntity chalk(GameTestHelper helper, BlockPos pos, UUID caster, BlockPos... altars) {
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

    static void put(GameTestHelper helper, ServerPlayer p, BlockPos at) {
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
        BlockPos east = base.offset(40, 0, 0);
        BlockPos south = base.offset(0, 0, 40);
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        put(helper, witch, base.offset(1, 0, 0));
        try {
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), base, 32) == 48, "range is " + WitchAltarBonus.rangeFor(helper.getLevel(), base, 32));
            AltarBlockEntity altar = altar(helper, base, 0);
            helper.assertTrue(altar.posWithinRange(east) || altar.posWithinRange(south), "40 blocks away is out of range with a witch at the altar");
        } finally {
            remove(helper, witch);
        }
        helper.succeed();
    }

    /** One altar loads with a witch beside it, one without: each keeps its range, so what it adds it can also remove. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_drift")
    public static void altarRangeIsDecidedWhenItLoads(GameTestHelper helper) {
        BlockPos wide = helper.absolutePos(new BlockPos(0, 200, 0));
        BlockPos narrow = helper.absolutePos(new BlockPos(0, 240, 0));
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        put(helper, witch, wide.offset(1, 0, 0));
        AltarBlockEntity wideAltar;
        try {
            wideAltar = altar(helper, wide, 0);
        } finally {
            remove(helper, witch);
        }
        AltarBlockEntity narrowAltar = altar(helper, narrow, 0); // no witch within reach: 32
        boolean wideEast = wideAltar.posWithinRange(wide.offset(40, 0, 0));
        boolean wideSouth = wideAltar.posWithinRange(wide.offset(0, 0, 40));
        helper.assertTrue(wideEast || wideSouth, "the altar loaded beside a witch is not wide");
        // the witch is gone: the wide altar still sees the same blocks
        helper.assertTrue(wideAltar.posWithinRange(wide.offset(40, 0, 0)) == wideEast && wideAltar.posWithinRange(wide.offset(0, 0, 40)) == wideSouth,
                "the wide altar narrowed when the witch left");
        // a witch arrives at the narrow one: it stays narrow
        RecordingPlayer late = player(Species.WITCH, 0, helper);
        put(helper, late, narrow.offset(1, 0, 0));
        try {
            helper.assertTrue(!narrowAltar.posWithinRange(narrow.offset(40, 0, 0)) && !narrowAltar.posWithinRange(narrow.offset(0, 0, 40)),
                    "the narrow altar widened when a witch arrived");
        } finally {
            remove(helper, late);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_off")
    public static void witchBonusDisabledWhenMixinMissing(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        put(helper, witch, chalkPos);
        SpeciesConfig.overrideWitchBonus(false);
        try {
            AltarBlockEntity altar = altar(helper, base, 5000);
            GoldChalkBlockEntity chalk = chalk(helper, chalkPos, witch.getUUID(), base);
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 4000, "with the bonus off a witch pays 1000, altar left with " + power(altar));
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), base, 32) == 32, "the range grew with the bonus off");
            helper.assertTrue(!altar.posWithinRange(base.offset(40, 0, 0)) && !altar.posWithinRange(base.offset(0, 0, 40)), "the altar grew with the bonus off");
        } finally {
            SpeciesConfig.overrideWitchBonus(null);
            remove(helper, witch);
        }
        helper.succeed();
    }

    // ---- siphoned power pays the start cost ----

    /**
     * Waits for a game tick divisible by 20 (the only one a gold chalk acts on), then runs the chalk's real
     * {@code serverTick} once with {@code rite} set up as if it had been matched, and checks the result.
     */
    private static void startThroughChalk(GameTestHelper helper, BlockPos pos, String rite, Runnable prepare, Runnable check, Runnable cleanup) {
        boolean[] ran = {false};
        helper.succeedWhen(() -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(ran[0] || level.getGameTime() % 20 == 0, "waiting for a tick divisible by 20");
            if (!ran[0]) {
                ran[0] = true;
                GoldChalkBlockEntity chalk = (GoldChalkBlockEntity) level.getBlockEntity(pos);
                set(chalk, "type", type(helper, rite));
                set(chalk, "itemsToConsume", new ArrayList<>());
                set(chalk, "entitiesToConsume", new ArrayList<>());
                set(chalk, "isInitialising", true);
                prepare.run();
                GoldChalkBlockEntity.serverTick(level, pos, level.getBlockState(pos), chalk);
            }
            check.run();
            cleanup.run();
        });
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_cost", timeoutTicks = 100)
    public static void siphonerPaysFromSiphonedPower(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer siphoner = player(Species.SIPHONER, 500, helper);
        chalk(helper, chalkPos, siphoner.getUUID(), base);
        put(helper, siphoner, chalkPos);
        startThroughChalk(helper, chalkPos, "blood_offering", () -> set(altar, "power", 5000.0), () -> {
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 0, "siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
            helper.assertTrue(power(altar) == 4500, "the altar pays what siphoned power cannot (500 of 1000), altar left with " + power(altar));
        }, () -> remove(helper, siphoner));
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_poor", timeoutTicks = 100)
    public static void siphonerKeepsPowerWhenTheAltarCannotPay(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 10);
        BlockPos chalkPos = base.offset(0, 0, -2);
        RecordingPlayer siphoner = player(Species.SIPHONER, 500, helper);
        chalk(helper, chalkPos, siphoner.getUUID(), base);
        put(helper, siphoner, chalkPos);
        startThroughChalk(helper, chalkPos, "blood_offering", () -> set(altar, "power", 10.0), () -> {
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 500, "siphoned power was spent on a rite that never started: " + BloodlineAccess.get(siphoner).siphonedPower());
        }, () -> remove(helper, siphoner));
    }

    /** Per-tick costs get the witch discount only: siphoned power would otherwise sync to the client every tick. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_tick")
    public static void perTickCostIgnoresSiphonedPower(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        RecordingPlayer siphoner = player(Species.SIPHONER, 500, helper);
        BlockPos chalkPos = base.offset(0, 0, -2);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, siphoner.getUUID(), base);
        GoldChalkBlockEntity alone = chalk(helper, base.offset(5, 0, -2), siphoner.getUUID());
        put(helper, siphoner, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(300), "the altar did not pay");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 500, "a per-tick charge took siphoned power: " + BloodlineAccess.get(siphoner).siphonedPower());
            helper.assertTrue(power(altar) == 4700, "the altar pays the whole 300, left with " + power(altar));
            helper.assertTrue(!alone.tryConsumePower(300), "siphoned power paid a per-tick cost with no altar");
        } finally {
            remove(helper, siphoner);
        }
        helper.succeed();
    }

    /** The target of a rite stands on the circle's centre, and the discount follows them, not only the caster. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "witch_centre")
    public static void witchOnTheCircleCheapensTheCast(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 0));
        AltarBlockEntity altar = altar(helper, base, 5000);
        BlockPos chalkPos = base.offset(0, 0, -2);
        GoldChalkBlockEntity chalk = chalk(helper, chalkPos, UUID.randomUUID(), base); // the caster is not online
        RecordingPlayer witch = player(Species.WITCH, 0, helper);
        put(helper, witch, chalkPos);
        try {
            helper.assertTrue(chalk.tryConsumePower(1000), "the altar did not pay");
            helper.assertTrue(power(altar) == 4250, "the witch on the circle pays 750, altar left with " + power(altar));
        } finally {
            remove(helper, witch);
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

    /** A heretic (vampire faction, can siphon) calling ancestors is refused by the rite itself, in onStart. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_cancel", timeoutTicks = 100)
    public static void siphonedPowerKeptWhenTheRiteCancels(GameTestHelper helper) {
        RecordingPlayer heretic = RecordingPlayer.create(helper);
        VampirismAPI.factionPlayerHandler(heretic).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        BloodlineAccess.set(heretic, state(Species.HERETIC, 4000));
        chalk(helper, centre(helper), heretic.getUUID());
        place(helper, heretic);
        startThroughChalk(helper, centre(helper), "ancestral_calling", () -> {}, () -> {
            helper.assertTrue(heretic.receivedOnActionBar("supernaturalbloodlines.refusal.vampire_no_magic"), "the rite did not refuse");
            helper.assertTrue(BloodlineAccess.get(heretic).siphonedPower() == 4000, "a refused rite cost " + (4000 - BloodlineAccess.get(heretic).siphonedPower()));
        }, () -> {
            remove(helper, heretic);
            VampirismAPI.factionPlayerHandler(heretic).leaveFaction(false);
        });
    }

    /** Same through the shared faction rite: a siphoner without the vampire faction casting a vampire rite. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_wrong", timeoutTicks = 100)
    public static void siphonedPowerKeptOnWrongFactionRite(GameTestHelper helper) {
        RecordingPlayer siphoner = player(Species.SIPHONER, 4000, helper);
        chalk(helper, centre(helper), siphoner.getUUID());
        place(helper, siphoner);
        startThroughChalk(helper, centre(helper), "cleansing_blood", () -> {}, () -> {
            helper.assertTrue(siphoner.receivedOnActionBar("supernaturalbloodlines.rite.wrong_faction"), "the rite did not refuse");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 4000, "a refused rite cost " + (4000 - BloodlineAccess.get(siphoner).siphonedPower()));
        }, () -> remove(helper, siphoner));
    }

    // ---- siphoning ----

    static PlayerInteractEvent.RightClickBlock click(ServerPlayer p, BlockPos pos) {
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
            helper.assertTrue(zombie.getLastDamageSource() != null && zombie.getLastDamageSource().getEntity() == siphoner, "the damage has no attacker");
            zombie.invulnerableTime = 0;
            helper.assertTrue(!Siphoning.siphonEntity(siphoner, zombie), "took power past the cap");
            helper.assertTrue(zombie.getHealth() == before - Siphoning.ENTITY_DAMAGE, "a full siphoner still hurt the zombie");
            helper.assertTrue(siphoner.receivedOnActionBar(Siphoning.FULL), "no 'full' message");
        } finally {
            BloodlineAccess.set(siphoner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonedCreatureCoolsDown(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        RecordingPlayer siphoner = player(Species.SIPHONER, 0, helper);
        try {
            helper.assertTrue(Siphoning.siphonEntity(siphoner, zombie), "the first siphon failed");
            float health = zombie.getHealth();
            zombie.invulnerableTime = 0;
            helper.assertTrue(!Siphoning.siphonEntity(siphoner, zombie), "the same creature gave power twice in 30 seconds");
            helper.assertTrue(zombie.getHealth() == health, "a cooling creature was hurt");
            helper.assertTrue(siphoner.receivedOnActionBar(Siphoning.COOLING), "no cooldown message");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == Siphoning.GAIN, "siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
        } finally {
            BloodlineAccess.set(siphoner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    /** The damage comes from the siphoner, so a player is only a target where PvP allows it, and a refused hit gives nothing. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "siphon_pvp")
    public static void siphoningFollowsPvpRules(GameTestHelper helper) throws ReflectiveOperationException {
        var server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        RecordingPlayer siphoner = player(Species.SIPHONER, 0, helper);
        // vanilla's rule: a player can be harmed by another player exactly when the server allows PvP
        ServerPlayer victim = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pvp-victim")) {
            @Override
            public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
                return false;
            }

            @Override
            public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return server.isPvpAllowed();
            }
        };
        try {
            Field spawnProtection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(victim, 0); // it only counts down in tick(), which a FakePlayer never runs
            server.setPvpAllowed(false);
            float health = victim.getHealth();
            helper.assertTrue(!Siphoning.siphonEntity(siphoner, victim), "siphoned a player with PvP off");
            helper.assertTrue(victim.getHealth() == health && BloodlineAccess.get(siphoner).siphonedPower() == 0, "PvP off: health " + victim.getHealth() + ", siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
            server.setPvpAllowed(true);
            helper.assertTrue(Siphoning.siphonEntity(siphoner, victim), "could not siphon a player with PvP on");
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == Siphoning.GAIN, "PvP on: siphoned " + BloodlineAccess.get(siphoner).siphonedPower());
        } finally {
            server.setPvpAllowed(pvp);
            BloodlineAccess.set(siphoner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- R65: death ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siphonedPowerResetsOnDeath(GameTestHelper helper) {
        RecordingPlayer siphoner = player(Species.SIPHONER, 700, helper);
        try {
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(siphoner, true));
            helper.assertTrue(BloodlineAccess.get(siphoner).siphonedPower() == 700, "leaving the End emptied the siphoned power");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(siphoner, false));
            BloodlineState s = BloodlineAccess.get(siphoner);
            helper.assertTrue(s.siphonedPower() == 0, "siphoned power is " + s.siphonedPower());
            helper.assertTrue(s.species() == Species.SIPHONER, "dying changed the species to " + s.species());
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

    /** A siphoner who joins the vampires becomes a heretic and still siphons blocks and creatures (and keeps the witch bonus). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void hereticKeepsSiphoning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = helper.absolutePos(new BlockPos(0, 1, 0));
        level.setBlock(stone, PartBlocks.BLOOD_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        zombie.setNoAi(true);
        RecordingPlayer p = player(Species.SIPHONER, 0, helper);
        try {
            VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.HERETIC, "species is " + BloodlineAccess.get(p).species());
            p.setShiftKeyDown(true);
            helper.assertTrue(click(p, stone).isCanceled(), "a heretic's sneak click on altar power was not taken");
            helper.assertTrue(BloodlineAccess.get(p).siphonedPower() == Siphoning.GAIN, "block: siphoned " + BloodlineAccess.get(p).siphonedPower());
            helper.assertTrue(Siphoning.siphonEntity(p, zombie), "the zombie gave a heretic nothing, health " + zombie.getHealth());
            helper.assertTrue(BloodlineAccess.get(p).siphonedPower() == 2 * Siphoning.GAIN, "creature: siphoned " + BloodlineAccess.get(p).siphonedPower());
            helper.assertTrue(WitchAltarBonus.hasBonus(Species.HERETIC), "a heretic lost the witch bonus");
        } finally {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
