package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.named;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.place;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Set;
import net.favouriteless.enchanted.api.altar.SimplePowerPosHolder;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import de.teamlapen.vampirism.api.VampirismRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterInstinct;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.MarkTracker;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.BloodThirst;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallFollowers;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.Compulsion;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.species.witch.Siphoning;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;

/**
 * Phase 12c Task 2, the skills that change a number of a rule of the species part (no Werewolves needed). Each test sets the same
 * player's state with and without the skill and compares what the rule gives. A skill only counts while the tree of the player's
 * race lists it, so every player here has the species whose tree has the skill.
 */
@PrefixGameTestTemplate(false)
public final class SkillRuleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceKey<DamageType> SUN_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"));

    private SkillRuleGameTests() {}

    private static void own(ServerPlayer p, String... skills) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
    }

    private static ServerPlayer player(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).turnedAt(1).build());
        return p;
    }

    private static void hit(LivingEntity victim, DamageSource source, float amount) {
        net.minecraft.server.MinecraftServer server = victim.getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            victim.invulnerableTime = 0;
            victim.hurt(source, amount);
        } finally {
            server.setPvpAllowed(pvp);
        }
    }

    // ---- S-VAM-3 Silver Tongue ----

    private static Villager villager(GameTestHelper helper, int x) {
        Villager v = helper.spawn(EntityType.VILLAGER, new Vec3(x + 0.5, 2, 4.5));
        v.setNoAi(true);
        return v;
    }

    private static void gaze(Entity a, Entity b) {
        a.lookAt(EntityAnchorArgument.Anchor.EYES, b.getEyePosition());
        a.setYHeadRot(a.getYRot());
        b.lookAt(EntityAnchorArgument.Anchor.EYES, a.getEyePosition());
        b.setYHeadRot(b.getYRot());
    }

    /** The real path: the sneak click opens the choice, the choice runs the command's code. The villager is 3 blocks away. */
    private static long[] compel(GameTestHelper helper, ServerPlayer caster, Villager v, String choice) {
        Compulsion.clearGuard(caster.getUUID());
        gaze(caster, v);
        Compulsion.onInteract(new PlayerInteractEvent.EntityInteract(caster, InteractionHand.MAIN_HAND, v));
        String token = Compulsion.pendingChoice(caster.getUUID(), v.getUUID()).orElseThrow(() -> new AssertionError("not compelled"));
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(Compulsion.choose(caster, token, choice) == null, choice + " failed");
        return new long[] {now, v.hasData(SpeciesRegistries.VILLAGER_DISCOUNT) ? v.getData(SpeciesRegistries.VILLAGER_DISCOUNT).expires() : -1,
                CallFollowers.followUntil(v.getUUID())};
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverTongueLengthensCompulsionAndTheDiscount(GameTestHelper helper) {
        ServerPlayer caster = named(helper, helper.getLevel(), "caster");
        species(caster, Species.ORIGINAL);
        caster.setShiftKeyDown(true);
        place(helper, caster, 4);
        Villager v = villager(helper, 7);
        Villager w = villager(helper, 2);
        try {
            long[] plain = compel(helper, caster, v, "discount");
            helper.assertTrue(plain[1] - plain[0] == 24000, "a day without the skill: " + (plain[1] - plain[0]));
            int rep = v.getGossips().getReputation(caster.getUUID(), t -> t == GossipType.MAJOR_POSITIVE);
            helper.assertTrue(rep == 100, "reputation " + rep + " without the skill");
            long[] plainFollow = compel(helper, caster, w, "follow");
            helper.assertTrue(plainFollow[2] - plainFollow[0] == 6000, "follow " + (plainFollow[2] - plainFollow[0]));
            own(caster, "S-VAM-3");
            Villager x = villager(helper, 6);
            Villager y = villager(helper, 1);
            long[] skilled = compel(helper, caster, x, "discount");
            helper.assertTrue(skilled[1] - skilled[0] == 36000, "a day and a half with the skill: " + (skilled[1] - skilled[0]));
            // a villager keeps 20 of the major kind and up to 25 of the minor kind: worth 125 reputation, not the planned 150
            int skilledRep = x.getGossips().getReputation(caster.getUUID(), t -> t == GossipType.MAJOR_POSITIVE || t == GossipType.MINOR_POSITIVE);
            helper.assertTrue(skilledRep == 125, "reputation " + skilledRep + " with the skill (the gossip caps make it 125)");
            long[] skilledFollow = compel(helper, caster, y, "follow");
            helper.assertTrue(skilledFollow[2] - skilledFollow[0] == 9000, "follow with the skill " + (skilledFollow[2] - skilledFollow[0]));
            // the reach is still the vanilla 4 blocks: no skill makes the click reach further
            Villager far = villager(helper, 9);
            Compulsion.clearGuard(caster.getUUID());
            gaze(caster, far);
            Compulsion.onInteract(new PlayerInteractEvent.EntityInteract(caster, InteractionHand.MAIN_HAND, far));
            helper.assertTrue(Compulsion.pendingChoice(caster.getUUID(), far.getUUID()).isEmpty(), "compelled from 5 blocks with the skill");
        } finally {
            cleanUp(caster);
            Compulsion.clearGuard(caster.getUUID());
        }
        helper.succeed();
    }
    // ---- S-ORI-2 Bloodmaker's Yield ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodmakersYieldFillsTwoBottles(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer original = RecordingPlayer.create(helper);
        net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place(helper, original);
        try {
            BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).turnedAt(1).build());
            start(helper, "original_blood", original, List.of(new ItemStack(Items.GLASS_BOTTLE)));
            int without = dropped(helper, "supernaturalbloodlines:original_blood");
            clearDropped(helper);
            own(original, "S-ORI-2");
            start(helper, "original_blood", original, List.of(new ItemStack(Items.GLASS_BOTTLE)));
            int with = dropped(helper, "supernaturalbloodlines:original_blood");
            helper.assertTrue(without == 1 && with == 2, "bottles: " + without + " without the skill, " + with + " with it");
        } finally {
            clearDropped(helper);
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove(helper, original);
            BloodlineAccess.set(original, BloodlineState.NONE);
        }
        helper.succeed();
    }

    // ---- S-ORI-3 Sire's Call ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void siresCallHalvesTheCooldown(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.ORIGINAL);
        helper.assertTrue(SireCall.cooldownTicks(p) == 12000, "base cooldown " + SireCall.cooldownTicks(p));
        own(p, "S-ORI-3");
        helper.assertTrue(SireCall.cooldownTicks(p) == 6000, "cooldown with the skill " + SireCall.cooldownTicks(p));
        helper.succeed();
    }

    /** Sire's Call (S-ORI-3) through the call itself: a call 7,000 ticks after the last one is refused, and goes through with the skill. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void siresCallShortensTheRealCooldown(GameTestHelper helper) {
        ServerPlayer caller = named(helper, helper.getLevel(), "caller");
        ServerPlayer sired = named(helper, helper.getLevel(), "sired");
        try {
            SireRecorder.record(caller, sired);
            net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.online(caller, sired);
            species(caller, Species.ORIGINAL);
            long now = helper.getLevel().getGameTime();
            caller.setData(SpeciesRegistries.LAST_CALL, now - 7000);
            var refused = SireCall.call(caller);
            helper.assertTrue(refused.error() != null && refused.error().getString().length() > 0 && refused.players() == 0, "a call 7,000 ticks after the last one went through without the skill");
            own(caller, "S-ORI-3");
            helper.assertTrue(SireCall.call(caller).error() == null, "the call was still refused with Sire's Call");
            helper.assertTrue(caller.getData(SpeciesRegistries.LAST_CALL) == now, "the cooldown did not start again");
            caller.setData(SpeciesRegistries.LAST_CALL, now - 5999);
            helper.assertTrue(SireCall.call(caller).error() != null, "the cooldown ended a tick early");
        } finally {
            SireCall.requests().forget(caller.getUUID());
            cleanUp(caller, sired);
        }
        helper.succeed();
    }

    // ---- S-HYB-1 Steady Thirst ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void steadyThirstFallsHalfAsFast(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.HYBRID);
        helper.assertTrue(BloodThirst.fallTicks(p) == BloodThirst.FALL_TICKS, "base " + BloodThirst.fallTicks(p));
        own(p, "S-HYB-1");
        helper.assertTrue(BloodThirst.fallTicks(p) == 2 * BloodThirst.FALL_TICKS, "with the skill " + BloodThirst.fallTicks(p));
        helper.succeed();
    }

    // ---- siphoner: S-SIP-1 .. S-SIP-5 ----

    private static Cow cow(GameTestHelper helper, int x) {
        Cow c = helper.spawn(EntityType.COW, new Vec3(x + 0.5, 2, 4.5));
        c.setNoAi(true);
        return c;
    }

    private static ServerPlayer siphoner(GameTestHelper helper, int power) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.SIPHONER).siphonedPower(power).build());
        return p;
    }

    private static int power(ServerPlayer p) {
        return BloodlineAccess.get(p).siphonedPower();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deepDrawRaisesTheCap(GameTestHelper helper) {
        ServerPlayer p = siphoner(helper, Siphoning.CAP);
        helper.assertTrue(!Siphoning.siphonEntity(p, cow(helper, 2)), "a full siphoner siphoned");
        own(p, "S-SIP-1");
        helper.assertTrue(Siphoning.cap(p) == 1500 && Siphoning.siphonEntity(p, cow(helper, 3)) && power(p) == 1050, "power " + power(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void quickDrawGivesAHalfMore(GameTestHelper helper) {
        ServerPlayer p = siphoner(helper, 0);
        helper.assertTrue(Siphoning.siphonEntity(p, cow(helper, 2)) && power(p) == 50, "base power " + power(p));
        own(p, "S-SIP-2");
        helper.assertTrue(Siphoning.siphonEntity(p, cow(helper, 3)) && power(p) == 125, "power with the skill " + power(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lifeSiphonHeals(GameTestHelper helper) {
        ServerPlayer p = siphoner(helper, 0);
        p.setHealth(10);
        helper.assertTrue(Siphoning.siphonEntity(p, cow(helper, 2)) && p.getHealth() == 10, "healed without the skill: " + p.getHealth());
        own(p, "S-SIP-3");
        helper.assertTrue(Siphoning.siphonEntity(p, cow(helper, 3)) && Math.abs(p.getHealth() - 10.5) < 1e-4, "health with the skill " + p.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void voidHungerWeakensAVampire(GameTestHelper helper) {
        ServerPlayer p = siphoner(helper, 0);
        Mob first = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        Mob second = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        for (Mob m : List.of(first, second)) {
            m.setNoAi(true);
            Vec3 at = helper.absoluteVec(new Vec3(3.5, 2, 4.5));
            m.moveTo(at.x, at.y, at.z);
            helper.getLevel().addFreshEntity(m);
        }
        helper.assertTrue(Siphoning.siphonEntity(p, first) && !first.hasEffect(MobEffects.WEAKNESS), "weakened without the skill");
        own(p, "S-SIP-5");
        helper.assertTrue(Siphoning.siphonEntity(p, second) && second.hasEffect(MobEffects.WEAKNESS), "not weakened with the skill");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void spentWiselyStretchesThePool(GameTestHelper helper) {
        ServerPlayer p = siphoner(helper, 90);
        RiteTestSupport.place(helper, p); // the cost hook finds the caster in the level
        try {
            spentWisely(helper, p);
        } finally {
            RiteTestSupport.remove(helper, p);
        }
        helper.succeed();
    }

    private static void spentWisely(GameTestHelper helper, ServerPlayer p) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        int plain = RiteCost.begin(helper.getLevel(), p.getUUID(), pos, new SimplePowerPosHolder(pos), 100, true, null);
        RiteCost.discard();
        own(p, "S-SIP-4");
        int skilled = RiteCost.begin(helper.getLevel(), p.getUUID(), pos, new SimplePowerPosHolder(pos), 100, true, null);
        RiteCost.commit();
        helper.assertTrue(plain == 10 && skilled == 0, "the altar pays " + plain + " without the skill and " + skilled + " with it");
        helper.assertTrue(power(p) == 0, "the pool is " + power(p) + ": 90 pays the whole 100 at x0.9 and is spent");
    }

    // ---- witch: S-WIT-1, S-WIT-2, S-WIT-6 ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void steadyCircleAndAncestralVoiceCutTheCost(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.WITCH);
        RiteTestSupport.place(helper, p);
        try {
            costs(helper, p);
        } finally {
            RiteTestSupport.remove(helper, p);
        }
        helper.succeed();
    }

    private static void costs(GameTestHelper helper, ServerPlayer p) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        var lineage = type(helper, "rite_of_lineage_reveal");
        var other = type(helper, "immortality");
        helper.assertTrue(cost(helper, p, pos, null) == 100, "base cost");
        own(p, "S-WIT-1");
        helper.assertTrue(cost(helper, p, pos, null) == 95 && cost(helper, p, pos, other) == 95, "Steady Circle: " + cost(helper, p, pos, null));
        if (lineage == null) {
            return; // the Rites of Lineage need Werewolves (their data is conditional)
        }
        own(p, "S-WIT-6");
        helper.assertTrue(cost(helper, p, pos, lineage) == 50, "Ancestral Voice on a Rite of Lineage: " + cost(helper, p, pos, lineage));
        helper.assertTrue(cost(helper, p, pos, other) == 100, "Ancestral Voice on another rite: " + cost(helper, p, pos, other));
        own(p, "S-WIT-1", "S-WIT-6");
        helper.assertTrue(cost(helper, p, pos, lineage) == 48, "both on a Rite of Lineage: " + cost(helper, p, pos, lineage));
    }

    private static int cost(GameTestHelper helper, ServerPlayer p, BlockPos pos, net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType type) {
        int cost = RiteCost.begin(helper.getLevel(), p.getUUID(), pos, new SimplePowerPosHolder(pos), 100, false, type);
        RiteCost.discard();
        return cost;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wideCircleWidensTheAltar(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.WITCH);
        BlockPos altar = helper.absolutePos(new BlockPos(4, 2, 4));
        Vec3 at = Vec3.atBottomCenterOf(altar);
        p.setPos(at.x, at.y, at.z);
        helper.getLevel().addNewPlayer(p); // the altar looks for witches among the players of the level
        try {
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), altar, 12) == 18, "base range " + WitchAltarBonus.rangeFor(helper.getLevel(), altar, 12));
            own(p, "S-WIT-2");
            helper.assertTrue(WitchAltarBonus.rangeFor(helper.getLevel(), altar, 12) == 21, "range with the skill " + WitchAltarBonus.rangeFor(helper.getLevel(), altar, 12));
        } finally {
            RiteTestSupport.remove(helper, p);
        }
        helper.succeed();
    }

    // ---- hunters: S-HUN-1, -2, -3, -5 ----

    private static Mob vampireMob(GameTestHelper helper) {
        Mob m = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        m.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 2, 4.5));
        m.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(m);
        return m;
    }

    private static ServerPlayer hunter(GameTestHelper helper, BloodlineState.Builder b) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(p, b.species(Species.HUNTER_OF_THE_FIVE).build());
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void steadyAimAddsFivePercentAgainstPrey(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, BloodlineState.NONE.toBuilder());
        Mob a = vampireMob(helper); // the same mob twice: a spawned vampire may wear armor
        a.setHealth(a.getMaxHealth());
        float before = a.getHealth();
        hit(a, p.damageSources().playerAttack(p), 4f);
        double plain = before - a.getHealth();
        own(p, "S-HUN-1");
        a.setHealth(a.getMaxHealth());
        before = a.getHealth();
        hit(a, p.damageSources().playerAttack(p), 4f);
        double skilled = before - a.getHealth();
        helper.assertTrue(plain > 0 && skilled > plain * 1.02 && skilled < plain * 1.1, "damage " + plain + " without the skill, " + skilled + " with it");
        Cow cow = cow(helper, 7);
        before = cow.getHealth();
        hit(cow, p.damageSources().playerAttack(p), 4f);
        helper.assertTrue(Math.abs((before - cow.getHealth()) - 4f) < 1e-3, "damage to a cow changed: " + (before - cow.getHealth()));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void huntersEyeSensesFurther(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, BloodlineState.NONE.toBuilder());
        helper.assertTrue(HunterInstinct.range(p) == 24, "base range " + HunterInstinct.range(p));
        own(p, "S-HUN-2");
        helper.assertTrue(HunterInstinct.range(p) == 32, "range with the skill " + HunterInstinct.range(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void patientHunterWaitsFiveDays(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, BloodlineState.NONE.toBuilder().lastVampireKill(1000));
        long fourDays = 1000 + 4 * 24000L;
        HunterInstinct.instinct(p, fourDays);
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "no instinct after 4 days without the skill");
        p.removeAllEffects();
        own(p, "S-HUN-3");
        HunterInstinct.instinct(p, fourDays);
        helper.assertTrue(!p.hasEffect(MobEffects.WEAKNESS), "instinct after 4 days with the skill");
        HunterInstinct.instinct(p, 1000 + 5 * 24000L + 1);
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "no instinct after 5 days with the skill");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void markMasteryRaisesTheMarkCaps(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, BloodlineState.NONE.toBuilder().markKills(MarkTracker.CAP));
        var attack = p.getAttribute(Attributes.ATTACK_DAMAGE);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(attack.getModifier(MarkTracker.MODIFIER_ID).amount() - 0.20) < 1e-9
                && Math.abs(speed.getModifier(MarkTracker.MODIFIER_ID).amount() - 0.10) < 1e-9, "the mark at the cap is not +20% / +10%");
        own(p, "S-HUN-5");
        helper.assertTrue(Math.abs(attack.getModifier(MarkTracker.MODIFIER_ID).amount() - 0.25) < 1e-9
                && Math.abs(speed.getModifier(MarkTracker.MODIFIER_ID).amount() - 0.125) < 1e-9,
                "with the skill: " + attack.getModifier(MarkTracker.MODIFIER_ID).amount() + " / " + speed.getModifier(MarkTracker.MODIFIER_ID).amount());
        own(p);
        helper.assertTrue(Math.abs(attack.getModifier(MarkTracker.MODIFIER_ID).amount() - 0.20) < 1e-9, "the caps did not go back");
        helper.succeed();
    }

    // ---- S-VAM-7 Elder's Skin: Vampirism's own less_sundamage skill (owner ruling, 13d) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eldersSkinEnablesVampirismsLessSundamageSkill(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        var skill = (de.teamlapen.vampirism.api.entity.player.skills.ISkill<?>) VampirismRegistries.SKILL.get().get(ResourceLocation.parse("vampirism:less_sundamage"));
        helper.assertTrue(skill != null, "vampirism:less_sundamage is not registered (a Vampirism update?)");
        var handler = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler();
        helper.assertTrue(!handler.isSkillEnabled(skill), "setup: the player already has the Vampirism skill");
        DamageSource sun = new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SUN_DAMAGE));
        p.setHealth(p.getMaxHealth());
        hit(p, sun, 4f);
        double plain = p.getMaxHealth() - p.getHealth();
        int pointsBefore = handler.getLeftSkillPoints();
        own(p, "S-VAM-7");
        helper.assertTrue(handler.getLeftSkillPoints() == pointsBefore, "the grant cost Vampirism skill points: " + pointsBefore + " -> " + handler.getLeftSkillPoints());
        helper.assertTrue(handler.isSkillEnabled(skill), "owning S-VAM-7 did not enable Vampirism's less_sundamage");
        // our own x0.75 on the damage event is gone: what softens the sun now is Vampirism's skill (its attribute, read by its own sun code)
        p.setHealth(p.getMaxHealth());
        hit(p, sun, 4f);
        double skilled = p.getMaxHealth() - p.getHealth();
        helper.assertTrue(plain > 0 && skilled <= plain + 1e-6 && skilled > plain * 0.9, "sun damage " + plain + " without the skill, " + skilled + " with it (our x0.75 rule must be gone)");
        helper.succeed();
    }

    private static de.teamlapen.vampirism.api.entity.player.skills.ISkill<?> lessSundamage() {
        return (de.teamlapen.vampirism.api.entity.player.skills.ISkill<?>) VampirismRegistries.SKILL.get().get(ResourceLocation.parse("vampirism:less_sundamage"));
    }

    private static ServerPlayer vampirePlayer(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static boolean has(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler().isSkillEnabled(lessSundamage());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eldersSkinIsTakenBackWhenTheLadderSkillGoes(GameTestHelper helper) {
        ServerPlayer p = vampirePlayer(helper);
        own(p, "S-VAM-7");
        helper.assertTrue(has(p), "the skill was not enabled");
        helper.assertTrue(SupernaturalAccess.get(p).vampirismGranted().contains("vampirism:less_sundamage"), "the enabled skill was not recorded");
        own(p); // a respec, or a lost race: the ladder skill is gone
        helper.assertTrue(!has(p), "Vampirism's less_sundamage stayed after the ladder skill went");
        helper.assertTrue(SupernaturalAccess.get(p).vampirismGranted().isEmpty(), "the record was not cleared");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eldersSkinNeverTakesASkillThePlayerBoughtThemselves(GameTestHelper helper) {
        ServerPlayer p = vampirePlayer(helper);
        VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler().enableSkill(
                (de.teamlapen.vampirism.api.entity.player.skills.ISkill) lessSundamage(), true);
        helper.assertTrue(has(p), "setup: the player could not get the skill");
        own(p, "S-VAM-7");
        helper.assertTrue(SupernaturalAccess.get(p).vampirismGranted().isEmpty(), "the player's own skill was recorded as ours");
        own(p);
        helper.assertTrue(has(p), "the player's own less_sundamage was taken away");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eldersSkinIsTakenBackWhenTheSupernaturalPartIsOff(GameTestHelper helper) {
        ServerPlayer p = vampirePlayer(helper);
        own(p, "S-VAM-7");
        helper.assertTrue(has(p), "the skill was not enabled");
        net.ofatech.supernaturalbloodlines.core.StalePartModifiers.strip(p, Set.of()); // what a login does with the part off
        helper.assertTrue(!has(p), "Vampirism's less_sundamage stayed with the supernatural part off");
        helper.assertTrue(SupernaturalAccess.get(p).vampirismGranted().isEmpty(), "the record was not cleared");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eldersSkinIsNotSoldToAPlayerWhoAlreadyHasTheVampirismSkill(GameTestHelper helper) {
        ServerPlayer p = vampirePlayer(helper);
        VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler().enableSkill(
                (de.teamlapen.vampirism.api.entity.player.skills.ISkill) lessSundamage(), true);
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().sp(50).build());
        var r = net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillService.buy(p, "S-VAM-7");
        helper.assertTrue(r.refusal() == net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules.Refusal.COVERED, "refusal was " + r.refusal());
        helper.assertTrue(SupernaturalAccess.get(p).sp() == 50, "skill points were spent for nothing");
        helper.succeed();
    }}
