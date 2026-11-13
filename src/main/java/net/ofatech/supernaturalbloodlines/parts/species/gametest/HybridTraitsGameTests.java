package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.BloodThirst;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.FormControl;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.HybridTraits;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.Venom;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Hybrid traits, thirst, venom and the sire bond (R42, R42v, R43). Registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class HybridTraitsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final double EPS = 1e-9;
    private static final long FULL = HybridTraits.AGE_TICKS;

    private HybridTraitsGameTests() {
    }

    private static BloodlineState species(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).turnedAt(1).build();
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static ServerPlayer werewolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static AttributeModifier modifier(ServerPlayer p, Holder<Attribute> a) {
        return p.getAttribute(a).getModifier(HybridTraits.MODIFIER_ID);
    }

    private static boolean amount(ServerPlayer p, Holder<Attribute> a, double expected) {
        AttributeModifier m = modifier(p, a);
        return m != null && Math.abs(m.amount() - expected) < EPS;
    }

    /** ServerPlayer.hurt refuses player-versus-player damage while the server has PvP off, as the test server does. */
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

    private static void bite(ServerPlayer attacker, LivingEntity victim) {
        hit(victim, attacker.damageSources().playerAttack(attacker), 4f);
    }

    private static boolean click(ServerPlayer p) {
        PlayerInteractEvent.RightClickItem click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(click);
        return click.isCanceled() && click.getCancellationResult().consumesAction();
    }

    private static ItemStack bloodBottle(int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:blood_bottle")), count);
    }

    // ---- attributes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridGetsSpeedAndDamage(GameTestHelper helper) {
        for (Species s : List.of(Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID)) {
            ServerPlayer p = TestPlayers.createDuelist(helper);
            try {
                BloodlineAccess.set(p, species(s));
                helper.assertTrue(modifier(p, Attributes.MOVEMENT_SPEED) != null && modifier(p, Attributes.ATTACK_DAMAGE) != null,
                        s + ": the species change did not apply the traits");
                HybridTraits.sync(p, 1 + FULL);
                helper.assertTrue(amount(p, Attributes.MOVEMENT_SPEED, 0.02) && amount(p, Attributes.ATTACK_DAMAGE, 2.0),
                        s + ": full age is " + modifier(p, Attributes.MOVEMENT_SPEED) + " / " + modifier(p, Attributes.ATTACK_DAMAGE));
                HybridTraits.sync(p, 1 + FULL / 2);
                helper.assertTrue(amount(p, Attributes.MOVEMENT_SPEED, 0.01) && amount(p, Attributes.ATTACK_DAMAGE, 1.0),
                        s + ": half age is " + modifier(p, Attributes.MOVEMENT_SPEED) + " / " + modifier(p, Attributes.ATTACK_DAMAGE));
                HybridTraits.sync(p, 1 + FULL * 5);
                helper.assertTrue(amount(p, Attributes.MOVEMENT_SPEED, 0.02) && amount(p, Attributes.ATTACK_DAMAGE, 2.0), s + ": age is not capped");
                helper.assertTrue(p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().stream().filter(m -> m.id().equals(HybridTraits.MODIFIER_ID)).count() == 1,
                        s + ": the modifier was stacked");
            } finally {
                reset(p);
            }
        }
        ServerPlayer witch = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(witch, species(Species.WITCH));
        HybridTraits.sync(witch, 1 + FULL);
        helper.assertTrue(modifier(witch, Attributes.MOVEMENT_SPEED) == null && modifier(witch, Attributes.ATTACK_DAMAGE) == null, "a witch got hybrid traits");
        ServerPlayer unset = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(unset, BloodlineState.NONE.toBuilder().species(Species.HYBRID).turnedAt(0).build());
        HybridTraits.sync(unset, 5);
        helper.assertTrue(amount(unset, Attributes.ATTACK_DAMAGE, 2.0), "an unset turnedAt did not count as grown: " + modifier(unset, Attributes.ATTACK_DAMAGE));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridTraitsRemovedOnCure(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        BloodlineAccess.set(p, species(Species.HYBRID));
        helper.assertTrue(modifier(p, Attributes.ATTACK_DAMAGE) != null, "no traits to remove");
        BloodlineAccess.set(p, BloodlineState.NONE); // Cleansing Blood / the Cure
        helper.assertTrue(modifier(p, Attributes.MOVEMENT_SPEED) == null && modifier(p, Attributes.ATTACK_DAMAGE) == null,
                "the traits outlived the species");
        helper.succeed();
    }

    // ---- thirst ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thirstFallsAndWeakens(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        try {
            BloodlineAccess.set(p, species(Species.HYBRID).toBuilder().thirst(2).build());
            BloodThirst.decay(p);
            helper.assertTrue(BloodlineAccess.get(p).thirst() == 1, "thirst is " + BloodlineAccess.get(p).thirst());
            BloodThirst.decay(p);
            BloodThirst.decay(p);
            helper.assertTrue(BloodlineAccess.get(p).thirst() == 0, "thirst fell below 0: " + BloodlineAccess.get(p).thirst());
            BloodThirst.refreshEffects(p);
            MobEffectInstance weak = p.getEffect(MobEffects.WEAKNESS);
            helper.assertTrue(weak != null && weak.getAmplifier() == 0 && !p.hasEffect(MobEffects.DAMAGE_BOOST), "no Weakness I at 0 thirst: " + weak);

            ServerPlayer fed = TestPlayers.createDuelist(helper);
            BloodlineAccess.set(fed, species(Species.HYBRID).toBuilder().thirst(15).build());
            BloodThirst.refreshEffects(fed);
            MobEffectInstance strong = fed.getEffect(MobEffects.DAMAGE_BOOST);
            helper.assertTrue(strong != null && strong.getAmplifier() == 0 && !fed.hasEffect(MobEffects.WEAKNESS), "no Strength I at 15 thirst: " + strong);

            ServerPlayer middle = TestPlayers.createDuelist(helper);
            BloodlineAccess.set(middle, species(Species.HYBRID).toBuilder().thirst(14).build());
            BloodThirst.refreshEffects(middle);
            helper.assertTrue(!middle.hasEffect(MobEffects.WEAKNESS) && !middle.hasEffect(MobEffects.DAMAGE_BOOST), "thirst 14 gives an effect");

            ServerPlayer human = TestPlayers.createDuelist(helper);
            BloodlineAccess.set(human, BloodlineState.NONE.toBuilder().thirst(0).build());
            BloodThirst.decay(human);
            BloodThirst.refreshEffects(human);
            helper.assertTrue(!human.hasEffect(MobEffects.WEAKNESS), "a non-hybrid is subject to thirst");
            ServerPlayer full = TestPlayers.createDuelist(helper);
            BloodlineAccess.set(full, BloodlineState.NONE);
            BloodThirst.decay(full);
            helper.assertTrue(BloodlineAccess.get(full).thirst() == 20, "a non-hybrid thirsts");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void drinkingBloodQuenchesAHybrid(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        try {
            BloodlineAccess.set(p, species(Species.HYBRID).toBuilder().thirst(3).build());
            p.setItemInHand(InteractionHand.MAIN_HAND, bloodBottle(2));
            helper.assertTrue(click(p), "the click was not taken over");
            helper.assertTrue(BloodlineAccess.get(p).thirst() == 3 + BloodThirst.GAIN, "thirst is " + BloodlineAccess.get(p).thirst());
            helper.assertTrue(p.getMainHandItem().getCount() == 1 && p.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)), "bottle not used up / returned");
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().thirst(20).build());
            helper.assertTrue(!click(p) && p.getMainHandItem().getCount() == 1, "a full hybrid wasted a bottle");
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().thirst(18).build());
            click(p);
            helper.assertTrue(BloodlineAccess.get(p).thirst() == 20, "thirst is not capped at 20");
            ServerPlayer plain = werewolf(helper);
            plain.setItemInHand(InteractionHand.MAIN_HAND, bloodBottle(1));
            helper.assertTrue(!click(plain), "a plain werewolf drank");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- forced shift ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forcedFullMoonShiftIsCancelledForHybrids(GameTestHelper helper) {
        ServerPlayer hybrid = werewolf(helper);
        ServerPlayer plain = werewolf(helper);
        try {
            BloodlineAccess.set(hybrid, species(Species.HYBRID));
            var form = ModActions.BEAST_FORM.get();
            FormControl.enterTick(hybrid);
            boolean forced = post(hybrid, form);
            FormControl.leaveTick(hybrid);
            boolean voluntary = post(hybrid, form);
            FormControl.enterTick(plain);
            boolean plainForced = post(plain, form);
            FormControl.leaveTick(plain);
            helper.assertTrue(forced, "the forced shift was not cancelled");
            helper.assertTrue(!voluntary, "a voluntary shift was cancelled");
            helper.assertTrue(!plainForced, "a plain werewolf was spared the shift");
        } finally {
            reset(hybrid);
        }
        helper.succeed();
    }

    private static boolean post(ServerPlayer p, de.teamlapen.vampirism.api.entity.player.actions.IAction<?> action) {
        var event = new de.teamlapen.vampirism.api.event.ActionEvent.ActionActivatedEvent(
                VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), cast(action), 0, 0);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static de.teamlapen.vampirism.api.entity.player.actions.IAction cast(de.teamlapen.vampirism.api.entity.player.actions.IAction<?> a) {
        return a;
    }

    // ---- venom ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void venomPoisonsVampire(GameTestHelper helper) {
        ServerPlayer wolf = werewolf(helper);
        ServerPlayer victim = vampire(helper);
        bite(wolf, victim);
        MobEffectInstance venom = victim.getEffect(SpeciesRegistries.HYBRID_VENOM);
        helper.assertTrue(venom != null && venom.getDuration() > Venom.DURATION_TICKS - 5 && Venom.DURATION_TICKS == 12000, "no venom: " + venom);

        ServerPlayer human = TestPlayers.createDuelist(helper);
        bite(wolf, human);
        helper.assertTrue(!human.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a human was poisoned");

        ServerPlayer vampire2 = vampire(helper);
        ServerPlayer other = vampire(helper);
        bite(vampire2, other);
        helper.assertTrue(!other.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a vampire poisoned a vampire");

        ServerPlayer wolf2 = werewolf(helper);
        ServerPlayer wolfVictim = werewolf(helper);
        bite(wolf2, wolfVictim);
        helper.assertTrue(!wolfVictim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a werewolf was poisoned");

        ServerPlayer arrowVictim = vampire(helper);
        Arrow arrow = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
        arrow.setOwner(wolf);
        hit(arrowVictim, arrowVictim.damageSources().arrow(arrow, wolf), 4f);
        helper.assertTrue(!arrowVictim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "an arrow poisoned (only bites and melee do)");

        Mob vampireMob = (Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), 1, 1, 1);
        bite(wolf, vampireMob);
        helper.assertTrue(vampireMob.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a vampire mob was not poisoned");
        vampireMob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void plainWerewolfBiteAlsoPoisons(GameTestHelper helper) {
        ServerPlayer plain = werewolf(helper);
        helper.assertTrue(BloodlineAccess.get(plain).species() == Species.NONE, "not a plain werewolf");
        ServerPlayer victim = vampire(helper);
        bite(plain, victim);
        helper.assertTrue(victim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a plain werewolf player did not poison");

        ServerPlayer victim2 = vampire(helper);
        Mob beast = (Mob) helper.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("werewolves:werewolf_beast")), 1, 1, 1);
        hit(victim2, victim2.damageSources().mobAttack(beast), 4f);
        helper.assertTrue(victim2.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a werewolf mob did not poison");
        beast.discard();

        ServerPlayer hybrid = werewolf(helper);
        BloodlineAccess.set(hybrid, species(Species.HYBRID));
        ServerPlayer victim3 = vampire(helper);
        bite(hybrid, victim3);
        helper.assertTrue(victim3.hasEffect(SpeciesRegistries.HYBRID_VENOM), "a hybrid did not poison");
        reset(hybrid);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void venomKillsNonOriginalAfterTimer(GameTestHelper helper) {
        ServerPlayer victim = vampire(helper);
        float before = victim.getHealth();
        Venom.pulse(victim, 40);
        helper.assertTrue(before - victim.getHealth() > 0.5f && before - victim.getHealth() <= 1f, "no 1 damage per pulse: " + victim.getHealth());
        helper.assertTrue(!victim.hasEffect(MobEffects.CONFUSION), "hallucination too early");
        Venom.pulse(victim, 600);
        MobEffectInstance nausea = victim.getEffect(MobEffects.CONFUSION);
        helper.assertTrue(nausea != null && nausea.getAmplifier() == 1 && nausea.getDuration() == 100, "no Nausea II for 100 ticks: " + nausea);
        helper.assertTrue(!victim.isDeadOrDying(), "dead before the timer ran out");
        Venom.pulse(victim, 1);
        helper.assertTrue(victim.isDeadOrDying(), "survived the venom");
        DamageSource last = victim.getLastDamageSource();
        helper.assertTrue(last != null && last.is(Venom.DAMAGE_TYPE), "died of " + last);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 600)
    public static void venomEffectKillsOnItsOwn(GameTestHelper helper) {
        Mob cow = (Mob) helper.spawn(EntityType.COW, 1, 1, 1);
        cow.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, 85));
        // the cell can sit idle for a while before its entities tick, so wait for the end rather than for a fixed time
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.isDeadOrDying(), "the venom did not run its course: hp " + cow.getHealth() + ", effect " + cow.getEffect(SpeciesRegistries.HYBRID_VENOM));
            helper.assertTrue(cow.getLastDamageSource() != null && cow.getLastDamageSource().is(Venom.DAMAGE_TYPE), "died of " + cow.getLastDamageSource());
        });
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalOnlyWeakenedByVenom(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer original = vampire(helper);
            try {
                BloodlineAccess.set(original, species(s));
                Venom.pulse(original, 40);
                MobEffectInstance weak = original.getEffect(MobEffects.WEAKNESS);
                helper.assertTrue(weak != null && weak.getAmplifier() == 1, s + ": no Weakness II: " + weak);
                Venom.pulse(original, 1);
                helper.assertTrue(!original.isDeadOrDying() && original.getHealth() > 0, s + " was killed by the venom");
            } finally {
                reset(original);
            }
        }
        ServerPlayer victim = vampire(helper);
        BloodlineAccess.set(victim, species(Species.ORIGINAL));
        bite(werewolf(helper), victim);
        helper.assertTrue(victim.hasEffect(SpeciesRegistries.HYBRID_VENOM), "an Original is not poisoned at all");
        reset(victim);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void creativePlayersIgnoreVenom(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        Venom.pulse(p, 1);
        helper.assertTrue(!p.isDeadOrDying(), "venom killed a creative player");
        helper.succeed();
    }

    // ---- Hybrid's Blood ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyHybridBloodCuresVenom(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        p.addEffect(new MobEffectInstance(SpeciesRegistries.HYBRID_VENOM, Venom.DURATION_TICKS));
        p.removeEffectsCuredBy(EffectCures.MILK);
        helper.assertTrue(p.hasEffect(SpeciesRegistries.HYBRID_VENOM), "milk cured the venom");

        ItemStack original = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        original.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), UUID.randomUUID());
        original.finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(SpeciesRegistries.HYBRID_VENOM), "Original's Blood cured the venom");

        ItemStack hybrid = new ItemStack(SpeciesRegistries.HYBRID_BLOOD.get());
        helper.assertTrue(hybrid.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "cannot drink Hybrid's Blood while poisoned");
        p.setItemInHand(InteractionHand.MAIN_HAND, hybrid.finishUsingItem(helper.getLevel(), p));
        helper.assertTrue(!p.hasEffect(SpeciesRegistries.HYBRID_VENOM), "Hybrid's Blood did not cure the venom");
        helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle was not given back");
        helper.assertTrue(!new ItemStack(SpeciesRegistries.HYBRID_BLOOD.get()).use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "Hybrid's Blood can be wasted on a healthy drinker");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridBloodRiteBottlesAHybrid(GameTestHelper helper) {
        helper.assertTrue(type(helper, "hybrid_blood") != null, "hybrid_blood rite is missing");
        for (Species s : List.of(Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID)) {
            clearDropped(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            place(helper, target);
            try {
                BloodlineAccess.set(target, species(s));
                start(helper, "hybrid_blood", target, List.of(new ItemStack(Items.GLASS_BOTTLE)));
                helper.assertTrue(dropped(helper, "supernaturalbloodlines:hybrid_blood") == 1, s + ": no Hybrid's Blood was made");
            } finally {
                clearDropped(helper);
                remove(helper, target);
                reset(target);
            }
        }
        for (Species s : List.of(Species.NONE, Species.ORIGINAL, Species.WITCH)) {
            clearDropped(helper);
            RecordingPlayer target = RecordingPlayer.create(helper);
            place(helper, target);
            try {
                BloodlineAccess.set(target, species(s));
                start(helper, "hybrid_blood", target, List.of(new ItemStack(Items.GLASS_BOTTLE)));
                helper.assertTrue(target.receivedOnActionBar("supernaturalbloodlines.refusal.not_hybrid"), s + ": not told it is not a hybrid");
                helper.assertTrue(dropped(helper, "supernaturalbloodlines:hybrid_blood") == 0, s + ": blood was made");
                helper.assertTrue(dropped(helper, "minecraft:glass_bottle") == 1, s + ": the ingredient was not given back");
            } finally {
                clearDropped(helper);
                remove(helper, target);
                reset(target);
            }
        }
        helper.succeed();
    }

    // ---- sire bond ----

    private static void assertNoDamage(GameTestHelper helper, ServerPlayer sire, DamageSource source, String what) {
        float before = sire.getHealth();
        hit(sire, source, 5f);
        helper.assertTrue(sire.getHealth() == before, what + " hurt the sire (" + before + " -> " + sire.getHealth() + ")");
    }

    private static void assertDamage(GameTestHelper helper, ServerPlayer target, DamageSource source, String what) {
        float before = target.getHealth();
        hit(target, source, 5f);
        helper.assertTrue(target.getHealth() < before, what + " did no damage");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sireProtectedFromIndirectDamage(GameTestHelper helper) {
        ServerPlayer sire = TestPlayers.createDuelist(helper);
        ServerPlayer hybrid = TestPlayers.createDuelist(helper);
        try {
            BloodlineAccess.set(hybrid, species(Species.HYBRID).toBuilder().sire(sire.getUUID()).build());
            assertNoDamage(helper, sire, hybrid.damageSources().playerAttack(hybrid), "melee");

            Arrow arrow = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
            arrow.setOwner(hybrid);
            assertNoDamage(helper, sire, sire.damageSources().arrow(arrow, hybrid), "an arrow");
            assertNoDamage(helper, sire, sire.damageSources().arrow(arrow, null), "an arrow without an indirect attacker");

            AreaEffectCloud cloud = new AreaEffectCloud(helper.getLevel(), 0, 0, 0);
            cloud.setOwner(hybrid);
            assertNoDamage(helper, sire, sire.damageSources().indirectMagic(cloud, hybrid), "a cloud");
            assertNoDamage(helper, sire, sire.damageSources().indirectMagic(cloud, null), "a cloud with no named attacker");

            Wolf pet = new Wolf(EntityType.WOLF, helper.getLevel()) {
                @Override
                public LivingEntity getOwner() {
                    return hybrid;
                }
            };
            assertNoDamage(helper, sire, sire.damageSources().mobAttack(pet), "the hybrid's pet");

            // the same species, other sire: not protected
            ServerPlayer stranger = TestPlayers.createDuelist(helper);
            assertDamage(helper, stranger, hybrid.damageSources().playerAttack(hybrid), "a hit on someone who is not the sire");
            // someone else hitting the sire
            ServerPlayer other = TestPlayers.createDuelist(helper);
            assertDamage(helper, sire, other.damageSources().playerAttack(other), "a non-hybrid hitting the sire");
            Arrow foreign = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
            foreign.setOwner(other);
            assertDamage(helper, sire, sire.damageSources().arrow(foreign, other), "someone else's arrow");
            // a cured hybrid keeps a stale sire but is no longer bound
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().sire(sire.getUUID()).build());
            assertDamage(helper, sire, hybrid.damageSources().playerAttack(hybrid), "a cured hybrid");
        } finally {
            reset(hybrid);
        }
        helper.succeed();
    }
}
