package net.ofatech.supernaturalbloodlines.gametest;

import java.util.Arrays;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;

/**
 * 13c Task 3: the hud part, switched off ({@code -PpartsOff=hud}), changes nothing in anyone else's classes, and, switched on, adds exactly its hooks.
 * It runs in every matrix run, so both sides are seen: with the part on (and Tensura, and Vampirism for the second hook) the hud mixins' methods are in
 * Tensura's classes; with it off they are not. Other parts' mixins are not counted (only the hud method names).
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class HudPartOffGameTests {
    private HudPartOffGameTests() {
    }

    private static boolean hasMethod(String className, String name) {
        try {
            // Mixin renames a merged handler to handler$<hash>$<name>
            return Arrays.stream(Class.forName(className).getDeclaredMethods()).anyMatch(m -> m.getName().endsWith(name));
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @GameTest(template = "empty")
    public static void partOffChangesNothingElse(GameTestHelper helper) {
        boolean hud = EnabledParts.isEnabled("hud");
        boolean tensura = ModList.get().isLoaded("tensura");
        boolean both = tensura && ModList.get().isLoaded("vampirism");
        boolean preset = hasMethod("io.github.manasmods.tensura.storage.ability.AbilityStorage", "supernaturalbloodlines$nextPassive");
        boolean feed = hasMethod("io.github.manasmods.tensura.race.vampire.VampireRace", "supernaturalbloodlines$needsFood");
        helper.assertTrue(preset == (hud && tensura), "next-preset hook: part " + (hud ? "on" : "off") + ", Tensura " + tensura + ", but the hook is " + (preset ? "in" : "out"));
        helper.assertTrue(feed == (hud && both), "race-feed hook: part " + (hud ? "on" : "off") + ", Tensura+Vampirism " + both + ", but the hook is " + (feed ? "in" : "out"));
        helper.succeed();
    }
}
