package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.circle;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.tidy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.CovenCostGameTests.Circle;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.LinkWindow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;

/** Plan 20b Task 7 (R256): the anchor's death while the link is live. */
@PrefixGameTestTemplate(false)
public final class CovenDeathGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<String> KEYS = List.of("link_window_seconds", "link_death_damage_percent", "link_death_lethal", "link_shaken_minutes");

    private CovenDeathGameTests() {
    }

    private static void startRite(GameTestHelper helper, Circle c) {
        CovenCostGameTests.begin(helper, c, c.anchor().getUUID(), 4500, true);
        RiteCost.commit();
    }

    private static void done(GameTestHelper helper, Circle c) {
        LinkWindow.forget(c.anchor().getUUID());
        KEYS.forEach(k -> GroupsConfig.override(k, null));
        tidy(helper, c.array());
    }

    private static void fall(TalkingDuelist p) {
        p.die(p.damageSources().genericKill());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAnchorsDeathHurtsEveryLinkedMember(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()) != null, "no link is live after a linked rite");
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.getHealth() == m.getMaxHealth() - 12, "60% of 20 hit points should be gone: " + m.getHealth());
                helper.assertTrue(m.hasEffect(MobEffects.WEAKNESS) && m.getEffect(MobEffects.WEAKNESS).getAmplifier() == 1 && m.hasEffect(MobEffects.DIG_SLOWDOWN), "no weakness II and mining fatigue");
                var row = GroupIndex.get(helper.getLevel()).channelOf(m.getUUID());
                helper.assertTrue(row != null && row.value() == 0, "the meter is not 0: " + row);
                helper.assertTrue(CircleLink.shaken(m.getUUID()), "the member is not shaken");
            }
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()) == null, "the link outlived the anchor's death");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nobodyDiesWithTheLethalOptionOff(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            c.others().forEach(m -> m.setHealth(5));
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.isAlive() && m.getHealth() == 1.0f, "a member at 5 HP should be left at 1, not " + m.getHealth());
            }
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLethalOptionKillsTheMembers(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            GroupsConfig.override("link_death_lethal", true);
            startRite(helper, c);
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(!m.isAlive(), "a linked member lived with the lethal option on: " + m.getHealth());
            }
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDeathAfterTheWindowIsNotAPenalty(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        long[] clock = {9_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            startRite(helper, c);
            clock[0] += (GroupsConfig.linkWindowSeconds() + 1) * 1000L;
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.getHealth() == m.getMaxHealth() && !m.hasEffect(MobEffects.WEAKNESS), "a death after the window hurt a member");
            }
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAnchorLoggingOutIsNotAPenaltyAndTheRiteGoesOn(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(c.anchor()));
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()) == null, "the live link was kept");
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.getHealth() == m.getMaxHealth(), "a member was hurt after the anchor logged out");
            }
            helper.assertTrue(LinkWindow.queuedStops() == 0, "a stop was queued");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMembersDeathOnlyRemovesThatMember(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            TalkingDuelist first = c.others().get(0);
            TalkingDuelist second = c.others().get(1);
            fall(first);
            helper.assertTrue(second.getHealth() == second.getMaxHealth() && c.anchor().getHealth() == c.anchor().getMaxHealth(), "a member's death hurt the others");
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()) != null && !LinkWindow.liveOf(c.anchor().getUUID()).members().contains(first.getUUID()), "the dead member is still linked");
            first.setHealth(0);
            fall(c.anchor());
            helper.assertTrue(second.getHealth() == second.getMaxHealth() - 12, "the other member was not hurt: " + second.getHealth());
            helper.assertTrue(first.getHealth() == 0, "the dead member was touched twice");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    private static final java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> MULTIPLY = e -> e.setAmount(e.getAmount() * 100);

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeftOrWasKickedIsNotHurtByTheAnchorsDeath(GameTestHelper helper) {
        Circle c = circle(helper, 4);
        try {
            startRite(helper, c);
            TalkingDuelist left = c.others().get(0);
            TalkingDuelist kicked = c.others().get(1);
            TalkingDuelist stays = c.others().get(2);
            helper.assertTrue(PackCommand.leave(left) == null, "the member could not leave");
            helper.assertTrue(PackCommand.kick(c.anchor(), kicked.getUUID(), "kicked") == null, "the member could not be kicked");
            helper.assertTrue(!LinkWindow.liveOf(c.anchor().getUUID()).members().contains(left.getUUID()) && !LinkWindow.liveOf(c.anchor().getUUID()).members().contains(kicked.getUUID()), "a leaver is still in the live link");
            fall(c.anchor());
            for (TalkingDuelist gone : List.of(left, kicked)) {
                helper.assertTrue(gone.getHealth() == gone.getMaxHealth() && !gone.hasEffect(MobEffects.WEAKNESS), "someone who was no longer in the coven was hurt");
                helper.assertTrue(!CircleLink.shaken(gone.getUUID()), "someone who was no longer in the coven was shaken");
                var row = GroupIndex.get(helper.getLevel()).channelOf(gone.getUUID());
                helper.assertTrue(row == null || row.value() != 0, "someone who was no longer in the coven had their meter emptied");
            }
            helper.assertTrue(stays.getHealth() == stays.getMaxHealth() - 12, "the member who stayed was not hurt: " + stays.getHealth());
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeftAndCameBackIsNotLinkedAnyMore(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            TalkingDuelist back = c.others().get(0);
            helper.assertTrue(PackCommand.leave(back) == null, "the member could not leave");
            helper.assertTrue(GroupTestKit.join(c.anchor(), back), "the member could not come back");
            fall(c.anchor());
            helper.assertTrue(back.getHealth() == back.getMaxHealth(), "a member who left after the rite and came back was hurt: " + back.getHealth());
            helper.assertTrue(c.others().get(1).getHealth() == c.others().get(1).getMaxHealth() - 12, "the member who stayed was not hurt");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberTakenOutOfTheCovenWithoutTheCircleHookIsStillNotHurt(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            TalkingDuelist out = c.others().get(0);
            PackIndex.get(helper.getLevel()).leaveVoluntarily(out.getUUID());
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()).members().contains(out.getUUID()), "the test needs the link to still list them");
            fall(c.anchor());
            helper.assertTrue(out.getHealth() == out.getMaxHealth() && !out.hasEffect(MobEffects.WEAKNESS), "a member no longer in the coven was hurt: " + out.getHealth());
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherModsDamageMultiplierCannotKillAMemberOfTheLink(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        NeoForge.EVENT_BUS.addListener(MULTIPLY);
        try {
            startRite(helper, c);
            c.others().forEach(m -> m.setHealth(5));
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.isAlive() && m.getHealth() == 1.0f, "a damage multiplier killed a linked member (or the hit was lost): " + m.getHealth());
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(MULTIPLY);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void strainPainCannotBeMultipliedIntoADeath(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        NeoForge.EVENT_BUS.addListener(MULTIPLY);
        try {
            TalkingDuelist m = c.others().get(0);
            m.setHealth(3);
            var share = new net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan.Share(4000, 0, net.ofatech.supernaturalbloodlines.core.group.circle.ChannelMeter.Zone.DAMAGE, 3000);
            net.ofatech.supernaturalbloodlines.parts.species.group.circle.Strain.apply(m, share, 3000);
            helper.assertTrue(m.isAlive() && m.getHealth() == 1.0f, "strain killed a member through a damage multiplier: " + m.getHealth());
        } finally {
            NeoForge.EVENT_BUS.unregister(MULTIPLY);
            done(helper, c);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineOrDeadMemberIsSkipped(GameTestHelper helper) {
        Circle c = circle(helper, 4);
        try {
            startRite(helper, c);
            TalkingDuelist away = c.others().get(0);
            TalkingDuelist dead = c.others().get(1);
            TalkingDuelist here = c.others().get(2);
            helper.getLevel().removePlayerImmediately(away, Entity.RemovalReason.DISCARDED);
            dead.setHealth(0);
            fall(c.anchor());
            helper.assertTrue(here.getHealth() == here.getMaxHealth() - 12, "the member who was there was not hurt: " + here.getHealth());
            helper.assertTrue(dead.getHealth() == 0 && away.getHealth() == away.getMaxHealth(), "a member who was away or dead was touched");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberInAnotherDimensionIsStillHurt(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the Nether is not loaded in the test server");
        TalkingDuelist far = c.others().get(0);
        try {
            startRite(helper, c);
            helper.getLevel().removePlayerImmediately(far, Entity.RemovalReason.DISCARDED);
            try {
                var removal = Entity.class.getDeclaredField("removalReason");
                removal.setAccessible(true);
                removal.set(far, null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            far.putIn(nether);
            nether.addNewPlayer(far);
            fall(c.anchor());
            helper.assertTrue(far.getHealth() == far.getMaxHealth() - 12, "a linked member in another dimension was not hurt: " + far.getHealth());
        } finally {
            nether.removePlayerImmediately(far, Entity.RemovalReason.DISCARDED);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCancelledDeathIsNotAPenalty(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        boolean[] live = {true};
        java.util.function.Consumer<LivingDeathEvent> totem = e -> {
            if (live[0] && e.getEntity() == c.anchor()) {
                e.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, totem);
        try {
            startRite(helper, c);
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.getHealth() == m.getMaxHealth(), "a totem-saved anchor hurt a member");
            }
            helper.assertTrue(LinkWindow.liveOf(c.anchor().getUUID()) != null, "the link ended with a death that did not happen");
        } finally {
            live[0] = false;
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShakenMemberCannotJoinACircle(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        long[] clock = {20_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            startRite(helper, c);
            fall(c.anchor());
            CircleLink.forget(c.anchor().getUUID(), c.others().get(0).getUUID(), c.others().get(1).getUUID());
            TalkingDuelist member = c.others().get(0);
            helper.assertTrue(CircleLink.shaken(member.getUUID()), "the member is not shaken");
            var refused = CircleLink.open(member);
            helper.assertTrue(refused != null && GroupTestKit.key(refused).equals("supernaturalbloodlines.coven.err.shaken"), "a shaken member was answered " + GroupTestKit.key(refused));
            clock[0] += (GroupsConfig.linkShakenMinutes() + 1) * 60_000L;
            helper.assertTrue(!CircleLink.shaken(member.getUUID()), "the member is still shaken after the time");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondDeathInTheWindowIsNotAPenaltyTwice(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c);
            fall(c.anchor());
            c.others().forEach(m -> m.setHealth(m.getMaxHealth()));
            fall(c.anchor());
            for (TalkingDuelist m : c.others()) {
                helper.assertTrue(m.getHealth() == m.getMaxHealth(), "a second death hurt a member again: " + m.getHealth());
            }
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSoloCastOpensNoWindow(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            UUID other = c.others().get(0).getUUID();
            CovenCostGameTests.begin(helper, c, other, 4500, true);
            RiteCost.commit();
            helper.assertTrue(LinkWindow.liveOf(other) == null && LinkWindow.liveOf(c.anchor().getUUID()) == null, "a solo cast opened a window");
            fall(c.anchor());
            helper.assertTrue(c.others().get(1).getHealth() == c.others().get(1).getMaxHealth(), "a member was hurt by a solo cast's death");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "covens_death_chalk", timeoutTicks = 100)
    public static void theAnchorsDeathStopsAPersistentRite(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 4));
        CovenHookGameTests.Probe probe = new CovenHookGameTests.Probe(new ArrayList<>(), new AtomicReference<>(), true);
        Circle[] circle = {null};
        boolean[] stage = {false};
        helper.succeedWhen(() -> {
            if (!stage[0]) {
                helper.assertTrue(helper.getLevel().getGameTime() % 20 == 0, "waiting for a tick divisible by 20");
                stage[0] = true;
                Circle c = circle(helper, 3);
                circle[0] = c;
                AltarBlockEntity altar = WitchGameTests.altar(helper, base, 20000);
                GoldChalkBlockEntity chalk = (GoldChalkBlockEntity) helper.getLevel().getBlockEntity(c.chalk());
                chalk.getPosHolder().add(base);
                WitchGameTests.set(chalk, "caster", c.anchor().getUUID());
                WitchGameTests.set(altar, "power", 20000.0);
                CovenHookGameTests.startThroughChalk(helper, c.chalk(), chalk, probe.type(4500, 0));
                helper.assertTrue(probe.log().contains("onStart") && RiteManager.getRiteAt(helper.getLevel(), c.chalk()) != null, "the persistent rite did not start");
                fall(c.anchor());
                helper.assertTrue(c.others().stream().allMatch(m -> m.getHealth() == m.getMaxHealth() - 12), "the members were not hurt");
                helper.assertTrue(LinkWindow.queuedStops() >= 1 && RiteManager.getRiteAt(helper.getLevel(), c.chalk()) != null, "the stop was done inside the death event instead of being queued");
                helper.assertTrue(false, "queued the stop; waiting for the end of the tick");
            }
            try {
                helper.assertTrue(RiteManager.getRiteAt(helper.getLevel(), circle[0].chalk()) == null, "the rite still runs after its anchor fell");
            } finally {
                done(helper, circle[0]);
            }
        });
    }
}