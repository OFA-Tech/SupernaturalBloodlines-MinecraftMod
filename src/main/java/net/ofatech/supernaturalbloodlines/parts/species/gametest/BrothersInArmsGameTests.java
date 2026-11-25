package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.BrotherhoodArms;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.SirelineBonuses;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20d Task 6 (R292): +3% attack damage for each other brother near, up to 12%. The refresh is called directly: no tick loops. */
@PrefixGameTestTemplate(false)
public final class BrothersInArmsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EPS = 1e-9;

    private BrothersInArmsGameTests() {
    }

    private static AttributeInstance attack(ServerPlayer p) {
        return p.getAttribute(Attributes.ATTACK_DAMAGE);
    }

    private static AttributeModifier arms(ServerPlayer p) {
        return attack(p).getModifier(BrotherhoodArms.MODIFIER_ID);
    }

    private static double percent(ServerPlayer p) {
        AttributeModifier m = arms(p);
        return m == null ? 0 : m.amount() * 100;
    }

    private static void refresh(GameTestHelper helper, ServerPlayer... all) {
        online(all);
        List<ServerPlayer> list = List.of(all);
        BrotherhoodArms.overridePlayers(() -> list);
        BrotherhoodArms.refresh(helper.getLevel().getServer());
    }

    /** A hunter-led brotherhood of {@code members} plain humans, all at the leader's spot; returns [leader, members...]. */
    private static List<TalkingDuelist> brotherhood(GameTestHelper helper, int members) {
        List<TalkingDuelist> all = new ArrayList<>();
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        all.add(leader);
        for (int i = 0; i < members; i++) {
            all.add(human(helper, 1.5));
        }
        online(all.toArray(new ServerPlayer[0]));
        for (int i = 1; i < all.size(); i++) {
            if (!join(leader, all.get(i))) {
                throw new AssertionError("a member could not join");
            }
        }
        return all;
    }

    private static ServerPlayer[] arr(List<TalkingDuelist> l) {
        return l.toArray(new ServerPlayer[0]);
    }

    private static void finish(GameTestHelper helper, List<TalkingDuelist> all) {
        BrotherhoodArms.overridePlayers(null);
        clean(helper, arr(all));
    }

    private static void near(ServerPlayer mate, ServerPlayer of, double dx) {
        mate.setPos(of.getX() + dx, of.getY(), of.getZ());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void oneBrotherNearGivesThreePercent(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            double base = attack(all.get(0)).getValue();
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 3) < EPS && Math.abs(percent(all.get(1)) - 3) < EPS, "percent " + percent(all.get(0)) + "/" + percent(all.get(1)));
            helper.assertTrue(Math.abs(attack(all.get(0)).getValue() - base * 1.03) < EPS, "value " + attack(all.get(0)).getValue() + " from " + base);
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void fourBrothersGiveTwelve(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 5);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 12) < EPS, "the leader has " + percent(all.get(0)) + " with 5 brothers");
            helper.assertTrue(Math.abs(percent(all.get(1)) - 12) < EPS, "a member has " + percent(all.get(1)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void exactlyFourGiveTwelveAndThreeNine(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 4);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 12) < EPS, "four brothers give " + percent(all.get(0)));
            near(all.get(4), all.get(0), 100);
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 9) < EPS, "three brothers give " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void fiveAndSixStillTwelve(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 7);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 12) < EPS, "seven brothers give " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherBeyondTwentyFourBlocksGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            near(all.get(1), all.get(0), 24.001);
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) == 0 && percent(all.get(1)) == 0, "a brother just beyond 24 blocks counts: " + percent(all.get(0)));
            near(all.get(1), all.get(0), 24);
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 3) < EPS, "a brother at exactly 24 blocks does not count: " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherInAnotherDimensionGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        ServerLevel own = helper.getLevel();
        try {
            ServerLevel nether = own.getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "no nether in the test server");
            all.get(1).setServerLevel(nether);
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) == 0 && percent(all.get(1)) == 0, "a brother in another dimension counts: " + percent(all.get(0)));
        } finally {
            all.get(1).setServerLevel(own);
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineDeadOrSpectatingBrotherGivesNothing(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 3);
        try {
            online(all.get(0), all.get(2), all.get(3)); // brother 1 is away
            all.get(2).setHealth(0.0F);
            helper.assertTrue(!all.get(2).isAlive(), "the dead brother is alive");
            all.get(3).setGameMode(GameType.SPECTATOR);
            BrotherhoodArms.overridePlayers(() -> List.of(all.get(0)));
            BrotherhoodArms.refresh(helper.getLevel().getServer());
            helper.assertTrue(percent(all.get(0)) == 0, "an offline, dead and spectating brother gives " + percent(all.get(0)));
        } finally {
            all.get(3).setGameMode(GameType.SURVIVAL);
            all.get(2).setHealth(20.0F);
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpectatorBrotherNeverReceives(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            all.get(1).setGameMode(GameType.SPECTATOR);
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(1)) == 0, "a spectator received " + percent(all.get(1)));
            helper.assertTrue(percent(all.get(0)) == 0, "a spectator gave " + percent(all.get(0)));
        } finally {
            all.get(1).setGameMode(GameType.SURVIVAL);
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainHumanGivesAndGets(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 2);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(1)) > 0 && percent(all.get(2)) > 0, "a plain human member does not get it");
            helper.assertTrue(Math.abs(percent(all.get(0)) - 6) < EPS, "the humans did not give: " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHunterGivesAndGets(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        TalkingDuelist second = GroupTestKit.hunter(helper, 1.5);
        all.add(second);
        online(arr(all));
        try {
            helper.assertTrue(join(all.get(0), second), "no join");
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(second) - 6) < EPS && Math.abs(percent(all.get(0)) - 6) < EPS, "hunters: " + percent(second) + "/" + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchMemberGivesAndGetsNothing(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        BloodlineAccess.set(all.get(1), BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        try {
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(1)) == 0, "a witch member got " + percent(all.get(1)));
            helper.assertTrue(percent(all.get(0)) == 0, "a witch member gave " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireMemberGivesAndGetsNothing(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 0);
        TalkingDuelist vampire = GroupTestKit.vampire(helper, 1.5);
        all.add(vampire);
        online(arr(all));
        try {
            helper.assertTrue(join(all.get(0), vampire), "a vampire could not join");
            refresh(helper, arr(all));
            helper.assertTrue(percent(vampire) == 0, "a vampire member got " + percent(vampire));
            helper.assertTrue(percent(all.get(0)) == 0, "a vampire member gave " + percent(all.get(0)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCodeBreakerNeitherGivesNorGets(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 2);
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            groups.setBreaker(all.get(1).getUUID(), new BreakerRow(PackIndex.now() + 3_600_000L, List.of()));
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(1)) == 0, "a breaker got " + percent(all.get(1)));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 3) < EPS && Math.abs(percent(all.get(2)) - 3) < EPS, "a breaker gave: " + percent(all.get(0)) + "/" + percent(all.get(2)));
        } finally {
            groups.clearBreaker(all.get(1).getUUID());
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBrotherhoodOffRemovesIt(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) > 0, "no bonus to begin with");
            GroupsConfig.override("brotherhood_enabled", false);
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) == 0 && percent(all.get(1)) == 0, "the bonus stayed with brotherhoods off");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTypeSwitchRemovesIt(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) > 0, "no bonus to begin with");
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(all.get(0)).leaveFaction(false);
            BloodlineAccess.set(all.get(0), BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            GroupSwitcher.refresh(all.get(0));
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.Groups.typeOf(all.get(1)) == GroupType.COVEN, "the group did not switch");
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) == 0 && percent(all.get(1)) == 0, "the bonus stayed after the type switch");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeavesTakesItAwayAtTheNextRefresh(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(percent(all.get(0)) > 0 && percent(all.get(1)) > 0, "no bonus to begin with");
            helper.assertTrue(PackCommand.leave(all.get(1)) == null, "could not leave");
            refresh(helper, arr(all));
            helper.assertTrue(arms(all.get(0)) == null && arms(all.get(1)) == null, "the bonus stayed after leaving: " + percent(all.get(0)) + "/" + percent(all.get(1)));
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void removedWhenAlone(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            near(all.get(1), all.get(0), 60);
            refresh(helper, arr(all));
            helper.assertTrue(arms(all.get(0)) == null && arms(all.get(1)) == null, "the modifier stayed when alone");
            BrotherhoodArms.remove(all.get(0));
            helper.assertTrue(arms(all.get(0)) == null, "remove left it");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void notSavedAcrossARelog(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            helper.assertTrue(arms(all.get(0)) != null, "no modifier");
            String saved = String.valueOf(attack(all.get(0)).save());
            helper.assertTrue(!saved.contains("brothers_in_arms"), "the bonus is saved: " + saved);
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void itMultipliesWithTheMarkThePackBonusAndTheSirelineBonus(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        ResourceLocation pack = ResourceLocation.fromNamespaceAndPath(NS, "pack_bonus");
        try {
            AttributeInstance a = attack(all.get(0));
            double none = a.getValue();
            BloodlineAccess.set(all.get(0), BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(4).build());
            a.addPermanentModifier(new AttributeModifier(pack, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            a.addOrUpdateTransientModifier(new AttributeModifier(SirelineBonuses.MODIFIER_ID, 0.09, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            refresh(helper, arr(all));
            helper.assertTrue(a.getModifier(MarkTracker.MODIFIER_ID) != null && a.getModifier(pack) != null && a.getModifier(SirelineBonuses.MODIFIER_ID) != null && arms(all.get(0)) != null, "an id is missing: four must coexist");
            double want = none * 1.04 * 1.10 * 1.09 * 1.03;
            helper.assertTrue(Math.abs(a.getValue() - want) < EPS, "value " + a.getValue() + " is not " + want);
            a.removeModifier(pack);
            a.removeModifier(SirelineBonuses.MODIFIER_ID);
        } finally {
            attack(all.get(0)).removeModifier(pack);
            attack(all.get(0)).removeModifier(SirelineBonuses.MODIFIER_ID);
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markSyncLeavesItAlone(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            MarkTracker.sync(all.get(0));
            MarkTracker.sync(all.get(0));
            BloodlineAccess.set(all.get(0), BloodlineState.NONE.toBuilder().species(Species.HUNTER_OF_THE_FIVE).markKills(7).build());
            helper.assertTrue(arms(all.get(0)) != null && Math.abs(percent(all.get(0)) - 3) < EPS, "MarkTracker.sync touched the bonus");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRealHitIsScaled(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 4);
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 1, 2));
        try {
            ServerPlayer p = all.get(0);
            p.setPos(cow.position().add(0.5, 0, 0));
            cow.setHealth(cow.getMaxHealth());
            cow.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            float before = cow.getHealth();
            p.attack(cow);
            float plain = before - cow.getHealth();
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(p) - 12) < EPS, "the bonus is " + percent(p));
            cow.setHealth(cow.getMaxHealth());
            cow.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            before = cow.getHealth();
            p.attack(cow);
            float scaled = before - cow.getHealth();
            helper.assertTrue(plain > 0 && scaled > plain && Math.abs(scaled / plain - 1.12) < 0.01, "plain " + plain + " scaled " + scaled);
        } finally {
            cow.discard();
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRespawnIsRefreshedAtTheNextPass(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 1);
        try {
            refresh(helper, arr(all));
            attack(all.get(0)).removeModifier(BrotherhoodArms.MODIFIER_ID); // a new body has no transient modifiers
            helper.assertTrue(arms(all.get(0)) == null, "setup");
            refresh(helper, arr(all));
            helper.assertTrue(Math.abs(percent(all.get(0)) - 3) < EPS, "the next pass did not give it back");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackOrClanMemberGetsNothingFromThisRule(GameTestHelper helper) {
        TalkingDuelist vamp = GroupTestKit.vampire(helper, 1.5);
        TalkingDuelist a = human(helper, 1.5);
        TalkingDuelist a2 = human(helper, 1.5);
        TalkingDuelist hybrid = human(helper, 1.5);
        TalkingDuelist b = human(helper, 1.5);
        TalkingDuelist b2 = human(helper, 1.5);
        online(vamp, a, a2, hybrid, b, b2);
        GroupsConfig.override("open_founding", true);
        try {
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            helper.assertTrue(join(vamp, a) && join(vamp, a2) && join(hybrid, b) && join(hybrid, b2), "no clan or pack");
            refresh(helper, vamp, a, a2, hybrid, b, b2);
            helper.assertTrue(arms(vamp) == null && arms(a) == null && arms(a2) == null && arms(hybrid) == null && arms(b) == null && arms(b2) == null, "a clan or a pack got the brothers' bonus");
        } finally {
            BrotherhoodArms.overridePlayers(null);
            clean(helper, vamp, a, a2, hybrid, b, b2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void percentZeroIsOff(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 2);
        try {
            GroupsConfig.override("arms_percent", 0);
            refresh(helper, arr(all));
            helper.assertTrue(arms(all.get(0)) == null && arms(all.get(1)) == null, "the bonus is on with arms_percent 0");
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCountAndThePercentTheViewReadsMatchTheModifier(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 3);
        try {
            refresh(helper, arr(all));
            for (ServerPlayer p : all) {
                helper.assertTrue(Math.abs(BrotherhoodArms.bonusFor(p) - percent(p) / 100) < EPS, "the call reads " + BrotherhoodArms.bonusFor(p) + " and the modifier " + percent(p));
                helper.assertTrue(BrotherhoodArms.brothersNear(p) == 3, "brothers near " + BrotherhoodArms.brothersNear(p));
            }
        } finally {
            finish(helper, all);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRefreshCostIsBounded(GameTestHelper helper) {
        List<TalkingDuelist> all = brotherhood(helper, 15);
        List<TalkingDuelist> outsiders = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            outsiders.add(human(helper, 1.5));
        }
        List<ServerPlayer> everyone = new ArrayList<>(all);
        everyone.addAll(outsiders);
        try {
            online(everyone.toArray(new ServerPlayer[0]));
            BrotherhoodArms.overridePlayers(() -> everyone);
            BrotherhoodArms.refresh(helper.getLevel().getServer());
            helper.assertTrue(BrotherhoodArms.lastLooks() == 16 * 15, "the refresh looked at " + BrotherhoodArms.lastLooks() + " brothers: only each member's own group, never every player (240 expected)");
        } finally {
            BrotherhoodArms.overridePlayers(null);
            clean(helper, arr(all));
            clean(helper, outsiders.toArray(new ServerPlayer[0]));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void killsAreNeverShared(GameTestHelper helper) {
        TalkingDuelist leader = GroupTestKit.hunter(helper, 1.5);
        ServerPlayer hunter = HunterMarkGameTests.human(helper);
        ServerPlayer other = HunterMarkGameTests.human(helper);
        ServerPlayer prey = HunterMarkGameTests.vampire(helper);
        online(leader, hunter, other, prey);
        try {
            helper.assertTrue(join(leader, hunter) && join(leader, other), "no brotherhood");
            BloodlineAccess.set(hunter, HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 2));
            BloodlineAccess.set(other, HunterMarkGameTests.state(Species.HUNTER_OF_THE_FIVE, 3));
            HunterMarkGameTests.kill(hunter, prey);
            helper.assertTrue(BloodlineAccess.get(hunter).markKills() == 3, "the killer's mark is " + BloodlineAccess.get(hunter).markKills());
            helper.assertTrue(BloodlineAccess.get(other).markKills() == 3 && BloodlineAccess.get(leader).markKills() == 0, "a kill was shared: " + BloodlineAccess.get(other).markKills() + "/" + BloodlineAccess.get(leader).markKills());
        } finally {
            HunterMarkGameTests.reset(hunter, other, prey);
            clean(helper, leader);
        }
        helper.succeed();
    }
}
