package net.ofatech.supernaturalbloodlines.parts.celestials;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.core.moon.LunarMood;
import net.ofatech.supernaturalbloodlines.core.moon.LunarSource;

/** The lunar mood from an {@link EcAdapter}. */
final class EcSource implements LunarSource {
    private final EcAdapter ec;

    EcSource(EcAdapter ec) {
        this.ec = ec;
    }

    @Override
    public LunarMood now(Level level) {
        return ec.active(level).map(e -> LunarMood.ofEvent(e.id().getNamespace(), e.id().getPath())).orElse(LunarMood.NONE);
    }

    @Override
    public List<Upcoming> ahead(Level level, int days) {
        List<Upcoming> out = new ArrayList<>();
        ec.upcoming(level, days).forEach((day, e) -> out.add(new Upcoming(day, LunarMood.ofEvent(e.id().getNamespace(), e.id().getPath()), e.color(), e.nameKey())));
        out.sort(java.util.Comparator.comparingInt(Upcoming::days));
        return out;
    }
}
