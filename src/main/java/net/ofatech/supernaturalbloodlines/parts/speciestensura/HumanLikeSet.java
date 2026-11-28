package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;

/**
 * Plan 23 Task 11: the dynamic human-like set. Reads {@code tensura:human_like} tag + evolution closure,
 * minus undead, daemon and the vampire line. Admin per-world add/remove is supported via {@link #adminAdd}
 * and {@link #adminRemove}. The set is rebuilt lazily when the registry changes or after an admin mutation.
 */
public final class HumanLikeSet {
    private static final TagKey<ManasRace> HUMAN_LIKE =
            TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", "human_like"));
    private static final Set<String> VAMPIRE_LINE = Set.of(
            "tensura:vampire", "tensura:vampire_overcomer", "tensura:vampire_lord", "tensura:divine_vampire");

    private static volatile Set<String> cached;
    private static final Set<String> adminAdded = new CopyOnWriteArraySet<>();
    private static final Set<String> adminRemoved = new CopyOnWriteArraySet<>();

    private HumanLikeSet() {}

    public static Set<String> current() {
        Set<String> c = cached;
        if (c == null) {
            c = build();
            cached = c;
        }
        return c;
    }

    public static void invalidate() {
        cached = null;
    }

    public static void adminAdd(String raceId) {
        adminRemoved.remove(raceId);
        if (adminAdded.add(raceId)) {
            invalidate();
        }
    }

    public static void adminRemove(String raceId) {
        adminAdded.remove(raceId);
        if (adminRemoved.add(raceId)) {
            invalidate();
        }
    }

    private static Set<String> build() {
        Set<String> base = new TreeSet<>();
        for (ResourceLocation id : RaceAPI.getRaceRegistry().getIds()) {
            ManasRace race = RaceAPI.getRaceRegistry().get(id);
            if (race != null && race.createDefaultInstance().is(HUMAN_LIKE)) {
                String s = id.toString();
                if (!s.startsWith(SupernaturalBloodlines.MODID + ":")) {
                    base.add(s);
                }
            }
        }
        Set<String> closure = new TreeSet<>(base);
        Deque<String> todo = new ArrayDeque<>(base);
        while (!todo.isEmpty()) {
            String id = todo.pop();
            ManasRace race = RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
            if (race == null) continue;
            try {
                for (ManasRace next : race.getNextEvolutions(race.createDefaultInstance(), null)) {
                    String nid = Objects.requireNonNull(RaceAPI.getRaceRegistry().getId(next)).toString();
                    if (closure.add(nid)) {
                        todo.push(nid);
                    }
                }
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.debug("human_like closure: {} threw {}", id, e.getMessage());
            }
        }
        Set<String> filtered = new TreeSet<>(closure);
        for (String id : Set.copyOf(filtered)) {
            ManasRace race = RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
            if (race == null) continue;
            var inst = race.createDefaultInstance();
            if (inst.is(RaceTags.UNDEAD) || inst.is(RaceTags.DAEMON) || inst.is(RaceTags.ADDON_UNDEAD)
                    || VAMPIRE_LINE.contains(id) || id.startsWith(SupernaturalBloodlines.MODID + ":")) {
                filtered.remove(id);
            }
        }
        filtered.addAll(adminAdded);
        filtered.removeAll(adminRemoved);
        return Collections.unmodifiableSet(filtered);
    }
}