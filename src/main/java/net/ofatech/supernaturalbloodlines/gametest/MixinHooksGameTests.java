package net.ofatech.supernaturalbloodlines.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesMixinPlugin;
import net.ofatech.supernaturalbloodlines.mixin.SupernaturalMixinPlugin;

/**
 * Plan 13d M9: the Enchanted mixins are {@code require = 0}, so a renamed Enchanted method would skip them silently. Their plugins prove each hook by
 * finding the handler calls in the target methods; here every mixin that should have applied must have, with Enchanted installed and its part on.
 * Loading the target class is what applies a mixin, so each one is loaded first.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class MixinHooksGameTests {
    private MixinHooksGameTests() {
    }

    @GameTest(template = "empty")
    public static void theEnchantedHooksLanded(GameTestHelper helper) throws ClassNotFoundException {
        if (!ModList.get().isLoaded("enchanted")) {
            helper.succeed();
            return;
        }
        if (EnabledParts.isEnabled("species")) {
            Class.forName("net.favouriteless.enchanted.common.blocks.entity.GoldChalkBlockEntity");
            helper.assertTrue(SpeciesMixinPlugin.riteCostHooked(), "RiteCostMixin did not land in GoldChalkBlockEntity (an Enchanted update?)");
        }
        if (EnabledParts.isEnabled("supernatural")) {
            Class.forName("net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite");
            helper.assertTrue(SupernaturalMixinPlugin.riteHooked(), "RiteCompleteMixin did not land in Rite (an Enchanted update?)");
        }
        helper.succeed();
    }
}
