package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactSlots;

/** Inventory path of the slot helper. Runs with and without Curios; with {@code -PnoCurios} no Curios class may load. */
@PrefixGameTestTemplate(false)
public final class ArtifactSlotsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ArtifactSlotsGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ringWorksWithoutCurios(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(!ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "control: worn before it was given");
        helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).isEmpty(), "control: found before it was given");

        ItemStack main = new ItemStack(Items.GOLD_INGOT);
        main.set(DataComponents.CUSTOM_NAME, Component.literal("bound"));
        p.getInventory().items.set(20, main);
        helper.assertTrue(ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "not worn from the main inventory");
        helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).orElseThrow() == main,
                "findWorn did not return the stack itself (components would be edited on a copy)");
        helper.assertTrue(!ArtifactSlots.isWorn(p, Items.IRON_INGOT), "a different item counts as worn");
        p.getInventory().items.set(20, ItemStack.EMPTY);

        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "not worn from the offhand");
        p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "not worn from an armor slot");
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        helper.assertTrue(!ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "still worn after it was removed");
        helper.succeed();
    }
}
