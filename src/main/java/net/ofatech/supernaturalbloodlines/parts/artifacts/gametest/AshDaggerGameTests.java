package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.AshDagger;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/**
 * The White Oak Ash Dagger, the {@code desiccated} effect, the Rite of the Cure and The Cure. Knows nothing of the
 * species part: an Original here is a bloodline state with the species set, so the tests run with the part off too.
 */
@PrefixGameTestTemplate(false)
public final class AshDaggerGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NEEDS_DESICCATED = "supernaturalbloodlines.rite.needs_desiccated";

    private AshDaggerGameTests() {
    }

    // ---- helpers ----

    private static ItemStack dagger() {
        return new ItemStack(ArtifactItems.ASH_DAGGER.get());
    }

    private static ServerPlayer original(GameTestHelper helper, Species s) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
        return p;
    }

    private static Zombie zombie(GameTestHelper helper, ItemStack weapon) {
        Zombie z = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        z.setNoAi(true);
        z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, weapon);
        return z;
    }

    @SuppressWarnings("unchecked")
    private static Mob vampireMob(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill((EntityType<Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")), new BlockPos(1, 1, 1));
    }

    private static void hit(LivingEntity victim, LivingEntity attacker, float amount) {
        victim.setHealth(victim.getMaxHealth());
        victim.invulnerableTime = 0;
        DamageSource source = attacker instanceof net.minecraft.world.entity.player.Player p
                ? victim.damageSources().playerAttack(p) : victim.damageSources().mobAttack(attacker);
        victim.hurt(source, amount);
    }

    private static boolean desiccated(LivingEntity e) {
        return e.hasEffect(ArtifactItems.DESICCATED);
    }

    private static int droppedDaggers(GameTestHelper helper, BlockPos around) {
        int n = 0;
        for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(around)).inflate(6))) {
            if (i.getItem().is(ArtifactItems.ASH_DAGGER.get())) {
                n += i.getItem().getCount();
            }
        }
        return n;
    }

    private static void clearDaggers(GameTestHelper helper, BlockPos around) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(around)).inflate(6))
                .stream().filter(i -> i.getItem().is(ArtifactItems.ASH_DAGGER.get())).forEach(ItemEntity::discard);
    }

    /** The magic hits {@code target} is about to take while {@code action} runs, as the first listener sees them. */
    private static List<DamageSource> magicHits(LivingEntity target, List<Float> amounts, Runnable action) {
        List<DamageSource> sources = new ArrayList<>();
        Consumer<LivingIncomingDamageEvent> spy = e -> {
            if (e.getEntity() == target && e.getSource().is(DamageTypes.MAGIC)) {
                amounts.add(e.getAmount());
                sources.add(e.getSource());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, spy);
        try {
            action.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(spy);
        }
        return sources;
    }

    // ---- the dagger ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void daggerDesiccatesOriginal(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer victim = original(helper, s);
            Zombie z = zombie(helper, dagger());
            hit(victim, z, 1f);
            MobEffectInstance effect = victim.getEffect(ArtifactItems.DESICCATED);
            helper.assertTrue(effect != null && effect.getDuration() > 35900 && effect.getDuration() <= 36000,
                    s + " was not desiccated for 36000 ticks: " + effect);
            helper.assertTrue(AshDagger.lodged(victim).is(ArtifactItems.ASH_DAGGER.get()), s + " has no dagger lodged: " + AshDagger.lodged(victim));
            helper.assertTrue(z.getMainHandItem().isEmpty(), "the wielder still holds the dagger");
            helper.assertTrue(victim.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "the victim can still move: " + victim.getAttributeValue(Attributes.MOVEMENT_SPEED));

            // a second dagger does not swallow the first
            Zombie second = zombie(helper, dagger());
            hit(victim, second, 1f);
            helper.assertTrue(second.getMainHandItem().is(ArtifactItems.ASH_DAGGER.get()), "a second dagger was taken from its wielder");
            helper.assertTrue(AshDagger.lodged(victim).getCount() == 1, "the lodged stack changed: " + AshDagger.lodged(victim));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void daggerOnlyDesiccatesOriginals(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        ServerPlayer hybrid = original(helper, Species.HYBRID);
        for (ServerPlayer victim : List.of(human, vampire, hybrid)) {
            Zombie z = zombie(helper, dagger());
            hit(victim, z, 1f);
            helper.assertTrue(!desiccated(victim), victim.getName().getString() + " was desiccated");
            helper.assertTrue(z.getMainHandItem().is(ArtifactItems.ASH_DAGGER.get()), "the dagger was used up on a non-Original");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void daggerHeldByPlayerDesiccatesOriginal(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        server.setPvpAllowed(true);
        try {
            ServerPlayer victim = TestPlayers.createDuelist(helper);
            BloodlineAccess.set(victim, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
            ServerPlayer wielder = TestPlayers.createDuelist(helper);
            wielder.setItemInHand(InteractionHand.MAIN_HAND, dagger());
            hit(victim, wielder, 1f);
            helper.assertTrue(desiccated(victim), "a player's hit did not desiccate");
            helper.assertTrue(wielder.getMainHandItem().isEmpty(), "the player still holds the dagger");
        } finally {
            server.setPvpAllowed(pvp);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireWieldingDaggerIsHurt(GameTestHelper helper) {
        // a vampire mob, a vampire player (hit by an attacker-attributed magic source), and the others
        Zombie target = zombie(helper, ItemStack.EMPTY);
        Mob vampMob = vampireMob(helper);
        vampMob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, dagger());
        List<Float> amounts = new ArrayList<>();
        List<DamageSource> sources = magicHits(vampMob, amounts, () -> hit(target, vampMob, 1f));
        helper.assertTrue(amounts.equals(List.of(10.0f)), "the vampire mob should take exactly one 10.0 hit: " + amounts);
        helper.assertTrue(sources.get(0).getDirectEntity() == vampMob, "the hit is not attributed to the wielder: " + sources.get(0));

        ServerPlayer vamp = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vamp).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        vamp.setItemInHand(InteractionHand.MAIN_HAND, dagger());
        vamp.setHealth(vamp.getMaxHealth());
        amounts.clear();
        magicHits(vamp, amounts, () -> hit(target, vamp, 1f));
        helper.assertTrue(amounts.equals(List.of(10.0f)), "the vampire player should take exactly one 10.0 hit: " + amounts);
        helper.assertTrue(vamp.getHealth() < vamp.getMaxHealth(), "the vampire player lost no health");

        // humans and plain mobs are fine, and so is a vampire holding it without hitting anything
        ServerPlayer human = TestPlayers.createVulnerable(helper);
        human.setItemInHand(InteractionHand.MAIN_HAND, dagger());
        Zombie zombieWielder = zombie(helper, dagger());
        amounts.clear();
        magicHits(human, amounts, () -> hit(target, human, 1f));
        magicHits(zombieWielder, amounts, () -> hit(target, zombieWielder, 1f));
        helper.assertTrue(amounts.isEmpty(), "a human or a zombie was hurt by the dagger: " + amounts);
        helper.succeed();
    }

    // ---- desiccation ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void desiccatedCannotAct(GameTestHelper helper) {
        ServerPlayer victim = original(helper, Species.ORIGINAL);
        ServerPlayer other = TestPlayers.createVulnerable(helper);
        helper.assertTrue(!NeoForge.EVENT_BUS.post(new AttackEntityEvent(victim, other)).isCanceled(), "control: a free player was stopped from attacking");
        victim.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, 36000));
        helper.assertTrue(NeoForge.EVENT_BUS.post(new AttackEntityEvent(victim, other)).isCanceled(), "a desiccated player attacked");
        helper.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(victim, InteractionHand.MAIN_HAND)).isCanceled(), "a desiccated player used an item");
        helper.assertTrue(NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(victim, new ItemStack(Items.APPLE), 32)).isCanceled(), "a desiccated player started eating");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void pullingDaggerFrees(GameTestHelper helper) {
        BlockPos spot = new BlockPos(1, 1, 1);
        ServerPlayer victim = original(helper, Species.ORIGINAL);
        victim.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        ServerPlayer puller = TestPlayers.createVulnerable(helper);
        try {
            hit(victim, zombie(helper, dagger()), 1f);
            helper.assertTrue(desiccated(victim), "setup: not desiccated");

            // the desiccated player cannot pull their own dagger, and a bystander's click on someone else does nothing
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(victim, InteractionHand.MAIN_HAND, victim));
            helper.assertTrue(desiccated(victim), "the desiccated player pulled their own dagger");
            helper.assertTrue(droppedDaggers(helper, spot) == 0, "a dagger was dropped too early");

            PlayerInteractEvent.EntityInteract click = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(puller, InteractionHand.MAIN_HAND, victim));
            helper.assertTrue(click.isCanceled(), "the pull was not handled");
            helper.assertTrue(!desiccated(victim), "still desiccated after the pull");
            helper.assertTrue(AshDagger.lodged(victim).isEmpty(), "the dagger is still lodged");
            helper.assertTrue(droppedDaggers(helper, spot) == 1, "expected exactly one dropped dagger, got " + droppedDaggers(helper, spot));
            helper.assertTrue(victim.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "the victim cannot move after the pull");

            // pulling again finds nothing
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(puller, InteractionHand.OFF_HAND, victim));
            helper.assertTrue(droppedDaggers(helper, spot) == 1, "a second pull dropped a second dagger");
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void timeoutAndRemovalDropTheDagger(GameTestHelper helper) {
        BlockPos spot = new BlockPos(1, 1, 1);
        try {
            // the effect runs out
            ServerPlayer expiring = original(helper, Species.ORIGINAL);
            expiring.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
            hit(expiring, zombie(helper, dagger()), 1f);
            MobEffectInstance instance = expiring.getEffect(ArtifactItems.DESICCATED);
            helper.assertTrue(instance != null, "setup: not desiccated");
            NeoForge.EVENT_BUS.post(new MobEffectEvent.Expired(expiring, instance));
            helper.assertTrue(AshDagger.lodged(expiring).isEmpty() && droppedDaggers(helper, spot) == 1,
                    "the timeout did not drop the dagger: " + droppedDaggers(helper, spot));
            clearDaggers(helper, spot);

            // milk, a command or another mod takes the effect off
            ServerPlayer cleared = original(helper, Species.ORIGINAL_HYBRID);
            cleared.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
            hit(cleared, zombie(helper, dagger()), 1f);
            cleared.removeEffect(ArtifactItems.DESICCATED);
            helper.assertTrue(AshDagger.lodged(cleared).isEmpty() && droppedDaggers(helper, spot) == 1,
                    "removing the effect did not drop the dagger: " + droppedDaggers(helper, spot));
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    /** Pinned by the plan's review focus: a desiccated Original who logs out is still desiccated on login, with the dagger. */
    /** The packet path still lets a desiccated vampire start a bite; it drinks nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void desiccatedVampireDrinksNothing(GameTestHelper helper) {
        ServerPlayer biter = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(biter).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        ServerPlayer victim = TestPlayers.createMortal(helper);
        var control = ResurrectionVervainGameTests.bite(biter, victim);
        helper.assertTrue(control.getAmount() == 10, "control: a normal bite drank " + control.getAmount());

        biter.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, 1000, 0, false, true, true));
        var bite = ResurrectionVervainGameTests.bite(biter, victim);
        helper.assertTrue(bite.getAmount() == 0 && bite.getSaturation() == 0f,
                "a desiccated vampire drank " + bite.getAmount() + " with saturation " + bite.getSaturation());

        biter.removeEffect(ArtifactItems.DESICCATED);
        helper.assertTrue(ResurrectionVervainGameTests.bite(biter, victim).getAmount() == 10, "the bite stayed empty after the effect ended");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void desiccationSurvivesRelog(GameTestHelper helper) {
        BlockPos spot = new BlockPos(1, 1, 1);
        ServerPlayer p = original(helper, Species.ORIGINAL);
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        ServerPlayer reloaded = TestPlayers.createMortal(helper);
        try {
            hit(p, zombie(helper, dagger()), 1f);
            MobEffectInstance running = p.getEffect(ArtifactItems.DESICCATED);
            for (int i = 0; i < 1234; i++) {
                running.tick(p, () -> { });
            }
            int left = running.getDuration();
            helper.assertTrue(left == ArtifactItems.DESICCATED_TICKS - 1234, "setup: the effect did not run down, " + left);
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            reloaded.load(saved);

            MobEffectInstance effect = reloaded.getEffect(ArtifactItems.DESICCATED);
            helper.assertTrue(effect != null && effect.getDuration() == left, "the relogged effect is " + effect + ", expected " + left + " ticks");
            helper.assertTrue(AshDagger.lodged(reloaded).is(ArtifactItems.ASH_DAGGER.get()), "the dagger was lost on relog: " + AshDagger.lodged(reloaded));
            helper.assertTrue(BloodlineAccess.get(reloaded).species() == Species.ORIGINAL, "the species was lost");
            helper.assertTrue(reloaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0,
                    "the relogged player can move: " + reloaded.getAttributeValue(Attributes.MOVEMENT_SPEED));

            // and it still ends: pulling frees them
            ServerPlayer puller = TestPlayers.createVulnerable(helper);
            reloaded.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(puller, InteractionHand.MAIN_HAND, reloaded));
            helper.assertTrue(!desiccated(reloaded) && droppedDaggers(helper, spot) == 1, "the relogged player could not be freed");
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    /** The effect is checked first: a refused effect must not eat the dagger. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void refusedEffectKeepsTheDagger(GameTestHelper helper) {
        BlockPos spot = new BlockPos(1, 1, 1);
        ServerPlayer victim = original(helper, Species.ORIGINAL);
        victim.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        Zombie z = zombie(helper, dagger());
        Consumer<MobEffectEvent.Applicable> refuse = e -> {
            if (e.getEntity() == victim) {
                e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, MobEffectEvent.Applicable.class, refuse);
        try {
            hit(victim, z, 1f);
        } finally {
            NeoForge.EVENT_BUS.unregister(refuse);
        }
        try {
            helper.assertTrue(!desiccated(victim), "the refused effect was applied");
            helper.assertTrue(z.getMainHandItem().is(ArtifactItems.ASH_DAGGER.get()), "the wielder lost the dagger to a refused effect");
            helper.assertTrue(AshDagger.lodged(victim).isEmpty(), "a dagger was lodged without the effect");

            // safety net: a dagger left lodged without the effect is dropped, not overwritten
            victim.setData(ArtifactItems.LODGED_DAGGER.get(), dagger());
            hit(victim, z, 1f);
            helper.assertTrue(desiccated(victim) && AshDagger.lodged(victim).is(ArtifactItems.ASH_DAGGER.get()), "setup: the second hit did not lodge");
            helper.assertTrue(droppedDaggers(helper, spot) == 1, "the stale dagger was not dropped: " + droppedDaggers(helper, spot));
            helper.assertTrue(z.getMainHandItem().isEmpty(), "the wielder still holds the new dagger");
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    /** /effect clear removes the effect with no instance in the event; the dagger still drops, with a pickup delay. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void effectClearReleasesTheDagger(GameTestHelper helper) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos spot = new BlockPos(1, 1, 1);
        ServerPlayer victim = original(helper, Species.ORIGINAL);
        victim.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        try {
            hit(victim, zombie(helper, dagger()), 1f);
            helper.assertTrue(desiccated(victim), "setup: not desiccated");
            helper.getLevel().getServer().getCommands().getDispatcher().execute("effect clear @s", victim.createCommandSourceStack().withPermission(4));
            helper.assertTrue(!desiccated(victim), "/effect clear left the effect");
            helper.assertTrue(AshDagger.lodged(victim).isEmpty() && droppedDaggers(helper, spot) == 1,
                    "/effect clear did not drop the dagger: " + droppedDaggers(helper, spot));
            boolean delayed = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(spot)).inflate(6))
                    .stream().filter(i -> i.getItem().is(ArtifactItems.ASH_DAGGER.get())).allMatch(ItemEntity::hasPickUpDelay);
            helper.assertTrue(delayed, "the dropped dagger can be picked up at once");
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    /** Someone holding The Cure who clicks a desiccated Original feeds it to them; that wins over the plain pull. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void feedingTheCureCuresDesiccatedOriginal(GameTestHelper helper) {
        BlockPos spot = new BlockPos(1, 1, 1);
        ServerPlayer victim = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(victim).setFactionAndLevel(VReference.VAMPIRE_FACTION, 2), "could not make a vampire");
        BloodlineAccess.set(victim, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
        victim.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        ServerPlayer feeder = TestPlayers.createVulnerable(helper);
        try {
            hit(victim, zombie(helper, dagger()), 1f);
            helper.assertTrue(desiccated(victim), "setup: not desiccated");

            // control: a cure in the pocket is not a cure in the hand
            feeder.getInventory().items.set(5, new ItemStack(ArtifactItems.THE_CURE.get()));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(feeder, InteractionHand.OFF_HAND, victim));
            helper.assertTrue(BloodlineAccess.get(victim).species() == Species.ORIGINAL && !desiccated(victim), "an unheld cure was used, or the pull failed");
            clearDaggers(helper, spot);
            hit(victim, zombie(helper, dagger()), 1f);
            helper.assertTrue(desiccated(victim), "setup: not desiccated again");

            feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ArtifactItems.THE_CURE.get()));
            PlayerInteractEvent.EntityInteract click = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(feeder, InteractionHand.MAIN_HAND, victim));
            helper.assertTrue(click.isCanceled(), "the feeding was not handled");
            helper.assertTrue(BloodlineAccess.get(victim).species() == Species.NONE, "not cured: " + BloodlineAccess.get(victim));
            helper.assertTrue(VampirismAPI.factionPlayerHandler(victim).getCurrentFaction() == null, "still in a faction");
            helper.assertTrue(!desiccated(victim) && AshDagger.lodged(victim).isEmpty(), "still desiccated");
            helper.assertTrue(droppedDaggers(helper, spot) == 1, "expected one dropped dagger: " + droppedDaggers(helper, spot));
            helper.assertTrue(feeder.getMainHandItem().is(Items.GLASS_BOTTLE), "the feeder kept " + feeder.getMainHandItem());
        } finally {
            clearDaggers(helper, spot);
        }
        helper.succeed();
    }

    // ---- the Rite of the Cure ----

    private static List<ItemStack> cureItems() {
        return List.of(new ItemStack(ArtifactItems.WHITE_OAK_ASH.get(), 2), new ItemStack(ArtifactItems.VERVAIN.get(), 4),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cureRiteDataLoads(GameTestHelper helper) {
        var type = RiteTestSupport.type(helper, "rite_of_the_cure");
        helper.assertTrue(type != null && type.getPower() == 20000, "rite_of_the_cure is missing or has the wrong power");
        var recipes = helper.getLevel().getRecipeManager();
        boolean species = EnabledParts.isEnabled("species");
        for (String name : List.of("white_oak_ash", "white_oak_ash_dagger")) {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(NS, name)).isPresent() == species,
                    "recipe " + name + " should be present exactly when the species part is on (" + species + ")");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cureRiteNeedsDesiccatedOriginal(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        ServerPlayer plain = original(helper, Species.ORIGINAL);
        ServerPlayer wrongSpecies = TestPlayers.createMortal(helper);
        RiteTestSupport.place(helper, caster);
        RiteTestSupport.place(helper, plain);
        RiteTestSupport.place(helper, wrongSpecies);
        try {
            // nobody desiccated: refused and refunded
            RiteTestSupport.start(helper, "rite_of_the_cure", caster, cureItems());
            helper.assertTrue(caster.received(NEEDS_DESICCATED), "the caster was not told why");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:the_cure") == 0, "a Cure was made without a desiccated Original");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:white_oak_ash") == 2, "the ash was not given back");
            RiteTestSupport.clearDropped(helper);

            // desiccated, but not an Original
            wrongSpecies.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, 36000));
            RiteTestSupport.start(helper, "rite_of_the_cure", caster, cureItems());
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:the_cure") == 0, "a Cure was made from a desiccated non-Original");
            RiteTestSupport.clearDropped(helper);

            // a desiccated Original in the circle
            plain.addEffect(new MobEffectInstance(ArtifactItems.DESICCATED, 36000));
            RiteTestSupport.start(helper, "rite_of_the_cure", caster, cureItems());
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:the_cure") == 1, "no Cure was made");
            helper.assertTrue(RiteTestSupport.dropped(helper, "supernaturalbloodlines:white_oak_ash") == 0, "the ash was given back");
        } finally {
            RiteTestSupport.remove(helper, caster);
            RiteTestSupport.remove(helper, plain);
            RiteTestSupport.remove(helper, wrongSpecies);
        }
        helper.succeed();
    }

    // ---- The Cure ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCureCuresOriginal(GameTestHelper helper) {
        for (Species s : List.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID)) {
            ServerPlayer p = TestPlayers.createMortal(helper);
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(s).build());
            ItemStack left = new ItemStack(ArtifactItems.THE_CURE.get()).finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, s + " was not cured: " + BloodlineAccess.get(p));
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == null,
                    s + " is still in " + VampirismAPI.factionPlayerHandler(p).getCurrentFaction());
            helper.assertTrue(left.is(Items.GLASS_BOTTLE), "the empty bottle did not come back: " + left);
        }

        // a plain vampire is cured too; a human just loses the potion
        ServerPlayer vamp = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vamp).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        new ItemStack(ArtifactItems.THE_CURE.get()).finishUsingItem(helper.getLevel(), vamp);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vamp).getCurrentFaction() == null, "a vampire was not cured");
        ServerPlayer human = TestPlayers.createMortal(helper);
        new ItemStack(ArtifactItems.THE_CURE.get()).finishUsingItem(helper.getLevel(), human);
        helper.assertTrue(BloodlineAccess.get(human).equals(BloodlineState.NONE) || BloodlineAccess.get(human).species() == Species.NONE, "a human changed");
        helper.succeed();
    }
}
