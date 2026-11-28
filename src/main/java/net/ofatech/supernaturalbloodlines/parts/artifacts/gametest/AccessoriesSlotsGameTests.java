package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactSlots;
import net.ofatech.supernaturalbloodlines.parts.artifacts.accessories.AccessoriesSlots;
import io.wispforest.accessories.api.AccessoriesCapability;

/** Links Accessories, so the part registers this class only when Accessories is loaded. */
@PrefixGameTestTemplate(false)
public final class AccessoriesSlotsGameTests {
 private static final String NS = SupernaturalBloodlines.MODID;

 private AccessoriesSlotsGameTests() {
 }

 private static ItemStack named(String name) {
 ItemStack s = new ItemStack(Items.GOLD_INGOT);
 s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
 return s;
 }

 private static void equipInRingSlot(GameTestHelper helper, ServerPlayer p, ItemStack stack) {
 var cap = AccessoriesCapability.get(p);
 helper.assertTrue(cap.isPresent(), "the player has no Accessories capability");
 var handler = cap.get().getEquipped("ring");
 helper.assertTrue(handler.isPresent(), "the player has no ring slot (slots: " + cap.get().getEquipped().keySet() + ")");
 handler.get().setStackInSlot(0, stack);
 }

 @GameTest(template = "empty", templateNamespace = NS)
 public static void ringFoundInAccessoriesSlot(GameTestHelper helper) {
 helper.assertTrue(!ArtifactSlots.isWorn(TestPlayers.create(helper), Items.GOLD_INGOT), "control: worn without a ring");

 ServerPlayer p = TestPlayers.create(helper);
 ItemStack ring = named("accessory");
 equipInRingSlot(helper, p, ring);

 helper.assertTrue(AccessoriesSlots.isEquipped(p, Items.GOLD_INGOT), "Accessories does not report the ring as equipped");
 boolean inInventory = java.util.stream.Stream.of(p.getInventory().items, p.getInventory().armor, p.getInventory().offhand)
 .flatMap(java.util.List::stream).anyMatch(s -> s.is(Items.GOLD_INGOT));
 helper.assertTrue(!inInventory, "the ring ended up in the inventory");
 helper.assertTrue(ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "not worn from the Accessories ring slot");
 helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).orElseThrow() == ring,
 "findWorn did not return the equipped stack itself");
 helper.succeed();
 }

 @GameTest(template = "empty", templateNamespace = NS)
 public static void accessoriesCheckedBeforeInventory(GameTestHelper helper) {
 ServerPlayer p = TestPlayers.create(helper);
 p.getInventory().items.set(20, named("inventory"));
 ItemStack ring = named("accessory");
 equipInRingSlot(helper, p, ring);

 helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).orElseThrow() == ring,
 "the inventory copy won over the Accessories one");
 helper.succeed();
 }
}
