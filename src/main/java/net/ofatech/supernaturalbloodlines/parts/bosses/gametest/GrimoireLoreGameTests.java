package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.RiteLoreBackfill;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/** Plan rites-unlock: the two grimoires teach their rites when read, every time, and are never used up. */
@PrefixGameTestTemplate(false)
public final class GrimoireLoreGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GrimoireLoreGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kolsGrimoireTeachesCloakingEvenAfterItWasRead(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
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
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
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

    /** A world that paid a boss before the rites were learnable: the first login teaches that boss's lore, once. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerLearnsTheLoreOfTheBossesAlreadyRewardedOnceOnly(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(p);
        BossIndex index = BossIndex.get(helper.getLevel().getServer());
        boolean kol = index.of(BossId.KOL).rewarded;
        boolean finn = index.of(BossId.FINN).rewarded;
        boolean elijah = index.of(BossId.ELIJAH).rewarded;
        try {
            index.of(BossId.KOL).rewarded = false;
            index.of(BossId.FINN).rewarded = true;
            index.of(BossId.ELIJAH).rewarded = false;
            int fresh = RiteLoreBackfill.backfill(p);
            int expected = 4 + (RiteUnlocks.present(p, "evolve_blood_sovereign") ? 1 : 0); // Finn's Notes also teach the Blood Sovereign where Ascension is installed
            helper.assertTrue(fresh == expected, "Finn's reward teaches Esther's and Finn's rites (" + expected + "), not " + fresh + ": " + RiteUnlocks.known(p));
            helper.assertTrue(RiteUnlocks.done(p, RiteAdvancements.known("rite_of_linking")) && RiteUnlocks.done(p, RiteAdvancements.known("rite_of_the_broken_line")), "Finn's lore is not known");
            helper.assertTrue(!RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")) && !RiteUnlocks.done(p, RiteAdvancements.known("rite_of_the_cure")), "a boss that was not rewarded taught its lore");
            helper.assertTrue(p.count("supernaturalbloodlines.rites.learned") == 1, "four rites were not one line: " + p.heard());
            // once: what an admin takes away later stays taken away
            RiteUnlocks.forget(p, "rite_of_linking");
            index.of(BossId.KOL).rewarded = true;
            helper.assertTrue(RiteLoreBackfill.backfill(p) == 0 && !RiteUnlocks.done(p, RiteAdvancements.known("rite_of_linking")) && !RiteUnlocks.done(p, RiteAdvancements.known("rite_of_cloaking")),
                    "the backfill ran twice");
            // another player is a new login
            LineTestPlayer q = LineTestPlayer.createWithAdvancements(helper);
            helper.assertTrue(RiteLoreBackfill.backfill(q) >= 5, "a second player did not get the lore of Kol and Finn: " + RiteUnlocks.known(q));
        } finally {
            index.of(BossId.KOL).rewarded = kol;
            index.of(BossId.FINN).rewarded = finn;
            index.of(BossId.ELIJAH).rewarded = elijah;
        }
        helper.succeed();
    }

    /** Dying and respawning makes a new player object: the once-flags must come with it, or the notice and the backfill would run again. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOnceFlagsSurviveDeath(GameTestHelper helper) {
        LineTestPlayer before = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(before);
        BossIndex index = BossIndex.get(helper.getLevel().getServer());
        boolean finn = index.of(BossId.FINN).rewarded;
        try {
            index.of(BossId.FINN).rewarded = true;
            helper.assertTrue(RiteUnlocks.firstTime(before), "control: the first login was not first");
            helper.assertTrue(RiteLoreBackfill.backfill(before) > 0, "control: the backfill taught nothing");
            LineTestPlayer after = LineTestPlayer.createWithAdvancements(helper);
            RiteUnlocks.reconcile(after);
            after.restoreFrom(before, false);
            helper.assertTrue(!RiteUnlocks.firstTime(after), "the notice would be shown again after a death");
            helper.assertTrue(RiteLoreBackfill.backfill(after) == 0 && !RiteUnlocks.done(after, RiteAdvancements.known("rite_of_linking")), "the backfill would run again after a death");
            helper.assertTrue(!after.received("supernaturalbloodlines.rites.learned"), "a respawn was told it learned something: " + after.heard());
        } finally {
            index.of(BossId.FINN).rewarded = finn;
        }
        helper.succeed();
    }
}
