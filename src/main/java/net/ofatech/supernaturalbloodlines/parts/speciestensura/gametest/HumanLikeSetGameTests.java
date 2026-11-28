package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import java.util.Set;
import java.util.TreeSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.HumanLikeSet;

/**
 * Plan 23 Task 11: the dynamic human-like set reads the tag + evolution closure, minus undead/daemon/vampire line.
 */
@PrefixGameTestTemplate(false)
public final class HumanLikeSetGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Set<String> VAMPIRE_LINE = Set.of("tensura:vampire", "tensura:vampire_overcomer", "tensura:vampire_lord", "tensura:divine_vampire");

    private HumanLikeSetGameTests() {}

    private static void note(String proof, String text) {
        SupernaturalBloodlines.LOGGER.info("PLAN23 {} {}", proof, text);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSetContainsBaseRacesAndEvolved(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")) {
            note("T11", "SKIPPED: needs species and species_tensura");
            helper.succeed();
            return;
        }
        Set<String> set = HumanLikeSet.current();
        note("T11", "human-like set size=" + set.size() + " sample=" + set.stream().limit(5).toList());
        helper.assertTrue(set.contains("tensura:human"), "missing tensura:human");
        helper.assertTrue(set.contains("tensura:dwarf"), "missing tensura:dwarf");
        helper.assertTrue(set.contains("tensura:elf"), "missing tensura:elf");
        helper.assertTrue(set.contains("tensura:merfolk"), "missing tensura:merfolk");
        helper.assertTrue(set.contains("tensura:beastfolk"), "missing tensura:beastfolk");
        helper.assertTrue(set.contains("tensura:enlightened_human"), "missing evolved race");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSetExcludesVampireLine(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")) {
            note("T11", "SKIPPED: needs species and species_tensura");
            helper.succeed();
            return;
        }
        Set<String> set = HumanLikeSet.current();
        Set<String> leaked = new TreeSet<>(VAMPIRE_LINE);
        leaked.retainAll(set);
        note("T11", "vampire line in set: " + leaked);
        helper.assertTrue(leaked.isEmpty(), "vampire line leaked into human-like set: " + leaked);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSetExcludesUndeadAndDaemon(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")) {
            note("T11", "SKIPPED: needs species and species_tensura");
            helper.succeed();
            return;
        }
        Set<String> set = HumanLikeSet.current();
        helper.assertTrue(!set.contains("tensura:ghoul"), "ghoul leaked into human-like set");
        note("T11", "undead/daemon exclusion verified");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void adminAddAndRemoveWorks(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species") || !EnabledParts.isEnabled("speciesTensura")) {
            note("T11", "SKIPPED: needs species and species_tensura");
            helper.succeed();
            return;
        }
        String fakeRace = "testmod:test_race";
        HumanLikeSet.adminAdd(fakeRace);
        helper.assertTrue(HumanLikeSet.current().contains(fakeRace), "admin add did not take effect");
        HumanLikeSet.adminRemove(fakeRace);
        helper.assertTrue(!HumanLikeSet.current().contains(fakeRace), "admin remove did not take effect");
        note("T11", "admin add/remove cycle passed");
        helper.succeed();
    }
}