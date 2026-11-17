package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.line.NpcLines;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/** Who belongs to whose line (R120, R121, R127): inheritance, rite-made Originals, NPC vampires, the login backfill and the cure. */
@PrefixGameTestTemplate(false)
public final class LineGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation VAMPIRE = ResourceLocation.parse("vampirism:vampire");
    private static final ResourceLocation BARON = ResourceLocation.parse("vampirism:vampire_baron");

    private LineGameTests() {
    }

    private static ServerPlayer mortal(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        species(p, s);
        return p;
    }

    private static LineIndex lines(GameTestHelper helper) {
        return LineIndex.get(helper.getLevel().getServer());
    }

    private static void forget(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            lines(helper).remove(p.getUUID());
        }
        cleanUp(ps);
    }

    private static Mob spawnVampire(GameTestHelper helper) {
        Entity e = helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(VAMPIRE), new Vec3(6.5, 2, 4.5));
        Mob mob = (Mob) e;
        mob.setNoAi(true);
        return mob;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalIsItsOwnRoot(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        try {
            helper.assertTrue(lines(helper).rootOf(o.getUUID()).filter(o.getUUID()::equals).isPresent(), "the Original is not the root of their own line");
            helper.assertTrue(lines(helper).membersOf(o.getUUID()).isEmpty(), "the Original is a member of their own line");
        } finally {
            forget(helper, o);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredPlayerInheritsTheOriginalsLine(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(o, a);
            helper.assertTrue(lines(helper).rootOf(a.getUUID()).filter(o.getUUID()::equals).isPresent(), "the sired player did not get the Original's line");
        } finally {
            forget(helper, o, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineFollowsTheSireDown(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer b = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, b);
            helper.assertTrue(lines(helper).rootOf(b.getUUID()).filter(o.getUUID()::equals).isPresent(), "the second generation did not get the Original's line");
        } finally {
            forget(helper, o, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredByALinelessVampireGivesNoLine(GameTestHelper helper) {
        ServerPlayer sire = mortal(helper, Species.NONE);
        ServerPlayer child = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(lines(helper).rootOf(child.getUUID()).isEmpty(), "a child of a lineless sire got a line");
        } finally {
            forget(helper, sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void playerMadeOriginalRootsAnOwnLineAndTakesTheBranch(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer b = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, b);
            species(a, Species.ORIGINAL);
            LineIndex idx = lines(helper);
            helper.assertTrue(idx.rootOf(a.getUUID()).filter(a.getUUID()::equals).isPresent(), "the made Original is not their own root");
            helper.assertTrue(idx.rootOf(b.getUUID()).filter(a.getUUID()::equals).isPresent(), "the branch did not move with the made Original");
            helper.assertTrue(idx.rootOf(o.getUUID()).filter(o.getUUID()::equals).isPresent(), "the first Original lost their line");
            helper.assertTrue(idx.membersOf(o.getUUID()).stream().noneMatch(m -> m.id().equals(a.getUUID()) || m.id().equals(b.getUUID())),
                    "the first Original's line still lists the branch");
        } finally {
            forget(helper, o, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curedPlayerLeavesTheLine(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, a);
            helper.assertTrue(lines(helper).rootOf(a.getUUID()).isPresent(), "control: the sired hybrid has no line");
            species(a, Species.NONE);
            helper.assertTrue(lines(helper).rootOf(a.getUUID()).isEmpty(), "a cured player stayed in the line");
        } finally {
            forget(helper, o, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void existingSireChainIsBackfilledAtLogin(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer b = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, b);
            // a world from before lines: the sires are recorded, the line index is empty
            lines(helper).remove(o.getUUID());
            lines(helper).remove(a.getUUID());
            lines(helper).remove(b.getUUID());
            helper.assertTrue(Lines.heal(b).filter(o.getUUID()::equals).isPresent(), "the backfill did not find the Original");
            helper.assertTrue(lines(helper).rootOf(b.getUUID()).filter(o.getUUID()::equals).isPresent(), "the backfill did not store the line");
            helper.assertTrue(Lines.heal(o).filter(o.getUUID()::equals).isPresent(), "an Original with no entry did not get their own line");
        } finally {
            forget(helper, o, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void healWithNoOriginalAboveLeavesNoLine(GameTestHelper helper) {
        ServerPlayer sire = mortal(helper, Species.NONE);
        ServerPlayer child = mortal(helper, Species.NONE);
        try {
            SireRecorder.record(sire, child);
            helper.assertTrue(Lines.heal(child).isEmpty(), "a line was invented");
            helper.assertTrue(lines(helper).rootOf(child.getUUID()).isEmpty(), "an entry was stored");
        } finally {
            forget(helper, sire, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void npcVampireJoinsAWeightedLine(GameTestHelper helper) {
        NpcLines.useRoll(() -> 0.0);
        SpeciesConfig.overrideNpcLineWeights(List.of("mikael=1", "klaus=1"));
        Mob mob = null;
        try {
            mob = spawnVampire(helper);
            helper.assertTrue(mob.hasData(SpeciesRegistries.LINE) && NpcOriginal.MIKAEL.uuid().equals(mob.getData(SpeciesRegistries.LINE)),
                    "the creature's mirror is wrong");
            helper.assertTrue(lines(helper).rootOf(mob.getUUID()).isEmpty(), "an un-sired NPC vampire was stored in the index");
        } finally {
            NpcLines.useRoll(null);
            SpeciesConfig.overrideNpcLineWeights(null);
            if (mob != null) {
                lines(helper).remove(mob.getUUID());
                mob.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void npcVampireNeverJoinsADeadRoot(GameTestHelper helper) {
        NpcLines.useRoll(() -> 0.0);
        SpeciesConfig.overrideNpcLineWeights(List.of("mikael=1", "finn=1"));
        lines(helper).markDead(NpcOriginal.MIKAEL.uuid());
        Mob mob = null;
        try {
            mob = spawnVampire(helper);
            helper.assertTrue(NpcOriginal.FINN.uuid().equals(mob.getData(SpeciesRegistries.LINE)), "the creature joined a dead Original's line: " + mob.getData(SpeciesRegistries.LINE));
        } finally {
            lines(helper).unmarkDead(NpcOriginal.MIKAEL.uuid());
            NpcLines.useRoll(null);
            SpeciesConfig.overrideNpcLineWeights(null);
            if (mob != null) {
                lines(helper).remove(mob.getUUID());
                mob.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCreatureKeepsItsLineWhenItsRootDiesAfterwards(GameTestHelper helper) {
        NpcLines.useRoll(() -> 0.0);
        SpeciesConfig.overrideNpcLineWeights(List.of("mikael=1"));
        Mob mob = null;
        try {
            mob = spawnVampire(helper);
            lines(helper).markDead(NpcOriginal.MIKAEL.uuid());
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(mob, helper.getLevel()));
            helper.assertTrue(NpcOriginal.MIKAEL.uuid().equals(mob.getData(SpeciesRegistries.LINE)), "an existing member lost its line");
        } finally {
            lines(helper).unmarkDead(NpcOriginal.MIKAEL.uuid());
            NpcLines.useRoll(null);
            SpeciesConfig.overrideNpcLineWeights(null);
            if (mob != null) {
                lines(helper).remove(mob.getUUID());
                mob.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void exemptTypeGetsNoLine(GameTestHelper helper) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(BARON)) {
            helper.succeed();
            return;
        }
        EntityType<?> baron = BuiltInRegistries.ENTITY_TYPE.get(BARON);
        helper.assertTrue(baron.is(NpcLines.EXEMPT), "the baron is not in #line_exempt");
        Entity e = helper.spawn(baron, new Vec3(6.5, 2, 4.5));
        try {
            helper.assertTrue(lines(helper).rootOf(e.getUUID()).isEmpty() && !e.hasData(SpeciesRegistries.LINE), "a boss got a line");
        } finally {
            e.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void convertedCreatureTakesItsSiresLine(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        Mob mob = null;
        try {
            mob = spawnVampire(helper);
            helper.assertTrue(SireRecorder.record(o, mob), "the sire was not recorded");
            helper.assertTrue(lines(helper).rootOf(mob.getUUID()).filter(o.getUUID()::equals).isPresent(), "the creature did not take its sire's line");
            helper.assertTrue(o.getUUID().equals(mob.getData(SpeciesRegistries.LINE)), "the creature's mirror was not overwritten");
        } finally {
            forget(helper, o);
            if (mob != null) {
                lines(helper).remove(mob.getUUID());
                SirelineIndex.get(helper.getLevel()).remove(mob.getUUID());
                mob.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siredCreatureLeavesTheIndexWhenDestroyedButNotWhenUnloaded(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        Mob unloaded = spawnVampire(helper);
        Mob killed = spawnVampire(helper);
        Mob discarded = spawnVampire(helper);
        try {
            for (Mob m : List.of(unloaded, killed, discarded)) {
                helper.assertTrue(SireRecorder.record(o, m), "the sire was not recorded");
                helper.assertTrue(lines(helper).rootOf(m.getUUID()).isPresent(), "control: the sired creature is not in the index");
            }
            unloaded.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            NeoForge.EVENT_BUS.post(new EntityLeaveLevelEvent(unloaded, helper.getLevel()));
            helper.assertTrue(lines(helper).rootOf(unloaded.getUUID()).isPresent(), "an unloaded creature left the index");
            killed.remove(Entity.RemovalReason.KILLED);
            helper.assertTrue(lines(helper).rootOf(killed.getUUID()).isEmpty(), "a killed creature stayed in the index");
            discarded.discard();
            helper.assertTrue(lines(helper).rootOf(discarded.getUUID()).isEmpty(), "a discarded creature stayed in the index");
        } finally {
            forget(helper, o);
            for (Mob m : List.of(unloaded, killed, discarded)) {
                lines(helper).remove(m.getUUID());
                SirelineIndex.get(helper.getLevel()).remove(m.getUUID());
            }
        }
        helper.succeed();
    }

    /** A made Original takes their branch with them, creatures in it as well (an unlined creature is filled when it joins, not here). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void madeOriginalReRootsTheCreaturesOfTheBranch(GameTestHelper helper) {
        ServerPlayer o = mortal(helper, Species.ORIGINAL);
        ServerPlayer a = mortal(helper, Species.NONE);
        ServerPlayer under = mortal(helper, Species.NONE);
        Mob creature = spawnVampire(helper);
        Mob unlined = spawnVampire(helper);
        try {
            SireRecorder.record(o, a);
            SireRecorder.record(a, creature);
            SireRecorder.record(a, unlined);
            lines(helper).remove(unlined.getUUID()); // a creature with no line yet
            unlined.removeData(SpeciesRegistries.LINE);
            SireRecorder.record(creature, under);
            helper.assertTrue(lines(helper).rootOf(creature.getUUID()).filter(o.getUUID()::equals).isPresent(), "control: the creature is not in the line");
            species(a, Species.ORIGINAL);
            helper.assertTrue(lines(helper).rootOf(creature.getUUID()).filter(a.getUUID()::equals).isPresent(), "a creature of the branch stayed in the old line");
            helper.assertTrue(a.getUUID().equals(creature.getData(SpeciesRegistries.LINE)), "the loaded creature's mirror was not moved");
            helper.assertTrue(lines(helper).rootOf(under.getUUID()).filter(a.getUUID()::equals).isPresent(), "a player below the creature did not move with the made Original");
            helper.assertTrue(lines(helper).rootOf(unlined.getUUID()).isEmpty(), "a creature with no line was given one here");
            helper.assertTrue(lines(helper).rootOf(o.getUUID()).filter(o.getUUID()::equals).isPresent(), "the first Original lost their line");
        } finally {
            forget(helper, o, a, under);
            for (Mob m : List.of(creature, unlined)) {
                lines(helper).remove(m.getUUID());
                SirelineIndex.get(helper.getLevel()).remove(m.getUUID());
                m.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creatureOwedADeathIsNotGivenALine(GameTestHelper helper) {
        NpcLines.useRoll(() -> 0.0);
        SpeciesConfig.overrideNpcLineWeights(List.of("mikael=1"));
        UUID id = UUID.randomUUID();
        try {
            Mob mob = (Mob) BuiltInRegistries.ENTITY_TYPE.get(VAMPIRE).create(helper.getLevel());
            mob.setUUID(id);
            SirelineIndex.get(helper.getLevel()).addPendingMob(id, helper.getLevel().getServer().overworld().getGameTime());
            NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(mob, helper.getLevel()));
            helper.assertTrue(!mob.hasData(SpeciesRegistries.LINE) && lines(helper).rootOf(id).isEmpty(), "a creature owed a death was given a line");
        } finally {
            NpcLines.useRoll(null);
            SpeciesConfig.overrideNpcLineWeights(null);
            SirelineIndex.get(helper.getLevel()).takePendingMob(id);
        }
        helper.succeed();
    }
}
