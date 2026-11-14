package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillEffects;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillService;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/** Buying skills on real players: the server's checks, the shared pool, and the modifiers that follow the active trees. */
@PrefixGameTestTemplate(false)
public final class SkillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SkillGameTests() {}

    private static ServerPlayer vampire(GameTestHelper helper, int level) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level), "could not make a vampire");
        return p;
    }

    private static SupernaturalState state(ServerPlayer p) {
        return SupernaturalAccess.get(p);
    }

    private static void give(ServerPlayer p, int sp, SupernaturalRace race, int rung, String... skills) {
        SupernaturalAccess.set(p, state(p).toBuilder().sp(sp).spEarned(sp).rung(race, rung).skills(Set.of(skills)).build());
    }

    private static boolean has(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String skill) {
        return p.getAttribute(attribute).hasModifier(SkillEffects.modifierId(skill));
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void buyPaysAndOwns(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 5, SupernaturalRace.VAMPIRE, 1);
        helper.assertTrue(!LadderQuery.owns(p, "S-VAM-1"), "owned before buying");
        helper.assertTrue(SkillService.buy(p, "S-VAM-1").ok(), "the purchase was refused");
        helper.assertTrue(state(p).sp() == 4 && state(p).spEarned() == 5 && state(p).skills().equals(Set.of("S-VAM-1")), "state: " + state(p));
        helper.assertTrue(LadderQuery.owns(p, "S-VAM-1"), "not owned after buying");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyRefusalChangesNothing(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 1, SupernaturalRace.VAMPIRE, 2);
        SupernaturalState before = state(p);
        // rung, SP, prerequisite, an unknown id, a skill of another race's tree, a granted one, and ids a client could invent
        for (String id : new String[] {"S-VAM-3", "S-VAM-4", "S-VAM-7", "S-NOPE", "S-WOL-1", "S-WOL-8", "", "x", "../../etc", "\u0000", "s-vam-1"}) {
            SkillRules.BuyResult r = SkillService.buy(p, id);
            helper.assertTrue(!r.ok() && state(p).equals(before), "'" + id + "' was bought: " + r.refusal());
        }
        helper.assertTrue(SkillService.buy(p, "S-VAM-3").refusal() == SkillRules.Refusal.RUNG, "S-VAM-3 needs rung 3");
        helper.assertTrue(SkillService.buy(p, "S-VAM-4").refusal() == SkillRules.Refusal.RUNG, "S-VAM-4 needs rung 3");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aQuestSkillNeedsItsQuest(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 9, SupernaturalRace.VAMPIRE, 4, "S-VAM-1");
        helper.assertTrue(SkillService.buy(p, "S-VAM-6").refusal() == SkillRules.Refusal.QUEST, "S-VAM-6 was bought without Q-VAM-6");
        SupernaturalAccess.set(p, state(p).toBuilder().quests(Set.of("Q-VAM-6")).build());
        helper.assertTrue(SkillService.buy(p, "S-VAM-6").ok() && state(p).sp() == 5, "S-VAM-6 was refused after the quest: " + state(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulBuysNothing(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 9, SupernaturalRace.VAMPIRE, 4);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        SupernaturalState before = state(p);
        helper.assertTrue(SkillService.buy(p, "S-VAM-1").refusal() == SkillRules.Refusal.GHOUL_FROZEN, "a Ghoul bought a skill");
        helper.assertTrue(state(p).equals(before), "a Ghoul's refusal changed the state");
        helper.succeed();
    }

    /** A skill bought as one race stays owned, and is paid once, when another tree lists it too (a vampire who becomes an Original). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void sharedSkillBoughtOnce(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 5, SupernaturalRace.VAMPIRE, 1);
        helper.assertTrue(SkillService.buy(p, "S-VAM-1").ok() && state(p).sp() == 4, "the first purchase");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
        helper.assertTrue(LadderQuery.owns(p, "S-VAM-1"), "the Original tree lists S-VAM-1, so it is still owned");
        helper.assertTrue(SkillService.buy(p, "S-VAM-1").refusal() == SkillRules.Refusal.ALREADY_OWNED && state(p).sp() == 4, "bought twice");
        helper.succeed();
    }

    /** A witch-werewolf has both trees and one pool: a werewolf skill is paid once and works as a witch-werewolf. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchWerewolfBuysOnce(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves")) {
            helper.succeed();
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        Wolves.join(helper, p);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        give(p, 5, SupernaturalRace.WEREWOLF, 1);
        helper.assertTrue(SkillService.buy(p, "S-WOL-1").ok() && state(p).sp() == 4, "the werewolf skill: " + state(p));
        helper.assertTrue(SkillService.buy(p, "S-WIT-1").refusal() == SkillRules.Refusal.RUNG, "the witch tree has its own rung");
        helper.assertTrue(has(p, Attributes.ARMOR, "S-WOL-1"), "the werewolf skill does not work for a witch-werewolf");
        helper.assertTrue(SkillService.buy(p, "S-WOL-1").refusal() == SkillRules.Refusal.ALREADY_OWNED && state(p).sp() == 4, "bought twice");
        helper.succeed();
    }

    /** A vampire cured to human loses S-VAM-4's +2 health; rejoining the vampires brings it back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void dormantSkillRemovesModifier(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 0, SupernaturalRace.VAMPIRE, 3, "S-VAM-4");
        SkillEffects.sync(p);
        helper.assertTrue(has(p, Attributes.MAX_HEALTH, "S-VAM-4"), "the active skill has no modifier");
        helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).getModifier(SkillEffects.modifierId("S-VAM-4")).amount() == 2, "not +2 health");
        helper.assertTrue(LadderQuery.owns(p, "S-VAM-4"), "not owned while a vampire");
        // cured: the faction is gone, the race is human, and the human tree does not list S-VAM-4
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0), "could not leave the vampires");
        helper.assertTrue(!has(p, Attributes.MAX_HEALTH, "S-VAM-4"), "the dormant skill kept its modifier");
        helper.assertTrue(!LadderQuery.owns(p, "S-VAM-4") && state(p).skills().contains("S-VAM-4"), "dormant must not be owned, and must not be lost");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not rejoin");
        helper.assertTrue(has(p, Attributes.MAX_HEALTH, "S-VAM-4") && LadderQuery.owns(p, "S-VAM-4"), "the skill did not come back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void timeConditionsFollowTheSky(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            give(p, 0, SupernaturalRace.HYBRID, 5, "S-VAM-2", "S-WOL-7");
            level.setDayTime(6000); // noon
            SkillEffects.sync(p);
            helper.assertTrue(!has(p, Attributes.MOVEMENT_SPEED, "S-VAM-2") && !has(p, Attributes.ATTACK_DAMAGE, "S-WOL-7"), "a night skill works at noon");
            level.setDayTime(18000); // midnight on a full-moon day
            SkillEffects.sync(p);
            helper.assertTrue(has(p, Attributes.MOVEMENT_SPEED, "S-VAM-2") && has(p, Attributes.ATTACK_DAMAGE, "S-WOL-7"), "night skills are off at a full-moon midnight");
            level.setDayTime(24000 + 18000); // night, but the moon is not full
            SkillEffects.sync(p);
            helper.assertTrue(has(p, Attributes.MOVEMENT_SPEED, "S-VAM-2") && !has(p, Attributes.ATTACK_DAMAGE, "S-WOL-7"), "Moon Heart works without a full moon");
        } finally {
            level.setDayTime(saved);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ruleNumbersComeFromTheJson(GameTestHelper helper) {
        ServerPlayer p = vampire(helper, 1);
        give(p, 0, SupernaturalRace.VAMPIRE, 3);
        helper.assertTrue(LadderQuery.value(p, "S-VAM-3", "range", 4) == 4, "a skill nobody owns changed the number");
        give(p, 0, SupernaturalRace.VAMPIRE, 3, "S-VAM-3");
        helper.assertTrue(LadderQuery.value(p, "S-VAM-3", "range", 4) == 6, "the owned skill did not give its number");
        helper.assertTrue(LadderQuery.value(p, "S-VAM-3", "no_such_number", 4) == 4, "an unknown key changed the number");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0), "could not leave the vampires");
        helper.assertTrue(LadderQuery.value(p, "S-VAM-3", "range", 4) == 4, "a dormant skill changed the number");
        helper.succeed();
    }

    /** Keeps the Werewolves classes out of this one's linking when the mod is missing. */
    private static final class Wolves {
        static void join(GameTestHelper helper, ServerPlayer p) {
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION, 1),
                    "could not make a werewolf");
        }
    }
}
