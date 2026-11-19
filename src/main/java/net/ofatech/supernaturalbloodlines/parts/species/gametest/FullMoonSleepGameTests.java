package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.SleepRules.Mode;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;

/** R141, no sleeping on a full moon: the bed refuses through the real {@code CanPlayerSleepEvent}. Needs no Werewolves (the werewolf mode is in {@link MoonWerewolfGameTests}). */
@PrefixGameTestTemplate(false)
public final class FullMoonSleepGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    /** Day 24 of the world: 24 % 8 = 0, the phase-0 day. */
    private static final long FULL_MOON_DAY = 24000L * 24;

    private FullMoonSleepGameTests() {
    }

    /** What the bed answers a player at this day time, with this mode. */
    private static Player.BedSleepingProblem ask(GameTestHelper helper, ServerPlayer p, Mode mode, long dayTime) {
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideFullMoonSleep(mode);
        try {
            level.setDayTime(dayTime);
            CanPlayerSleepEvent e = new CanPlayerSleepEvent(p, BlockPos.ZERO, null);
            NeoForge.EVENT_BUS.post(e);
            return e.getProblem();
        } finally {
            SpeciesConfig.overrideFullMoonSleep(null);
            level.setDayTime(saved);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyoneCannotSleepOnAFullMoonNight(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 13000) == Player.BedSleepingProblem.OTHER_PROBLEM, "the bed accepted a human on a full-moon night");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theNightStartsAtDusk(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 11999) == null, "refused before dusk");
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 12000) != null, "accepted at dusk");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void offAllowsEveryone(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(ask(helper, p, Mode.OFF, FULL_MOON_DAY + 13000) == null, "off still refused");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherPhaseOrTheDayIsAllowed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 24000 + 13000) == null, "the night after the full moon was refused");
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 6000) == null, "the day of the full moon was refused");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aHumanIsLeftAloneInWerewolvesMode(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(ask(helper, p, Mode.WEREWOLVES, FULL_MOON_DAY + 13000) == null, "werewolves mode refused a human");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCreativePlayerCanStillSleep(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(ask(helper, p, Mode.EVERYONE, FULL_MOON_DAY + 13000) == null, "a creative player was refused");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBedAlreadyRefusedIsNotOverwritten(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        ServerLevel level = helper.getLevel();
        long saved = level.getDayTime();
        SpeciesConfig.overrideFullMoonSleep(Mode.EVERYONE);
        try {
            level.setDayTime(FULL_MOON_DAY + 13000);
            CanPlayerSleepEvent e = new CanPlayerSleepEvent(p, BlockPos.ZERO, Player.BedSleepingProblem.NOT_SAFE);
            NeoForge.EVENT_BUS.post(e);
            helper.assertTrue(e.getProblem() == Player.BedSleepingProblem.NOT_SAFE, "the vanilla reason was replaced by " + e.getProblem());
        } finally {
            SpeciesConfig.overrideFullMoonSleep(null);
            level.setDayTime(saved);
        }
        helper.succeed();
    }
}
