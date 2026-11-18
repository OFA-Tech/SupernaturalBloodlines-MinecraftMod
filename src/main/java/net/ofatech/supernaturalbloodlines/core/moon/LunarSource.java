package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.List;
import net.minecraft.world.level.Level;

/** Where the lunar mood comes from: nothing by default, Enhanced Celestials 2 when the celestials part has found it. Works on both sides. */
public interface LunarSource {
    /** An event on a coming day ({@code days} from today, 0 = tonight): its mood, name colour (RGB, -1 for none) and translation key. */
    record Upcoming(int days, LunarMood mood, int color, String nameKey) {}

    LunarSource NONE = new LunarSource() {
        @Override
        public LunarMood now(Level level) {
            return LunarMood.NONE;
        }

        @Override
        public List<Upcoming> ahead(Level level, int days) {
            return List.of();
        }
    };

    LunarMood now(Level level);

    /** The events of the next {@code days} days, nearest first. */
    List<Upcoming> ahead(Level level, int days);
}
