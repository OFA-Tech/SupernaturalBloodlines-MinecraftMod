package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.altar.PowerProvider;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.PartBlocks;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.PartItems;

/** The Purification and Magicule Offering rites and the block the offering makes. */
@PrefixGameTestTemplate(false)
public final class EnchantedTensuraRiteGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private EnchantedTensuraRiteGameTests() {
    }

    // ---- helpers ----

    private static RiteType type(GameTestHelper helper, String path) {
        return helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY)
                .get(ResourceLocation.fromNamespaceAndPath(NS, path));
    }

    private static BlockPos centre(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(1, 1, 1));
    }

    private static Rite start(GameTestHelper helper, String path, Player caster) {
        Rite rite = type(helper, path).create(helper.getLevel(), centre(helper), caster.getUUID(), List.of());
        rite.start();
        return rite;
    }

    /** FakePlayers are not in the level, which is where the rites look for players: put one there at {@code pos}. */
    private static void place(ServerLevel level, ServerPlayer p, Vec3 pos) {
        p.setPos(pos);
        level.addNewPlayer(p);
    }

    private static void remove(ServerLevel level, ServerPlayer p) {
        level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static void setRace(Entity e, String id) {
        RaceAPI.getRaceFrom((LivingEntity) e).setRace(ResourceLocation.parse(id), false);
    }

    private static Vec3 near(GameTestHelper helper, double dx) {
        return Vec3.atBottomCenterOf(centre(helper)).add(dx, 1, 0);
    }

    private static int dropped(GameTestHelper helper) {
        int n = 0;
        for (ItemEntity i : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5))) {
            if (i.getItem().is(PartItems.CONDENSED_MAGICULE.get())) {
                n += i.getItem().getCount();
            }
        }
        return n;
    }

    private static void clearDropped(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre(helper)).inflate(1.5)).forEach(Entity::discard);
    }

    // ---- data ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ritesLoad(GameTestHelper helper) {
        RiteType purification = type(helper, "purification");
        helper.assertTrue(purification != null && purification.getPower() == 3000, "purification rite missing or wrong power");
        RiteType offering = type(helper, "magicule_offering");
        helper.assertTrue(offering != null && offering.getPower() == 0, "magicule offering rite missing or wrong power");
        helper.assertTrue(offering.getOutputs().stream().anyMatch(s -> s.is(PartItems.CONDENSED_MAGICULE.get())),
                "the offering rite does not list the condensed magicule as its output");
        if (net.neoforged.fml.ModList.get().isLoaded("vampirism")) {
            RiteType holy = type(helper, "purification_holy_water");
            helper.assertTrue(holy != null && holy.getPower() == 3000, "holy water purification rite missing or wrong power");
            helper.assertTrue(!holy.getItems().equals(purification.getItems()), "the two purification rites have the same items");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void holyPurificationDamageIsHoly(GameTestHelper helper) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(NS, "holy_purification"));
        DamageSource source = new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key));
        helper.assertTrue(TensuraDamageHelper.isHoly(source), "Tensura does not see supernaturalbloodlines:holy_purification as holy");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void condensedMagiculeIsAnAltarBlock(GameTestHelper helper) {
        PowerProvider p = PowerProvider.get(helper.getLevel(), PartBlocks.CONDENSED_MAGICULE.get());
        helper.assertTrue(p != null, "no altar power data for condensed_magicule");
        helper.assertTrue(p.power() == 30 && p.limit() == 16, "condensed_magicule has " + p.power() + "/" + p.limit());
        helper.assertTrue(PartBlocks.CONDENSED_MAGICULE.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "condensed_magicule is not in #minecraft:mineable/pickaxe");
        helper.assertTrue(PartItems.CONDENSED_MAGICULE.get().getDescriptionId().equals("block." + NS + ".condensed_magicule"),
                "the block item is not the condensed magicule block");
        helper.succeed();
    }

    // ---- Purification ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void purificationHurtsUndeadRace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer caster = TestPlayers.create(helper);
        ServerPlayer ghoul = TestPlayers.createVulnerable(helper);
        place(level, ghoul, near(helper, 2));
        try {
            setRace(ghoul, "tensura:ghoul");
            float before = ghoul.getHealth();
            start(helper, "purification", caster);
            helper.assertTrue(ghoul.getHealth() < before, "an undead-race player took no damage: " + ghoul.getHealth() + " of " + before);
        } finally {
            remove(level, ghoul);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void purificationHurtsUndeadMobsInRangeOnly(GameTestHelper helper) {
        ServerPlayer caster = TestPlayers.create(helper);
        var near = helper.spawnWithNoFreeWill(EntityType.HUSK, 2.5f, 1f, 1.5f);
        var far = helper.spawnWithNoFreeWill(EntityType.HUSK, 1.5f + 12, 1f, 1.5f);
        float nearBefore = near.getHealth();
        float farBefore = far.getHealth();
        start(helper, "purification", caster);
        helper.assertTrue(near.getHealth() < nearBefore, "a husk in range took no damage");
        helper.assertTrue(far.getHealth() == farBefore, "a husk 12 blocks away took damage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void purificationSparesLiving(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer caster = TestPlayers.create(helper);
        ServerPlayer human = TestPlayers.createVulnerable(helper);
        place(level, human, near(helper, 2));
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, 2.5f, 1f, 1.5f);
        try {
            setRace(human, "tensura:human");
            float before = human.getHealth();
            float cowBefore = cow.getHealth();
            start(helper, "purification", caster);
            helper.assertTrue(human.getHealth() == before, "a human took holy damage: " + human.getHealth() + " of " + before);
            helper.assertTrue(cow.getHealth() == cowBefore, "a cow took holy damage");
        } finally {
            remove(level, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void purificationHitsUndeadCasterAndSparesLivingCaster(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer ghoul = TestPlayers.createVulnerable(helper);
        ServerPlayer human = TestPlayers.createVulnerable(helper);
        place(level, ghoul, near(helper, 1));
        place(level, human, near(helper, -1));
        try {
            setRace(ghoul, "tensura:ghoul");
            setRace(human, "tensura:human");
            float ghoulBefore = ghoul.getHealth();
            float humanBefore = human.getHealth();
            start(helper, "purification", ghoul);
            helper.assertTrue(ghoul.getHealth() < ghoulBefore, "an undead caster was not purified");
            helper.assertTrue(human.getHealth() == humanBefore, "a living bystander was hurt");
            start(helper, "purification", human);
            helper.assertTrue(human.getHealth() == humanBefore, "a living caster was hurt by their own rite");
        } finally {
            remove(level, ghoul);
            remove(level, human);
        }
        helper.succeed();
    }

    // ---- Magicule Offering ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offeringFailsWithoutMagicules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(level, caster, near(helper, 0));
        try {
            IExistence ex = TensuraStorages.getExistenceFrom(caster);
            ex.setMagicule(100);
            helper.assertTrue(ex.getMagicule() == 100, "could not give the test player magicules");
            start(helper, "magicule_offering", caster);
            helper.assertTrue(ex.getMagicule() == 100, "a failed offering took magicules: " + ex.getMagicule());
            helper.assertTrue(dropped(helper) == 0, "a failed offering dropped condensed magicule");
            helper.assertTrue(caster.receivedOnActionBar("supernaturalbloodlines.enchanted_tensura.offering_failed"),
                    "the caster was not told on the action bar that the offering failed");
        } finally {
            remove(level, caster);
            clearDropped(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offeringConsumesMagicules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(level, caster, near(helper, 0));
        try {
            IExistence ex = TensuraStorages.getExistenceFrom(caster);
            ex.setMagicule(6000);
            helper.assertTrue(ex.getMagicule() == 6000, "could not give the test player 6000 magicules: " + ex.getMagicule());
            start(helper, "magicule_offering", caster);
            helper.assertTrue(ex.getMagicule() == 1000, "expected 1000 magicules left, have " + ex.getMagicule());
            helper.assertTrue(dropped(helper) == 1, "expected one condensed magicule, found " + dropped(helper));
            helper.assertTrue(!caster.received("supernaturalbloodlines.enchanted_tensura.offering_failed"),
                    "a good offering sent the failure message");
        } finally {
            remove(level, caster);
            clearDropped(helper);
        }
        helper.succeed();
    }
}
