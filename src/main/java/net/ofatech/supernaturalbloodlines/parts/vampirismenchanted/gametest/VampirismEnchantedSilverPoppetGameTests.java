package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import net.favouriteless.enchanted.common.enchanted.poppet.PoppetColour;
import net.favouriteless.enchanted.common.items.poppets.PlayerPoppetItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.PartItems;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.VampirismEnchantedPoppetGameTests.*;

/** The Silver Poppet. Registered by the part only when Werewolves is loaded, so the item is never null here. */
@PrefixGameTestTemplate(false)
public final class VampirismEnchantedSilverPoppetGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private VampirismEnchantedSilverPoppetGameTests() {
    }

    private static DamageSource silverHit(GameTestHelper helper, ItemStack weapon) {
        // a mob, not a player: the test server has PvP off, so a player attacker would never hurt anyone
        Zombie attacker = EntityType.ZOMBIE.create(helper.getLevel());
        attacker.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        return helper.getLevel().damageSources().mobAttack(attacker);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverPoppetBlocksSilver(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SILVER_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(silverHit(helper, new ItemStack(item("werewolves:silver_sword"))), before + 100);
        helper.assertTrue(player.isAlive(), "a lethal silver hit killed a player with a bound silver poppet");
        helper.assertTrue(player.getHealth() == before, "lethal silver damage got through: " + player.getHealth() + " of " + before);
        helper.assertTrue(used(player, poppet), "the silver poppet was not used");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverPoppetBlocksWerewolfBite(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SILVER_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(damage(helper, "werewolves:bite"), before + 100);
        helper.assertTrue(player.isAlive() && player.getHealth() == before, "a lethal werewolf bite got through: " + player.getHealth());
        helper.assertTrue(used(player, poppet), "the silver poppet was not used");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverPoppetIgnoresOtherDamage(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.createVulnerable(helper);
        Item poppet = PartItems.SILVER_POPPET.get();
        bound(player, poppet);
        float before = player.getHealth();
        player.hurt(silverHit(helper, new ItemStack(item("minecraft:iron_sword"))), before + 100);
        helper.assertTrue(!player.isAlive() && player.getHealth() <= 0, "a lethal iron sword hit was absorbed by the silver poppet");
        helper.assertTrue(!used(player, poppet), "an iron sword hit used up the silver poppet");

        PlayerPoppetItem item = (PlayerPoppetItem) poppet;
        helper.assertTrue(item.protectsAgainst(damage(helper, "werewolves:bite")), "the bite is not protected");
        helper.assertTrue(item.protectsAgainst(silverHit(helper, new ItemStack(item("werewolves:silver_sword")))), "a silver sword is not protected");
        helper.assertTrue(!item.protectsAgainst(damage(helper, DamageTypes.ON_FIRE)), "fire is protected");
        helper.assertTrue(!item.protectsAgainst(damage(helper, "werewolves:blood_loss")), "blood loss is protected");
        helper.assertTrue(!item.protectsAgainst(damage(helper, "vampirism:sun_damage")), "sun damage is protected");
        helper.assertTrue(!item.protectsAgainst(damage(helper, DamageTypes.GENERIC_KILL)), "an unarmed generic hit is protected");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverPoppetMatchesEnchantedStyle(GameTestHelper helper) {
        PlayerPoppetItem poppet = PartItems.SILVER_POPPET.get();
        PlayerPoppetItem sun = PartItems.SUN_POPPET.get();
        helper.assertTrue(poppet.getColour().equals(PoppetColour.EARTH), "the silver poppet is not earth coloured");
        helper.assertTrue(poppet.getDefaultInstance().getMaxDamage() == sun.getDefaultInstance().getMaxDamage(), "durability differs from the sun poppet");
        helper.assertTrue(poppet.getDescriptionId().equals("item." + NS + ".silver_poppet"), "wrong item id");
        helper.succeed();
    }
}
