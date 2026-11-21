package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/** Plan rites-unlock: the two grimoires teach their rites when read, every time, and are never used up. */
@PrefixGameTestTemplate(false)
public final class GrimoireLoreGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GrimoireLoreGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kolsGrimoireTeachesCloakingEvenAfterItWasRead(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        RiteUnlocks.reconcile(p);
        ItemStack grimoire = new ItemStack(BossItems.KOL_GRIMOIRE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, grimoire);
        helper.assertTrue(!RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")), "control");
        grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")), "reading Kol's Grimoire did not teach Cloaking");
        helper.assertTrue(!RiteUnlocks.done(p, RiteAdvancements.sealed("rite_of_cloaking")), "Cloaking is still sealed");
        helper.assertTrue(!p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the grimoire was used up");
        // a copy that was read for its skill points still teaches a player who has not learned the rite
        RiteUnlocks.forget(p, "rite_of_cloaking");
        grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")), "a read copy did not teach");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void esthersGrimoireTeachesLinkingAndSeveranceAndIsKept(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        RiteUnlocks.reconcile(p);
        ItemStack grimoire = new ItemStack(BossItems.ESTHER_GRIMOIRE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, grimoire);
        grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(RiteUnlocks.done(p, RiteAdvancements.known("rite_of_linking")) && RiteUnlocks.done(p, RiteAdvancements.known("rite_of_severance")),
                "Esther's Grimoire did not teach Linking and Severance");
        helper.assertTrue(!p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the grimoire was used up");
        helper.assertTrue(!RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")), "it taught Kol's rite");
        grimoire.getItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.received("supernaturalbloodlines.rites.nothing_new"), "a second reading was not answered: " + p.heard());
        helper.succeed();
    }
}
