package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.augustine.AugustineRules;

/** Phase 18c (R200, R203): what an Augustine is in play: the bonuses, the blood, the two gifts. */
@PrefixGameTestTemplate(false)
public final class AugustineGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation GARLIC = ResourceLocation.fromNamespaceAndPath("vampirism", "garlic");

    private AugustineGameTests() {
    }

    private static ServerPlayer vampire(GameTestHelper helper, Species species) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        return p;
    }

    private static void own(ServerPlayer p, String... skills) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
    }

    private static void forget(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            BloodlineAccess.set(p, BloodlineState.NONE);
            own(p);
        }
    }

    private static IDrinkBloodContext from(LivingEntity source) {
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

    /** Posts the event Vampirism fires when {@code drinker} feeds and returns the amount the drinker ends up with. */
    private static int feed(ServerPlayer drinker, LivingEntity source, int amount) {
        var event = new BloodDrinkEvent.PlayerDrinkBloodEvent(VampirismAPI.vampirePlayer(drinker), amount, 0.5f, false, from(source));
        NeoForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    private static LivingEntity mob(GameTestHelper helper, String id) {
        return (LivingEntity) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).create(helper.getLevel());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAugustineHasTheBonusesAndTheyGoWithTheSpecies(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, Species.NONE);
        double health = p.getMaxHealth();
        double attack = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(!p.getAttribute(Attributes.MAX_HEALTH).hasModifier(AugustineRules.MODIFIER_ID), "a plain vampire has the Augustine modifier");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
        helper.assertTrue(p.getMaxHealth() == health + SpeciesConfig.augustineExtraHealth(), "max health " + p.getMaxHealth() + ", was " + health);
        helper.assertTrue(Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE) - attack * (1 + SpeciesConfig.augustineAttackBonus())) < 1e-6,
                "attack " + p.getAttributeValue(Attributes.ATTACK_DAMAGE) + ", was " + attack);
        helper.assertTrue(SpeciesConfig.augustineExtraHealth() == 2.0 && SpeciesConfig.augustineAttackBonus() == 0.10, "the defaults are R200's: +2 health, +10% attack");
        // a second sync adds nothing
        AugustineRules.sync(p);
        helper.assertTrue(p.getMaxHealth() == health + 2.0, "the modifier stacked: " + p.getMaxHealth());
        // a cure takes the species, and the bonuses with it
        BloodlineAccess.set(p, BloodlineState.NONE);
        helper.assertTrue(p.getMaxHealth() == health && p.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack, "the bonuses stayed after the cure");
        forget(p);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAugustineFeedsTripleFromAVampireAndAThirdFromAnyoneElse(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        LivingEntity vampireMob = mob(helper, "vampirism:vampire");
        LivingEntity villager = EntityType.VILLAGER.create(helper.getLevel());
        LivingEntity cow = EntityType.COW.create(helper.getLevel());
        try {
            int fromVampire = feed(p, vampireMob, 3);
            helper.assertTrue(fromVampire == 9, "a vampire gave " + fromVampire + ", not 9");
            int fromVillager = feed(p, villager, 3);
            helper.assertTrue(fromVillager == 1, "a villager gave " + fromVillager + ", not 1");
            helper.assertTrue(feed(p, cow, 1) == 1, "a bite that gave 1 gave nothing from a cow");
            helper.assertTrue(feed(p, villager, 10) == 3, "ten from a villager did not give a third");
            helper.assertTrue(feed(p, villager, 0) == 0, "a refused bite gave blood");
            // a plain vampire is not scaled
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.assertTrue(feed(p, villager, 3) == 3, "a plain vampire's blood was scaled");
        } finally {
            forget(p);
        }
        helper.succeed();
    }

    /** R20f: a vampire who bites a vampire gets nothing (Predator's Thirst lifts it); an Augustine is not refused. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAugustineBitingAVampireIsNotRefusedByR20f(GameTestHelper helper) {
        ServerPlayer plain = vampire(helper, Species.NONE);
        ServerPlayer augustine = vampire(helper, Species.AUGUSTINE);
        LivingEntity vampireMob = mob(helper, "vampirism:vampire");
        try {
            helper.assertTrue(feed(plain, vampireMob, 3) == 0, "control: a plain vampire without Predator's Thirst was fed by a vampire");
            helper.assertTrue(feed(augustine, vampireMob, 3) == 9, "an Augustine was refused or not scaled by a vampire's bite");
            if (EnabledParts.isEnabled("supernatural")) {
                helper.assertTrue(vampireMob.hasEffect(MobEffects.WEAKNESS) || vampireMob.getHealth() < vampireMob.getMaxHealth(),
                        "the bite did not drain the vampire (an Augustine drains without the skill)");
            }
        } finally {
            forget(plain, augustine);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void garlicIsDeniedOnlyToAnAugustineWithTheWard(GameTestHelper helper) {
        ServerPlayer augustine = vampire(helper, Species.AUGUSTINE);
        ServerPlayer plain = vampire(helper, Species.NONE);
        var garlic = BuiltInRegistries.MOB_EFFECT.getHolder(GARLIC).orElseThrow();
        try {
            helper.assertTrue(plain.addEffect(new MobEffectInstance(garlic, 200)) && plain.hasEffect(garlic), "control: a plain vampire does not take garlic");
            augustine.addEffect(new MobEffectInstance(garlic, 200));
            helper.assertTrue(augustine.hasEffect(garlic), "without the skill an Augustine was warded");
            augustine.removeAllEffects();
            own(augustine, "S-AUG-1");
            helper.assertTrue(!augustine.addEffect(new MobEffectInstance(garlic, 200)) && !augustine.hasEffect(garlic), "Garlic Ward did not deny garlic");
            helper.assertTrue(augustine.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200)) && augustine.hasEffect(MobEffects.WEAKNESS), "Garlic Ward denied an unrelated effect");
            // the skill is the Augustine's: on anyone else it is dormant
            own(plain, "S-AUG-1");
            plain.removeAllEffects();
            helper.assertTrue(plain.addEffect(new MobEffectInstance(garlic, 200)), "a plain vampire who somehow owns the skill was warded");
        } finally {
            augustine.removeAllEffects();
            plain.removeAllEffects();
            forget(augustine, plain);
        }
        helper.succeed();
    }

    private static float holyWater(GameTestHelper helper, ServerPlayer p, float amount) {
        var holder = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("vampirism", "holy_water")));
        var event = new LivingIncomingDamageEvent(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(new DamageSource(holder), amount));
        NeoForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void holyWaterIsHalvedForAnAugustineWithBlessedSkin(GameTestHelper helper) {
        ServerPlayer augustine = vampire(helper, Species.AUGUSTINE);
        ServerPlayer plain = vampire(helper, Species.NONE);
        try {
            helper.assertTrue(holyWater(helper, augustine, 5f) == 5f, "without the skill holy water was reduced");
            own(augustine, "S-AUG-2");
            float reduced = holyWater(helper, augustine, 5f);
            helper.assertTrue(reduced == 2.5f, "Blessed Skin gave " + reduced + ", not 2.5");
            // other damage is not touched
            var event = new LivingIncomingDamageEvent(augustine, new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().generic(), 5f));
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getAmount() == 5f, "Blessed Skin reduced ordinary damage");
            // dormant outside the branch: a plain vampire that owns the skill takes full damage
            own(plain, "S-AUG-2");
            helper.assertTrue(holyWater(helper, plain, 5f) == 5f, "a plain vampire was blessed");
        } finally {
            forget(augustine, plain);
        }
        helper.succeed();
    }
}
