package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlockCommand;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RitePageItem;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;
import net.ofatech.supernaturalbloodlines.shared.species.RiteLearned;

/** Plan rites-unlock: what a player knows of the rites is two advancements per rite, and exactly one of them is done. */
@PrefixGameTestTemplate(false)
public final class RiteUnlockGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private RiteUnlockGameTests() {}

    private static LineTestPlayer player(GameTestHelper helper, Species species) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        return p;
    }

    private static boolean known(ServerPlayer p, String rite) {
        return RiteUnlocks.done(p, RiteAdvancements.known(rite));
    }

    private static boolean sealed(ServerPlayer p, String rite) {
        return RiteUnlocks.done(p, RiteAdvancements.sealed(rite));
    }

    private static AdvancementHolder holder(ServerPlayer p, String id) {
        return p.server.getAdvancements().get(ResourceLocation.parse(id));
    }

    /** The invariant, for every gated rite: known xor sealed. */
    private static String broken(ServerPlayer p) {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            if (known(p, rite) == sealed(p, rite)) {
                return rite + " known=" + known(p, rite) + " sealed=" + sealed(p, rite);
            }
        }
        return null;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAdvancementsAreLoaded(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            helper.assertTrue(holder(p, RiteAdvancements.known(rite)) != null, rite + ": no known advancement in the datapack");
            helper.assertTrue(holder(p, RiteAdvancements.sealed(rite)) != null, rite + ": no sealed advancement in the datapack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNewPlayerIsSealedOutOfEveryGatedRite(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            helper.assertTrue(!known(p, rite), rite + " is known to a plain human");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void becomingAKindTeachesItsRites(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(!known(p, "rite_of_severance"), "control: Severance known before being a witch");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        RiteUnlocks.reconcile(p);
        helper.assertTrue(known(p, "rite_of_severance") && known(p, "ward_of_return") && known(p, "rite_of_the_cure"), "a witch does not know the witch rites");
        helper.assertTrue(!sealed(p, "rite_of_severance"), "a known rite is still sealed");
        helper.assertTrue(!known(p, "rite_of_cloaking") && sealed(p, "rite_of_cloaking"), "a witch learned a lore rite by being a witch");
        helper.assertTrue(!known(p, "alpha_trial"), "a witch learned a werewolf rite");
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loreTeachesItsRitesOnce(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(RiteUnlocks.learnLore(p, RiteUnlockTable.FINN) == 2, "Finn's Notes did not teach two rites");
        helper.assertTrue(known(p, "rite_of_the_broken_line") && known(p, "rite_of_the_imprisoned_original"), "the rites are not known");
        helper.assertTrue(!sealed(p, "rite_of_the_broken_line"), "a learned rite is still sealed");
        helper.assertTrue(!known(p, "rite_of_the_cure"), "another lore's rite was taught");
        helper.assertTrue(RiteUnlocks.learnLore(p, RiteUnlockTable.FINN) == 0, "a second reading taught again");
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void performingARiteTeachesIt(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(!known(p, "immortality"), "control");
        RiteLearned.performed(p, NS + ":immortality");
        helper.assertTrue(known(p, "immortality") && !sealed(p, "immortality"), "the performer did not learn the rite");
        RiteLearned.performed(p, NS + ":purification"); // an open rite teaches nothing and is not an error
        RiteLearned.performed(p, "enchanted:something"); // another mod's rite likewise
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAdminsGrantOrRevokeCannotLeaveBothOrNeither(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        String rite = "immortality";
        // /advancement grant on the sealed one of a rite that is known
        RiteUnlocks.learn(p, rite, false);
        p.getAdvancements().award(holder(p, RiteAdvancements.sealed(rite)), RiteAdvancements.CRITERION);
        helper.assertTrue(known(p, rite) && !sealed(p, rite), "both: the sealed one was kept for a known rite");
        // /advancement revoke on the known one: it is not known, so it is sealed again
        p.getAdvancements().revoke(holder(p, RiteAdvancements.known(rite)), RiteAdvancements.CRITERION);
        helper.assertTrue(!known(p, rite) && sealed(p, rite), "neither: the rite is neither known nor sealed");
        // /advancement revoke on the sealed one of a rite that is not known
        p.getAdvancements().revoke(holder(p, RiteAdvancements.sealed(rite)), RiteAdvancements.CRITERION);
        helper.assertTrue(sealed(p, rite), "neither: the sealed one was not put back");
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void knowledgeIsKeptWhenTheKindIsLost(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.WITCH);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(known(p, "ward_of_return"), "control");
        BloodlineAccess.set(p, BloodlineState.NONE);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(known(p, "ward_of_return") && !sealed(p, "ward_of_return"), "a rite was forgotten with the kind");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTornPageTeachesEveryTimeAndIsNeverUsedUp(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(p);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var page = RitePageItem.of(RiteUnlockTable.ELIJAH);
        p.setItemInHand(hand, page);
        page.getItem().use(helper.getLevel(), p, hand);
        helper.assertTrue(known(p, "rite_of_the_cure"), "the page did not teach the Cure");
        helper.assertTrue(!p.getItemInHand(hand).isEmpty(), "the page was used up");
        // a page that was read for a player who forgot teaches again, like a grimoire
        RiteUnlocks.forget(p, "rite_of_the_cure");
        page.getItem().use(helper.getLevel(), p, hand);
        helper.assertTrue(known(p, "rite_of_the_cure") && !p.getItemInHand(hand).isEmpty(), "a kept page did not teach a second time");
        // read when there is nothing new, it says so
        page.getItem().use(helper.getLevel(), p, hand);
        helper.assertTrue(p.received("supernaturalbloodlines.rites.nothing_new"), "a page with nothing new did not say so: " + p.heard());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlankPageSaysItHasNothing(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(p);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var blank = new net.minecraft.world.item.ItemStack(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.RITE_PAGE.get());
        p.setItemInHand(hand, blank);
        blank.getItem().use(helper.getLevel(), p, hand);
        helper.assertTrue(p.received("supernaturalbloodlines.rites.nothing_new"), "a blank page said nothing: " + p.heard());
        helper.assertTrue(!p.getItemInHand(hand).isEmpty() && RiteUnlocks.known(p).isEmpty(), "a blank page did something");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void severalRitesLearnedAtOnceAreOneLine(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(RiteUnlocks.learnLore(p, RiteUnlockTable.ESTHER) == 2, "control: Esther teaches two");
        helper.assertTrue(p.count("supernaturalbloodlines.rites.learned") == 1, "two rites were two lines: " + p.heard());
        helper.assertTrue(p.heard().contains("rite.supernaturalbloodlines.rite_of_linking") && p.heard().contains("rite.supernaturalbloodlines.rite_of_severance"), "the line does not name both: " + p.heard());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void becomingAKindSaysSoOnlyWhenAskedAndOnlyWhenThereIsNews(GameTestHelper helper) {
        LineTestPlayer p = player(helper, Species.NONE);
        helper.assertTrue(RiteUnlocks.reconcile(p, true) == 0 && !p.received("supernaturalbloodlines.rites.new_kind"), "nothing is new for a human: " + p.heard());
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        // the change itself already taught them, quietly (a player who has just joined is not told about the login catching up)
        helper.assertTrue(known(p, "rite_of_severance"), "a witch learned nothing");
        helper.assertTrue(!p.received("supernaturalbloodlines.rites.new_kind"), "the login said it: " + p.heard());
        RiteUnlocks.forget(p, "rite_of_severance");
        helper.assertTrue(RiteUnlocks.reconcile(p, true) == 1 && p.count("supernaturalbloodlines.rites.new_kind") == 1, "a rite that came back by kind was not announced once: " + p.heard());
        helper.assertTrue(RiteUnlocks.reconcile(p, true) == 0 && p.count("supernaturalbloodlines.rites.new_kind") == 1, "an unchanged player was announced again");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMovedNoticeIsForTheFirstLoginOnly(GameTestHelper helper) {
        LineTestPlayer p = player(helper, Species.NONE);
        helper.assertTrue(RiteUnlocks.firstTime(p), "a new player was not first");
        helper.assertTrue(!RiteUnlocks.firstTime(p), "the same player was first twice");
        helper.assertTrue(RiteUnlocks.firstTime(player(helper, Species.NONE)), "another player shares the flag");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withTheSpeciesPartOffEveryRiteIsKnown(GameTestHelper helper) {
        LineTestPlayer p = LineTestPlayer.createWithAdvancements(helper);
        RiteUnlocks.reconcile(p);
        helper.assertTrue(RiteUnlocks.known(p).isEmpty(), "control: a human knows a gated rite");
        net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockFallback.knowAll(p);
        helper.assertTrue(RiteUnlocks.known(p).size() == RiteUnlockTable.GATED.size(), "not every rite is known: " + RiteUnlocks.known(p));
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandTeachesAndForgets(GameTestHelper helper) {
        ServerPlayer p = player(helper, Species.NONE);
        RiteUnlocks.reconcile(p);
        var source = p.createCommandSourceStack().withPermission(2);
        helper.assertTrue(RiteUnlockCommand.change(source, p, "esther", true) == 2, "learning Esther's rites changed " + RiteUnlocks.known(p));
        helper.assertTrue(known(p, "rite_of_linking") && known(p, "rite_of_severance"), "not learned");
        helper.assertTrue(RiteUnlockCommand.change(source, p, "rite_of_linking", false) == 1, "forgetting one rite");
        helper.assertTrue(!known(p, "rite_of_linking") && sealed(p, "rite_of_linking") && known(p, "rite_of_severance"), "the wrong rite was forgotten");
        helper.assertTrue(RiteUnlockCommand.change(source, p, "all", true) == RiteUnlockTable.GATED.size() - 1, "all did not teach the rest");
        helper.assertTrue(RiteUnlockCommand.change(source, p, "nonsense", true) == 0, "an unknown rite changed something");
        helper.assertTrue(RiteUnlockCommand.change(source, p, "all", false) == RiteUnlockTable.GATED.size(), "forgetting all");
        helper.assertTrue(broken(p) == null, "known and sealed disagree: " + broken(p));
        helper.assertTrue(RiteUnlockCommand.list(source, p) == 0, "the list of a player who knows nothing");
        helper.succeed();
    }
}
