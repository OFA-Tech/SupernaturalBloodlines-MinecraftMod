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

    /** The species is a fact the viewer must have discovered (like a heretic): a stranger's profile of an Augustine does not show it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aStrangersProfileOfAnAugustineDoesNotShowTheSpecies(GameTestHelper helper) {
        ServerPlayer observer = TestPlayers.create(helper);
        ServerPlayer augustine = vampire(helper, Species.AUGUSTINE);
        ServerPlayer heretic = vampire(helper, Species.HERETIC);
        // the profile reads the subject through the server's player list: test players are not in it, so say where they are (else "not shown" below would be trivially true)
        var here = new java.util.HashMap<java.util.UUID, ServerPlayer>();
        for (ServerPlayer who : new ServerPlayer[] {observer, augustine, heretic}) {
            here.put(who.getUUID(), who);
        }
        net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(here::get);
        try {
            var seen = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(observer, augustine.getUUID()).facts();
            var control = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(observer, heretic.getUUID()).facts();
            helper.assertTrue(!seen.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES), "a stranger sees an Augustine's species: " + seen);
            // the positive control: the same fact is there for the one it is about, with the right value (so "not shown" above is the rule, not a missing fact)
            net.ofatech.supernaturalbloodlines.parts.species.profile.ProfileKnowledge.get(observer.serverLevel()).discover(
                    net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex.get(observer.serverLevel()), observer.getUUID(), augustine.getUUID(),
                    java.util.List.of(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES));
            var own = net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService.of(observer, augustine.getUUID()).facts();
            helper.assertTrue(own.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES)
                    && own.get(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES).equals(java.util.List.of("AUGUSTINE")), "an observer who discovered an Augustine's species does not see it: " + own);
            helper.assertTrue(seen.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES) == control.containsKey(net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact.SPECIES),
                    "an Augustine is not treated like a heretic");
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(null);
            forget(augustine, heretic);
        }
        helper.succeed();
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
    public static void anAugustineFeedsTripleOnTheFirstBiteOnAVampireAndAThirdFromAnyoneElse(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        LivingEntity vampireMob = mob(helper, "vampirism:vampire");
        LivingEntity villager = EntityType.VILLAGER.create(helper.getLevel());
        LivingEntity cow = EntityType.COW.create(helper.getLevel());
        AugustineRules.forgetFeedsForTest();
        try {
            helper.assertTrue(feed(p, vampireMob, 3) == 9, "the first bite on a vampire was not tripled");
            // the x3 is paid once per feed: the bites that follow on the same victim are as Vampirism gives them
            helper.assertTrue(feed(p, vampireMob, 3) == 3 && feed(p, vampireMob, 3) == 3, "the next bites of the same feed were tripled again");
            AugustineRules.forgetFeedsForTest();
            helper.assertTrue(feed(p, vampireMob, 3) == 9, "a new feed was not tripled");
            helper.assertTrue(feed(p, villager, 3) == 1, "three from a villager did not give exactly a third");
            int ten = feed(p, villager, 10);
            helper.assertTrue(ten == 3 || ten == 4, "ten from a villager gave " + ten);
            helper.assertTrue(feed(p, villager, 0) == 0, "a refused bite gave blood");
            // a third is a third on average: 1 gives 1 in a third of the bites (a floor of 1 would give 600 of 600)
            int sum = 0;
            for (int i = 0; i < 600; i++) {
                int one = feed(p, cow, 1);
                helper.assertTrue(one == 0 || one == 1, "1 from a cow gave " + one);
                sum += one;
            }
            helper.assertTrue(sum > 130 && sum < 270, "600 bites of 1 gave " + sum + ", not about 200");
            // a plain vampire is not scaled
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.assertTrue(feed(p, villager, 3) == 3, "a plain vampire's blood was scaled");
        } finally {
            AugustineRules.forgetFeedsForTest();
            forget(p);
        }
        helper.succeed();
    }

    /** Nothing is made from nothing: a vampire player an Augustine bites pays all the Augustine gets, so two of them cannot feed each other. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void twoAugustinesBitingEachOtherCannotGainBlood(GameTestHelper helper) {
        ServerPlayer a = vampire(helper, Species.NONE);
        ServerPlayer b = vampire(helper, Species.NONE);
        AugustineRules.forgetFeedsForTest();
        try {
            // fill them as plain vampires (Vampirism fires the event for their own drink too), then make them Augustines
            for (ServerPlayer p : new ServerPlayer[] {a, b}) {
                VampirismAPI.vampirePlayer(p).drinkBlood(20, 0.5f, from(EntityType.COW.create(helper.getLevel())));
                BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
            }
            int total = VampirismAPI.vampirePlayer(a).getBloodStats().getBloodLevel() + VampirismAPI.vampirePlayer(b).getBloodStats().getBloodLevel();
            helper.assertTrue(total >= 30, "setup: they hold " + total);
            int gained = 0;
            int paid = 0;
            for (int round = 0; round < 6; round++) {
                ServerPlayer biter = round % 2 == 0 ? a : b;
                ServerPlayer victim = round % 2 == 0 ? b : a;
                int victimBefore = VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel();
                int got = feed(biter, victim, 3);
                int lost = victimBefore - VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel();
                helper.assertTrue(got > 0 && lost > 0, "round " + round + ": the bite did nothing (" + got + ", " + lost + ")");
                helper.assertTrue(got == (round < 2 ? 9 : 3), "round " + round + ": the biter got " + got + " (the first bite of a feed is tripled and paid for by the victim, the next are not)");
                helper.assertTrue(got <= lost, "round " + round + ": the biter got " + got + " from a victim who lost " + lost);
                gained += got;
                paid += lost;
            }
            helper.assertTrue(gained <= paid, "the two gained " + gained + " by biting each other, who paid " + paid);
        } finally {
            AugustineRules.forgetFeedsForTest();
            forget(a, b);
        }
        helper.succeed();
    }

    /** Leaving the vampire faction through Vampirism's own cure takes the bonuses at once, and the species with them. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingTheVampireFactionTakesTheAugustineBonuses(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        double health = p.getMaxHealth();
        try {
            helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).hasModifier(AugustineRules.MODIFIER_ID), "setup: no bonus");
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            helper.assertTrue(!p.getAttribute(Attributes.MAX_HEALTH).hasModifier(AugustineRules.MODIFIER_ID) && !p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(AugustineRules.MODIFIER_ID),
                    "the bonuses stayed after leaving the faction: " + p.getMaxHealth() + " from " + health);
            helper.assertTrue(BloodlineAccess.get(p).species() != Species.AUGUSTINE, "the species stayed after leaving the faction: " + BloodlineAccess.get(p).species());
            // and a sync alone (a species that says Augustine, no faction) is not enough to keep them
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
            AugustineRules.sync(p);
            helper.assertTrue(!p.getAttribute(Attributes.MAX_HEALTH).hasModifier(AugustineRules.MODIFIER_ID), "an Augustine outside the vampires got the bonuses");
        } finally {
            forget(p);
        }
        helper.succeed();
    }
    /** R20f: a vampire who bites a vampire gets nothing (Predator's Thirst lifts it); an Augustine is not refused. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAugustineBitingAVampireIsNotRefusedByR20f(GameTestHelper helper) {
        TestPlayers.TalkingDuelist plain = TestPlayers.createTalkingDuelist(helper);
        TestPlayers.TalkingDuelist augustine = TestPlayers.createTalkingDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(plain).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1)
                && VampirismAPI.factionPlayerHandler(augustine).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make vampires");
        BloodlineAccess.set(augustine, BloodlineState.NONE.toBuilder().species(Species.AUGUSTINE).build());
        LivingEntity vampireMob = mob(helper, "vampirism:vampire");
        vampireMob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
        vampireMob.setHealth(20f);
        String refused = "supernaturalbloodlines.turning.vampire_blood_no_fill";
        try {
            helper.assertTrue(feed(plain, vampireMob, 3) == 0, "control: a plain vampire without Predator's Thirst was fed by a vampire");
            helper.assertTrue(plain.receivedOnActionBar(refused) || plain.receivedOnActionBar(refused + "_free"), "control: the plain vampire was not told the bite gave nothing");
            helper.assertTrue(feed(augustine, vampireMob, 3) == 9, "an Augustine was refused or not scaled by a vampire's bite");
            helper.assertTrue(!augustine.receivedOnActionBar(refused) && !augustine.receivedOnActionBar(refused + "_free"), "an Augustine was told its bite on a vampire gave nothing (R20f is lifted)");
            if (EnabledParts.isEnabled("supernatural")) {
                helper.assertTrue(vampireMob.hasEffect(MobEffects.WEAKNESS) && vampireMob.getHealth() < vampireMob.getMaxHealth(),
                        "the bite did not both weaken and hurt the vampire (an Augustine drains without the skill): weakness " + vampireMob.hasEffect(MobEffects.WEAKNESS) + ", health " + vampireMob.getHealth());
            }
        } finally {
            forget(plain, augustine);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void garlicIsDeniedOnlyToAnAugustineWithTheWard(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("supernatural")) {
            helper.succeed(); // no skills without the supernatural part: the gifts are dormant, the species rules still work
            return;
        }
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
        if (!EnabledParts.isEnabled("supernatural")) {
            helper.succeed(); // no skills without the supernatural part: the gifts are dormant, the species rules still work
            return;
        }
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

    private static void setRung(ServerPlayer p, int rung) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().schema(net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules.SCHEMA)
                .rung(net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.AUGUSTINE, rung).build());
    }

    /** The Valet Pike counts the blood of an Augustine on their own ladder (L-AUG-4 is "advanced"), as it counts a vampire's. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theValetPikeAcceptsTheBloodOfAnAdvancedAugustine(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("bosses") || !EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        try {
            var server = helper.getLevel().getServer();
            var here = new java.util.HashMap<java.util.UUID, ServerPlayer>();
            here.put(p.getUUID(), p);
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(here::get);
            setRung(p, 3);
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.core.boss.PikeRules.qualifies(
                    net.ofatech.supernaturalbloodlines.parts.bosses.rite.PikeRiteFactory.giver(server, p.getUUID())), "L-AUG-3 qualified");
            setRung(p, 4);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.core.boss.PikeRules.qualifies(
                    net.ofatech.supernaturalbloodlines.parts.bosses.rite.PikeRiteFactory.giver(server, p.getUUID())), "L-AUG-4 did not qualify");
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall.overridePlayerLookup(null);
            setRung(p, 0);
            forget(p);
        }
        helper.succeed();
    }

    /** Mikael reads an Augustine's own rung: an advanced one is a vampire he comes for. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void mikaelCountsTheRungOfAnAugustine(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("bosses") || !EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        try {
            setRung(p, 4);
            var facts = net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.factsOf(p);
            helper.assertTrue(facts.vampire() && facts.rung() == 4, "Mikael sees an Augustine at L-AUG-4 as vampire=" + facts.vampire() + ", rung " + facts.rung());
            setRung(p, 0);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector.factsOf(p).rung() == 0, "the rung stayed");
        } finally {
            setRung(p, 0);
            forget(p);
        }
        helper.succeed();
    }

    /** An Augustine put into the dying state wakes as an Augustine, bonuses and all. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAugustineInTheDyingStateWakesAsOne(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, Species.AUGUSTINE);
        try {
            double health = p.getMaxHealth();
            net.ofatech.supernaturalbloodlines.parts.species.original.Downed.startDying(p, helper.getLevel().damageSources().generic());
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.species.original.Downed.isDowned(p), "not dying");
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.AUGUSTINE && p.getMaxHealth() == health, "the dying state changed the species or the bonuses");
            for (int i = 0; i < net.ofatech.supernaturalbloodlines.parts.species.original.Downed.TICKS; i++) {
                net.ofatech.supernaturalbloodlines.parts.species.original.Downed.tick(p);
            }
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.species.original.Downed.isDowned(p), "did not wake");
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.AUGUSTINE && p.getAttribute(Attributes.MAX_HEALTH).hasModifier(AugustineRules.MODIFIER_ID), "the wake lost the species or the bonus");
        } finally {
            net.ofatech.supernaturalbloodlines.parts.species.original.Downed.clearForTest(p);
            forget(p);
        }
        helper.succeed();
    }}
