package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains.Strain;

/** The wolfsbane strains (R95). Links Werewolves, so it is registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class WolfsbaneGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private WolfsbaneGameTests() {
    }

    // ---- helpers ----

    private static ServerPlayer werewolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static void eat(LivingEntity who, Item item) {
        ItemStack stack = new ItemStack(item);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(who, stack, 0, stack));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static boolean has(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return e.hasEffect(effect);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean formCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        if (insideOwnTick) {
            net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift.enterTick(p);
        }
        try {
            var event = new ActionEvent.ActionActivatedEvent(
                    VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) action, 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift.leaveTick(p);
        }
    }

    // ---- the strains ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void commonWolfsbaneWeakens(GameTestHelper helper) {
        ServerPlayer plain = werewolf(helper);
        eat(plain, item("werewolves:wolfsbane"));
        MobEffectInstance weak = plain.getEffect(MobEffects.WEAKNESS);
        helper.assertTrue(weak != null && weak.getAmplifier() == 0 && weak.getDuration() > 590 && weak.getDuration() <= 600, "no Weakness I for 30 s: " + weak);
        helper.assertTrue(has(plain, MobEffects.CONFUSION), "no Nausea");

        // Enchanted's flower counts as wolfsbane too
        ServerPlayer other = werewolf(helper);
        eat(other, item("enchanted:wolfsbane_flower"));
        helper.assertTrue(has(other, MobEffects.WEAKNESS) && has(other, MobEffects.CONFUSION), "Enchanted's wolfsbane did nothing");

        TagKey<Item> common = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NS, "wolfsbane/common"));
        helper.assertTrue(new ItemStack(item("werewolves:wolfsbane")).is(common) && new ItemStack(item("enchanted:wolfsbane_flower")).is(common),
                "the common tag lacks one of the two plants");
        helper.assertTrue(new ItemStack(item("werewolves:wolfsbane")).has(DataComponents.FOOD), "the common plant is not edible");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void strainsOnlyTouchWerewolfTypes(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = vampire(helper);
        ServerPlayer hybrid = TestPlayers.createMortal(helper);
        BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        ServerPlayer wolf = werewolf(helper);
        for (Item plant : new Item[] {item("werewolves:wolfsbane"), WolfsbaneStrains.YELLOW_PLANT.get(), WolfsbaneStrains.NORTHERN_BLUE_PLANT.get(),
                WolfsbaneStrains.LICHEN.get()}) {
            for (ServerPlayer p : new ServerPlayer[] {human, vampire}) {
                eat(p, plant);
                helper.assertTrue(p.getActiveEffects().isEmpty(), "eating " + plant + " affected a non-werewolf: " + p.getActiveEffects());
            }
            eat(hybrid, plant);
            eat(wolf, plant);
        }
        helper.assertTrue(has(hybrid, WolfsbaneStrains.NORTHERN_BLUE_EFFECT) && has(hybrid, WolfsbaneStrains.YELLOW_EFFECT), "a hybrid species is not hurt");
        helper.assertTrue(has(wolf, WolfsbaneStrains.NORTHERN_BLUE_EFFECT) && has(wolf, WolfsbaneStrains.LICHEN_EFFECT), "a werewolf is not hurt");
        // a potion goes through addEffect, so the effect itself must refuse the wrong targets
        helper.assertTrue(!human.addEffect(new MobEffectInstance(WolfsbaneStrains.YELLOW_EFFECT, 100)), "a human took a strain effect");
        helper.assertTrue(!vampire.addEffect(new MobEffectInstance(WolfsbaneStrains.LICHEN_EFFECT, 100)), "a vampire took a strain effect");
        helper.assertTrue(BuiltInRegistries.POTION.containsKey(ResourceLocation.fromNamespaceAndPath(NS, "wolfsbane_northern_blue")), "no potion");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void yellowWolfsbaneHurtsOverTime(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        eat(p, WolfsbaneStrains.YELLOW_PLANT.get());
        MobEffectInstance e = p.getEffect(WolfsbaneStrains.YELLOW_EFFECT);
        helper.assertTrue(e != null && e.getDuration() > 1190 && e.getDuration() <= 1200, "yellow lasts 60 s: " + e);
        var effect = WolfsbaneStrains.YELLOW_EFFECT.value();
        helper.assertTrue(effect.shouldApplyEffectTickThisTick(40, 0) && !effect.shouldApplyEffectTickThisTick(39, 0), "yellow pulses every 40 ticks");
        float before = p.getHealth();
        effect.applyEffectTick(p, 0);
        helper.assertTrue(before - p.getHealth() == 1f, "one damage per pulse, got " + (before - p.getHealth()));
        p.setHealth(1f);
        effect.applyEffectTick(p, 0);
        helper.assertTrue(p.isAlive() && p.getHealth() == 1f, "yellow wolfsbane must not kill");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void northernBlueKillsWithoutAsh(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        eat(p, WolfsbaneStrains.NORTHERN_BLUE_PLANT.get());
        MobEffectInstance e = p.getEffect(WolfsbaneStrains.NORTHERN_BLUE_EFFECT);
        helper.assertTrue(e != null && e.getDuration() > 5990 && e.getDuration() <= 6000, "northern blue lasts 6000 ticks: " + e);
        var effect = WolfsbaneStrains.NORTHERN_BLUE_EFFECT.value();
        helper.assertTrue(effect.shouldApplyEffectTickThisTick(100, 0) && effect.shouldApplyEffectTickThisTick(1, 0)
                && !effect.shouldApplyEffectTickThisTick(99, 0), "pulse every 100 ticks and on the last tick");
        float before = p.getHealth();
        p.removeEffect(WolfsbaneStrains.NORTHERN_BLUE_EFFECT);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 100));
        effect.applyEffectTick(p, 0);
        helper.assertTrue(before - p.getHealth() == 1f && p.isAlive(), "meanwhile: 1 damage, no death yet");
        // eating it again must not restart the clock
        p.removeEffect(WolfsbaneStrains.NORTHERN_BLUE_EFFECT);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 1));
        eat(p, WolfsbaneStrains.NORTHERN_BLUE_PLANT.get());
        helper.assertTrue(p.getEffect(WolfsbaneStrains.NORTHERN_BLUE_EFFECT).getDuration() == 1, "a second plant restarted the clock");
        effect.applyEffectTick(p, 0);
        helper.assertTrue(!p.isAlive(), "northern blue did not kill on its last tick");
        helper.assertTrue(p.getLastDamageSource() != null
                && p.getLastDamageSource().is(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(NS, "wolfsbane_poison"))), "wrong damage type: " + p.getLastDamageSource());
        helper.succeed();
    }

    /** Cured of lycanthropy while it runs (The Cure, a rite, leaving the faction): the strain is dropped, nobody dies of it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void strainEndsWhenNoLongerAWerewolf(GameTestHelper helper) {
        var nb = WolfsbaneStrains.NORTHERN_BLUE_EFFECT.value();
        ServerPlayer p = werewolf(helper);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 1));
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.LICHEN_EFFECT, 1000));
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.assertTrue(!WolfsbaneStrains.isWerewolfType(p), "setup: still a werewolf type");
        float before = p.getHealth();
        nb.applyEffectTick(p, 0);
        helper.assertTrue(p.isAlive() && p.getHealth() == before, "northern blue still hurt someone who is no longer a werewolf");
        helper.assertTrue(!has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT), "northern blue stayed on a non-werewolf");
        WolfsbaneStrains.LICHEN_EFFECT.value().applyEffectTick(p, 0);
        helper.assertTrue(!has(p, WolfsbaneStrains.LICHEN_EFFECT), "lichen stayed on a non-werewolf");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anvilCoatingKeepsTheRename(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ItemStack plant = new ItemStack(WolfsbaneStrains.YELLOW_PLANT.get());
        var renamed = new AnvilUpdateEvent(new ItemStack(Items.IRON_SWORD), plant, "Fang", 0, p);
        NeoForge.EVENT_BUS.post(renamed);
        helper.assertTrue(renamed.getOutput().get(DataComponents.CUSTOM_NAME) != null
                && renamed.getOutput().get(DataComponents.CUSTOM_NAME).getString().equals("Fang"), "the rename was lost: " + renamed.getOutput().get(DataComponents.CUSTOM_NAME));
        helper.assertTrue(renamed.getOutput().get(WolfsbaneStrains.COATING.get()) != null && renamed.getCost() == 2, "renamed coating: cost " + renamed.getCost());

        for (String untouched : new String[] {"", "Iron Sword"}) {
            var plain = new AnvilUpdateEvent(new ItemStack(Items.IRON_SWORD), plant, untouched, 0, p);
            NeoForge.EVENT_BUS.post(plain);
            helper.assertTrue(plain.getOutput().get(DataComponents.CUSTOM_NAME) == null && plain.getCost() == 1,
                    "'" + untouched + "' renamed the sword or cost " + plain.getCost());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void strainCodecDecodesAnyId(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (Strain s : Strain.values()) {
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
            Strain.STREAM_CODEC.encode(buf, s);
            helper.assertTrue(Strain.STREAM_CODEC.decode(buf) == s, "round trip failed for " + s);
        }
        var bad = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
        bad.writeVarInt(99);
        helper.assertTrue(Strain.STREAM_CODEC.decode(bad) == Strain.COMMON, "an unknown id did not decode to the first strain");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sameStrainAshCures(GameTestHelper helper) {
        for (Strain s : new Strain[] {Strain.YELLOW, Strain.NORTHERN_BLUE, Strain.LICHEN}) {
            ServerPlayer p = werewolf(helper);
            p.addEffect(new MobEffectInstance(s.effect(), 1000));
            eat(p, s.ash());
            helper.assertTrue(!has(p, s.effect()), "eating " + s + " ash did not cure it");
        }
        // burning it on yourself = using it while sneaking
        ServerPlayer p = werewolf(helper);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 1000));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WolfsbaneStrains.NORTHERN_BLUE_ASH.get(), 2));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        helper.assertTrue(has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT), "using the ash standing up burned it");
        p.setShiftKeyDown(true);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        helper.assertTrue(!has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT), "burning the ash on yourself did not cure");
        helper.assertTrue(p.getMainHandItem().getCount() == 1, "the ash was not used up: " + p.getMainHandItem());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void otherAshDoesNotCure(GameTestHelper helper) {
        ServerPlayer p = werewolf(helper);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 1000));
        eat(p, WolfsbaneStrains.YELLOW_ASH.get());
        eat(p, WolfsbaneStrains.LICHEN_ASH.get());
        helper.assertTrue(has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT), "another strain's ash cured northern blue");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WolfsbaneStrains.YELLOW_ASH.get()));
        p.setShiftKeyDown(true);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
        helper.assertTrue(has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT) && p.getMainHandItem().getCount() == 1, "burning another strain's ash did something");
        // milk is no cure either
        p.removeEffect(WolfsbaneStrains.NORTHERN_BLUE_EFFECT);
        p.addEffect(new MobEffectInstance(WolfsbaneStrains.NORTHERN_BLUE_EFFECT, 1000));
        p.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        helper.assertTrue(has(p, WolfsbaneStrains.NORTHERN_BLUE_EFFECT), "milk cured northern blue");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lichenBlocksForms(GameTestHelper helper) {
        ServerPlayer plain = werewolf(helper);
        helper.assertTrue(!formCancelled(plain, ModActions.BEAST_FORM.get(), false), "control: a form was cancelled without lichen");

        ServerPlayer p = werewolf(helper);
        eat(p, WolfsbaneStrains.LICHEN.get());
        MobEffectInstance e = p.getEffect(WolfsbaneStrains.LICHEN_EFFECT);
        helper.assertTrue(e != null && e.getDuration() > 1190 && e.getDuration() <= 1200, "lichen lasts 1200 ticks: " + e);
        helper.assertTrue(formCancelled(p, ModActions.BEAST_FORM.get(), false), "a voluntary shift was not cancelled");
        helper.assertTrue(formCancelled(p, ModActions.SURVIVAL_FORM.get(), true), "a forced shift was not cancelled");
        helper.assertTrue(!formCancelled(p, ModActions.HUMAN_FORM.get(), false), "going back to human form must stay possible");

        // natural regeneration (FoodData) is cancelled; other healing is not
        ServerPlayer control = werewolf(helper);
        control.setHealth(10f);
        control.getFoodData().setFoodLevel(20);
        control.getFoodData().setSaturation(5f);
        for (int i = 0; i < 12; i++) {
            control.getFoodData().tick(control);
        }
        helper.assertTrue(control.getHealth() > 10f, "control: natural regeneration did not heal (" + control.getHealth() + ")");
        p.setHealth(10f);
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(5f);
        for (int i = 0; i < 12; i++) {
            p.getFoodData().tick(p);
        }
        helper.assertTrue(p.getHealth() == 10f, "natural regeneration healed through lichen: " + p.getHealth());
        p.heal(2f);
        helper.assertTrue(p.getHealth() == 12f, "a direct heal (potion, golden apple) was cancelled: " + p.getHealth());
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void coatedWeaponAppliesStrain(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        var anvil = new AnvilUpdateEvent(sword, new ItemStack(WolfsbaneStrains.YELLOW_PLANT.get()), "", 0, TestPlayers.create(helper));
        NeoForge.EVENT_BUS.post(anvil);
        ItemStack coated = anvil.getOutput();
        var coating = coated.get(WolfsbaneStrains.COATING.get());
        helper.assertTrue(coating != null && coating.strain() == Strain.YELLOW && coating.hits() == 8, "no coating on the anvil output: " + coating);
        helper.assertTrue(sword.get(WolfsbaneStrains.COATING.get()) == null, "the input sword was changed");

        var pickaxe = new AnvilUpdateEvent(new ItemStack(Items.IRON_PICKAXE), new ItemStack(WolfsbaneStrains.YELLOW_PLANT.get()), "", 0, TestPlayers.create(helper));
        NeoForge.EVENT_BUS.post(pickaxe);
        helper.assertTrue(pickaxe.getOutput().isEmpty(), "a pickaxe was coated");

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, coated);
        ServerPlayer human = TestPlayers.createMortal(helper);
        zombie.doHurtTarget(human);
        helper.assertTrue(human.getActiveEffects().isEmpty(), "a coated weapon poisoned a human");
        helper.assertTrue(zombie.getMainHandItem().get(WolfsbaneStrains.COATING.get()).hits() == 8, "a hit on a human used a charge");

        ServerPlayer wolf = werewolf(helper);
        wolf.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
        for (int i = 1; i <= 8; i++) {
            wolf.setHealth(wolf.getMaxHealth());
            wolf.invulnerableTime = 0;
            wolf.removeEffect(WolfsbaneStrains.YELLOW_EFFECT);
            zombie.doHurtTarget(wolf);
            helper.assertTrue(has(wolf, WolfsbaneStrains.YELLOW_EFFECT), "hit " + i + " did not apply the strain");
            var left = zombie.getMainHandItem().get(WolfsbaneStrains.COATING.get());
            helper.assertTrue(i == 8 ? left == null : left != null && left.hits() == 8 - i, "wrong hits left after hit " + i + ": " + left);
        }
        wolf.setHealth(wolf.getMaxHealth());
        wolf.invulnerableTime = 0;
        wolf.removeEffect(WolfsbaneStrains.YELLOW_EFFECT);
        zombie.doHurtTarget(wolf);
        helper.assertTrue(!has(wolf, WolfsbaneStrains.YELLOW_EFFECT), "a used up coating still applied the strain");
        helper.succeed();
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void lichenGrowsOnlyOnSolidGround(GameTestHelper helper) {
        BlockPos on = new BlockPos(2, 1, 2);
        helper.setBlock(on, Blocks.STONE);
        helper.setBlock(on.above(), WolfsbaneStrains.LICHEN_BLOCK.get());
        helper.assertTrue(helper.getBlockState(on.above()).is(WolfsbaneStrains.LICHEN_BLOCK.get()), "lichen cannot stand on stone");
        helper.setBlock(on, Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(on.above()).isAir(), "lichen floats without a floor");
            helper.succeed();
        });
    }
}
