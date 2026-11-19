package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.parts.celestials.EcAdapter;

/** An Enhanced Celestials stand-in for the tests: one running event, a schedule, and a record of what the part asked of it. */
public final class FakeEc implements EcAdapter {
    public static final String DLE = net.ofatech.supernaturalbloodlines.core.moon.LunarMood.EC_DEFAULT_NAMESPACE;

    public Optional<Event> running = Optional.empty();
    public final Map<Integer, Event> schedule = new TreeMap<>();
    public final List<String> calls = new ArrayList<>();
    /** When set, starting and ending an event throws, like an Enhanced Celestials that changed. */
    public boolean broken;

    public static Event event(String path) {
        return new Event(ResourceLocation.fromNamespaceAndPath(DLE, path), 0xFF5555);
    }

    public FakeEc running(String path) {
        running = Optional.of(event(path));
        return this;
    }

    @Override
    public Optional<Event> active(Level level) {
        return running;
    }

    @Override
    public Map<Integer, Event> upcoming(Level level, int days) {
        Map<Integer, Event> out = new TreeMap<>();
        schedule.forEach((day, e) -> {
            if (day < days) {
                out.put(day, e);
            }
        });
        return out;
    }

    @Override
    public boolean knows(ServerLevel level, ResourceLocation id) {
        return true;
    }

    @Override
    public boolean start(ServerLevel level, ResourceLocation id) {
        calls.add("start " + id);
        if (broken) {
            throw new IllegalStateException("EC changed its API");
        }
        if (running.isPresent()) {
            return false;
        }
        running = Optional.of(new Event(id, 0xB4501E));
        return true;
    }

    @Override
    public boolean stop(ServerLevel level, ResourceLocation id) {
        calls.add("stop " + id);
        if (broken) {
            throw new IllegalStateException("EC changed its API");
        }
        if (running.isEmpty() || !running.get().id().equals(id)) {
            return false;
        }
        running = Optional.empty();
        return true;
    }
}
