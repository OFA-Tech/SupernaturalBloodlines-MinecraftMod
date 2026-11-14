package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import static net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.BloodSkillGameTests.feed;
import static net.ofatech.supernaturalbloodlines.parts.supernatural.gametest.BloodSkillGameTests.from;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskEvent;
import net.ofatech.supernaturalbloodlines.core.supernatural.TaskSpec.Detector;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillService;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;

/** Phase 12c Task 4: Q-VAM-6 "Hunter of Hunters" and what Predator's Thirst does to a bite on a vampire. */
@PrefixGameTestTemplate(false)
public final class PredatorsThirstGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private PredatorsThirstGameTests() {}

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        return p;
    }

    private static SupernaturalState state(ServerPlayer p) {
        return SupernaturalAccess.get(p);
    }

    private static void at(ServerPlayer p, int rung, int sp, String... skills) {
        SupernaturalAccess.set(p, state(p).toBuilder().rung(SupernaturalRace.VAMPIRE, rung).sp(sp).spEarned(sp).skills(Set.of(skills)).build());
    }

    private static void emit(ServerPlayer p, Detector d, long n, String... args) {
        TaskTracker.emit(p, TaskEvent.of(d, n, args));
    }

    private static void finishQuest(ServerPlayer p) {
        emit(p, Detector.KILL, 10, "category", "vampire");
        emit(p, Detector.SIGNAL, 3, "signal", "vampire_bite_refused");
        emit(p, Detector.FEED, 25);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void questOpensAtRung(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        at(p, 3, 0);
        emit(p, Detector.KILL, 4, "category", "vampire");
        helper.assertTrue(!state(p).counters().containsKey("Q-VAM-6a"), "the quest counted before L-VAM-4 was reached: " + state(p).counters());
        at(p, 4, 0);
        emit(p, Detector.KILL, 4, "category", "vampire");
        helper.assertTrue(state(p).counters().getOrDefault("Q-VAM-6a", 0L) == 4, "the quest did not count at L-VAM-4: " + state(p).counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void questCompletes(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        at(p, 4, 0);
        emit(p, Detector.KILL, 10, "category", "vampire");
        emit(p, Detector.FEED, 25);
        if (EnabledParts.isEnabled("species")) {
            helper.assertTrue(!state(p).quests().contains("Q-VAM-6"), "the quest finished without the bite task");
            emit(p, Detector.SIGNAL, 2, "signal", "vampire_bite_refused");
            helper.assertTrue(!state(p).quests().contains("Q-VAM-6"), "the quest finished after 2 refusals");
            emit(p, Detector.SIGNAL, 1, "signal", "vampire_bite_refused");
        }
        helper.assertTrue(state(p).quests().contains("Q-VAM-6"), "the quest did not finish: " + state(p));
        helper.assertTrue(state(p).sp() == 0 && state(p).spEarned() == 0, "a quest paid");
        helper.assertTrue(state(p).counters().keySet().stream().noneMatch(k -> k.startsWith("Q-VAM-6")), "task records stayed: " + state(p).counters());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cannotBuyWithoutQuest(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        at(p, 4, 9, "S-VAM-1");
        helper.assertTrue(SkillService.buy(p, "S-VAM-6").refusal() == SkillRules.Refusal.QUEST, "S-VAM-6 was bought without Q-VAM-6");
        finishQuest(p);
        helper.assertTrue(SkillService.buy(p, "S-VAM-6").ok() && state(p).sp() == 5, "S-VAM-6 was refused after the quest: " + state(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void predatorsThirstFeedsAndDrains(GameTestHelper helper) {
        ServerPlayer biter = vampire(helper);
        ServerPlayer victim = vampire(helper);
        VampirismAPI.vampirePlayer(victim).drinkBlood(10, 0.1f, from(EntityType.VILLAGER.create(helper.getLevel())));
        int before = VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel();
        helper.assertTrue(before >= 4, "setup: the victim has " + before + " blood");
        at(biter, 4, 0, "S-VAM-1", "S-VAM-6");
        helper.assertTrue(feed(biter, victim, 0) == SupernaturalConfig.DEFAULT_VAMPIRE_FEED_BLOOD, "no value from Vampirism: the biter gets the configured blood");
        helper.assertTrue(VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel() == before - 3, "the victim lost "
                + (before - VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel()));
        helper.assertTrue(feed(biter, victim, 2) == 2, "Vampirism's own value is kept");
        helper.assertTrue(VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel() == before - 5, "the second bite drained another 2");
        helper.succeed();
    }

    // its own batch: a vampire mob in the world would be found by a compass test running in the next structure
    @GameTest(template = "empty", templateNamespace = NS, batch = "predators_thirst_mob")
    public static void aVampireMobIsHurtButNeverKilled(GameTestHelper helper) {
        ServerPlayer biter = vampire(helper);
        at(biter, 4, 0, "S-VAM-6");
        Mob mob = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire")).create(helper.getLevel());
        mob.setNoAi(true);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 2, 4.5));
        mob.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(mob);
        mob.setHealth(mob.getMaxHealth());
        float full = mob.getHealth();
        helper.assertTrue(feed(biter, mob, 0) == 3, "the biter got nothing from a vampire mob");
        // Vampirism trims damage to its own vampires, so the figure is not exactly 2
        helper.assertTrue(full - mob.getHealth() > 0.5f && full - mob.getHealth() <= 2.01f && mob.hasEffect(MobEffects.WEAKNESS), "the mob: " + mob.getHealth() + " of " + full);
        mob.setHealth(1.5f);
        feed(biter, mob, 0);
        helper.assertTrue(mob.isAlive() && mob.getHealth() >= 1f, "the drain killed the mob");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalVictimSurvivesDrain(GameTestHelper helper) {
        ServerPlayer biter = vampire(helper);
        ServerPlayer original = vampire(helper);
        BloodlineAccess.set(original, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).turnedAt(1).build());
        VampirismAPI.vampirePlayer(original).drinkBlood(10, 0.1f, from(EntityType.VILLAGER.create(helper.getLevel())));
        int before = VampirismAPI.vampirePlayer(original).getBloodStats().getBloodLevel();
        original.setHealth(1f);
        at(biter, 4, 0, "S-VAM-6");
        for (int i = 0; i < 6; i++) {
            feed(biter, original, 0);
        }
        helper.assertTrue(original.isAlive() && original.getHealth() == 1f, "the drain hurt an Original: " + original.getHealth());
        helper.assertTrue(VampirismAPI.vampirePlayer(original).getBloodStats().getBloodLevel() < before, "the Original was not drained");
        helper.succeed();
    }

    /** Plan 11's rule is the species part's: a bite on a vampire gives the biter nothing and costs them blood. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void noSkillStillRefused(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species")) {
            helper.succeed();
            return;
        }
        ServerPlayer biter = vampire(helper);
        ServerPlayer victim = vampire(helper);
        at(biter, 4, 0, "S-VAM-1");
        int before = VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel();
        helper.assertTrue(feed(biter, victim, 2) == 0, "a bite on a vampire fed a biter without Predator's Thirst");
        helper.assertTrue(VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel() == before, "the victim was drained");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDormantSkillDoesNothing(GameTestHelper helper) {
        ServerPlayer biter = vampire(helper);
        ServerPlayer victim = vampire(helper);
        at(biter, 4, 0, "S-VAM-6");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(biter).setFactionAndLevel(null, 0), "could not leave the vampires");
        int before = VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel();
        feed(biter, victim, 2);
        helper.assertTrue(VampirismAPI.vampirePlayer(victim).getBloodStats().getBloodLevel() == before, "a dormant Predator's Thirst drained");
        helper.succeed();
    }
}