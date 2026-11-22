package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page.SourceRulesTest.body;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The few rules about the HUD's drawing code that no behaviour test can reach, because they are about calls into the game's renderer and event bus. Everything
 * that can be decided by a pure function has its test beside the function.
 */
class HudSourceRulesTest {
    private static String source(String file) throws IOException {
        Path at = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return Files.readString(at.resolve("src/main/java/net/ofatech/supernaturalbloodlines/" + file));
    }

    @Test
    void everyKitBlitTurnsBlendingOnAndSetsTheNormalBlendFunctionWhateverTheAlpha() throws IOException {
        String blend = body(source("shared/hub/kit/client/UiKit.java"), "private static void blend(");
        int gate = blend.indexOf("if (alpha < 1f)");
        assertTrue(gate > 0, "the tint is gated by the alpha");
        String before = blend.substring(0, gate);
        assertTrue(before.contains("enableBlend()") && before.contains("defaultBlendFunc()"), "but the blending is not: " + blend);
    }

    @Test
    void aPackThatResizesAStripIsToldOnceAndTheHudDrawsItsStandIn() throws IOException {
        String frame = body(source("shared/hub/kit/client/UiKit.java"), "public static boolean frame(");
        assertTrue(frame.contains("WARNED.add(") && frame.contains("return false"));
    }

    @Test
    void theLayerFeedsTheTrackerOnlyUsableFilledSlotsAndStartsOverWhenTheHudIsHidden() throws IOException {
        String layer = source("parts/hud/client/AbilityHudLayer.java");
        assertTrue(body(layer, "private float[] glows(").contains("!v.isEmpty() && !v.greyed()"));
        assertTrue(body(layer, "public void render(").contains("glowTracker.reset()"));
        assertTrue(body(layer, "public void render(").contains("AltHold.detailed("), "the panels follow Alt held");
    }

    @Test
    void theToastAsksForTheOtherModsOnceAndAnchorsWhereTheHudSays() throws IOException {
        String toast = body(source("parts/supernatural/client/RequestToast.java"), "private static void draw(");
        assertTrue(toast.contains("statusCorner == null"), "the mod list is asked once");
        assertTrue(toast.contains("HudPlacement.toastAnchor()") && toast.contains("HudPlacement.trackerBox()") && toast.contains("HudPlacement.bossBottom()"));
    }

    @Test
    void theHudForgetsLastFramesPlacementBeforeEveryGuiFrameAndNotOnlyWhenItsLayerDraws() throws IOException {
        String client = source("parts/hud/client/HudClient.java");
        assertTrue(client.contains("RenderGuiEvent.Pre") && client.contains("HudPlacement.beginFrame()"));
        assertFalse(body(source("parts/hud/client/AbilityHudLayer.java"), "public void render(").contains("trackerBox(null)"), "a hidden GUI draws no layer to clear it");
    }

    @Test
    void aColumnWhoseLabelsDoNotFitShowsItsPanelAndTheModeMarkIsNamedOnce() throws IOException {
        String chrome = source("parts/hud/client/HudChrome.java");
        assertTrue(body(chrome, "static void labels(").contains("SlotLabel.fallbackColumns("));
        assertFalse(chrome.contains("\"kit/shared/mode_mark\"") || source("parts/hud/client/SlotArt.java").contains("\"kit/shared/mode_mark\""), "one constant: KitSprites.MODE_MARK");
    }
}
