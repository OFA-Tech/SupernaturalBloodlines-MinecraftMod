package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMoods;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;
import java.util.List;

/**
 * The celestials part, switched off ({@code -PpartsOff=celestials}), hooks nothing; switched on, it hooks the bite and the forced shift. A mood is
 * installed by hand through the core API (the test runtime has no Enhanced Celestials), so what is seen is only whether the part's listeners exist.
 * Runs in every matrix run.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class CelestialsPartOffGameTests {
    private CelestialsPartOffGameTests() {
    }

    private static LunarSource always(String path) {
        LunarMood mood = LunarMood.ofEvent(FakeEc.DLE, path);
        return new LunarSource() {
            @Override
            public LunarMood now(Level level) {
                return mood;
            }

            @Override
            public List<Upcoming> ahead(Level level, int days) {
                return List.of();
            }
        };
    }

    private static final LunarMoods.Tuning ON = new LunarMoods.Tuning(0.8, 0.75, 1.25, 0.5, 2.0, true);

    @GameTest(template = "empty", templateNamespace = SupernaturalBloodlines.MODID)
    public static void theSwitchGovernsTheHooks(GameTestHelper helper) {
        boolean on = EnabledParts.isEnabled("celestials");
        try {
            LunarMoods.use(LunarSource.NONE, null);
            helper.assertTrue(LunarMoods.now(helper.getLevel()).isNone() && LunarMoods.biteFactor(helper.getLevel()) == 1, "a mood without a source");
            if (ModList.get().isLoaded("vampirism")) {
                LunarMoods.use(always("blood_moon"), () -> ON);
                CelestialsVampireGameTests.biteHook(helper, on);
            }
            if (ModList.get().isLoaded("vampirism") && ModList.get().isLoaded("werewolves")) {
                LunarMoods.use(always("super_blue_moon"), () -> ON);
                CelestialsWolfGameTests.forcedShiftHook(helper, on);
            }
        } finally {
            LunarMoods.use(LunarSource.NONE, null);
        }
        helper.succeed();
    }
}
