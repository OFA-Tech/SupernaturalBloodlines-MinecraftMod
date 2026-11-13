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
import net.ofatech.supernaturalbloodlines.parts.artifacts.curios.CuriosSlots;
import top.theillusivec4.curios.api.CuriosApi;

/** Links Curios, so the part registers this class only when Curios is loaded. */
@PrefixGameTestTemplate(false)
public final class CuriosSlotsGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CuriosSlotsGameTests() {
    }

    private static ItemStack named(String name) {
        ItemStack s = new ItemStack(Items.GOLD_INGOT);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    private static void equipInRingSlot(GameTestHelper helper, ServerPlayer p, ItemStack stack) {
        var inv = CuriosApi.getCuriosInventory(p);
        helper.assertTrue(inv.isPresent(), "the player has no Curios inventory");
        helper.assertTrue(inv.get().getStacksHandler("ring").isPresent(),
                "the player has no ring slot (slots: " + inv.get().getCurios().keySet() + ")");
        // straight into the slot: the test item is not in #curios:ring, and Curios moves invalid stacks out via setEquippedCurio
        inv.get().getStacksHandler("ring").get().getStacks().setStackInSlot(0, stack);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ringFoundInCuriosSlot(GameTestHelper helper) {
        // Curios caches lookups per player for the current game tick, so the control uses another player
        helper.assertTrue(!ArtifactSlots.isWorn(TestPlayers.create(helper), Items.GOLD_INGOT), "control: worn without a ring");

        ServerPlayer p = TestPlayers.create(helper);
        ItemStack ring = named("curio");
        equipInRingSlot(helper, p, ring);

        helper.assertTrue(CuriosSlots.isEquipped(p, Items.GOLD_INGOT), "Curios does not report the ring as equipped");
        // Inventory.contains is patched by Curios to look into the curio slots too, so look at the vanilla lists
        boolean inInventory = java.util.stream.Stream.of(p.getInventory().items, p.getInventory().armor, p.getInventory().offhand)
                .flatMap(java.util.List::stream).anyMatch(s -> s.is(Items.GOLD_INGOT));
        helper.assertTrue(!inInventory, "the ring ended up in the inventory");
        helper.assertTrue(ArtifactSlots.isWorn(p, Items.GOLD_INGOT), "not worn from the Curios ring slot");
        helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).orElseThrow() == ring,
                "findWorn did not return the equipped stack itself");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curiosCheckedBeforeInventory(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        p.getInventory().items.set(20, named("inventory"));
        ItemStack ring = named("curio");
        equipInRingSlot(helper, p, ring);

        helper.assertTrue(ArtifactSlots.findWorn(p, Items.GOLD_INGOT).orElseThrow() == ring,
                "the inventory copy won over the Curios one");
        helper.succeed();
    }
}
