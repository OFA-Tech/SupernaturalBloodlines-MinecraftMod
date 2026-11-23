package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Rules about how the drawing code is written that a unit test cannot reach through the screen: read from the sources, like {@code AdminCoverageTest}. */
class SourceRulesTest {
    private static final String BASE = "net/ofatech/supernaturalbloodlines/";

    private static Path root() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path at = p; at != null; at = at.getParent()) {
            if (Files.isDirectory(at.resolve("src/main/java"))) {
                return at.resolve("src/main/java");
            }
        }
        throw new IllegalStateException("no src/main/java above " + p);
    }

    private static String source(String file) throws IOException {
        return Files.readString(root().resolve(BASE + file));
    }

    /** The text of the method or block that starts at {@code signature}: from it to the brace that closes it. */
    static String body(String source, String signature) {
        int at = source.indexOf(signature);
        assertTrue(at >= 0, "no " + signature);
        int open = source.indexOf('{', at);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return source.substring(open, i + 1);
            }
        }
        throw new AssertionError("no end of " + signature);
    }

    @Test
    void theBodyHelperStopsAtTheClosingBrace() {
        String s = "void a() { if (x) { y(); } z(); } void b() { w(); }";
        assertTrue(body(s, "void a()").contains("z()") && !body(s, "void a()").contains("w()"));
    }

    @Test
    void anEmptyStateEndsItsRegionAtItsOwnBottomSoItsLastButtonIsNeverCut() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (f.getFileName().toString().equals("EmptyState.java")) {
                    continue;
                }
                for (String line : Files.readAllLines(f)) {
                    String s = line.strip();
                    if (s.startsWith("EmptyState.render(")) {
                        bad.add(f.getFileName() + ": " + s);
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "EmptyState.render returns the y under its last button: use it (f.end(...) or a variable), do not drop it: " + bad);
    }

    @Test
    void theBloodMoonStageTitlesGoThroughTheThemesTitleFontInsideMoonHead() throws IOException {
        String moonHead = body(source("parts/supernatural/client/hub/page/BookKit.java"), "static void moonHead(");
        assertTrue(moonHead.contains("PageKit.themedTitle("), "moonHead (every Blood Moon page title) must try the theme's title font first");
    }

    @Test
    void aFillOfANineSliceIsNotScaledByAlphaAgain() throws IOException {
        String draw = body(source("shared/hub/kit/client/UiKit.java"), "public static void draw(GuiGraphics g, KitColors theme, Part part, int x, int y, int w, int h, float alpha)");
        assertTrue(draw.contains("op.color()"), "the fill takes the plan's colour");
        assertFalse(draw.contains(">>> 24"), "the shader colour set by blend() already carries the alpha: scaling it here too applies it twice");
    }

    @Test
    void aHiResPackSpriteIsLeftToVanillaWhenItsSizeIsNotTheMcmetas() throws IOException {
        String slice = body(source("shared/hub/kit/client/UiKit.java"), "private static Slice slice(");
        assertTrue(slice.contains("if (!NineSlicePlan.sizesMatch(") && slice.contains("return null"), "slice() must give up on a picture that is not the size its .mcmeta says");
    }

    @Test
    void anUnreadableMiddleIsLoggedOnce() throws IOException {
        String middle = body(source("shared/hub/kit/client/UiKit.java"), "private static Integer middle(");
        assertTrue(middle.contains("WARNED.add(") && middle.contains("LOGGER.warn("), "an unreadable PNG says so, once per sprite");
    }

    @Test
    void theHubDoesNotThrowAwayTheKitCacheOnEveryResize() throws IOException {
        String hub = source("parts/supernatural/client/hub/HubScreen.java");
        assertFalse(hub.contains("UiKit.reload("), "a resource reload clears it (KitClient); init() runs on every resize");
    }

    @Test
    void aPageChangeForgetsWhetherTheOldPageOverflowed() throws IOException {
        String hub = source("parts/supernatural/client/hub/HubScreen.java");
        assertTrue(body(hub, "private void resetScroll()").contains("overflowed.clear()"));
        String withoutHelper = hub.replace(body(hub, "private void resetScroll()"), "");
        assertFalse(withoutHelper.contains("scroll.clear()"), "every place that clears the scroll goes through resetScroll()");
    }

    @Test
    void aSealLetterIsDrawnInAColourThatReadsOnTheFlatSealToo() throws IOException {
        for (String theme : List.of("parts/supernatural/client/hub/theme/BloodMoonTheme.java", "parts/supernatural/client/hub/theme/GrimoireTheme.java")) {
            String seal = body(source(theme), "public void drawSeal(");
            assertTrue(seal.contains("UiKit.labelOver("), theme + ": the letter must follow the seal's face when its sprite is missing");
        }
    }

    @Test
    void wrappedTextNeverBreaksAWordInTheMiddle() throws IOException {
        String wrapped = body(source("parts/supernatural/client/hub/page/PageKit.java"), "static int wrapped(");
        assertTrue(wrapped.contains("LabelFit.hasOverlongWord(") && wrapped.contains("LabelFit.wrapWords("));
    }

    @Test
    void theSlotsAreDrawnFromTheKitAndNoShapeIsFilledByHandAnymore() throws IOException {
        String slot = source("parts/hud/client/SlotArt.java");
        assertFalse(slot.contains("KitDraw.shape("), "a slot's frame, badge and ring are the kit's pictures");
        assertFalse(slot.contains("BakedTexture"), "the pie and the ring are the kit's strips");
        assertFalse(slot.contains("g.fill(b.x()") && slot.contains("PIE_STEPS"), "no hand-baked pie");
        assertTrue(slot.contains("FrameStrip.SWEEP") && slot.contains("FrameStrip.RING"));
        String chrome = source("parts/hud/client/HudChrome.java");
        assertFalse(chrome.contains("pillBox("), "the pill, the labels and the tracker are the kit's boxes");
        assertTrue(chrome.contains("Part.HUD_PILL") && chrome.contains("Part.HUD_LABEL"));
    }
}
