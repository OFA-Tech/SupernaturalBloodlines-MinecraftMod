package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.rite.NightWatchRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.rite.SeveranceRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed;

/** R358: an eclipse counts as a full moon for a rite that needs one, inside the one night window, and night_watch reads that window too. */
@PrefixGameTestTemplate(false)
public final class EclipseFullMoonGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    /** A night that is not a full moon: day 3 (phase 3), the middle of the night. */
    private static final long HALF_MOON_NIGHT = 3 * 24000L + 18000;

    private EclipseFullMoonGameTests() {
    }

    /** The Rite of Severance worked by a plain player: past the moon check it refuses for the caster being no witch, before it for the moon. */
    private static RecordingPlayer severance(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RiteTestSupport.place(helper, caster);
        try {
            RiteTestSupport.start(helper, "rite_of_severance", caster, List.of());
        } finally {
            RiteTestSupport.remove(helper, caster);
        }
        return caster;
    }

    private static void at(GameTestHelper helper, long dayTime, boolean eclipse, Runnable body) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        long before = level.getDayTime();
        try {
            level.setDayTime(dayTime);
            if (eclipse) {
                helper.assertTrue(LunarEclipse.start(server) || LunarEclipse.active(server), "setup: the eclipse did not start");
            }
            body.run();
        } finally {
            LunarEclipse.stop(server);
            level.setDayTime(before);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFullMoonRiteRunsDuringAnEclipse(GameTestHelper helper) {
        at(helper, HALF_MOON_NIGHT, true, () -> {
            helper.assertTrue(EclipseFeed.active(helper.getLevel().getServer()), "the species part did not install the eclipse");
            helper.assertTrue(helper.getLevel().getMoonPhase() != 0, "setup: a full moon");
            RecordingPlayer caster = severance(helper);
            helper.assertTrue(!caster.received(FactionRiteFactory.NEEDS_FULL_MOON) && caster.received(SeveranceRiteFactory.NOT_WITCH),
                    "the eclipse did not count as a full moon: " + caster.heard());
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFullMoonRiteIsRefusedWithNoEclipseAndNoMoon(GameTestHelper helper) {
        at(helper, HALF_MOON_NIGHT, false, () -> {
            helper.assertTrue(!EclipseFeed.active(helper.getLevel().getServer()), "setup: an eclipse is running");
            RecordingPlayer caster = severance(helper);
            helper.assertTrue(caster.received(FactionRiteFactory.NEEDS_FULL_MOON) && !caster.received(SeveranceRiteFactory.NOT_WITCH), "a half moon passed: " + caster.heard());
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theRefusalNamesTheNeed(GameTestHelper helper) {
        at(helper, 6000, false, () -> {
            RecordingPlayer caster = severance(helper);
            helper.assertTrue(caster.refusedOnBothChannels(FactionRiteFactory.NEEDS_FULL_MOON), "the moon refusal is not on both channels: " + caster.heard());
        });
        helper.succeed();
    }

    /** An eclipse started at noon is stored for tonight; before dusk it is not a full moon for a rite. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEclipseBeforeDuskIsNotAFullMoonYet(GameTestHelper helper) {
        at(helper, 3 * 24000L + 6000, true, () -> {
            RecordingPlayer caster = severance(helper);
            helper.assertTrue(caster.received(FactionRiteFactory.NEEDS_FULL_MOON), "an eclipse at noon counted: " + caster.heard());
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void nightWatchStartsAtTheMoonWindowsEdge(GameTestHelper helper) {
        for (long t : new long[] {MoonRule.NIGHT_START, MoonRule.NIGHT_START + 1, MoonRule.NIGHT_END - 1, MoonRule.NIGHT_END}) {
            boolean night = t > MoonRule.NIGHT_START && t < MoonRule.NIGHT_END;
            at(helper, 5 * 24000L + t, false, () -> {
                RecordingPlayer caster = RecordingPlayer.create(helper);
                RiteTestSupport.place(helper, caster);
                try {
                    RiteTestSupport.start(helper, "night_watch", caster, List.of());
                } finally {
                    RiteTestSupport.remove(helper, caster);
                }
                helper.assertTrue(caster.received(NightWatchRiteFactory.DAY) != night, "at " + t + " night_watch heard " + caster.heard());
            });
        }
        helper.succeed();
    }
}
