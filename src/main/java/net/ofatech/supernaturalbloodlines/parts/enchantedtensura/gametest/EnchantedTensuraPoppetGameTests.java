package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest;

import net.favouriteless.enchanted.common.enchanted.poppet.PoppetColour;
import net.favouriteless.enchanted.common.enchanted.poppet.PoppetHelper;
import net.favouriteless.enchanted.common.init.EItems;
import net.favouriteless.enchanted.common.items.poppets.PlayerPoppetItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.PartItems;

/**
 * The Spirit Poppet. Enchanted only consults player poppets on a would-be lethal hit (damage at least the player's
 * health), so "blocks spiritual damage" means: lethal spiritual damage is cancelled and the poppet is used.
 */
@PrefixGameTestTemplate(false)
public final class EnchantedTensuraPoppetGameTests {
    private EnchantedTensuraPoppetGameTests() {
    }

    private static DamageSource tensura(GameTestHelper helper, String path) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("tensura", path));
        return new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    private static DamageSource vanilla(GameTestHelper helper, ResourceKey<DamageType> key) {
        return new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    private static void boundPoppet(ServerPlayer player) {
        ItemStack poppet = new ItemStack(PartItems.SPIRIT_POPPET.get());
        PoppetHelper.bind(poppet, player);
        player.getInventory().add(poppet);
    }

    /** {@code Inventory.add} copies a damageable stack, so look at what is in the inventory, not the original. */
    private static boolean used(ServerPlayer player) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(PartItems.SPIRIT_POPPET.get())) {
                return s.getDamageValue() > 0;
            }
        }
        return true;
    }

    @GameTest(template = "empty", templateNamespace = SupernaturalBloodlines.MODID)
    public static void spiritPoppetBlocksSpiritualDamage(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        boundPoppet(player);
        float before = player.getHealth();
        player.hurt(tensura(helper, "soul_scatter"), before + 100);
        helper.assertTrue(player.isAlive(), "a lethal spiritual hit killed a player with a bound spirit poppet");
        helper.assertTrue(player.getHealth() == before, "lethal spiritual damage got through: " + player.getHealth() + " of " + before);
        helper.assertTrue(used(player), "the spirit poppet was not used");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SupernaturalBloodlines.MODID)
    public static void spiritPoppetIgnoresNonLethalHits(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        boundPoppet(player);
        float before = player.getHealth();
        player.hurt(tensura(helper, "soul_scatter"), 1);
        helper.assertTrue(player.getHealth() < before, "a non-lethal spiritual hit did no damage, so Enchanted's lethal-only rule is not in play");
        helper.assertTrue(!used(player), "a non-lethal hit used up the poppet");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SupernaturalBloodlines.MODID)
    public static void spiritPoppetOnlyProtectsAgainstSpiritualTypes(GameTestHelper helper) {
        PlayerPoppetItem poppet = PartItems.SPIRIT_POPPET.get();
        for (String id : new String[] {"soul_scatter", "soul_consumed", "synthesise"}) {
            helper.assertTrue(poppet.protectsAgainst(tensura(helper, id)), "tensura:" + id + " is not protected");
        }
        helper.assertTrue(!poppet.protectsAgainst(vanilla(helper, DamageTypes.ON_FIRE)), "fire is protected");
        helper.assertTrue(!poppet.protectsAgainst(vanilla(helper, DamageTypes.GENERIC_KILL)), "generic damage is protected");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = SupernaturalBloodlines.MODID)
    public static void spiritPoppetMatchesEnchantedProtectionPoppets(GameTestHelper helper) {
        PlayerPoppetItem poppet = PartItems.SPIRIT_POPPET.get();
        helper.assertTrue(poppet.getColour().equals(PoppetColour.MAGIC), "the spirit poppet is not magic coloured");
        helper.assertTrue(poppet.getDefaultInstance().getMaxDamage() == EItems.MAGIC_POPPET.get().getDefaultInstance().getMaxDamage(),
                "durability differs from Enchanted's magic poppet: " + poppet.getDefaultInstance().getMaxDamage());
        helper.assertTrue(poppet.getDescriptionId().equals("item." + SupernaturalBloodlines.MODID + ".spirit_poppet"), "wrong item id");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                        ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "spinning/spirit_poppet")).isPresent(),
                "the spirit poppet recipe did not load");
        helper.succeed();
    }
}
