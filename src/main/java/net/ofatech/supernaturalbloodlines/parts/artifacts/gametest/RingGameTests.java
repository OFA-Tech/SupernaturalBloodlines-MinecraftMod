package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.SunRing;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;

/** The Sun Ring and the bind rite (they link no Werewolves class; the Moon Ring has its own class). */
@PrefixGameTestTemplate(false)
public final class RingGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceKey<DamageType> SUN_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"));

    private RingGameTests() {
    }

    static DamageSource sun(GameTestHelper helper) {
        return new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SUN_DAMAGE));
    }

    static ItemStack ring(Item item, UUID boundTo) {
        ItemStack s = new ItemStack(item);
        if (boundTo != null) {
            s.set(ArtifactItems.BOUND_TO.get(), boundTo);
        }
        return s;
    }

    /** Hurts with sun damage from a clean slate; true when the health went down. */
    private static boolean sunHurts(GameTestHelper helper, ServerPlayer p) {
        p.setHealth(20f);
        p.invulnerableTime = 0;
        p.hurt(sun(helper), 4f);
        return p.getHealth() < 20f;
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createVulnerable(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static List<ItemStack> droppedRings(GameTestHelper helper, Item item) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(RiteTestSupport.centre(helper)).inflate(1.5))) {
            if (i.getItem().is(item)) {
                out.add(i.getItem());
            }
        }
        return out;
    }

    // ---- protection ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingBlocksSunForBoundVampire(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        helper.assertTrue(sunHurts(helper, p), "control: sun damage did not hurt without a ring");
        p.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), p.getUUID()));
        helper.assertTrue(!sunHurts(helper, p), "the bound Sun Ring did not stop the sun damage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingOnlyProtectsBoundVampire(GameTestHelper helper) {
        ServerPlayer owner = vampire(helper);
        ServerPlayer thief = vampire(helper);
        thief.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), owner.getUUID()));
        helper.assertTrue(sunHurts(helper, thief), "a ring bound to someone else protected its wearer");

        thief.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), null));
        helper.assertTrue(sunHurts(helper, thief), "an unbound ring protected its wearer");

        // a foreign ring in front of their own must not hide it
        thief.getInventory().items.set(4, ring(ArtifactItems.SUN_RING.get(), owner.getUUID()));
        thief.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), thief.getUUID()));
        helper.assertTrue(!sunHurts(helper, thief), "a foreign ring hid the wearer's own ring");

        // only sun damage is stopped
        thief.setHealth(20f);
        thief.invulnerableTime = 0;
        thief.hurt(helper.getLevel().damageSources().generic(), 4f);
        helper.assertTrue(thief.getHealth() < 20f, "the Sun Ring stopped damage that is not sun damage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingKeepsSunscreenForBoundVampire(GameTestHelper helper) {
        Holder<MobEffect> sunscreen = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:sunscreen")).orElseThrow();
        ServerPlayer p = vampire(helper);
        SunRing.refreshSunscreen(p);
        helper.assertTrue(!p.hasEffect(sunscreen), "sunscreen without a ring");

        p.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), p.getUUID()));
        SunRing.refreshSunscreen(p);
        helper.assertTrue(p.hasEffect(sunscreen), "no sunscreen with the bound ring");

        ServerPlayer other = vampire(helper);
        other.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), p.getUUID()));
        SunRing.refreshSunscreen(other);
        helper.assertTrue(!other.hasEffect(sunscreen), "sunscreen from a ring bound to someone else");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sunRingLeavesARealSunscreenAlone(GameTestHelper helper) {
        Holder<MobEffect> sunscreen = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:sunscreen")).orElseThrow();
        ServerPlayer p = vampire(helper);
        p.getInventory().items.set(5, ring(ArtifactItems.SUN_RING.get(), p.getUUID()));

        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(sunscreen, 6000, 0));
        SunRing.refreshSunscreen(p);
        helper.assertTrue(p.getEffect(sunscreen).getDuration() > 5000 && p.getEffect(sunscreen).isVisible(), "a visible sunscreen was replaced");

        p.removeEffect(sunscreen);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(sunscreen, 100, 3, true, false, false));
        SunRing.refreshSunscreen(p);
        helper.assertTrue(p.getEffect(sunscreen).getAmplifier() == 3, "a stronger sunscreen was replaced");
        helper.succeed();
    }

    // ---- the bind rite ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfDaylightBindsTarget(GameTestHelper helper) {
        ServerPlayer target = vampire(helper);
        RiteTestSupport.place(helper, target);
        try {
            RiteTestSupport.start(helper, "ward_of_daylight", target, List.of(ring(ArtifactItems.SUN_RING.get(), null)));
            List<ItemStack> rings = droppedRings(helper, ArtifactItems.SUN_RING.get());
            helper.assertTrue(rings.size() == 1, "expected one ring, got " + rings.size());
            helper.assertTrue(target.getUUID().equals(rings.get(0).get(ArtifactItems.BOUND_TO.get())), "the ring is not bound to the player on the circle");
        } finally {
            RiteTestSupport.remove(helper, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfDaylightRefundsOnWrongTarget(GameTestHelper helper) {
        ServerPlayer notVampire = TestPlayers.createVulnerable(helper);
        RiteTestSupport.place(helper, notVampire);
        try {
            RiteTestSupport.start(helper, "ward_of_daylight", notVampire, List.of(ring(ArtifactItems.SUN_RING.get(), null)));
            List<ItemStack> rings = droppedRings(helper, ArtifactItems.SUN_RING.get());
            helper.assertTrue(rings.size() == 1, "expected the ring back, got " + rings.size());
            helper.assertTrue(rings.get(0).get(ArtifactItems.BOUND_TO.get()) == null, "a non-vampire got a bound ring");
        } finally {
            RiteTestSupport.remove(helper, notVampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfDaylightRefundsWithoutTarget(GameTestHelper helper) {
        // not placed in the level: the rite finds neither a player on the circle nor its caster
        ServerPlayer caster = vampire(helper);
        try {
            RiteTestSupport.start(helper, "ward_of_daylight", caster, List.of(ring(ArtifactItems.SUN_RING.get(), null)));
            List<ItemStack> rings = droppedRings(helper, ArtifactItems.SUN_RING.get());
            helper.assertTrue(rings.size() == 1, "expected the ring back, got " + rings.size());
            helper.assertTrue(rings.get(0).get(ArtifactItems.BOUND_TO.get()) == null, "a ring was bound without a target");
        } finally {
            RiteTestSupport.clearDropped(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void wardOfDaylightDoesNotRebindABoundRing(GameTestHelper helper) {
        ServerPlayer owner = vampire(helper);
        ServerPlayer thief = vampire(helper);
        RiteTestSupport.place(helper, thief);
        try {
            RiteTestSupport.start(helper, "ward_of_daylight", thief, List.of(ring(ArtifactItems.SUN_RING.get(), owner.getUUID())));
            List<ItemStack> rings = droppedRings(helper, ArtifactItems.SUN_RING.get());
            helper.assertTrue(rings.size() == 1, "expected the ring back, got " + rings.size());
            helper.assertTrue(owner.getUUID().equals(rings.get(0).get(ArtifactItems.BOUND_TO.get())), "a bound ring was rebound");
        } finally {
            RiteTestSupport.remove(helper, thief);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bindRiteNeedsItsInput(GameTestHelper helper) {
        ServerPlayer target = vampire(helper);
        RiteTestSupport.place(helper, target);
        try {
            RiteTestSupport.start(helper, "ward_of_daylight", target, List.of(new ItemStack(net.minecraft.world.item.Items.SUNFLOWER)));
            helper.assertTrue(droppedRings(helper, ArtifactItems.SUN_RING.get()).isEmpty(), "a ring came out of nothing");
            helper.assertTrue(RiteTestSupport.dropped(helper, "minecraft:sunflower") == 1, "the ingredient was not given back");
        } finally {
            RiteTestSupport.remove(helper, target);
        }
        helper.succeed();
    }
}
