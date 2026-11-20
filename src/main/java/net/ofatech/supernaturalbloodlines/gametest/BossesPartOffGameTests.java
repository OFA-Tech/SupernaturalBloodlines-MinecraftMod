package net.ofatech.supernaturalbloodlines.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;

/**
 * Plan 17: the bosses part is a switch. Off ({@code -PpartsOff=bosses}, or species off, or GeckoLib missing) it leaves no entity type, no item and no
 * command; on, it has all three. Runs in every matrix run, so both sides are seen.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class BossesPartOffGameTests {
    private BossesPartOffGameTests() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, path);
    }

    @GameTest(template = "empty")
    public static void thePartLeavesNothingBehindWhenOffAndEverythingWhenOn(GameTestHelper helper) {
        boolean on = EnabledParts.isEnabled("bosses");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("mikael")) == on, "entity type: part " + (on ? "on" : "off") + ", registry says " + !on);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id("mikaels_stake")) == on, "item: part " + (on ? "on" : "off") + ", registry says " + !on);
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("klaus")) == on, "klaus: part " + (on ? "on" : "off") + ", registry says " + !on);
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id("klaus_blood")) == on, "klaus_blood: part " + (on ? "on" : "off") + ", registry says " + !on);
        boolean command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild(SupernaturalBloodlines.MODID).getChild("boss") != null;
        helper.assertTrue(command == on, "command: part " + (on ? "on" : "off") + ", dispatcher says " + command);
        helper.succeed();
    }
}
