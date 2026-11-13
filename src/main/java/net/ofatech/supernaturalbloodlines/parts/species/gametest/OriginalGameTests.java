package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesData;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Originals: the Rite of Immortality, Original's Blood, the white oak rule, the dying state and the attribute bonus. */
@PrefixGameTestTemplate(false)
public final class OriginalGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long FULL_MOON_CYCLE = 24000L * 8;
    private static final String NOT_ORIGINAL = "supernaturalbloodlines.refusal.not_original";

    private OriginalGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static BloodlineState species(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    /** A player who takes damage, with the species set (and, for Originals proper, in the vampire faction). */
    private static ServerPlayer original(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        if (s == Species.ORIGINAL) {
            handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        }
        BloodlineAccess.set(p, species(s));
        return p;
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    private static boolean dbno(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).isDBNO();
    }

    private static Zombie zombie(GameTestHelper helper, boolean withStake) {
        Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        z.setNoAi(true);
        if (withStake) {
            z.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
        }
        return z;
    }

    private static DamageSource hitBy(GameTestHelper helper, Zombie z) {
        return helper.getLevel().damageSources().mobAttack(z);
    }

    private static void atFullMoon(GameTestHelper helper, boolean full, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        level.setDayTime(full ? base : base + 24000L * 4);
        try {
            helper.assertTrue((level.getMoonPhase() == 0) == full, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    private static List<ItemStack> immortalityItems() {
        return List.of(new ItemStack(WhiteOak.SAPLING.get()), new ItemStack(Items.GOLDEN_APPLE), new ItemStack(item("vampirism:human_heart")));
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalDataLoads(GameTestHelper helper) {
        RiteType immortality = type(helper, "immortality");
        helper.assertTrue(immortality != null && immortality.getPower() == 12000, "immortality missing or wrong power");
        helper.assertTrue(immortality.getItems().size() == 3, "immortality has " + immortality.getItems().size() + " ingredients");
        helper.assertTrue(immortality.getEntities().size() == 1 && immortality.getEntities().get(0) == EntityType.VILLAGER,
                "immortality does not sacrifice one villager: " + immortality.getEntities());
        helper.assertTrue(type(helper, "original_blood") != null, "original_blood rite is missing");
        helper.assertTrue(new ItemStack(WhiteOak.STAKE.get()).is(WhiteOak.TAG), "the stake is not tagged white_oak");
        helper.assertTrue(new ItemStack(WhiteOak.LOG_ITEM.get()).is(net.minecraft.tags.ItemTags.LOGS), "the log is not in #minecraft:logs");
        helper.assertTrue(new ItemStack(WhiteOak.PLANKS_ITEM.get()).is(net.minecraft.tags.ItemTags.PLANKS), "the planks are not in #minecraft:planks");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(WhiteOak.TREE),
                "the white oak tree feature is missing");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "white_oak_stake")).isPresent(),
                "the stake recipe is missing");
        helper.succeed();
    }

    // ---- Rite of Immortality ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void immortalityMakesOriginalVampire(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            atFullMoon(helper, true, () -> start(helper, "immortality", caster, immortalityItems()));
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.ORIGINAL, "species is " + BloodlineAccess.get(caster).species());
            helper.assertTrue(handler(caster).getCurrentFaction() == VReference.VAMPIRE_FACTION, "faction is " + handler(caster).getCurrentFaction());
            helper.assertTrue(SpeciesData.get(helper.getLevel().getServer()).count() >= 1, "the Original is not counted");
            helper.assertTrue(caster.getAttribute(Attributes.MAX_HEALTH).hasModifier(OriginalRules.MODIFIER_ID), "the new Original has no health bonus");
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void immortalityNeedsAFullMoon(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            atFullMoon(helper, false, () -> start(helper, "immortality", caster, immortalityItems()));
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.rite.needs_full_moon"), "the caster was not told it needs a full moon");
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.NONE, "species is " + BloodlineAccess.get(caster).species());
        } finally {
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void immortalityRefusedWhenCapReached(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        SpeciesData data = SpeciesData.get(helper.getLevel().getServer());
        UUID other = UUID.randomUUID();
        try {
            data.setOriginal(other, true);
            SpeciesConfig.overrideMaxOriginals(data.count());
            atFullMoon(helper, true, () -> start(helper, "immortality", caster, immortalityItems()));
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.refusal.original_cap"), "no original_cap refusal sent");
            helper.assertTrue(BloodlineAccess.get(caster).species() == Species.NONE, "the cap did not hold: " + BloodlineAccess.get(caster).species());
            helper.assertTrue(handler(caster).getCurrentFaction() == null, "the refused player joined " + handler(caster).getCurrentFaction());
        } finally {
            SpeciesConfig.overrideMaxOriginals(null);
            data.setOriginal(other, false);
            remove(helper, caster);
            reset(caster);
        }
        helper.succeed();
    }

    // ---- the dying state (R32) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalGoesDownOnNonOakLethalDamage(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL);
        try {
            Zombie z = zombie(helper, false);
            p.hurt(hitBy(helper, z), 1000f);
            helper.assertTrue(!p.isDeadOrDying(), "the Original died, health " + p.getHealth());
            helper.assertTrue(dbno(p), "the Original is not in Vampirism's dying state");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillsOriginalInstantly(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL);
        try {
            Zombie z = zombie(helper, true);
            p.hurt(hitBy(helper, z), 1000f);
            helper.assertTrue(p.isDeadOrDying(), "white oak did not kill, health " + p.getHealth());
            helper.assertTrue(!dbno(p), "the dying state was left behind");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void voidAndKillBypassOriginalProtection(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer inVoid = original(helper, s);
            ServerPlayer killed = original(helper, s);
            try {
                inVoid.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 1000f);
                helper.assertTrue(inVoid.isDeadOrDying(), s + " survived the void");
                killed.kill();
                helper.assertTrue(killed.isDeadOrDying(), s + " survived /kill");
                helper.assertTrue(!Downed.isDowned(inVoid) && !Downed.isDowned(killed), s + " was left downed");
            } finally {
                reset(inVoid);
                reset(killed);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ordinaryPlayersStillDie(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.hurt(hitBy(helper, zombie(helper, false)), 1000f);
        helper.assertTrue(p.isDeadOrDying(), "a player without a species survived");
        helper.assertTrue(!Downed.isDowned(p), "a player without a species went down");
        helper.succeed();
    }

    // ---- our own downed state (Original hybrids) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hybridGoesDownAndIsHeldStill(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        try {
            Zombie z = zombie(helper, false);
            p.hurt(hitBy(helper, z), 1000f);
            helper.assertTrue(!p.isDeadOrDying() && Downed.isDowned(p), "the hybrid is not downed, health " + p.getHealth());
            helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0, "the downed hybrid can still walk");
            helper.assertTrue(p.getAttributeValue(Attributes.JUMP_STRENGTH) == 0.0, "the downed hybrid can still jump");
            AttackEntityEvent attack = new AttackEntityEvent(p, z);
            NeoForge.EVENT_BUS.post(attack);
            helper.assertTrue(attack.isCanceled(), "the downed hybrid can still attack");
            p.invulnerableTime = 0;
            helper.assertTrue(!p.hurt(hitBy(helper, z), 1000f) && Downed.isDowned(p) && !p.isDeadOrDying(), "more damage finished off a downed hybrid");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void downedHybridRevivesAtHalfHealth(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        try {
            p.hurt(hitBy(helper, zombie(helper, false)), 1000f);
            helper.assertTrue(Downed.isDowned(p), "not downed");
            for (int i = 0; i < Downed.TICKS - 1; i++) {
                Downed.tick(p);
            }
            helper.assertTrue(Downed.isDowned(p), "revived after " + (Downed.TICKS - 1) + " ticks");
            Downed.tick(p);
            helper.assertTrue(!Downed.isDowned(p), "still downed after " + Downed.TICKS + " ticks");
            helper.assertTrue(p.getHealth() == p.getMaxHealth() / 2, "health is " + p.getHealth() + " of " + p.getMaxHealth());
            helper.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "the revived hybrid cannot walk");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void giveUpKillsWithTheOriginalSource(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        List<DamageSource> deaths = new ArrayList<>();
        Consumer<LivingDeathEvent> spy = e -> {
            if (e.getEntity() == p) {
                deaths.add(e.getSource());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, spy);
        try {
            Zombie z = zombie(helper, false);
            DamageSource hit = hitBy(helper, z);
            p.hurt(hit, 1000f);
            helper.assertTrue(Downed.isDowned(p), "not downed");
            deaths.clear();
            Downed.giveUp(p);
            helper.assertTrue(p.isDeadOrDying() && !Downed.isDowned(p), "the hybrid did not die when giving up");
            helper.assertTrue(deaths.size() == 1 && deaths.get(0).getEntity() == z, "the death did not name the original killer: " + deaths);
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillsHybridsInstantlyAndWhileDowned(GameTestHelper helper) {
        ServerPlayer instant = original(helper, Species.ORIGINAL_HYBRID);
        ServerPlayer downed = original(helper, Species.ORIGINAL_HYBRID);
        try {
            instant.hurt(hitBy(helper, zombie(helper, true)), 1000f);
            helper.assertTrue(instant.isDeadOrDying() && !Downed.isDowned(instant), "white oak did not kill a hybrid outright");
            downed.hurt(hitBy(helper, zombie(helper, false)), 1000f);
            helper.assertTrue(Downed.isDowned(downed), "not downed");
            downed.invulnerableTime = 0; // a FakePlayer never ticks, so the hit-cooldown of the first blow would swallow this one
            downed.hurt(hitBy(helper, zombie(helper, true)), 1000f);
            helper.assertTrue(downed.isDeadOrDying() && !Downed.isDowned(downed), "white oak did not finish a downed hybrid");
        } finally {
            reset(instant);
            reset(downed);
        }
        helper.succeed();
    }

    // ---- attributes (R31) ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalsGetTheirBonusesAndLoseThem(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        double health = p.getAttributeValue(Attributes.MAX_HEALTH);
        double attack = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        try {
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == health + 4, "max health is " + p.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE) - attack * 1.2) < 1e-6, "attack is " + p.getAttributeValue(Attributes.ATTACK_DAMAGE));
            helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).getModifier(OriginalRules.MODIFIER_ID) != null, "the modifier id is not supernaturalbloodlines:original");
            BloodlineAccess.set(p, species(Species.ORIGINAL_HYBRID));
            helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == health + 4, "the hybrid lost the bonus");
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == health, "max health is still " + p.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack, "attack is still " + p.getAttributeValue(Attributes.ATTACK_DAMAGE));
            // a new player entity (login, respawn) starts without the transient modifier: the sync restores it
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            p.getAttribute(Attributes.MAX_HEALTH).removeModifier(OriginalRules.MODIFIER_ID);
            OriginalRules.sync(p);
            helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == health + 4, "sync did not restore the bonus");
            OriginalRules.sync(p);
            helper.assertTrue(p.getAttributeValue(Attributes.MAX_HEALTH) == health + 4, "a second sync stacked the bonus");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void attackBonusIsTwentyPercentOfTheTotal(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        try {
            // a FakePlayer never ticks, so put the sword's modifiers on by hand
            new ItemStack(Items.IRON_SWORD).forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    (attribute, modifier) -> p.getAttribute(attribute).addTransientModifier(modifier));
            double before = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(before > 3.0, "the sword is not in effect: " + before);
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            double after = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(Math.abs(after / before - 1.2) < 1e-6, "attack went from " + before + " to " + after + ", not x1.2");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalKeepsItsHealthThroughASaveAndLoad(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ServerPlayer reloaded = TestPlayers.create(helper);
        try {
            BloodlineAccess.set(p, species(Species.ORIGINAL));
            p.setHealth(p.getMaxHealth());
            helper.assertTrue(p.getHealth() == 24.0F, "health is " + p.getHealth());
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            p.addAdditionalSaveData(saved);
            reloaded.readAdditionalSaveData(saved); // before any login sync: the modifier has to come back with the player
            helper.assertTrue(reloaded.getHealth() == 24.0F, "the reloaded Original has " + reloaded.getHealth() + " health");
            helper.assertTrue(reloaded.getMaxHealth() == 24.0F, "the reloaded Original has max health " + reloaded.getMaxHealth());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void respawnedOriginalStartsAtFullBonusHealth(GameTestHelper helper) {
        ServerPlayer fresh = TestPlayers.create(helper); // a fresh entity: 20 health, no modifiers yet
        try {
            BloodlineAccess.set(fresh, species(Species.ORIGINAL));
            fresh.getAttribute(Attributes.MAX_HEALTH).removeModifier(OriginalRules.MODIFIER_ID);
            fresh.setHealth(20.0F);
            OriginalRules.restore(fresh, true);
            helper.assertTrue(fresh.getHealth() == 24.0F, "health is " + fresh.getHealth());
            fresh.setHealth(10.0F);
            OriginalRules.restore(fresh, false); // coming back from the End keeps the health
            helper.assertTrue(fresh.getHealth() == 10.0F, "health is " + fresh.getHealth());
        } finally {
            reset(fresh);
        }
        helper.succeed();
    }

    // ---- review fixes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deathListenerRunsBeforeNormalListeners(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        int[] seen = {0};
        Consumer<LivingDeathEvent> other = e -> {
            if (e.getEntity() == p) {
                seen[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, other);
        try {
            p.hurt(hitBy(helper, zombie(helper, false)), 1000f);
            helper.assertTrue(Downed.isDowned(p), "not downed");
            helper.assertTrue(seen[0] == 0, "another mod's NORMAL handler saw the death before it was cancelled");
        } finally {
            NeoForge.EVENT_BUS.unregister(other);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakKillsAnOriginalAlreadyInVampirismDbno(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL);
        List<DamageSource> deaths = new ArrayList<>();
        Consumer<LivingDeathEvent> spy = e -> {
            if (e.getEntity() == p) {
                deaths.add(e.getSource());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, spy);
        try {
            p.hurt(hitBy(helper, zombie(helper, false)), 1000f);
            helper.assertTrue(dbno(p) && !p.isDeadOrDying(), "not in Vampirism's dying state");
            p.invulnerableTime = 0;
            Zombie oak = zombie(helper, true);
            p.hurt(hitBy(helper, oak), 1000f);
            helper.assertTrue(p.isDeadOrDying(), "white oak did not kill an Original who was already dying, health " + p.getHealth());
            helper.assertTrue(!dbno(p), "the dying state was left behind");
            helper.assertTrue(deaths.stream().anyMatch(s -> s.getEntity() == oak), "the kill did not name the white oak attacker: " + deaths);
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thornsIsNotWhiteOak(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        try {
            Zombie oak = zombie(helper, true);
            p.hurt(helper.getLevel().damageSources().thorns(oak), 1000f);
            helper.assertTrue(!p.isDeadOrDying() && Downed.isDowned(p), "thorns from a stake-holder killed an Original");
            ServerPlayer other = original(helper, Species.ORIGINAL_HYBRID);
            try {
                other.hurt(hitBy(helper, oak), 1000f);
                helper.assertTrue(other.isDeadOrDying(), "a plain melee hit with the stake no longer kills");
            } finally {
                reset(other);
            }
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void downedPlayersStopGliding(GameTestHelper helper) {
        ServerPlayer p = original(helper, Species.ORIGINAL_HYBRID);
        try {
            p.startFallFlying();
            helper.assertTrue(p.isFallFlying(), "could not start gliding");
            p.hurt(hitBy(helper, zombie(helper, false)), 1000f);
            helper.assertTrue(Downed.isDowned(p) && !p.isFallFlying(), "the downed player is still gliding");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void downedScreenIsResentEveryHundredTicks(GameTestHelper helper) {
        int resends = 0;
        for (int left = Downed.TICKS - 1; left >= 1; left--) {
            if (Downed.reopensScreenAt(left)) {
                resends++;
            }
        }
        helper.assertTrue(resends == 5, "the screen is re-sent " + resends + " times in a downed spell, expected 5");
        helper.succeed();
    }

    // ---- Original's Blood ----

    private static ItemStack blood(UUID source) {
        ItemStack s = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        s.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get(), source);
        return s;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalBloodRecordsSource(GameTestHelper helper) {
        ServerPlayer drinker = TestPlayers.create(helper);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ItemStack firstBottle = blood(first);
        drinker.setItemInHand(InteractionHand.MAIN_HAND, firstBottle.finishUsingItem(helper.getLevel(), drinker));
        helper.assertTrue(drinker.hasData(SpeciesRegistries.BLOOD_SOURCE) && drinker.getData(SpeciesRegistries.BLOOD_SOURCE).equals(first),
                "the source was not recorded");
        var effect = drinker.getEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT);
        helper.assertTrue(effect != null && effect.getDuration() > 5900 && effect.getDuration() <= 6000, "effect is " + effect);
        helper.assertTrue(drinker.getMainHandItem().is(Items.GLASS_BOTTLE), "the empty bottle was not given back: " + drinker.getMainHandItem());
        ItemStack secondBottle = blood(second);
        secondBottle.finishUsingItem(helper.getLevel(), drinker);
        helper.assertTrue(drinker.getData(SpeciesRegistries.BLOOD_SOURCE).equals(second), "the last blood drunk is not the source");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodWithoutASourceCannotBeDrunk(GameTestHelper helper) {
        ServerPlayer drinker = TestPlayers.create(helper);
        ItemStack empty = new ItemStack(SpeciesRegistries.ORIGINAL_BLOOD.get());
        helper.assertTrue(!empty.use(helper.getLevel(), drinker, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "blood without a source can be drunk");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalBloodRiteBottlesTheOriginal(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer original = RecordingPlayer.create(helper);
        place(helper, original);
        try {
            BloodlineAccess.set(original, species(Species.ORIGINAL));
            start(helper, "original_blood", original, List.of(new ItemStack(Items.GLASS_BOTTLE)));
            ItemStack found = ItemStack.EMPTY;
            for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5))) {
                if (i.getItem().is(SpeciesRegistries.ORIGINAL_BLOOD.get())) {
                    found = i.getItem();
                }
            }
            helper.assertTrue(!found.isEmpty(), "no Original's Blood was made");
            helper.assertTrue(original.getUUID().equals(found.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get())),
                    "the blood names " + found.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT.get()));
        } finally {
            clearDropped(helper);
            remove(helper, original);
            reset(original);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalBloodRiteRefusesNonOriginals(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer human = RecordingPlayer.create(helper);
        place(helper, human);
        try {
            BloodlineAccess.set(human, species(Species.WITCH));
            start(helper, "original_blood", human, List.of(new ItemStack(Items.GLASS_BOTTLE)));
            helper.assertTrue(human.receivedOnActionBar(NOT_ORIGINAL), "the caster was not told the target is not an Original");
            helper.assertTrue(dropped(helper, "minecraft:glass_bottle") == 1, "the ingredient was not given back");
            for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5))) {
                helper.assertTrue(!i.getItem().is(SpeciesRegistries.ORIGINAL_BLOOD.get()), "blood was made from a non-Original");
            }
        } finally {
            clearDropped(helper);
            remove(helper, human);
            reset(human);
        }
        helper.succeed();
    }
}
