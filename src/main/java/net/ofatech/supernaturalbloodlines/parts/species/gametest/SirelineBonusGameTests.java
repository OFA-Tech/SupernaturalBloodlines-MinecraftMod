package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.SirelineBonuses;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/** Plan 20c Task 8 (R275): the sire-line bonus is a transient attack modifier for each online clanmate of one line within 32 blocks. */
@PrefixGameTestTemplate(false)
public final class SirelineBonusGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String PLAYER = "minecraft:player";

    private SirelineBonusGameTests() {
    }

    /** A clan of {@code n} vampires, the first leading, every other one sired by the leader. */
    private static List<TalkingDuelist> clan(GameTestHelper helper, int n, boolean sired) {
        List<TalkingDuelist> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(vampire(helper, 1.5 + i * 0.5));
        }
        online(all.toArray(new ServerPlayer[0]));
        SirelineBonuses.overridePlayers(() -> new ArrayList<>(all));
        for (int i = 1; i < n; i++) {
            helper.assertTrue(join(all.get(0), all.get(i)), "setup: " + i + " could not join");
            if (sired) {
                index(helper).add(all.get(0).getUUID(), all.get(i).getUUID(), PLAYER, "m" + i);
            }
        }
        helper.assertTrue(Groups.typeOf(all.get(1)) == GroupType.CLAN, "no clan formed");
        return all;
    }

    private static SirelineIndex index(GameTestHelper helper) {
        return SirelineIndex.get(helper.getLevel());
    }

    private static void refresh(GameTestHelper helper) {
        SirelineBonuses.refresh(helper.getLevel().getServer());
    }

    private static double bonus(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeModifier m = a.getModifier(SirelineBonuses.MODIFIER_ID);
        return m == null ? 0 : m.amount();
    }

    private static void done(GameTestHelper helper, List<TalkingDuelist> all) {
        SirelineBonuses.overridePlayers(null);
        for (TalkingDuelist p : all) {
            p.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(SirelineBonuses.MODIFIER_ID);
        }
        clean(helper, all.toArray(new ServerPlayer[0]));
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void oneMateOfMyLineGivesThreePercent(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, false);
        try {
            index(helper).add(c.get(0).getUUID(), c.get(1).getUUID(), PLAYER, "a");
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.03) && near(bonus(c.get(1)), 0.03), "the line got " + bonus(c.get(0)) + " and " + bonus(c.get(1)));
            helper.assertTrue(bonus(c.get(2)) == 0, "a member of no line got " + bonus(c.get(2)));
            AttributeModifier m = c.get(0).getAttribute(Attributes.ATTACK_DAMAGE).getModifier(SirelineBonuses.MODIFIER_ID);
            helper.assertTrue(m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "wrong operation");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void fiveMatesCapAtFifteenAndSixMatesStillFifteen(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 7, true);
        try {
            c.get(6).setPos(c.get(0).getX() + 40, c.get(0).getY(), c.get(0).getZ());
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.15), "five mates gave " + bonus(c.get(0)));
            c.get(6).setPos(c.get(0).getX() + 1, c.get(0).getY(), c.get(0).getZ());
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.15), "six mates gave " + bonus(c.get(0)) + ", not the cap");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateOfAnotherLineGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, false);
        try {
            index(helper).add(c.get(0).getUUID(), c.get(1).getUUID(), PLAYER, "a");
            index(helper).add(UUID.randomUUID(), c.get(2).getUUID(), PLAYER, "b");
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.03), "the head got " + bonus(c.get(0)) + ": the third is of another line");
            helper.assertTrue(bonus(c.get(2)) == 0, "a mate of another line got " + bonus(c.get(2)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateBeyondThirtyTwoBlocksGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, true);
        try {
            c.get(1).setPos(c.get(0).getX() + 33, c.get(0).getY(), c.get(0).getZ());
            c.get(2).setPos(c.get(0).getX() + 31, c.get(0).getY(), c.get(0).getZ());
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.03), "33 and 31 blocks gave the head " + bonus(c.get(0)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateInAnotherDimensionGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            c.get(1).setServerLevel(nether);
            c.get(1).setPos(c.get(0).getX(), 70, c.get(0).getZ());
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0, "a mate in the Nether gave " + bonus(c.get(0)));
        } finally {
            c.get(1).setServerLevel(helper.getLevel());
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anOfflineOrDeadMateGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, true);
        try {
            online(c.get(0), c.get(1));
            c.get(1).setHealth(0f);
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0, "an offline or dead mate gave " + bonus(c.get(0)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateOutsideTheClanGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, false);
        TalkingDuelist outsider = vampire(helper, 6.5);
        try {
            List<TalkingDuelist> everyone = new ArrayList<>(c);
            everyone.add(outsider);
            online(everyone.toArray(new ServerPlayer[0]));
            SirelineBonuses.overridePlayers(() -> new ArrayList<>(everyone));
            index(helper).add(c.get(0).getUUID(), outsider.getUUID(), PLAYER, "o");
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0 && bonus(outsider) == 0, "a mate of another group counted: " + bonus(c.get(0)) + " " + bonus(outsider));
        } finally {
            done(helper, c);
            clean(helper, outsider);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void grandchildrenAndSiblingsShareTheRoot(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 4, false);
        try {
            index(helper).add(c.get(0).getUUID(), c.get(1).getUUID(), PLAYER, "a");
            index(helper).add(c.get(1).getUUID(), c.get(2).getUUID(), PLAYER, "b");
            index(helper).add(c.get(0).getUUID(), c.get(3).getUUID(), PLAYER, "c");
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(2)), 0.09) && near(bonus(c.get(3)), 0.09) && near(bonus(c.get(1)), 0.09), "grandchild " + bonus(c.get(2)) + " sibling " + bonus(c.get(3)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberWhoLeavesTakesTheBonusAwayAtTheNextRefresh(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, true);
        try {
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.06), "setup: " + bonus(c.get(0)));
            helper.assertTrue(PackCommand.leave(c.get(2)) == null, "setup: leaving refused");
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.03) && bonus(c.get(2)) == 0, "the modifier stayed: " + bonus(c.get(0)) + " " + bonus(c.get(2)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void removedWhenAlone(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) > 0, "setup");
            helper.assertTrue(PackCommand.leave(c.get(1)) == null, "setup: leaving refused");
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0, "a lone head kept it: " + bonus(c.get(0)));
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void notSavedAcrossARelog(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            refresh(helper);
            AttributeInstance a = c.get(0).getAttribute(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(a.getModifier(SirelineBonuses.MODIFIER_ID) != null && !a.save().toString().contains("sireline_bonus"), "the modifier is saved with the player: " + a.save());
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void itMultipliesWithThePackBonus(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            AttributeInstance a = c.get(0).getAttribute(Attributes.ATTACK_DAMAGE);
            a.addTransientModifier(new AttributeModifier(PackBonus.MODIFIER_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            refresh(helper);
            helper.assertTrue(a.getModifier(PackBonus.MODIFIER_ID) != null && a.getModifier(SirelineBonuses.MODIFIER_ID) != null, "the two ids do not coexist");
            helper.assertTrue(!SirelineBonuses.MODIFIER_ID.equals(PackBonus.MODIFIER_ID), "the same id");
        } finally {
            c.get(0).getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(PackBonus.MODIFIER_ID);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void aTypeSwitchRemovesIt(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            refresh(helper);
            helper.assertTrue(bonus(c.get(1)) > 0, "setup");
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.get(0)).leaveFaction(false);
            GroupSwitcher.refresh(c.get(0));
            refresh(helper);
            helper.assertTrue(bonus(c.get(1)) == 0 && bonus(c.get(0)) == 0, "a group that is no longer a clan kept it");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void clansOffAndPercentZeroRemoveIt(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, true);
        try {
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) > 0, "setup");
            GroupsConfig.override("clans_enabled", false);
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0, "clans off kept it");
            GroupsConfig.override("clans_enabled", null);
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) > 0, "setup 2");
            GroupsConfig.override("sireline_bonus_percent", 0);
            refresh(helper);
            helper.assertTrue(bonus(c.get(0)) == 0, "percent 0 kept it");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDeadSireOrAnNpcRootStillDefinesTheLine(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 3, false);
        try {
            UUID creature = UUID.randomUUID();
            index(helper).add(creature, c.get(0).getUUID(), "minecraft:villager", "Old Sire");
            index(helper).add(creature, c.get(1).getUUID(), PLAYER, "a");
            refresh(helper);
            helper.assertTrue(near(bonus(c.get(0)), 0.03) && near(bonus(c.get(1)), 0.03) && bonus(c.get(2)) == 0, "a creature root did not define the line");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCycleInTheIndexDoesNotHangTheRefresh(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 2, false);
        try {
            index(helper).add(c.get(0).getUUID(), c.get(1).getUUID(), PLAYER, "a");
            index(helper).add(c.get(1).getUUID(), c.get(0).getUUID(), PLAYER, "b");
            refresh(helper);
            helper.assertTrue(SirelineBonuses.lastWalks() <= 2, "a cycle cost " + SirelineBonuses.lastWalks() + " walks");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void theRefreshCostIsBounded(GameTestHelper helper) {
        List<TalkingDuelist> c = clan(helper, 12, true);
        try {
            refresh(helper);
            helper.assertTrue(SirelineBonuses.lastWalks() <= 12, "a clan of 12 made " + SirelineBonuses.lastWalks() + " root walks");
        } finally {
            done(helper, c);
        }
        helper.succeed();
    }
}
