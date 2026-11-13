package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.VervainRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** The Resurrection Ring and Vervain. Links Vampirism only (the Werewolves-only cases live in the Werewolves tests). */
@PrefixGameTestTemplate(false)
public final class ResurrectionVervainGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ResurrectionVervainGameTests() {
    }

    // ---- helpers ----

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    @SuppressWarnings("unchecked")
    private static Mob mob(GameTestHelper helper, String entityId) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId)), new BlockPos(1, 1, 1));
    }

    private static ItemStack ring(int charges) {
        ItemStack s = new ItemStack(ArtifactItems.RESURRECTION_RING.get());
        s.set(ArtifactItems.CHARGES.get(), charges);
        return s;
    }

    private static int charges(ItemStack ring) {
        return ring.getOrDefault(ArtifactItems.CHARGES.get(), -1);
    }

    /** A lethal blow from {@code attacker}: the mortal victim's die() announces the death like a real player. */
    private static void kill(ServerPlayer victim, LivingEntity attacker) {
        victim.setHealth(victim.getMaxHealth());
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().mobAttack(attacker), 1000f);
    }

    private static IDrinkBloodContext bitten(LivingEntity victim) {
        return new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.of(victim);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<net.minecraft.world.level.block.state.BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        };
    }

    /** Posts the event Vampirism fires when {@code biter} drinks from {@code victim}; returns it so the test can read the amount. */
    static BloodDrinkEvent.PlayerDrinkBloodEvent bite(ServerPlayer biter, LivingEntity victim) {
        var event = new BloodDrinkEvent.PlayerDrinkBloodEvent(VampirismAPI.vampirePlayer(biter), 10, 0.5f, false, bitten(victim));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    private static void protect(ServerPlayer victim) {
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(ArtifactItems.VERVAIN_EFFECT, 1200));
    }

    /**
     * The magic damage {@code target} is about to take while {@code action} runs, as the first listener sees it. Vampirism
     * trims damage to vampires later on, so the health lost is not a stable figure; the amount we ask for is.
     */
    private static List<Float> magicHits(LivingEntity target, Runnable action) {
        List<Float> amounts = new ArrayList<>();
        Consumer<LivingIncomingDamageEvent> spy = e -> {
            if (e.getEntity() == target && e.getSource().is(DamageTypes.MAGIC)) {
                amounts.add(e.getAmount());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, spy);
        try {
            action.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
        }
        return amounts;
    }

    private static void fresh(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
    }

    // ---- Supernatural registry ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampiresAreSupernaturalHuntersAreNot(GameTestHelper helper) {
        ServerPlayer vamp = vampire(helper);
        ServerPlayer hunter = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
        for (var e : List.of(vamp, mob(helper, "vampirism:vampire"))) {
            helper.assertTrue(Supernatural.is(e) && Supernatural.isVampiric(e) && Supernatural.isPrey(e), e + " is a vampire");
        }
        for (var e : List.of(hunter, TestPlayers.createVulnerable(helper), mob(helper, "minecraft:zombie"), mob(helper, "vampirism:hunter"))) {
            helper.assertTrue(!Supernatural.is(e) && !Supernatural.isVampiric(e) && !Supernatural.isPrey(e), e + " is not supernatural");
        }
        helper.succeed();
    }

    // ---- Resurrection Ring ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void resurrectionRingRevivesHumanFromVampire(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createMortal(helper);
        victim.getInventory().items.set(5, ring(3));
        kill(victim, mob(helper, "vampirism:vampire"));
        helper.assertTrue(victim.getHealth() == victim.getMaxHealth(), "not back at full health: " + victim.getHealth());
        helper.assertTrue(charges(victim.getInventory().items.get(5)) == 2, "expected 2 charges left, got " + charges(victim.getInventory().items.get(5)));
        var regen = victim.getEffect(MobEffects.REGENERATION);
        helper.assertTrue(regen != null && regen.getAmplifier() == 1 && regen.getDuration() > 0 && regen.getDuration() <= 200, "no Regeneration II for 10 s: " + regen);
        helper.succeed();
    }

    /** The faction half of the victim rule: a hunter is a human and is revived; a vampire is not, whatever its species says. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void resurrectionRingFollowsTheVictimsFaction(GameTestHelper helper) {
        ServerPlayer hunter = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(hunter).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
        hunter.getInventory().items.set(5, ring(3));
        kill(hunter, mob(helper, "vampirism:vampire"));
        helper.assertTrue(hunter.getHealth() == hunter.getMaxHealth(), "a hunter was not revived: " + hunter.getHealth());
        helper.assertTrue(charges(hunter.getInventory().items.get(5)) == 2, "the hunter's ring was not used");

        // species NONE (the default) but in the vampire faction
        ServerPlayer vamp = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vamp).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        vamp.getInventory().items.set(5, ring(3));
        kill(vamp, mob(helper, "vampirism:vampire"));
        helper.assertTrue(charges(vamp.getInventory().items.get(5)) == 3, "a vampire used a charge of the ring");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void resurrectionRingIgnoresZombieKill(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createMortal(helper);
        victim.getInventory().items.set(5, ring(3));
        kill(victim, mob(helper, "minecraft:zombie"));
        helper.assertTrue(victim.getHealth() <= 0, "a zombie kill was undone: " + victim.getHealth());
        helper.assertTrue(charges(victim.getInventory().items.get(5)) == 3, "a charge was used");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void emptyResurrectionRingDoesNothing(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createMortal(helper);
        victim.getInventory().items.set(5, ring(0));
        kill(victim, mob(helper, "vampirism:vampire"));
        helper.assertTrue(victim.getHealth() <= 0, "an empty ring stopped the death");
        helper.assertTrue(charges(victim.getInventory().items.get(5)) == 0, "an empty ring went below zero");

        // the ring leaves the inventory in the middle of the death (an earlier listener, a death drop): no crash, no revival
        ServerPlayer second = TestPlayers.createMortal(helper);
        second.getInventory().items.set(5, ring(3));
        Consumer<LivingDeathEvent> thief = e -> second.getInventory().items.set(5, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, thief);
        try {
            kill(second, mob(helper, "vampirism:vampire"));
        } finally {
            NeoForge.EVENT_BUS.unregister(thief);
        }
        helper.assertTrue(second.getHealth() <= 0, "a ring that was gone still stopped the death");
        helper.succeed();
    }

    /** Pins the listener order: after Vampirism (HIGHEST) and OriginalRules (HIGH), before TransitionHandler and GeneAwakening (LOW). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void resurrectionRingRunsBetweenSaversAndConsequences(GameTestHelper helper) {
        List<String> seen = new ArrayList<>();
        Consumer<LivingDeathEvent> high = e -> seen.add("high:" + e.isCanceled());
        Consumer<LivingDeathEvent> low = e -> seen.add("low:" + e.isCanceled());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, LivingDeathEvent.class, high);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDeathEvent.class, low);
        try {
            ServerPlayer victim = TestPlayers.createMortal(helper);
            victim.getInventory().items.set(5, ring(3));
            kill(victim, mob(helper, "vampirism:vampire"));
            helper.assertTrue(seen.equals(List.of("high:false")), "the ring did not run between HIGH and LOW: " + seen);
            helper.assertTrue(victim.getHealth() == victim.getMaxHealth(), "setup: not revived");

            // a death something earlier already prevented uses no charge
            seen.clear();
            ServerPlayer saved = TestPlayers.createMortal(helper);
            saved.getInventory().items.set(5, ring(3));
            Consumer<LivingDeathEvent> savior = e -> e.setCanceled(true);
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, savior);
            try {
                kill(saved, mob(helper, "vampirism:vampire"));
            } finally {
                NeoForge.EVENT_BUS.unregister(savior);
            }
            helper.assertTrue(charges(saved.getInventory().items.get(5)) == 3, "a charge was used on a death that was already prevented");
        } finally {
            NeoForge.EVENT_BUS.unregister(high);
            NeoForge.EVENT_BUS.unregister(low);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfReturnMakesTheRing(GameTestHelper helper) {
        ServerPlayer caster = vampire(helper);
        try {
            RiteTestSupport.start(helper, "ward_of_return", caster, List.of(new ItemStack(Items.TOTEM_OF_UNDYING)));
            var items = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(RiteTestSupport.centre(helper)).inflate(1.5));
            ItemStack made = items.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                    .filter(s -> s.is(ArtifactItems.RESURRECTION_RING.get())).findFirst().orElse(ItemStack.EMPTY);
            helper.assertTrue(!made.isEmpty(), "the rite made no ring");
            helper.assertTrue(charges(made) == 3, "a new ring does not start with 3 charges: " + charges(made));
        } finally {
            RiteTestSupport.clearDropped(helper);
        }
        helper.succeed();
    }

    // ---- Vervain ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainBurnsBitingVampire(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createVulnerable(helper);
        ServerPlayer biter = vampire(helper);
        fresh(biter);
        float full = biter.getMaxHealth();
        List<Float> control = magicHits(biter, () -> helper.assertTrue(bite(biter, victim).getAmount() == 10, "control: a bite without vervain was changed"));
        helper.assertTrue(control.isEmpty() && biter.getHealth() == full, "control: a bite without vervain burned: " + control);

        protect(victim);
        fresh(biter);
        BloodDrinkEvent.PlayerDrinkBloodEvent[] bitten = new BloodDrinkEvent.PlayerDrinkBloodEvent[1];
        List<Float> burn = magicHits(biter, () -> bitten[0] = bite(biter, victim));
        var event = bitten[0];
        helper.assertTrue(burn.equals(List.of(4.0f)), "the biter should take exactly one 4.0 burn: " + burn);
        var weakness = biter.getEffect(MobEffects.WEAKNESS);
        helper.assertTrue(weakness != null && weakness.getAmplifier() == 0 && weakness.getDuration() > 0 && weakness.getDuration() <= 200, "no Weakness I for 10 s: " + weakness);
        helper.assertTrue(event.getAmount() == 0, "the bite still drank " + event.getAmount());

        // vervain in the offhand protects too
        ServerPlayer second = TestPlayers.createVulnerable(helper);
        second.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ArtifactItems.VERVAIN.get()));
        ServerPlayer biter2 = vampire(helper);
        fresh(biter2);
        List<Float> offhandBurn = magicHits(biter2, () -> bite(biter2, second));
        helper.assertTrue(offhandBurn.equals(List.of(4.0f)), "offhand vervain did not burn the biter by 4.0: " + offhandBurn);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainBurnsBarehandedVampireOnly(GameTestHelper helper) {
        ServerPlayer victim = TestPlayers.createVulnerable(helper);
        protect(victim);
        Mob vamp = mob(helper, "vampirism:vampire");
        // spawned mobs may get a random weapon, and this case is about bare hands
        vamp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        fresh(victim);
        List<Float> burn = magicHits(vamp, () -> victim.hurt(victim.damageSources().mobAttack(vamp), 1f));
        helper.assertTrue(burn.equals(List.of(4.0f)), "the barehanded vampire should take exactly one 4.0 burn: " + burn + ", main hand " + vamp.getMainHandItem());
        helper.assertTrue(vamp.hasEffect(MobEffects.WEAKNESS), "no Weakness");

        // armed: no burn
        Mob armed = mob(helper, "vampirism:vampire");
        armed.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        float armedBefore = armed.getHealth();
        fresh(victim);
        victim.hurt(victim.damageSources().mobAttack(armed), 1f);
        helper.assertTrue(armed.getHealth() == armedBefore, "an armed vampire was burned");

        // a zombie is not a vampire
        Mob zombie = mob(helper, "minecraft:zombie");
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        float zombieBefore = zombie.getHealth();
        fresh(victim);
        victim.hurt(victim.damageSources().mobAttack(zombie), 1f);
        helper.assertTrue(zombie.getHealth() == zombieBefore, "a zombie was burned");

        // an unprotected victim burns nobody
        ServerPlayer bare = TestPlayers.createVulnerable(helper);
        Mob other = mob(helper, "vampirism:vampire");
        float otherBefore = other.getHealth();
        bare.hurt(bare.damageSources().mobAttack(other), 1f);
        helper.assertTrue(other.getHealth() == otherBefore, "a victim with no vervain burned the attacker");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainProtectsFromCompulsion(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(!VervainRules.protects(p), "protected with nothing");
        p.getInventory().items.set(0, new ItemStack(ArtifactItems.VERVAIN.get()));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ArtifactItems.VERVAIN.get()));
        helper.assertTrue(!VervainRules.protects(p), "protected by vervain that is only carried or held in the main hand");

        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ArtifactItems.VERVAIN.get()));
        helper.assertTrue(VervainRules.protects(p), "offhand vervain did not protect");
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!VervainRules.protects(p), "still protected after putting it away");

        protect(p);
        helper.assertTrue(VervainRules.protects(p), "the vervain effect did not protect");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainTeaGivesTenMinutes(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        ItemStack tea = new ItemStack(ArtifactItems.VERVAIN_TEA.get(), 2);
        ItemStack left = tea.finishUsingItem(helper.getLevel(), p);
        var effect = p.getEffect(ArtifactItems.VERVAIN_EFFECT);
        helper.assertTrue(effect != null && effect.getDuration() == 12000, "the tea should give 10 minutes of vervain: " + effect);
        helper.assertTrue(VervainRules.protects(p), "tea did not protect");
        helper.assertTrue(left.is(ArtifactItems.VERVAIN_TEA.get()) && left.getCount() == 1, "one tea should be used up: " + left);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireEatingVervainTakesDamage(GameTestHelper helper) {
        for (Item food : List.of(ArtifactItems.VERVAIN.get(), ArtifactItems.VERVAIN_TEA.get())) {
            ServerPlayer vamp = vampire(helper);
            fresh(vamp);
            ItemStack stack = new ItemStack(food);
            List<Float> hurt = magicHits(vamp, () -> NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(vamp, stack, 0, ItemStack.EMPTY)));
            helper.assertTrue(hurt.equals(List.of(6.0f)), "a vampire eating " + food + " should take exactly one 6.0 hit: " + hurt);

            ServerPlayer human = TestPlayers.createVulnerable(helper);
            fresh(human);
            List<Float> none = magicHits(human, () -> NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(human, stack, 0, ItemStack.EMPTY)));
            helper.assertTrue(none.isEmpty() && human.getHealth() == human.getMaxHealth(), "a human was hurt by " + food + ": " + none);
        }
        helper.succeed();
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainSeedsTurnUpInVillageChestsOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(seedsIn(level, "minecraft:chests/village/village_plains_house") > 0, "no vervain seeds in 400 village house chests");
        helper.assertTrue(seedsIn(level, "minecraft:chests/simple_dungeon") == 0, "vervain seeds turned up in a dungeon");
        helper.succeed();
    }

    private static int seedsIn(ServerLevel level, String table) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        int n = 0;
        for (int i = 0; i < 400; i++) {
            for (ItemStack s : loot.getRandomItems(params)) {
                if (s.is(ArtifactItems.VERVAIN_SEEDS.get())) {
                    n += s.getCount();
                }
            }
        }
        return n;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vervainTeaHasAKettleRecipe(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, "kettle/vervain_tea")).isPresent(), "no kettle recipe for the tea");
        helper.succeed();
    }
}
