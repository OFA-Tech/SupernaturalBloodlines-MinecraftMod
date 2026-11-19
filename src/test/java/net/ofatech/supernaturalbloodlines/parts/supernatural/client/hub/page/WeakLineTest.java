package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;
import org.junit.jupiter.api.Test;

/** The weaker-spark line of the Pack page (R145) shows the days the server's own setting gave, never a guess. */
class WeakLineTest {
    private static Object days(WolfState w) {
        var line = BookPackPage.weakLine(w).orElseThrow();
        return ((TranslatableContents) line.getContents()).getArgs()[0];
    }

    @Test
    void noWeakSparkNoLine() {
        assertTrue(BookPackPage.weakLine(WolfState.NONE).isEmpty());
    }

    @Test
    void theDaysFollowTheTimeThatCameWithTheSpark() {
        assertEquals(7, days(WolfState.NONE.toBuilder().weak(new Weak(1, 0, 1, 7 * 24000)).build()), "weak_spark_grow_days = 7");
        assertEquals(1, days(WolfState.NONE.toBuilder().weak(new Weak(1, 0, 1, 24000)).build()), "weak_spark_grow_days = 1");
    }

    @Test
    void aSparkWithoutATimeUsesTheDefault() {
        assertEquals(3, days(WolfState.NONE.toBuilder().weak(new Weak(1, 0, 1)).build()));
    }
}
