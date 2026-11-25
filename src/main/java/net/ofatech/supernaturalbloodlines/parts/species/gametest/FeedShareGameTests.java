package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.augustine.AugustineRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.Groups;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.SharedFeeding;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20c Task 7 (R274): shared feeding, through real {@code drinkBlood} calls and the guard every other feed rule reads. */
@PrefixGameTestTemplate(false)
public final class FeedShareGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long[] TICK = {1_000L};
    private static final long[] CLOCK = {30_000_000_000L};

    private FeedShareGameTests() {
    }

    private record Clan(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        TalkingDuelist[] all() {
            return new TalkingDuelist[] {leader, a, b};
        }
    }

    private static void pin() {
        TICK[0] = 1_000L;
        CLOCK[0] = 30_000_000_000L;
        SharedFeeding.overrideTicks(() -> TICK[0]);
        PackIndex.overrideClock(() -> CLOCK[0]);
    }

    private static Clan clan(GameTestHelper helper) {
        pin();
        TalkingDuelist l = vampire(helper, 1.5);
        TalkingDuelist a = vampire(helper, 2.5);
        TalkingDuelist b = vampire(helper, 3.5);
        online(l, a, b);
        helper.assertTrue(join(l, a) && join(l, b) && Groups.typeOf(a) == GroupType.CLAN, "no clan formed");
        drain(l, a, b);
        return new Clan(l, a, b);
    }

    private static void drain(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            VampirismAPI.vampirePlayer(p).useBlood(40, true);
        }
    }

    private static int blood(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
    }

    private static LivingEntity villager(GameTestHelper helper) {
        return EntityType.VILLAGER.create(helper.getLevel());
    }

    static IDrinkBloodContext from(LivingEntity source) {
        return new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.of(source);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        };
    }

    private static IDrinkBloodContext bottle() {
        return new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.empty();
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.of(new ItemStack(Items.GLASS_BOTTLE));
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        };
    }

    private static IDrinkBloodContext block() {
        return new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.empty();
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.of(Blocks.GLASS.defaultBlockState());
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.of(BlockPos.ZERO);
            }
        };
    }

    private static void bite(ServerPlayer feeder, IDrinkBloodContext source, int amount) {
        VampirismAPI.vampirePlayer(feeder).drinkBlood(amount, 0f, source);
    }

    private static void advance() {
        TICK[0] += 200;
        CLOCK[0] += 10_000;
    }

    // ---- happy path ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aBiteSharesWithAClanmateInRange(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 2 && blood(c.b()) == 2, "the mates got " + blood(c.leader()) + " and " + blood(c.b()));
            helper.assertTrue(blood(c.a()) == 20, "the feeder got " + blood(c.a()) + ", not what they would have");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theFeederIsUnchangedByHavingMates(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist loner = vampire(helper, 9.5);
        online(c.leader(), c.a(), c.b(), loner);
        try {
            drain(loner, c.a());
            VampirismAPI.vampirePlayer(loner).drinkBlood(10, 0f, IDrinkBloodContext.none());
            VampirismAPI.vampirePlayer(c.a()).drinkBlood(10, 0f, IDrinkBloodContext.none());
            helper.assertTrue(blood(loner) == 10 && blood(c.a()) == 10, "setup: " + blood(loner) + " " + blood(c.a()));
            bite(loner, from(villager(helper)), 5);
            int alone = blood(loner);
            bite(c.a(), from(villager(helper)), 5);
            helper.assertTrue(blood(c.a()) == alone && alone == 15, "having mates changed the feeder: " + blood(c.a()) + " against " + alone);
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), loner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theVictimIsUntouched(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            LivingEntity v = villager(helper);
            float before = v.getHealth();
            bite(c.a(), from(v), 20);
            helper.assertTrue(v.getHealth() == before, "the villager was hurt by the share");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLeaderFeedsTheirMembers(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.leader(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.a()) == 2 && blood(c.b()) == 2, "the members got " + blood(c.a()) + " and " + blood(c.b()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberFeedsTheLeader(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 2, "the head got " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theShareTableThroughARealBite(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            int[][] table = {{10, 1}, {20, 2}, {40, 2}};
            for (int[] row : table) {
                advance();
                drain(c.leader(), c.b());
                CLOCK[0] += 120_000;
                bite(c.a(), from(villager(helper)), row[0]);
                helper.assertTrue(blood(c.leader()) == row[1], row[0] + " blood gave a share of " + blood(c.leader()) + ", not " + row[1]);
            }
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    // ---- not a feed ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aBottleNeverShares(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), bottle(), 20);
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.b()) == 0, "a bottle was shared");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aBlockNeverShares(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), block(), 20);
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.b()) == 0, "a block was shared");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNoneContextNeverShares(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), IDrinkBloodContext.none(), 20);
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.b()) == 0, "a none context was shared");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aShareDoesNotRefire(GameTestHelper helper) {
        Clan c = clan(helper);
        List<String> events = new ArrayList<>();
        boolean[] live = {true};
        Consumer<BloodDrinkEvent.PlayerDrinkBloodEvent> probe = e -> {
            if (live[0]) {
                events.add(e.getVampire().getRepresentingEntity().getUUID().equals(c.a().getUUID()) ? "feeder" : "mate/" + e.getBloodSource().getEntity().isPresent());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BloodDrinkEvent.PlayerDrinkBloodEvent.class, probe);
        try {
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(events.size() == 3 && events.stream().filter("feeder"::equals).count() == 1 && events.stream().filter("mate/false"::equals).count() == 2, "the events were " + events);
        } finally {
            live[0] = false;
            NeoForge.EVENT_BUS.unregister(probe);
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDrinkInsideTheGuardIsNeverShared(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            net.ofatech.supernaturalbloodlines.shared.supernatural.SharedFeed.run(() -> bite(c.a(), from(villager(helper)), 20));
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.b()) == 0, "a drink made inside a share was shared again");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void appliesToNeedsTheLeadersGroupToBeAClanAndTheMemberToFit(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist human = human(helper, 7.5);
        TalkingDuelist wolfLeader = human(helper, 8.5);
        TalkingDuelist packMate = vampire(helper, 9.5);
        online(c.leader(), c.a(), c.b(), human, wolfLeader, packMate);
        try {
            var level = helper.getLevel();
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.appliesTo(level, c.leader().getUUID(), c.a()), "a vampire of a clan does not fit");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.appliesTo(level, c.leader().getUUID(), human), "a human fits a clan");
            PackIndex.get(level).join(wolfLeader.getUUID(), packMate.getUUID());
            GroupIndex.get(level).ensure(wolfLeader.getUUID(), GroupType.PACK, PackIndex.now());
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.group.clan.ClanScope.appliesTo(level, wolfLeader.getUUID(), packMate), "a vampire in a pack counts as a clanmate");
        } finally {
            PackIndex.get(helper.getLevel()).leave(packMate.getUUID());
            GroupIndex.get(helper.getLevel()).drop(wolfLeader.getUUID());
            clean(helper, c.leader(), c.a(), c.b(), human, wolfLeader, packMate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aShareFiresNoOtherFeedRule(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.set(c.leader(), net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.get(c.leader()).toBuilder().skills(java.util.Set.of("S-VAM-1")).build());
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 2, "a share carrying Keen Fangs gave " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theShareReadsTheAmountAfterKeenFangs(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed(); // Keen Fangs is the supernatural part's skill
            return;
        }
        Clan c = clan(helper);
        try {
            net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.set(c.a(), net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.get(c.a()).toBuilder().skills(java.util.Set.of("S-VAM-1")).build());
            bite(c.a(), from(villager(helper)), 19);
            helper.assertTrue(blood(c.a()) == 20, "setup: Keen Fangs did not add its blood: " + blood(c.a()));
            helper.assertTrue(blood(c.leader()) == 2, "the share read the amount before Keen Fangs: " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aShareIsNotRescaledByTheAugustineDiet(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            BloodlineAccess.set(c.leader(), BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
            AugustineRules.forgetFeedsForTest();
            c.leader().getPersistentData().remove("supernaturalbloodlines:augustine_diet_noted");
            bite(c.a(), from(villager(helper)), 10);
            helper.assertTrue(blood(c.leader()) == 1, "an Augustine mate gained " + blood(c.leader()) + ", not the unscaled share");
            helper.assertTrue(!c.leader().getPersistentData().getBoolean("supernaturalbloodlines:augustine_diet_noted"), "the diet note was shown for a share");
        } finally {
            AugustineRules.forgetFeedsForTest();
            c.leader().getPersistentData().remove("supernaturalbloodlines:augustine_diet_noted");
            clean(helper, c.all());
        }
        helper.succeed();
    }

    // ---- who does not share or get ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aClanmateAsVictimNeverShares(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.leader(), from(c.a()), 20);
            helper.assertTrue(blood(c.b()) == 0, "a clanmate victim shared with the third");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHumanClanmateAsVictimNeverShares(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist h = human(helper, 7.5);
        online(c.leader(), c.a(), c.b(), h);
        try {
            helper.assertTrue(join(c.leader(), h), "setup: the human joined");
            bite(c.leader(), from(h), 20);
            helper.assertTrue(blood(c.leader()) > 0, "setup: the biter got nothing");
            helper.assertTrue(blood(c.a()) == 0 && blood(c.b()) == 0, "a bite on a clanmate shared: " + blood(c.a()) + " " + blood(c.b()));
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void twoClanmatesBitingEachOtherFarmNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            int a = blood(c.a());
            int b = blood(c.b());
            bite(c.a(), from(c.b()), 20);
            advance();
            bite(c.b(), from(c.a()), 20);
            helper.assertTrue(blood(c.leader()) == 0, "the head was fed by a bite between mates: " + blood(c.leader()));
            helper.assertTrue(blood(c.a()) - a <= 20 && blood(c.b()) - b <= 20, "a bite gave more than a bite gives: an echo");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateOutOfRangeGetsNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            c.b().setPos(c.a().getX() + 17, c.a().getY(), c.a().getZ());
            c.leader().setPos(c.a().getX() + 15, c.a().getY(), c.a().getZ());
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.b()) == 0 && blood(c.leader()) == 2, "range: 17 blocks got " + blood(c.b()) + ", 15 blocks got " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMateInAnotherDimensionGetsNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            c.b().setServerLevel(nether);
            c.b().setPos(c.a().getX(), c.a().getY(), c.a().getZ());
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.b()) == 0 && blood(c.leader()) == 2, "the mate in the Nether got " + blood(c.b()));
        } finally {
            c.b().setServerLevel(helper.getLevel());
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aFullMateGetsNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("feed_share_cap_per_minute", 2);
            VampirismAPI.vampirePlayer(c.b()).drinkBlood(60, 0f, IDrinkBloodContext.none());
            int full = blood(c.b());
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.b()) == full && blood(c.leader()) == 2, "a full mate was given blood");
            drain(c.b());
            advance();
            CLOCK[0] -= 10_000;
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.b()) == 2, "a full mate used up the cap of the minute: " + blood(c.b()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHumanClanmateGetsNothing(GameTestHelper helper) {
        pin();
        TalkingDuelist l = vampire(helper, 1.5);
        TalkingDuelist a = vampire(helper, 2.5);
        TalkingDuelist h = human(helper, 3.5);
        online(l, a, h);
        try {
            helper.assertTrue(join(l, a) && join(l, h), "setup");
            drain(l, a, h);
            int humanBefore = blood(h);
            helper.assertTrue(humanBefore < VampirismAPI.vampirePlayer(h).getBloodStats().getMaxBlood(), "setup: the human has room for blood");
            bite(a, from(villager(helper)), 20);
            helper.assertTrue(blood(l) == 2, "the vampire mate got " + blood(l));
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural.isVampire(h) && blood(h) == humanBefore, "the human clanmate was given blood");
        } finally {
            clean(helper, l, a, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anOfflineOrDeadMateGetsNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            online(c.leader(), c.a());
            c.leader().setHealth(0f);
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.b()) == 0, "a dead or offline mate got blood: " + blood(c.leader()) + " " + blood(c.b()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aFeederNotInAClanGivesNothing(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist loner = vampire(helper, 4.5);
        online(c.leader(), c.a(), c.b(), loner);
        try {
            bite(loner, from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 0 && blood(c.a()) == 0, "a feeder with no clan shared");
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), loner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPackMateIsNotAClanmate(GameTestHelper helper) {
        pin();
        TalkingDuelist l = human(helper, 1.5);
        TalkingDuelist a = vampire(helper, 2.5);
        TalkingDuelist b = vampire(helper, 3.5);
        online(l, a, b);
        try {
            PackIndex.get(helper.getLevel()).join(l.getUUID(), a.getUUID());
            PackIndex.get(helper.getLevel()).join(l.getUUID(), b.getUUID());
            GroupIndex.get(helper.getLevel()).ensure(l.getUUID(), GroupType.PACK, PackIndex.now());
            drain(a, b);
            bite(a, from(villager(helper)), 20);
            helper.assertTrue(blood(b) == 0, "a pack mate received a share");
        } finally {
            clean(helper, l, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCovenIsNotAClan(GameTestHelper helper) {
        pin();
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist a = vampire(helper, 2.5);
        TalkingDuelist b = vampire(helper, 3.5);
        online(w, a, b);
        try {
            helper.assertTrue(join(w, a) && join(w, b) && Groups.typeOf(a) == GroupType.COVEN, "setup: no coven");
            drain(a, b);
            bite(a, from(villager(helper)), 20);
            helper.assertTrue(blood(b) == 0, "a coven mate received a share");
        } finally {
            clean(helper, w, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aSoloVampireInAClanOfOneGivesNothing(GameTestHelper helper) {
        pin();
        TalkingDuelist l = vampire(helper, 1.5);
        online(l);
        try {
            drain(l);
            bite(l, from(villager(helper)), 20);
            helper.assertTrue(blood(l) == 20, "a lone vampire got " + blood(l));
        } finally {
            clean(helper, l);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aVampireVictimOfAnotherClanShares(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist rival = vampire(helper, 5.5);
        online(c.leader(), c.a(), c.b(), rival);
        try {
            int before = blood(c.a());
            bite(c.a(), from(rival), 20);
            int got = blood(c.a()) - before;
            int expected = got > 0 ? net.ofatech.supernaturalbloodlines.core.group.clan.FeedShare.of(got, 10, 2) : 0;
            helper.assertTrue(blood(c.leader()) == expected, "a bite on another clan's vampire (the feeder got " + got + ") shared " + blood(c.leader()) + ", not " + expected);
        } finally {
            clean(helper, c.leader(), c.a(), c.b(), rival);
        }
        helper.succeed();
    }

    // ---- caps ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theMateCapPerMinute(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            for (int i = 0; i < 3; i++) {
                bite(c.a(), from(villager(helper)), 20);
                TICK[0] += 200;
                CLOCK[0] += 5_000;
            }
            helper.assertTrue(blood(c.leader()) == 4, "three feeds gave the head " + blood(c.leader()) + ", not the cap of 4");
            CLOCK[0] += 70_000;
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 6, "a minute later it did not resume: " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theSameFeederSpacing(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            bite(c.a(), from(villager(helper)), 20);
            TICK[0] += 99;
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 2, "the second bite inside 100 ticks shared: " + blood(c.leader()));
            TICK[0] += 1;
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 4, "the bite at 100 ticks did not share: " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anotherFeederIsNotBlocked(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("feed_share_cap_per_minute", 100);
            bite(c.a(), from(villager(helper)), 20);
            bite(c.b(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 4, "a second feeder was blocked by the first's spacing: " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theTensuraFeedExtraDrinkSharesOnce(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            LivingEntity v = villager(helper);
            bite(c.a(), from(v), 20);
            bite(c.a(), from(v), 20);
            helper.assertTrue(blood(c.leader()) == 2, "two living-source drinks of one feed shared twice: " + blood(c.leader()));
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    // ---- switches ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void clansOffMeansNoShare(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("clans_enabled", false);
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 0, "a share with clans off");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void feedShareOffMeansNoShare(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("feed_share_enabled", false);
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.leader()) == 0, "a share with feed_share_enabled off");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void aTypeSwitchStopsTheShares(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            VampirismAPI.factionPlayerHandler(c.leader()).leaveFaction(false);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(Groups.typeOf(c.a()) != GroupType.CLAN, "setup: still a clan");
            drain(c.b());
            bite(c.a(), from(villager(helper)), 20);
            helper.assertTrue(blood(c.b()) == 0, "a group that is no longer a clan shared");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forcedFeedDoesNotShare(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            var source = new net.ofatech.supernaturalbloodlines.core.blood.BloodSource(c.a().getUUID(), "a", false, net.ofatech.supernaturalbloodlines.core.blood.BloodKind.of(BloodlineAccess.get(c.a()).species()));
            net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles.applyWithoutConsent(c.b(), source);
            helper.assertTrue(blood(c.leader()) == 0, "a forced feed shared");
        } finally {
            clean(helper, c.all());
        }
        helper.succeed();
    }
}
