package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.favouriteless.enchanted.common.blocks.entity.AltarBlockEntity;
import net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleCost;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;

/**
 * Plan 20b Task 5 (R253, R257): the circle plan rides the rite cost hook. The direct tests call {@code RiteCost.begin / commit / discard} the way the
 * chalk's tick does and finish in the tick they start; the real-chalk ones wait for the chalk's own tick (a batch of their own).
 */
@PrefixGameTestTemplate(false)
public final class CovenCostGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final List<String> KEYS = List.of("link_enabled", "link_member_share", "channel_cap", "channel_other_factor", "channel_regen_minutes", "ep_keep_percent");

    private CovenCostGameTests() {
    }

    /** A coven of witches (full-size meters) standing at a chalk, with its circle open and everybody joined. */
    public record Circle(BlockPos chalk, GoldChalkBlockEntity chalkEntity, TalkingDuelist anchor, List<TalkingDuelist> others) {
        public List<TalkingDuelist> all() {
            List<TalkingDuelist> out = new ArrayList<>();
            out.add(anchor);
            out.addAll(others);
            return out;
        }

        public ServerPlayer[] array() {
            return all().toArray(new ServerPlayer[0]);
        }
    }

    public static TalkingDuelist witchMember(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).witchLayer(true).build());
        return p;
    }

    static Circle circle(GameTestHelper helper, int n) {
        return circleAt(helper, new BlockPos(2, 1, 1), n);
    }

    /** The circle with its gold chalk at {@code chalkRelative} (a cell position); the witches stand 2 to 5 blocks from its centre. */
    public static Circle circleAt(GameTestHelper helper, BlockPos chalkRelative, int n) {
        BlockPos chalkAbs = helper.absolutePos(chalkRelative);
        WitchGameTests.chalk(helper, chalkAbs, UUID.randomUUID());
        TalkingDuelist anchor = witch(helper, 2.0);
        List<TalkingDuelist> others = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            others.add(witchMember(helper, 2.0));
        }
        Circle c = new Circle(chalkAbs, null, anchor, others);
        for (int i = 0; i < c.all().size(); i++) {
            c.all().get(i).setPos(chalkAbs.getX() + 2.5 + 0.2 * i, chalkAbs.getY(), chalkAbs.getZ() + 1.5);
        }
        online(c.array());
        for (ServerPlayer p : c.array()) {
            helper.getLevel().addNewPlayer(p);
        }
        for (TalkingDuelist o : others) {
            helper.assertTrue(join(anchor, o), "the coven did not take everyone");
        }
        helper.assertTrue(CircleLink.open(anchor) == null, "the circle did not open");
        for (TalkingDuelist o : others) {
            helper.assertTrue(CircleLink.join(o, null) == null, "a member could not join the circle");
        }
        GoldChalkBlockEntity entity = (GoldChalkBlockEntity) helper.getLevel().getBlockEntity(c.chalk());
        return new Circle(c.chalk(), entity, anchor, others);
    }

    public static void tidy(GameTestHelper helper, ServerPlayer... players) {
        RiteCost.discard();
        for (ServerPlayer p : players) {
            if (!p.isRemoved()) {
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            }
            CircleLink.forget(p.getUUID());
            GroupIndex.get(helper.getLevel()).setConsent(p.getUUID(), Consent.NONE);
            GroupIndex.get(helper.getLevel()).setChannel(p.getUUID(), 99999, 0, 99999);
        }
        KEYS.forEach(k -> GroupsConfig.override(k, null));
        PackIndex.overrideClock(null);
        clean(helper, players);
    }

    public static int begin(GameTestHelper helper, Circle c, UUID caster, int base, boolean start) {
        return RiteCost.begin(helper.getLevel(), caster, c.chalk(), c.chalkEntity().getPosHolder(), base, start, null);
    }

    /** What a solo cast pays for {@code base}: the link switched off for the call. */
    static int soloBill(GameTestHelper helper, Circle c, UUID caster, int base) {
        GroupsConfig.override("link_enabled", false);
        try {
            int bill = begin(helper, c, caster, base, true);
            RiteCost.discard();
            return bill;
        } finally {
            GroupsConfig.override("link_enabled", null);
        }
    }

    private static GroupIndex.ChannelRow meter(GameTestHelper helper, UUID id) {
        return GroupIndex.get(helper.getLevel()).channelOf(id);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSoloCastIsTodaysBill(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            int solo = soloBill(helper, c, c.anchor().getUUID(), 6000);
            int lone = begin(helper, c, UUID.randomUUID(), 6000, true);
            helper.assertTrue(lone == begin(helper, c, UUID.randomUUID(), 6000, true) && CircleStage.current() == null, "a caster with no circle staged a plan");
            helper.assertTrue(solo > 0 && solo <= 6000, "the solo bill is " + solo);
            helper.assertTrue(CircleStage.current() == null, "the stage is not clear");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLinkedRiteCutsTheAltarBillToTheAltarShare(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            for (UUID id : List.of(c.others().get(0).getUUID(), c.others().get(1).getUUID(), c.anchor().getUUID())) {
                GroupIndex.get(helper.getLevel()).setConsent(id, Consent.NOSEBLEED);
            }
            for (int base : new int[] {4500, 15000}) {
                int solo = soloBill(helper, c, c.anchor().getUUID(), base);
                int linked = begin(helper, c, c.anchor().getUUID(), base, true);
                int pool = (int) Math.floor(solo * 0.70 + 1e-9);
                helper.assertTrue(linked == solo - pool, "base " + base + ": the altar is asked " + linked + ", solo is " + solo + ", the members' pool " + pool);
                CircleStage.Staged staged = CircleStage.current();
                helper.assertTrue(staged != null && staged.result().shares().size() == 3, "no plan of three was staged");
                helper.assertTrue(staged.result().shares().values().stream().mapToInt(s -> s.power()).sum() == pool, "the shares do not add up to the pool");
                RiteCost.discard();
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void membersPayOnCommitOnly(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(meter(helper, p.getUUID()) == null, "a meter changed before the rite started");
            }
            CircleStage.Staged staged = CircleStage.current();
            helper.assertTrue(staged != null, "no plan");
            var shares = staged.result().shares();
            RiteCost.commit();
            helper.assertTrue(CircleStage.current() == null, "the plan outlived the commit");
            for (TalkingDuelist p : c.all()) {
                int cap = CircleCost.capacityOf(p);
                var row = meter(helper, p.getUUID());
                helper.assertTrue(row != null && row.value() == cap - shares.get(p.getUUID()).power(), "the meter of a member is " + row + ", share " + shares.get(p.getUUID()));
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRiteThatCancelsItselfChargesNobody(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            RiteCost.discard();
            RiteCost.commit();
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(meter(helper, p.getUUID()) == null, "a member paid for a rite that cancelled itself");
            }
            helper.assertTrue(CircleStage.current() == null, "the plan survived a cancel");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void perTickPowerIsNeverSplit(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            int solo = soloBill(helper, c, c.anchor().getUUID(), 1000);
            int tick = begin(helper, c, c.anchor().getUUID(), 1000, false);
            helper.assertTrue(tick == solo && CircleStage.current() == null, "a per-tick cost read the circle: " + tick + " against " + solo);
            RiteCost.commit();
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(meter(helper, p.getUUID()) == null, "a per-tick cost charged a member");
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void someoneElseUsingTheChalkIsASoloCast(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            UUID other = c.others().get(0).getUUID();
            int solo = soloBill(helper, c, other, 6000);
            int bill = begin(helper, c, other, 6000, true);
            helper.assertTrue(bill == solo && CircleStage.current() == null, "another caster at the chalk got the circle: " + bill + " against " + solo);
            RiteCost.commit();
            helper.assertTrue(meter(helper, other) == null, "a member paid for another's rite");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoWalkedOutIsDroppedAndTold(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            TalkingDuelist gone = c.others().get(1);
            gone.setPos(gone.getX() + 20, gone.getY(), gone.getZ());
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            var shares = CircleStage.current().result().shares();
            helper.assertTrue(shares.size() == 2 && !shares.containsKey(gone.getUUID()), "the plan has " + shares.keySet());
            helper.assertTrue(gone.received("supernaturalbloodlines.coven.link_dropped.away"), "the one who walked out was not told: " + gone.received());
            RiteCost.commit();
            helper.assertTrue(meter(helper, gone.getUUID()) == null, "the one who walked out paid");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anIneligibleAnchorCannotChargeEligibleHelpers(GameTestHelper helper) {
        Circle c = circle(helper, 4);
        try {
            int solo = soloBill(helper, c, c.anchor().getUUID(), 4500);
            c.anchor().setHealth(c.anchor().getMaxHealth() * 0.4f);
            int bill = begin(helper, c, c.anchor().getUUID(), 4500, true);
            RiteCost.commit();
            helper.assertTrue(bill == solo && c.all().stream().allMatch(p -> meter(helper, p.getUUID()) == null),
                    "an ineligible anchor split the solo bill " + solo + " and left " + bill + " for the altar");
            helper.assertTrue(c.anchor().received("supernaturalbloodlines.coven.link_dropped.anchor_hurt") && !c.anchor().received("supernaturalbloodlines.coven.link_dropped.hurt"),
                    "the hurt anchor was not told the rite is charged to them alone: " + c.anchor().received());
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberAtLowHealthIsDropped(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            TalkingDuelist hurt = c.others().get(0);
            hurt.setHealth(hurt.getMaxHealth() * 0.4f);
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            var shares = CircleStage.current().result().shares();
            helper.assertTrue(shares.size() == 2 && !shares.containsKey(hurt.getUUID()), "the plan has " + shares.keySet());
            RiteCost.discard();
            hurt.setHealth(hurt.getMaxHealth() * 0.5f);
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            helper.assertTrue(CircleStage.current().result().shares().containsKey(hurt.getUUID()), "a member at exactly half health was dropped");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineMemberIsDropped(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            TalkingDuelist away = c.others().get(0);
            helper.getLevel().removePlayerImmediately(away, Entity.RemovalReason.DISCARDED);
            begin(helper, c, c.anchor().getUUID(), 6000, true);
            var shares = CircleStage.current().result().shares();
            helper.assertTrue(shares.size() == 2 && !shares.containsKey(away.getUUID()), "the plan has " + shares.keySet());
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleOfOneIsASoloCast(GameTestHelper helper) {
        Circle c = circle(helper, 2);
        try {
            TalkingDuelist other = c.others().get(0);
            helper.getLevel().removePlayerImmediately(other, Entity.RemovalReason.DISCARDED);
            int solo = soloBill(helper, c, c.anchor().getUUID(), 6000);
            int bill = begin(helper, c, c.anchor().getUUID(), 6000, true);
            helper.assertTrue(bill == solo && CircleStage.current().result().shares().isEmpty(), "an anchor alone was given shares");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    // ---- strain (R257): the meter, the nosebleed and the pain ----

    private static void consentAll(GameTestHelper helper, Circle c, Consent level) {
        for (TalkingDuelist p : c.all()) {
            GroupIndex.get(helper.getLevel()).setConsent(p.getUUID(), level);
        }
    }

    private static void setMeter(GameTestHelper helper, Circle c, int value) {
        for (TalkingDuelist p : c.all()) {
            GroupIndex.get(helper.getLevel()).setChannel(p.getUUID(), value, PackIndex.now(), CircleCost.capacityOf(p));
        }
    }

    private static void startRite(GameTestHelper helper, Circle c, int base) {
        begin(helper, c, c.anchor().getUUID(), base, true);
        RiteCost.commit();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCleanShareOnlyLowersTheMeter(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            startRite(helper, c, 4500);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION) && !p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "a clean share gave an effect");
                helper.assertTrue(p.getHealth() == p.getMaxHealth(), "a clean share hurt");
                helper.assertTrue(meter(helper, p.getUUID()) != null && meter(helper, p.getUUID()).value() < 3000, "the meter did not go down");
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNosebleedShareGivesNauseaAndWeakness(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            consentAll(helper, c, Consent.NOSEBLEED);
            startRite(helper, c, 15000);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION) && p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "no nosebleed for " + p.getGameProfile().getName());
                helper.assertTrue(p.getHealth() == p.getMaxHealth(), "a nosebleed share hurt");
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDamageShareHurtsByTheTableAndNeverBelowOneHeart(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            consentAll(helper, c, Consent.DAMAGE);
            setMeter(helper, c, 1500);
            startRite(helper, c, 15000);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(p.getHealth() == p.getMaxHealth() - 3, "an overdraw of 2000 over a cap of 3000 should cost 3 hit points: " + p.getHealth());
            }
        } finally {
            tidy(helper, c.array());
        }
        Circle low = circle(helper, 3);
        try {
            consentAll(helper, low, Consent.DAMAGE);
            setMeter(helper, low, 0);
            TalkingDuelist weak = low.others().get(0);
            weak.setHealth(weak.getMaxHealth() * 0.5f);
            startRite(helper, low, 12858);
            helper.assertTrue(weak.getHealth() == 1.0f && weak.isAlive(), "the hardest share should leave a member at 1 HP, not " + weak.getHealth());
            helper.assertTrue(low.anchor().getHealth() == low.anchor().getMaxHealth() - 10, "a share at the hard cap should cost 10 hit points: " + low.anchor().getHealth());
        } finally {
            tidy(helper, low.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void damageNeverKillsEvenWithTheLethalOptionOff(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            GroupsConfig.override("link_death_lethal", false);
            consentAll(helper, c, Consent.DAMAGE);
            setMeter(helper, c, 0);
            for (TalkingDuelist p : c.all()) {
                p.setHealth(p.getMaxHealth() * 0.5f);
            }
            startRite(helper, c, 12858);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(p.isAlive() && p.getHealth() >= 1.0f, "the strain killed " + p.getGameProfile().getName());
            }
        } finally {
            GroupsConfig.override("link_death_lethal", null);
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMeterIsZeroAfterAnOverdraw(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        try {
            consentAll(helper, c, Consent.NOSEBLEED);
            startRite(helper, c, 15000);
            for (TalkingDuelist p : c.all()) {
                helper.assertTrue(meter(helper, p.getUUID()).value() == 0, "the meter after an overdraw is " + meter(helper, p.getUUID()).value());
            }
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMeterRefillsLazilyAfterTenMinutes(GameTestHelper helper) {
        Circle c = circle(helper, 3);
        long[] clock = {5_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            consentAll(helper, c, Consent.NOSEBLEED);
            startRite(helper, c, 15000);
            GroupIndex groups = GroupIndex.get(helper.getLevel());
            UUID id = c.anchor().getUUID();
            int cap = CircleCost.capacityOf(c.anchor());
            helper.assertTrue(CircleCost.meterOf(groups, id, cap, clock[0]) == 0, "not empty right after");
            helper.assertTrue(CircleCost.meterOf(groups, id, cap, clock[0] + 5 * 60_000L) == cap / 2, "not half after five minutes: " + CircleCost.meterOf(groups, id, cap, clock[0] + 5 * 60_000L));
            helper.assertTrue(CircleCost.meterOf(groups, id, cap, clock[0] + 10 * 60_000L) == cap, "not full after ten minutes");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aShareIsChargedToTheMemberWhoseConsentAllowedIt(GameTestHelper helper) {
        Circle c = circle(helper, 4);
        try {
            consentAll(helper, c, Consent.NOSEBLEED);
            TalkingDuelist strict = c.others().get(1);
            GroupIndex.get(helper.getLevel()).setConsent(strict.getUUID(), Consent.NONE);
            setMeter(helper, c, 2000);
            startRite(helper, c, 15000);
            helper.assertTrue(!strict.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "a member who consented to nothing was strained");
            helper.assertTrue(meter(helper, strict.getUUID()).value() == 2000, "a member who consented to nothing paid: " + meter(helper, strict.getUUID()));
            helper.assertTrue(c.anchor().hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "the others were not strained");
        } finally {
            tidy(helper, c.array());
        }
        helper.succeed();
    }

    // ---- through the chalk's own tick: a batch of its own; the circle is built inside the tick that starts the rite, so nothing can close it while waiting ----

    @GameTest(template = "empty", templateNamespace = NS, batch = "covens_cost_chalk", timeoutTicks = 100)
    public static void aRealChalkStartsALinkedRite(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 4));
        CovenHookGameTests.Probe probe = new CovenHookGameTests.Probe(new ArrayList<>(), new AtomicReference<>(), false);
        CovenHookGameTests.onAlignedTick(helper, () -> {
            Circle c = circle(helper, 3);
            try {
                AltarBlockEntity altar = WitchGameTests.altar(helper, base, 20000);
                GoldChalkBlockEntity chalk = (GoldChalkBlockEntity) helper.getLevel().getBlockEntity(c.chalk());
                chalk.getPosHolder().add(base);
                WitchGameTests.set(chalk, "caster", c.anchor().getUUID());
                int solo = soloBill(helper, c, c.anchor().getUUID(), 4500);
                WitchGameTests.set(altar, "power", 20000.0);
                CovenHookGameTests.startThroughChalk(helper, c.chalk(), chalk, probe.type(4500, 0));
                double paid = 20000 - WitchGameTests.power(altar);
                int pool = (int) Math.floor(solo * 0.70 + 1e-9);
                helper.assertTrue(solo > 0 && Math.round(paid) == solo - pool, "the altar paid " + paid + " of a solo bill " + solo + " with members' pool " + pool);
                helper.assertTrue(probe.log().contains("onStart"), "the rite did not start");
                for (TalkingDuelist p : c.all()) {
                    helper.assertTrue(meter(helper, p.getUUID()) != null, "a member's meter did not move");
                }
            } finally {
                tidy(helper, c.array());
            }
        });
    }

    @GameTest(template = "empty", templateNamespace = NS, batch = "covens_cost_chalk", timeoutTicks = 100)
    public static void anAltarThatCannotPayChargesNobody(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 200, 4));
        CovenHookGameTests.Probe probe = new CovenHookGameTests.Probe(new ArrayList<>(), new AtomicReference<>(), false);
        CovenHookGameTests.onAlignedTick(helper, () -> {
            Circle c = circle(helper, 3);
            try {
                AltarBlockEntity altar = WitchGameTests.altar(helper, base, 0);
                GoldChalkBlockEntity chalk = (GoldChalkBlockEntity) helper.getLevel().getBlockEntity(c.chalk());
                chalk.getPosHolder().add(base);
                WitchGameTests.set(chalk, "caster", c.anchor().getUUID());
                WitchGameTests.set(altar, "power", 0.0);
                List<String> seen = CovenHookGameTests.traced(() -> CovenHookGameTests.startThroughChalk(helper, c.chalk(), chalk, probe.type(6000, 0)));
                helper.assertTrue(!probe.log().contains("onStart"), "the rite started with an empty altar: " + seen + " altar " + WitchGameTests.power(altar) + " stage " + CircleStage.current());
                helper.assertTrue(CircleStage.current() == null, "the plan outlived the chalk's cancel");
                for (TalkingDuelist p : c.all()) {
                    helper.assertTrue(meter(helper, p.getUUID()) == null, "a member paid although the altar could not");
                }
            } finally {
                tidy(helper, c.array());
            }
        });
    }
}
