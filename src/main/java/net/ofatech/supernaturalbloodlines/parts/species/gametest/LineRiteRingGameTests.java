package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.LineRiteGameTests.IMPRISONED;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.LineRiteGameTests.OTHERWHERE;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.LineRiteGameTests.eclipse;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineDeath;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.rite.ImprisonedOriginalRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;

/**
 * The Rite of the Imprisoned Original (R126) in a world with a mountain ash ring. Registered only when the artifacts part's ring block
 * exists. The rite is driven tick by tick by hand, as Enchanted's rite manager would, with a 5 second hold.
 */
@PrefixGameTestTemplate(false)
public final class LineRiteRingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final int RADIUS = 6;
    private static final int HOLD_TICKS = 5 * 20;

    private LineRiteRingGameTests() {
    }

    private static BlockState ringBlock() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("supernaturalbloodlines:mountain_ash_line")).defaultBlockState();
    }

    private static List<BlockPos> ring(GameTestHelper helper) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == RADIUS) {
                    out.add(centre(helper).offset(dx, 0, dz));
                }
            }
        }
        return out;
    }

    private static void buildRing(GameTestHelper helper) {
        ring(helper).forEach(p -> helper.getLevel().setBlock(p, ringBlock(), 2));
    }

    private static void clearRing(GameTestHelper helper) {
        ring(helper).forEach(p -> helper.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 2));
    }

    /** Ticks the rite until it ends or {@code limit} ticks pass; returns the tick it ended on, or -1. */
    private static int run(Rite rite, int limit, BooleanSupplier beforeEachTick) {
        for (int i = 1; i <= limit; i++) {
            if (!beforeEachTick.getAsBoolean()) {
                return -2;
            }
            if (!rite.tick()) {
                return i;
            }
        }
        return -1;
    }

    private static void common(GameTestHelper helper) {
        SpeciesConfig.overrideImprisonmentSeconds(5);
    }

    private static void finish(GameTestHelper helper, LineRiteGameTests.Rig rig, boolean ring) {
        SpeciesConfig.overrideImprisonmentSeconds(null);
        SpeciesConfig.overrideRingRadius(null);
        if (ring) {
            clearRing(helper);
        }
        remove(helper, rig.original);
    }

    private static Rite begin(GameTestHelper helper, LineRiteGameTests.Rig rig) {
        return start(helper, IMPRISONED, rig.caster(), rig.imprisonedItems());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedFreesTargetAndBranch(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        LineTestPlayer c = LineTestPlayer.create(helper);
        LineTestPlayer d = LineTestPlayer.create(helper);
        species(c, Species.HYBRID);
        species(d, Species.HYBRID);
        try {
            SireRecorder.record(rig.target, c);
            SireRecorder.record(rig.target, d);
            place(helper, rig.original);
            eclipse(helper, () -> {
                Rite rite = begin(helper, rig);
                helper.assertTrue(run(rite, HOLD_TICKS + 20, () -> true) > 0, "the rite did not end");
            });
            UUID t = rig.target.getUUID();
            LineIndex idx = rig.lines();
            helper.assertTrue(idx.rootOf(t).filter(t::equals).isPresent(), "the target is not their own head");
            helper.assertTrue(idx.rootOf(c.getUUID()).filter(t::equals).isPresent() && idx.rootOf(d.getUUID()).filter(t::equals).isPresent(),
                    "the branch did not follow the target");
            helper.assertTrue(idx.freedOf(t).filter(f -> f.kind() == LineIndex.Kind.IMPRISONED && f.from().equals(rig.original.getUUID())).isPresent(),
                    "the freed record is " + idx.freedOf(t));
            helper.assertTrue(idx.freedOf(c.getUUID()).isEmpty(), "a member of the branch was marked freed");
            helper.assertTrue(rig.original.getUUID().equals(BloodlineAccess.get(rig.target).knownLine()), "the target did not learn the Original");
        } finally {
            finish(helper, rig, true);
            rig.close(c, d);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedNeverFreesTheWholeLine(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        LineTestPlayer c = LineTestPlayer.create(helper);
        LineTestPlayer s = LineTestPlayer.create(helper);
        species(c, Species.HYBRID);
        species(s, Species.HYBRID);
        try {
            SireRecorder.record(rig.target, c);
            SireRecorder.record(rig.original, s);
            online(rig.original, rig.target, c, s);
            place(helper, rig.original);
            eclipse(helper, () -> {
                Rite rite = begin(helper, rig);
                run(rite, HOLD_TICKS + 20, () -> true);
            });
            LineIndex idx = rig.lines();
            helper.assertTrue(idx.rootOf(s.getUUID()).filter(rig.original.getUUID()::equals).isPresent(), "someone outside the branch was freed");
            helper.assertTrue(idx.membersOf(rig.original.getUUID()).stream().anyMatch(m -> m.id().equals(s.getUUID()))
                    && idx.membersOf(rig.original.getUUID()).stream().noneMatch(m -> m.id().equals(rig.target.getUUID()) || m.id().equals(c.getUUID())),
                    "the Original's line is " + idx.membersOf(rig.original.getUUID()));
            LineDeath.run(rig.server(), rig.original.getUUID());
            helper.assertTrue(s.isDeadOrDying() && !rig.target.isDeadOrDying() && !c.isDeadOrDying(), "line death reached the freed branch or missed the rest");
        } finally {
            finish(helper, rig, true);
            rig.close(c, s);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedWorksOnAnNpcOriginalEntity(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        UUID klaus = NpcOriginal.KLAUS.uuid();
        Zombie stand = helper.spawn(EntityType.ZOMBIE, new Vec3(8.5, 1, 8.5));
        stand.setNoAi(true);
        stand.setData(SpeciesRegistries.ORIGINAL_ROOT, "klaus");
        try {
            rig.lines().setRoot(rig.target.getUUID(), klaus, true);
            eclipse(helper, () -> {
                Rite rite = begin(helper, rig);
                run(rite, HOLD_TICKS + 20, () -> true);
            });
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).filter(f -> f.from().equals(klaus)).isPresent(), "an NPC Original entity could not be imprisoned");
        } finally {
            stand.discard();
            finish(helper, rig, true);
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedWorksOnAPlayerOriginal(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        try {
            helper.assertTrue(rig.lines().rootOf(rig.target.getUUID()).filter(rig.original.getUUID()::equals).isPresent(), "control: the Original is not a player of the line");
            place(helper, rig.original);
            eclipse(helper, () -> run(begin(helper, rig), HOLD_TICKS + 20, () -> true));
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isPresent(), "a player Original could not be imprisoned");
        } finally {
            finish(helper, rig, true);
            rig.close();
        }
        helper.succeed();
    }

    // ---- it must hold for the whole rite ----

    private static void failsMidRite(GameTestHelper helper, String why, Runnable breakIt) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        try {
            place(helper, rig.original);
            eclipse(helper, () -> {
                Rite rite = begin(helper, rig);
                int half = run(rite, HOLD_TICKS / 2, () -> true);
                helper.assertTrue(half == -1, why + ": control: the rite ended at tick " + half + " before the break");
                breakIt.run();
                int ended = run(rite, HOLD_TICKS, () -> true);
                helper.assertTrue(ended > 0 && ended <= 12, why + ": the rite did not end within half a second of the break: " + ended);
            });
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isEmpty(), why + ": the target was freed");
            helper.assertTrue(rig.lines().rootOf(rig.target.getUUID()).filter(rig.original.getUUID()::equals).isPresent(), why + ": the line changed");
            helper.assertTrue(rig.caster().received(ImprisonedOriginalRiteFactory.RING_BROKEN), why + ": the caster was not told the rite failed");
            helper.assertTrue(dropped(helper, OTHERWHERE) == 0, why + ": the ingredients came back");
        } finally {
            finish(helper, rig, true);
            rig.close();
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedFailsWhenTheRingBreaksMidRite(GameTestHelper helper) {
        failsMidRite(helper, "ring", () -> helper.getLevel().setBlock(centre(helper).offset(RADIUS, 0, 0), Blocks.AIR.defaultBlockState(), 2));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedFailsWhenTheOriginalStepsOut(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        try {
            place(helper, rig.original);
            eclipse(helper, () -> {
                Rite rite = begin(helper, rig);
                helper.assertTrue(run(rite, HOLD_TICKS / 2, () -> true) == -1, "control: the rite ended early");
                rig.original.setPos(Vec3.atBottomCenterOf(centre(helper)).add(3, 1, 3));
                int ended = run(rite, HOLD_TICKS, () -> true);
                helper.assertTrue(ended > 0 && ended <= 12, "the rite went on with the Original out of the centre: " + ended);
            });
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isEmpty(), "the target was freed");
        } finally {
            finish(helper, rig, true);
            rig.close();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedFailsWhenTheEclipseEnds(GameTestHelper helper) {
        failsMidRite(helper, "eclipse", () -> LunarEclipse.stop(helper.getLevel().getServer()));
        helper.succeed();
    }

    // ---- refusals at the start ----

    private static void refusedAtStart(GameTestHelper helper, String reason, Species performer, boolean ring, boolean withEclipse, boolean originalOnCentre,
                                       java.util.function.Consumer<LineRiteGameTests.Rig> tweak) {
        common(helper);
        if (ring) {
            buildRing(helper);
        }
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, performer);
        try {
            if (originalOnCentre) {
                place(helper, rig.original);
            }
            tweak.accept(rig);
            Runnable go = () -> begin(helper, rig);
            if (withEclipse) {
                eclipse(helper, go);
            } else {
                go.run();
            }
            String key = LineRules.REFUSAL_PREFIX + reason;
            helper.assertTrue(rig.caster().received(key), reason + ": the caster was not told " + key + ": " + rig.caster().heard());
            helper.assertTrue(rig.lines().freedOf(rig.target.getUUID()).isEmpty(), reason + ": the target was freed");
            helper.assertTrue(dropped(helper, OTHERWHERE) == 2, reason + ": the ingredients were not given back: " + dropped(helper, OTHERWHERE));
        } finally {
            finish(helper, rig, ring);
            rig.close();
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedRefusals(GameTestHelper helper) {
        refusedAtStart(helper, "ring_open", Species.WITCH, false, true, true, r -> { });
        refusedAtStart(helper, "nobody_trapped", Species.WITCH, true, true, false, r -> { });
        refusedAtStart(helper, "wrong_original", Species.WITCH, true, true, true, r -> {
            // the target belongs to another Original than the one on the circle
            r.lines().setRoot(r.target.getUUID(), NpcOriginal.ELIJAH.uuid(), true);
        });
        refusedAtStart(helper, "no_eclipse", Species.WITCH, true, false, true, r -> { });
        refusedAtStart(helper, "not_witch", Species.HYBRID, true, true, true, r -> { });
        refusedAtStart(helper, "target_away", Species.WITCH, true, true, true, r -> online(r.original));
        refusedAtStart(helper, "target_original", Species.WITCH, true, true, true, r -> {
            species(r.target, Species.ORIGINAL);
            r.lines().setRoot(r.target.getUUID(), r.original.getUUID(), true);
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imprisonedRingRadiusIsAConfig(GameTestHelper helper) {
        common(helper);
        buildRing(helper);
        SpeciesConfig.overrideRingRadius(4); // the ring is 6 out
        LineRiteGameTests.Rig rig = new LineRiteGameTests.Rig(helper, Species.WITCH);
        try {
            place(helper, rig.original);
            eclipse(helper, () -> begin(helper, rig));
            helper.assertTrue(rig.caster().received(LineRules.REFUSAL_PREFIX + "ring_open"), "a ring beyond the configured radius closed: " + rig.caster().heard());
        } finally {
            finish(helper, rig, true);
            rig.close();
        }
        helper.succeed();
    }
}
