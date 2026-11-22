package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.shared.hub.RungStatus;
import net.ofatech.supernaturalbloodlines.shared.hub.TaskState;
import org.junit.jupiter.api.Test;

class KitMetricsTest {
    private static final String DIR = "/assets/supernaturalbloodlines/textures/gui/sprites/";

    @Test
    void theBarFillStartsWhereTheTrackPictureEndsItsEdge() throws Exception {
        for (String kit : KitSprites.KITS) {
            try (InputStream in = KitMetricsTest.class.getResourceAsStream(DIR + KitSprites.of(kit, "bar_track") + ".png.mcmeta")) {
                JsonObject border = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonObject("gui").getAsJsonObject("scaling").getAsJsonObject("border");
                assertEquals(border.get("left").getAsInt(), KitMetrics.barInset(kit), kit);
                assertEquals(border.get("right").getAsInt(), KitMetrics.barInset(kit), kit);
            }
        }
    }

    @Test
    void theBarPicturesHaveTheHeightsTheDrawingAssumes() throws Exception {
        for (String kit : KitSprites.KITS) {
            assertEquals(KitMetrics.BAR_H, UiKitSlicesTest.png(KitSprites.of(kit, "bar_track")).getHeight(), kit);
            assertEquals(KitMetrics.BAR_FILL_H, UiKitSlicesTest.png(KitSprites.of(kit, "bar_fill")).getHeight(), kit);
            assertEquals(KitMetrics.SCROLL_W, UiKitSlicesTest.png(KitSprites.of(kit, "scroll_track")).getWidth(), kit);
            assertEquals(KitMetrics.SCROLL_W, UiKitSlicesTest.png(KitSprites.of(kit, "scroll_thumb")).getWidth(), kit);
            assertEquals(KitMetrics.DIVIDER_H, UiKitSlicesTest.png(KitSprites.of(kit, "divider")).getHeight(), kit);
        }
    }

    @Test
    void aFillIsProportionalAndNeverMoreThanTheTrack() {
        assertEquals(0, KitMetrics.barFill(50, 0f));
        assertEquals(25, KitMetrics.barFill(50, 0.5f));
        assertEquals(50, KitMetrics.barFill(50, 1f));
        assertEquals(50, KitMetrics.barFill(50, 7f));
        assertEquals(0, KitMetrics.barFill(50, -1f));
        assertEquals(0, KitMetrics.barFill(50, Float.NaN));
    }

    @Test
    void aBarThatIsNotEmptyShowsAtLeastOnePixel() {
        assertEquals(1, KitMetrics.barFill(50, 0.001f));
    }

    @Test
    void theThumbStaysInsideItsRegionAndReachesBothEnds() {
        for (int max : new int[] {1, 10, 100, 1000}) {
            for (int h : new int[] {12, 40, 120, 300}) {
                KitMetrics.Thumb top = KitMetrics.thumb(h, 0, max);
                KitMetrics.Thumb bottom = KitMetrics.thumb(h, max, max);
                assertEquals(0, top.top(), "h " + h + " max " + max);
                assertEquals(h, bottom.top() + bottom.length(), "h " + h + " max " + max);
                assertTrue(top.length() >= Math.min(h, KitMetrics.MIN_THUMB) && top.length() <= h);
            }
        }
        // scrolled past the end is the end, and a longer content makes a shorter thumb
        assertEquals(KitMetrics.thumb(100, 50, 50), KitMetrics.thumb(100, 99, 50));
        assertTrue(KitMetrics.thumb(100, 0, 400).length() < KitMetrics.thumb(100, 0, 40).length());
    }

    @Test
    void everyStateHasItsOwnMark() {
        Set<KitSprites.Part> rungs = EnumSet.noneOf(KitSprites.Part.class);
        for (RungStatus s : RungStatus.values()) {
            rungs.add(KitMetrics.rung(s));
        }
        assertEquals(RungStatus.values().length, rungs.size());
        Set<KitSprites.Part> tasks = EnumSet.noneOf(KitSprites.Part.class);
        for (TaskState s : TaskState.values()) {
            tasks.add(KitMetrics.task(s));
        }
        assertEquals(TaskState.values().length, tasks.size());
        assertNotEquals(KitMetrics.task(TaskState.CLAIMABLE), KitMetrics.task(TaskState.CLAIMED));
    }
}
