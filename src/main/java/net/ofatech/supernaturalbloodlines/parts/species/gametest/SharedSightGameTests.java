package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterInstinct;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20d Task 7 (R293): what a brother who senses prey senses, the brothers who lack the sense see too. The pass is driven directly, never by looping ticks. */
@PrefixGameTestTemplate(false)
public final class SharedSightGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SharedSightGameTests() {
    }

    private static void on(ServerPlayer... players) {
        GroupTestKit.online(players);
        HunterMarkGameTests.online(players);
    }

    private static Mob vampireMob(GameTestHelper helper, Vec3 at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire"));
        Mob mob = (Mob) type.create(helper.getLevel());
        mob.setNoAi(true);
        mob.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static String entry(ServerPlayer p, Mob... mobs) {
        return p.getUUID() + ":" + java.util.Arrays.stream(mobs).map(Mob::getId).sorted().toList();
    }

    private static ServerPlayer member(GameTestHelper helper, ServerPlayer leader, Vec3 home) {
        ServerPlayer m = HunterMarkGameTests.human(helper);
        m.setPos(home);
        return m;
    }

    /** A sensor with 5 kills who leads a brotherhood, standing at {@code home}. */
    private static ServerPlayer sensor(GameTestHelper helper, Vec3 home) {
        ServerPlayer s = HunterMarkGameTests.marked(helper, 5);
        s.setPos(home);
        return s;
    }

    private static void finish(GameTestHelper helper, List<Mob> mobs, ServerPlayer... players) {
        HunterInstinct.overrideRange(null);
        GlowPayload.overrideSpy(null);
        HunterMarkGameTests.clearHooks();
        mobs.forEach(Mob::discard);
        HunterMarkGameTests.reset(players);
        clean(helper, players);
    }

    private static List<String> pass(GameTestHelper helper, long tick) {
        List<String> sent = new ArrayList<>();
        GlowPayload.overrideSpy((p, ids) -> sent.add(p.getUUID() + ":" + ids.stream().sorted().toList()));
        HunterInstinct.tick(helper.getLevel().getServer(), tick);
        return sent;
    }

    private static Vec3 home(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(1.5, 1, 1.5));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherWithoutTheSenseGetsTheSensorsIds(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(b, prey))), "sent " + sent);
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherWithTheSenseGetsNothingExtra(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer other = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, other);
        try {
            helper.assertTrue(join(s, other), "no brotherhood");
            BloodlineAccess.set(other, HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 5));
            HunterInstinct.overrideRange(8.0);
            List<String> sent = pass(helper, 40);
            helper.assertTrue(sent.size() == 2 && new HashSet<>(sent).equals(Set.of(entry(s, prey), entry(other, prey))), "two sense holders: each only its own list, sent " + sent);
        } finally {
            finish(helper, List.of(prey), s, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPotentialGets(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer potential = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, potential);
        try {
            helper.assertTrue(join(s, potential), "no brotherhood");
            BloodlineAccess.set(potential, HunterMarkGameTests.state(Species.POTENTIAL, 0));
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.contains(entry(potential, prey)), "a Potential got nothing: " + sent);
        } finally {
            finish(helper, List.of(prey), s, potential);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchMemberGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer witch = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, witch);
        try {
            helper.assertTrue(join(s, witch), "no brotherhood");
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey))), "a witch member was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireMemberGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer vampire = HunterMarkGameTests.vampire(helper);
        vampire.setPos(home.add(30, 0, 0));
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, vampire);
        try {
            helper.assertTrue(join(s, vampire), "no brotherhood");
            vampire.setPos(home);
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.stream().noneMatch(e -> e.startsWith(vampire.getUUID().toString())), "a vampire member was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCodeBreakerSensorGivesNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            groups.setBreaker(s.getUUID(), new BreakerRow(PackIndex.now() + 3_600_000L, List.of()));
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey))), "a breaker sensor gave: " + sent);
        } finally {
            groups.clearBreaker(s.getUUID());
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCodeBreakerRecipientGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            groups.setBreaker(b.getUUID(), new BreakerRow(PackIndex.now() + 3_600_000L, List.of()));
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey))), "a breaker received: " + sent);
        } finally {
            groups.clearBreaker(b.getUUID());
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void outsideTheSightRadiusGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer near = member(helper, s, home.add(8, 0, 0));
        ServerPlayer far = member(helper, s, home.add(8.01, 0, 0));
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, near, far);
        try {
            helper.assertTrue(join(s, near) && join(s, far), "no brotherhood");
            GroupsConfig.override("sight_radius", 8);
            HunterInstinct.overrideRange(8.0);
            near.setPos(home.add(8, 0, 0));
            far.setPos(home.add(8.01, 0, 0));
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(near, prey))), "the exact radius gets, just beyond does not: " + sent);
        } finally {
            finish(helper, List.of(prey), s, near, far);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRadiusIsMeasuredFromTheSensorNotFromThePrey(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home.add(-9, 0, 0));
        Mob prey = vampireMob(helper, home.add(7, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            GroupsConfig.override("sight_radius", 10);
            HunterInstinct.overrideRange(8.0);
            b.setPos(home.add(-9, 0, 0));
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(b, prey))), "9 blocks from the sensor, 16 from the prey: " + sent);
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherInAnotherDimensionGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        ServerLevel own = helper.getLevel();
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            b.setServerLevel(own.getServer().getLevel(Level.NETHER));
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey))), "a brother in another dimension was sent: " + sent);
        } finally {
            b.setServerLevel(own);
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineBrotherGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            on(s);
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey))), "an offline brother was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cloakedPreyIsNeverSent(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob open = vampireMob(helper, home.add(3, 0, 0));
        Mob cloaked = vampireMob(helper, home.add(4, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            CloakAccess.set(cloaked, helper.getLevel().getGameTime() + 10_000);
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, open), entry(b, open))), "a cloaked id was sent: " + sent);
        } finally {
            finish(helper, List.of(open, cloaked), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchesAndSiphonersAreNeverSent(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        ServerPlayer witch = HunterMarkGameTests.human(helper);
        ServerPlayer siphoner = HunterMarkGameTests.human(helper);
        witch.setPos(home.add(2, 0, 0));
        siphoner.setPos(home.add(2, 0, 1));
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            BloodlineAccess.set(siphoner, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).build());
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(witch).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(siphoner).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(b, prey))), "a witch or siphoner id was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, b, witch, siphoner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRecipientIsNotInItsOwnIds(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer odd = HunterMarkGameTests.vampire(helper);
        odd.setPos(home.add(30, 0, 0));
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, odd);
        try {
            helper.assertTrue(join(s, odd), "no brotherhood");
            odd.setPos(home.add(2, 0, 0));
            BloodlineAccess.set(odd, HunterMarkGameTests.state(Species.POTENTIAL, 0));
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.contains(entry(s, prey)) && sent.stream().filter(e -> e.startsWith(s.getUUID().toString())).count() == 1, "the sensor's own list is wrong: " + sent);
            String mine = sent.stream().filter(e -> e.startsWith(odd.getUUID().toString())).findFirst().orElse("none");
            helper.assertTrue(mine.equals(entry(odd, prey)) && !mine.contains(String.valueOf(odd.getId())), "the recipient was sent its own id: " + mine);
        } finally {
            finish(helper, List.of(prey), s, odd);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void noIdsSendsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            HunterInstinct.overrideRange(8.0);
            helper.assertTrue(pass(helper, 40).isEmpty(), "something was sent with no prey");
        } finally {
            finish(helper, List.of(), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sightOffSendsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            GroupsConfig.override("sight_share_enabled", false);
            HunterInstinct.overrideRange(8.0);
            helper.assertTrue(new HashSet<>(pass(helper, 40)).equals(Set.of(entry(s, prey))), "shared with the switch off");
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brotherhoodOffSendsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            GroupsConfig.override("brotherhood_enabled", false);
            HunterInstinct.overrideRange(8.0);
            helper.assertTrue(new HashSet<>(pass(helper, 40)).equals(Set.of(entry(s, prey))), "shared with brotherhoods off");
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackMemberGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        ServerPlayer hybrid = HunterMarkGameTests.human(helper);
        ServerPlayer wolfMate = HunterMarkGameTests.human(helper);
        hybrid.setPos(home);
        wolfMate.setPos(home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b, hybrid, wolfMate);
        GroupsConfig.override("open_founding", true);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(join(s, b) && join(hybrid, wolfMate), "no groups");
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(b, prey))), "a pack member was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, b, hybrid, wolfMate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlliedBrotherhoodGetsNothing(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        ServerPlayer otherLeader = HunterMarkGameTests.huntNone(helper);
        ServerPlayer otherMember = HunterMarkGameTests.human(helper);
        otherLeader.setPos(home);
        otherMember.setPos(home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b, otherLeader, otherMember);
        try {
            helper.assertTrue(join(s, b) && join(otherLeader, otherMember), "no groups");
            helper.assertTrue(PackIndex.get(helper.getLevel()).ally(s.getUUID(), otherLeader.getUUID()), "no alliance");
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.equals(Set.of(entry(s, prey), entry(b, prey))), "an allied brotherhood was sent: " + sent);
        } finally {
            finish(helper, List.of(prey), s, b, otherLeader, otherMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSensorInNoGroupSharesWithNobody(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer stranger = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, stranger);
        try {
            HunterInstinct.overrideRange(8.0);
            helper.assertTrue(new HashSet<>(pass(helper, 40)).equals(Set.of(entry(s, prey))), "a stranger was sent");
        } finally {
            finish(helper, List.of(prey), s, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePassKeepsTheSensorsCadence(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            HunterInstinct.overrideRange(8.0);
            helper.assertTrue(pass(helper, 41).isEmpty(), "sent off the 40 tick cadence");
            helper.assertTrue(pass(helper, 39).isEmpty(), "sent at tick 39");
            helper.assertTrue(pass(helper, 80).size() == 2, "not sent at tick 80");
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSendToAFakePlayerIsAttemptedAndNotDelivered(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            HunterInstinct.overrideRange(8.0);
            Set<String> sent = new HashSet<>(pass(helper, 40));
            helper.assertTrue(sent.contains(entry(b, prey)), "the attempt to a fake player was not seen");
            helper.assertTrue(!GlowPayload.send(b, List.of(prey.getId())), "a fake player was sent a packet");
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHookDoesNotChangeTheSensorsMarkOrClock(GameTestHelper helper) {
        Vec3 home = home(helper);
        ServerPlayer s = sensor(helper, home);
        ServerPlayer b = member(helper, s, home);
        Mob prey = vampireMob(helper, home.add(3, 0, 0));
        on(s, b);
        try {
            helper.assertTrue(join(s, b), "no brotherhood");
            BloodlineState before = BloodlineAccess.get(s);
            BloodlineState brotherBefore = BloodlineAccess.get(b);
            HunterInstinct.overrideRange(8.0);
            pass(helper, 40);
            BloodlineState after = BloodlineAccess.get(s);
            helper.assertTrue(after.markKills() == before.markKills() && after.lastVampireKill() == before.lastVampireKill(), "the sensor's mark or clock changed");
            helper.assertTrue(BloodlineAccess.get(b).equals(brotherBefore), "the brother's state changed");
        } finally {
            finish(helper, List.of(prey), s, b);
        }
        helper.succeed();
    }
}
