package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.race.vampire.VampireRace;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Plan 13c Task 2: Tensura's vampire race ability, used on a creature by a Vampirism vampire, fills the same Vampirism blood that a Vampirism bite
 * would, and takes it from the victim. Needs Tensura and Vampirism. The ability is run the way ManasCore runs it: the activation event first, then
 * {@code VampireRace.onActivateAbility}.
 */
@PrefixGameTestTemplate(false)
public final class TensuraFeedGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TensuraFeedGameTests() {
    }

    private static ServerPlayer vampire(GameTestHelper helper) {
        ServerPlayer p = BiteGameTests.vampire(helper);
        helper.assertTrue(RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("tensura:vampire"), false), "could not give the player Tensura's vampire race");
        p.setYRot(-90f);
        p.setYHeadRot(-90f);
        p.setXRot(0f);
        return p;
    }

    private static void useRaceAbility(ServerPlayer p) {
        ManasRaceInstance inst = RaceAPI.getRaceFrom(p).getRace().orElseThrow();
        RaceEvents.ACTIVATE_ABILITY.invoker().activateAbility(inst, p);
        ((VampireRace) inst.getRace()).onActivateAbility(inst, p);
    }

    private static int blood(ServerPlayer p) {
        return VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void raceFeedFillsBlood(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        p.setHealth(10f);
        int mine = blood(p);
        int theirs = BiteGameTests.blood(v);
        useRaceAbility(p);
        helper.assertTrue(v.getHealth() < v.getMaxHealth(), "Tensura's feed did not run (the villager was not hurt)");
        helper.assertTrue(blood(p) == mine + 2, "the vampire's blood is " + blood(p) + ", was " + mine);
        helper.assertTrue(BiteGameTests.blood(v) == theirs - 2, "the villager's blood is " + BiteGameTests.blood(v) + ", was " + theirs);
        v.discard();
        helper.succeed();
    }

    /** Tensura's own test is "hurt or food not full"; a vampire's food is never short, so without the change a thirsty, healthy vampire got no feed. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThirstyHealthyVampireFeeds(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        helper.assertTrue(p.getHealth() >= p.getMaxHealth(), "setup: not healthy");
        int mine = blood(p);
        useRaceAbility(p);
        helper.assertTrue(v.getHealth() < v.getMaxHealth(), "a thirsty healthy vampire got no feed (the villager was not hurt)");
        helper.assertTrue(blood(p) == mine + 2, "the vampire's blood is " + blood(p) + ", was " + mine);
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withTheSwitchOffTensuraKeepsItsOwnBehaviour(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        p.setHealth(10f);
        int mine = blood(p);
        net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.TENSURA_FEED_FILLS_BLOOD.set(false);
        try {
            useRaceAbility(p);
        } finally {
            net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.TENSURA_FEED_FILLS_BLOOD.set(true);
        }
        helper.assertTrue(v.getHealth() < v.getMaxHealth(), "Tensura's own feed (hurt vampire) should still run");
        helper.assertTrue(blood(p) == mine, "blood was credited with the switch off: " + blood(p) + ", was " + mine);
        v.discard();
        helper.succeed();
    }

    /** Hurt, so Tensura's own feed runs, but the blood is full: the victim must not be drained for blood that cannot be taken. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFullVampireDoesNotDrainTheVictim(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        p.setHealth(10f);
        int mine = blood(p);
        int theirs = BiteGameTests.blood(v);
        useRaceAbility(p);
        helper.assertTrue(v.getHealth() < v.getMaxHealth(), "Tensura's feed did not run (the villager was not hurt)");
        helper.assertTrue(blood(p) == mine, "blood changed from " + mine + " to " + blood(p));
        helper.assertTrue(BiteGameTests.blood(v) == theirs, "the villager was drained to " + BiteGameTests.blood(v) + " from " + theirs + " for nothing");
        v.discard();
        helper.succeed();
    }

    /** A vampire victim is Vampirism's and the supernatural part's business (a refused bite, or Predator's Thirst, which drains by itself): we drain nothing. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampirePlayerVictimIsNotDrainedByTheRaceFeed(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        ServerPlayer victim = net.ofatech.supernaturalbloodlines.gametest.TestPlayers.createDuelist(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(victim).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1), "could not make the victim a vampire");
        BiteGameTests.place(helper, victim, 2.5);
        helper.getLevel().addNewPlayer(victim);
        boolean pvp = helper.getLevel().getServer().isPvpAllowed();
        int theirs;
        try {
            helper.getLevel().getServer().setPvpAllowed(true);
            helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the biter's blood");
            p.setHealth(10f);
            theirs = blood(victim);
            float health = victim.getHealth();
            // the race ability's own target search is not what is under test (it may skip players); what it does to the victim is: the activation, then its blood_drain hit
            RaceEvents.ACTIVATE_ABILITY.invoker().activateAbility(RaceAPI.getRaceFrom(p).getRace().orElseThrow(), p);
            helper.assertTrue(victim.hurt(TensuraDamageTypes.getEntityDamageSource(helper.getLevel(), TensuraDamageTypes.BLOOD_DRAIN, p), 2f), "the drain hit did not land");
            helper.assertTrue(victim.getHealth() < health, "the vampire victim was not hurt");
        } finally {
            helper.getLevel().getServer().setPvpAllowed(pvp);
        }
        helper.assertTrue(blood(victim) == theirs, "the vampire victim lost blood without Predator's Thirst: " + blood(victim) + ", was " + theirs);
        helper.succeed();
    }

    /** Vervain, a desiccated biter: something empties the drink on purpose. The victim must not lose blood the vampire never got. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aDrinkSomethingEmptiedDoesNotDrainTheVictim(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        p.setHealth(10f);
        int mine = blood(p);
        int theirs = BiteGameTests.blood(v);
        java.util.function.Consumer<de.teamlapen.vampirism.api.event.BloodDrinkEvent.PlayerDrinkBloodEvent> empty = e -> e.setAmount(0);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, empty);
        try {
            useRaceAbility(p);
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(empty);
        }
        helper.assertTrue(v.getHealth() < v.getMaxHealth(), "Tensura's feed did not run (the villager was not hurt)");
        helper.assertTrue(blood(p) == mine, "the emptied drink gave blood: " + blood(p) + ", was " + mine);
        helper.assertTrue(BiteGameTests.blood(v) == theirs, "the villager lost blood for an emptied drink: " + BiteGameTests.blood(v) + ", was " + theirs);
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aVampireWithFullBloodAndHealthDoesNotFeed(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        PathfinderMob v = BiteGameTests.villager(helper, 2.5);
        int mine = blood(p);
        useRaceAbility(p);
        helper.assertTrue(v.getHealth() == v.getMaxHealth(), "the villager was hurt by a vampire that did not need to feed");
        helper.assertTrue(blood(p) == mine, "blood changed from " + mine + " to " + blood(p));
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBloodlessVictimGivesNoBlood(GameTestHelper helper) {
        ServerPlayer p = vampire(helper);
        IronGolem golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        golem.setNoAi(true);
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 1.5));
        golem.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(golem);
        helper.assertTrue(VampirismAPI.vampirePlayer(p).useBlood(15, true), "could not empty the vampire's blood");
        p.setHealth(10f);
        int mine = blood(p);
        useRaceAbility(p);
        helper.assertTrue(blood(p) == mine, "a golem gave blood: " + blood(p) + ", was " + mine);
        golem.discard();
        helper.succeed();
    }
}
