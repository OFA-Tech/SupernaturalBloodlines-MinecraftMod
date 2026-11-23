package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.blood.GiveBloodAction;
import net.ofatech.supernaturalbloodlines.parts.species.blood.VampireBloodBuff;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodDrink;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Plan 18f: Force-feed my blood (R208) and what the blood does while it lasts (R207). Needs no Werewolves: see {@code GiveBloodWerewolfGameTests}. */
@PrefixGameTestTemplate(false)
public final class GiveBloodGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ExtraActions.Action ACTION = GiveBloodAction.INSTANCE;

    private GiveBloodGameTests() {
    }

    private static void asVampire(GameTestHelper helper, ServerPlayer p, Species species) {
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
    }

    private static TestPlayers.TalkingDuelist vampire(GameTestHelper helper, Species species) {
        TestPlayers.TalkingDuelist p = TestPlayers.createTalkingDuelist(helper);
        asVampire(helper, p, species);
        return p;
    }

    private static TestPlayers.TalkingDuelist human(GameTestHelper helper) {
        TestPlayers.TalkingDuelist t = TestPlayers.createTalkingDuelist(helper);
        t.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        return t;
    }

    private static void clean(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Downed.clearForTest(p);
            p.removeAllEffects();
            p.setShiftKeyDown(false);
            p.setHealth(p.getMaxHealth());
            p.setData(SpeciesRegistries.ACTION_READY, new HashMap<String, Long>());
            p.removeData(SpeciesRegistries.BLOOD_SOURCE);
            p.removeData(SpeciesRegistries.BLOOD_SOURCE_SPECIES);
            BloodlineAccess.set(p, BloodlineState.NONE);
            if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null) {
                VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            }
        }
    }

    private static void noCooldown(ServerPlayer p) {
        p.setData(SpeciesRegistries.ACTION_READY, new HashMap<String, Long>());
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static boolean marked(ServerPlayer p) {
        return BloodlineAccess.get(p).vampireBloodUntil() > p.level().getGameTime();
    }

    private static int cooldown(ServerPlayer p) {
        return ActionClock.left(p, ExtraActions.GIVE_BLOOD);
    }

    private static void pvp(GameTestHelper helper, boolean on) {
        helper.getLevel().getServer().setPvpAllowed(on);
    }

    // ---- who may be fed ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forceFeedMarksAnUnwillingHumanAtLowHealth(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        pvp(helper, true);
        try {
            target.setHealth(target.getMaxHealth() * 0.25f);
            helper.assertTrue(ACTION.trigger(giver, target) == null, "the action was refused");
            helper.assertTrue(BloodlineAccess.get(target).vampireBloodUntil() == helper.getLevel().getGameTime() + SpeciesConfig.vampireBloodTicks(), "the blood did not mark them: " + BloodlineAccess.get(target));
            helper.assertTrue(giver.receivedOnActionBar(GiveBloodAction.DONE) && cooldown(giver) == SpeciesConfig.giveBloodCooldownSeconds() * 20, "the giver was not told or has no cooldown");
            helper.assertTrue(target.received(GiveBloodAction.RECEIVED) && !target.received(GiveBloodAction.RECEIVED_WILLING) && target.received(BloodBottles.MARKED), "the target was not told what is in them: " + target.keys());
            helper.assertTrue(target.message(GiveBloodAction.RECEIVED).getContents() instanceof TranslatableContents t && t.getArgs()[0] instanceof Component c && c.getString().equals(giver.getDisplayName().getString()),
                    "the target was not told who");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusesWhenPvpIsOffAndTheRefusalCostsTheCooldown(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, false);
            target.setHealth(1f);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))), "pvp off did not refuse");
            helper.assertTrue(!marked(target) && cooldown(giver) > 0, "it acted, or the refusal was free");
            noCooldown(giver);
            Downed.startSnapped(target, 600);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a downed target was fed with pvp off");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusesAHealthyUnwillingTargetAndFeedsAtTheLimitExactly(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, true);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a healthy unwilling target was fed");
            noCooldown(giver);
            target.setHealth((float) (target.getMaxHealth() * (SpeciesConfig.forceFeedHealthLimit() + 0.05)));
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a target over the limit was fed");
            noCooldown(giver);
            target.setHealth((float) (target.getMaxHealth() * SpeciesConfig.forceFeedHealthLimit()));
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a target at the limit exactly was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDownedUnwillingTargetIsFedAtFullHealth(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, true);
            Downed.startSnapped(target, 600);
            helper.assertTrue(Downed.isDowned(target), "the target is not down");
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a downed target was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSneakingTargetAcceptsAtFullHealthWithPvpOffAndIsToldSo(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.HYBRID);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, false);
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a sneaking target did not accept");
            helper.assertTrue(target.received(GiveBloodAction.RECEIVED_WILLING) && !target.received(GiveBloodAction.RECEIVED), "the target was not told they accepted: " + target.keys());
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainBlocksEvenAWillingTargetButOnlyAfterConsent(GameTestHelper helper) {
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "vervain"))) {
            helper.succeed(); // no artifacts part: nothing keeps vampire blood out
            return;
        }
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, true);
            target.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NS, "vervain"))));
            target.setShiftKeyDown(true);
            helper.assertTrue(GiveBloodAction.VERVAIN.equals(key(ACTION.trigger(giver, target))) && cooldown(giver) > 0, "vervain did not block a willing target, or the refusal was free");
            noCooldown(giver);
            target.setShiftKeyDown(false);
            helper.assertTrue(GiveBloodAction.NOT_WILLING.equals(key(ACTION.trigger(giver, target))), "a healthy unwilling target's refusal told about vervain");
            noCooldown(giver);
            target.setHealth(1f);
            helper.assertTrue(GiveBloodAction.VERVAIN.equals(key(ACTION.trigger(giver, target))) && !marked(target), "vervain did not block an unwilling one");
            noCooldown(giver);
            target.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "without vervain the same target was not fed");
        } finally {
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClaimOnTheBlowBlocksBothTheWillingAndTheUnwilling(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        Consumer<AttackEntityEvent> claim = e -> e.setCanceled(true);
        try {
            pvp(helper, true);
            target.setHealth(1f);
            NeoForge.EVENT_BUS.addListener(claim);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a claim did not block an unwilling feed");
            helper.assertTrue(cooldown(giver) > 0, "a refusal was free");
            noCooldown(giver);
            target.setShiftKeyDown(true);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a claim did not block a willing feed");
            NeoForge.EVENT_BUS.unregister(claim);
            noCooldown(giver);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "without a claim the feed did not happen");
        } finally {
            NeoForge.EVENT_BUS.unregister(claim);
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDuelOrProtectionThatCancelsTheBlowRefusesAndNoClickIsPosted(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        boolean[] probed = new boolean[1];
        int[] clicks = new int[1];
        Consumer<LivingIncomingDamageEvent> protection = e -> {
            if (e.getEntity() == target) {
                probed[0] = DamageProbe.active();
                e.setCanceled(true);
            }
        };
        Consumer<PlayerInteractEvent.EntityInteract> click = e -> clicks[0]++;
        NeoForge.EVENT_BUS.addListener(protection);
        NeoForge.EVENT_BUS.addListener(click);
        try {
            pvp(helper, true);
            target.setShiftKeyDown(true);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a protection of the target did not refuse a willing feed");
            helper.assertTrue(probed[0], "the question was not asked as a probe");
            noCooldown(giver);
            target.setShiftKeyDown(false);
            target.setHealth(1f);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a protection of the target did not refuse an unwilling feed");
            helper.assertTrue(clicks[0] == 0, "a click was posted to ask the claims");
            helper.assertTrue(!DamageProbe.active(), "the probe is still on");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
            NeoForge.EVENT_BUS.unregister(click);
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTeamWithFriendlyFireOffRefusesAnUnwillingFeedButNotAWillingOne(GameTestHelper helper) {
        ServerPlayer giver = TestPlayers.createPvpAware(helper);
        ServerPlayer target = TestPlayers.createPvpAware(helper);
        target.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        asVampire(helper, giver, Species.NONE);
        var board = helper.getLevel().getScoreboard();
        var team = board.addPlayerTeam("give-blood-" + UUID.randomUUID().toString().substring(0, 8));
        team.setAllowFriendlyFire(false);
        try {
            pvp(helper, true);
            board.addPlayerToTeam(giver.getScoreboardName(), team);
            board.addPlayerToTeam(target.getScoreboardName(), team);
            target.setHealth(target.getMaxHealth() * 0.2f);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, target))) && !marked(target), "a team mate with friendly fire off was force-fed");
            noCooldown(giver);
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null && marked(target), "a team mate who sneaks was refused");
        } finally {
            board.removePlayerTeam(team);
            pvp(helper, false);
            clean(giver, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void selfAndMobAndCreativeAndNoTargetAreRefused(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist creative = human(helper);
        Zombie z = EntityType.ZOMBIE.create(helper.getLevel());
        z.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        z.setNoAi(true);
        helper.getLevel().addFreshEntity(z);
        try {
            pvp(helper, true);
            helper.assertTrue(GiveBloodAction.SELF.equals(key(ACTION.trigger(giver, giver))) && cooldown(giver) == 0, "self was not refused, or cost the cooldown");
            helper.assertTrue(GiveBloodAction.NO_TARGET.equals(key(ACTION.trigger(giver, null))) && cooldown(giver) == 0, "no target was not said, or cost the cooldown");
            helper.assertTrue(GiveBloodAction.NOT_A_PLAYER.equals(key(ACTION.trigger(giver, z))) && cooldown(giver) == 0, "a mob was not refused, or a misclick cost the cooldown");
            creative.setShiftKeyDown(true);
            creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(giver, creative))) && !marked(creative), "a creative player was fed");
            helper.assertTrue(cooldown(giver) > 0, "the refusal was free");
        } finally {
            creative.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            z.discard();
            pvp(helper, false);
            clean(giver, creative);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyVampireTypeAndAGhoulCanGiveAndAHumanCannot(GameTestHelper helper) {
        TestPlayers.TalkingDuelist human = TestPlayers.createTalkingDuelist(helper);
        try {
            helper.assertTrue(!ACTION.available(human), "a human has the action");
            helper.assertTrue(GiveBloodAction.REFUSED.equals(key(ACTION.trigger(human, human))) && cooldown(human) == 0, "a human's trigger was not refused");
            // the vampire-side species have their real faction: the hybrid kinds sit in the werewolf faction, which GiveBloodWerewolfGameTests builds; here none at all
            for (Species s : new Species[] {Species.NONE, Species.HERETIC, Species.ORIGINAL, Species.AUGUSTINE}) {
                TestPlayers.TalkingDuelist v = vampire(helper, s);
                try {
                    helper.assertTrue(ACTION.available(v), s + " cannot give blood");
                } finally {
                    clean(v);
                }
            }
            for (Species s : new Species[] {Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID}) {
                TestPlayers.TalkingDuelist v = TestPlayers.createTalkingDuelist(helper);
                try {
                    BloodlineAccess.set(v, BloodlineState.NONE.toBuilder().species(s).build());
                    helper.assertTrue(ACTION.available(v), s + " cannot give blood without a faction");
                } finally {
                    clean(v);
                }
            }
            TestPlayers.TalkingDuelist ghoul = TestPlayers.createTalkingDuelist(helper);
            TestPlayers.TalkingDuelist target = human(helper);
            try {
                helper.assertTrue(VampirismAPI.factionPlayerHandler(ghoul).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a Ghoul");
                BloodlineAccess.set(ghoul, BloodlineState.NONE.toBuilder().ghoul(true).build());
                target.setShiftKeyDown(true);
                helper.assertTrue(ACTION.available(ghoul) && ACTION.trigger(ghoul, target) == null && marked(target), "a Ghoul cannot give blood, like Bottle my blood");
            } finally {
                clean(ghoul, target);
            }
        } finally {
            clean(human);
        }
        helper.succeed();
    }

    // ---- no oracle ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theGiverHearsTheSameThingWhoeverTheTargetIs(GameTestHelper helper) {
        TestPlayers.TalkingDuelist human = human(helper);
        TestPlayers.TalkingDuelist vampire = vampire(helper, Species.HYBRID);
        vampire.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
        TestPlayers.TalkingDuelist giverA = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist giverB = vampire(helper, Species.NONE);
        try {
            pvp(helper, true);
            human.setShiftKeyDown(true);
            vampire.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giverA, human) == null && ACTION.trigger(giverB, vampire) == null, "one was refused");
            helper.assertTrue(marked(human) && !marked(vampire), "the blood did something to a vampire");
            helper.assertTrue(giverA.countOnActionBar(GiveBloodAction.DONE) == 1 && giverB.countOnActionBar(GiveBloodAction.DONE) == 1, "the done lines differ");
            helper.assertTrue(cooldown(giverA) == cooldown(giverB) && cooldown(giverA) > 0, "the cooldowns differ");
        } finally {
            pvp(helper, false);
            clean(giverA, giverB, human, vampire);
        }
        helper.succeed();
    }

    // ---- the Original's blood, and consent ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalGiverNamesThePendingSireOfAWillingTargetThroughShownName(GameTestHelper helper) {
        for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID}) {
            TestPlayers.TalkingDuelist giver = vampire(helper, s);
            TestPlayers.TalkingDuelist target = human(helper);
            try {
                target.setShiftKeyDown(true);
                helper.assertTrue(ACTION.trigger(giver, target) == null, s + ": the action was refused");
                helper.assertTrue(target.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && giver.getUUID().equals(target.getData(SpeciesRegistries.BLOOD_SOURCE))
                        && s.name().equals(target.getData(SpeciesRegistries.BLOOD_SOURCE_SPECIES)), s + ": the blood is not the pending sire");
                Component said = target.message(BloodDrink.DRUNK_MARKED);
                helper.assertTrue(said != null && said.getContents() instanceof TranslatableContents t
                        && t.getArgs()[0] instanceof Component c && c.getString().equals(Lines.shownName(helper.getLevel().getServer(), target, giver.getUUID()).getString()),
                        s + ": the message does not name the source through shownName");
                target.hurt(helper.getLevel().damageSources().generic(), 1000f);
                helper.assertTrue(BloodlineAccess.get(target).inTurning() && giver.getUUID().equals(BloodlineAccess.get(target).pendingSire()), s + ": the pending sire is " + BloodlineAccess.get(target));
            } finally {
                clean(giver, target);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodForcedOnAnUnwillingPlayerNeverNamesASire(GameTestHelper helper) {
        UUID sireA = UUID.randomUUID();
        TestPlayers.TalkingDuelist giverB = vampire(helper, Species.ORIGINAL);
        TestPlayers.TalkingDuelist control = vampire(helper, Species.ORIGINAL);
        TestPlayers.TalkingDuelist inTurning = human(helper);
        TestPlayers.TalkingDuelist plain = human(helper);
        try {
            pvp(helper, true);
            // a player in the turning whose pending sire is A: B's forced blood must not replace A
            BloodlineAccess.set(inTurning, BloodlineState.NONE.toBuilder().turningTicksLeft(5000).pendingSire(sireA).build());
            inTurning.setData(SpeciesRegistries.BLOOD_SOURCE, sireA);
            inTurning.setHealth(inTurning.getMaxHealth() * 0.2f);
            helper.assertTrue(ACTION.trigger(giverB, inTurning) == null, "the action was refused");
            helper.assertTrue(sireA.equals(BloodlineAccess.get(inTurning).pendingSire()) && sireA.equals(inTurning.getData(SpeciesRegistries.BLOOD_SOURCE))
                    && !inTurning.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT), "forced blood replaced the pending sire: " + BloodlineAccess.get(inTurning));
            // the giver cannot tell: same line, same cooldown as for a human who is fed for real
            plain.setHealth(plain.getMaxHealth() * 0.2f);
            helper.assertTrue(ACTION.trigger(control, plain) == null, "the control was refused");
            helper.assertTrue(giverB.countOnActionBar(GiveBloodAction.DONE) == 1 && control.countOnActionBar(GiveBloodAction.DONE) == 1 && cooldown(giverB) == cooldown(control) && cooldown(giverB) > 0,
                    "the giver heard or paid something different");
            // a human outside the turning is marked with plain vampire blood and no sire: dying turns them with no pending sire
            helper.assertTrue(marked(plain) && !plain.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT) && !plain.hasData(SpeciesRegistries.BLOOD_SOURCE), "an Original's forced blood named a sire");
            plain.hurt(helper.getLevel().damageSources().generic(), 1000f);
            helper.assertTrue(BloodlineAccess.get(plain).inTurning() && BloodlineAccess.get(plain).pendingSire() == null, "the turning has a sire: " + BloodlineAccess.get(plain));
        } finally {
            pvp(helper, false);
            clean(giverB, control, inTurning, plain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theReceiverIsNotToldAnOriginalsNameUntilTheyKnowTheLine(GameTestHelper helper) {
        TestPlayers.TalkingDuelist original = vampire(helper, Species.ORIGINAL);
        TestPlayers.TalkingDuelist common = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist a = human(helper);
        TestPlayers.TalkingDuelist b = human(helper);
        try {
            pvp(helper, true);
            for (boolean willing : new boolean[] {true, false}) {
                for (TestPlayers.TalkingDuelist t : new TestPlayers.TalkingDuelist[] {a, b}) {
                    t.setShiftKeyDown(willing);
                    t.setHealth(willing ? t.getMaxHealth() : 1f);
                }
                String k = willing ? GiveBloodAction.RECEIVED_WILLING : GiveBloodAction.RECEIVED;
                noCooldown(original);
                noCooldown(common);
                helper.assertTrue(ACTION.trigger(original, a) == null && ACTION.trigger(common, b) == null, "a feed was refused");
                Component secret = a.message(k);
                Component plain = b.message(k);
                helper.assertTrue(secret != null && !secret.getString().contains(original.getDisplayName().getString()), "an Original's name reached a receiver who does not know the line: " + secret);
                helper.assertTrue(secret.getContents() instanceof TranslatableContents t && t.getArgs()[0] instanceof Component n
                        && n.getString().equals(Lines.shownName(helper.getLevel().getServer(), a, original.getUUID()).getString()), "the name is not the one the line rules show");
                helper.assertTrue(plain != null && plain.getContents() instanceof TranslatableContents p && p.getArgs()[0] instanceof Component n2 && n2.getString().equals(common.getDisplayName().getString()),
                        "a plain vampire is not named");
                clean(a, b);
                a.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
                b.setPos(helper.absoluteVec(new Vec3(1.5, 1, 3.0)));
            }
        } finally {
            pvp(helper, false);
            clean(original, common, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aForcedFeedFromAnOriginalDoesNotUseTheLineWording(GameTestHelper helper) {
        TestPlayers.TalkingDuelist original = vampire(helper, Species.ORIGINAL);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            pvp(helper, true);
            target.setHealth(1f);
            helper.assertTrue(ACTION.trigger(original, target) == null, "the feed was refused");
            Component said = target.message(GiveBloodAction.RECEIVED);
            helper.assertTrue(said != null && said.getContents() instanceof TranslatableContents t && t.getArgs()[0] instanceof Component n
                    && n.getString().equals(original.getDisplayName().getString()) && !(n.getContents() instanceof TranslatableContents nt && nt.getKey().contains("line")),
                    "a forced feed used the line wording: " + said);
        } finally {
            pvp(helper, false);
            clean(original, target);
        }
        helper.succeed();
    }

    // ---- the blood's gifts ----

    private static MobEffectInstance regen(ServerPlayer p) {
        return p.getEffect(MobEffects.REGENERATION);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDrinkGivesNinetySecondsOfRegenerationIIWithItsIcon(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        try {
            helper.assertTrue(BloodBottles.apply(p, BloodBottles.UNSTAMPED) == net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.MARK, "a plain bottle did not mark");
            MobEffectInstance r = regen(p);
            helper.assertTrue(r != null && r.getAmplifier() == 1 && r.getDuration() == 90 * 20 && r.showIcon() && !r.isVisible(), "Regeneration II for 90 s with its icon and no particles: " + r);
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRegenerationPulseComesOncePerTenMinutesWhateverTheSource(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        try {
            pvp(helper, true);
            helper.assertTrue(TurningHandler.drankVampireBlood(p) && regen(p) != null, "the first drink gave no pulse");
            long first = ActionClock.left(p, VampireBloodBuff.REGEN_CLOCK);
            helper.assertTrue(first == SpeciesConfig.vampireBloodRegenCooldownSeconds() * 20L, "the clock reads " + first);
            // a bottle, then a forced feed from someone else, each after the last one is gone: no new pulse
            for (int i = 0; i < 2; i++) {
                p.removeAllEffects();
                BloodlineAccess.set(p, BloodlineState.NONE); // the window is spent, so the next drink changes the state again
                if (i == 0) {
                    helper.assertTrue(BloodBottles.apply(p, BloodBottles.UNSTAMPED) == net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect.MARK && marked(p), "the bottle did not mark");
                } else {
                    p.setHealth(1f);
                    helper.assertTrue(ACTION.trigger(giver, p) == null && marked(p), "the feed did not mark");
                }
                helper.assertTrue(regen(p) == null, "a drink " + i + " within the ten minutes gave Regeneration again");
            }
            // once the ten minutes are over the next drink gives it again
            p.removeAllEffects();
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.setData(SpeciesRegistries.ACTION_READY, new HashMap<String, Long>());
            helper.assertTrue(TurningHandler.drankVampireBlood(p) && regen(p) != null && regen(p).getDuration() == 90 * 20, "no pulse after the ten minutes");
        } finally {
            pvp(helper, false);
            clean(p, giver);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownIsAConfigAndZeroMeansEveryDrink(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        try {
            SpeciesConfig.overrideVampireBloodRegenCooldown(0);
            for (int i = 0; i < 2; i++) {
                p.removeAllEffects();
                BloodlineAccess.set(p, BloodlineState.NONE);
                helper.assertTrue(TurningHandler.drankVampireBlood(p) && regen(p) != null, "drink " + i + " gave no pulse with a zero cooldown");
            }
            SpeciesConfig.overrideVampireBloodRegenCooldown(60);
            p.removeAllEffects();
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.setData(SpeciesRegistries.ACTION_READY, new HashMap<String, Long>());
            TurningHandler.drankVampireBlood(p);
            helper.assertTrue(ActionClock.left(p, VampireBloodBuff.REGEN_CLOCK) == 60 * 20, "the clock does not follow the config: " + ActionClock.left(p, VampireBloodBuff.REGEN_CLOCK));
        } finally {
            SpeciesConfig.overrideVampireBloodRegenCooldown(null);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHungerEaseLastsTheWholeWindowAndTheSwitchTurnsTheGiftsOff(GameTestHelper helper) {
        TestPlayers.TalkingDuelist p = human(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() + 100000).build());
            p.getFoodData().setExhaustion(1.0f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(Math.abs(p.getFoodData().getExhaustionLevel() - 0.85f) < 0.001f, "exhaustion is " + p.getFoodData().getExhaustionLevel());
            helper.assertTrue(regen(p) == null, "the tick gave Regeneration: the pulse is only at the drink");
            p.getFoodData().setExhaustion(0.05f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(p.getFoodData().getExhaustionLevel() == 0f, "exhaustion went below zero or was not eased");
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().vampireBloodUntil(helper.getLevel().getGameTime() - 1).build());
            p.getFoodData().setExhaustion(1.0f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(p.getFoodData().getExhaustionLevel() == 1.0f, "the ease outlived the blood");
            BloodlineAccess.set(p, BloodlineState.NONE);
            SpeciesConfig.overrideVampireBloodBuff(false);
            TurningHandler.drankVampireBlood(p);
            helper.assertTrue(regen(p) == null && marked(p), "the switch did not turn the pulse off, or the blood stopped marking");
            p.getFoodData().setExhaustion(1.0f);
            VampireBloodBuff.tick(p);
            helper.assertTrue(p.getFoodData().getExhaustionLevel() == 1.0f, "the switch did not turn the hunger ease off");
        } finally {
            SpeciesConfig.overrideVampireBloodBuff(null);
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDrinkThatMarksNobodyGivesNoRegeneration(GameTestHelper helper) {
        TestPlayers.TalkingDuelist vampire = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist turning = human(helper);
        try {
            helper.assertTrue(!TurningHandler.drankVampireBlood(vampire) && regen(vampire) == null, "a vampire got the pulse");
            BloodlineAccess.set(turning, BloodlineState.NONE.toBuilder().turningTicksLeft(5000).pendingSire(UUID.randomUUID()).build());
            helper.assertTrue(!TurningHandler.drankVampireBlood(turning) && regen(turning) == null, "a player already in the turning got the pulse");
        } finally {
            clean(vampire, turning);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCooldownSurvivesDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ActionClock.start(p, ExtraActions.GIVE_BLOOD, 100);
        ActionClock.startSilent(p, VampireBloodBuff.REGEN_CLOCK, 200);
        ServerPlayer reborn = TestPlayers.createMortal(helper);
        reborn.copyAttachmentsFrom(p, true);
        helper.assertTrue(ActionClock.left(reborn, ExtraActions.GIVE_BLOOD) == 100, "the reborn player's clock reads " + ActionClock.left(reborn, ExtraActions.GIVE_BLOOD));
        helper.assertTrue(ActionClock.left(reborn, VampireBloodBuff.REGEN_CLOCK) == 200, "dying reset the Regeneration clock");
        helper.assertTrue(ActionClock.ready(p, ExtraActions.BOTTLE_BLOOD), "one clock holds up another action");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondTryWaitsForTheCooldown(GameTestHelper helper) {
        TestPlayers.TalkingDuelist giver = vampire(helper, Species.NONE);
        TestPlayers.TalkingDuelist target = human(helper);
        try {
            target.setShiftKeyDown(true);
            helper.assertTrue(ACTION.trigger(giver, target) == null, "the first feed was refused");
            helper.assertTrue(GiveBloodAction.COOLDOWN.equals(key(ACTION.trigger(giver, target))), "a second feed came at once");
        } finally {
            clean(giver, target);
        }
        helper.succeed();
    }
}
