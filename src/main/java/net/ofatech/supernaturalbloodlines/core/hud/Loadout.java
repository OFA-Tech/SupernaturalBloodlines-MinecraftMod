package net.ofatech.supernaturalbloodlines.core.hud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/**
 * What the player put in the six Supernatural-mode slots: a Vampirism or Werewolves action id, or {@link #BITE}, or nothing. The ids are only names:
 * the server checks that an id may be assigned, and reads the id from here (never from a trigger packet). Duplicates are allowed. A new player has
 * Bite in slot 1; once cleared or moved it is never put back on its own, so the saved form always lists all six slots.
 */
public record Loadout(List<Optional<ResourceLocation>> slots) {
    public static final int SLOTS = 6;
    /** The pseudo action our own Bite uses. */
    public static final ResourceLocation BITE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bite");

    public static final Loadout DEFAULT = new Loadout(withBiteFirst());

    /** An empty string is an empty slot, so a cleared Bite survives a save; an id that does not parse is an empty slot too. */
    public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots").forGetter(l -> Optional.of(l.slots().stream().map(s -> s.map(ResourceLocation::toString).orElse("")).toList()))
    ).apply(i, saved -> saved.map(Loadout::fromStrings).orElse(DEFAULT)));

    public Loadout {
        if (slots.size() != SLOTS) {
            throw new IllegalArgumentException("a loadout has " + SLOTS + " slots, not " + slots.size());
        }
        slots = List.copyOf(slots);
    }

    private static List<Optional<ResourceLocation>> withBiteFirst() {
        List<Optional<ResourceLocation>> list = new ArrayList<>();
        list.add(Optional.of(BITE));
        for (int i = 1; i < SLOTS; i++) {
            list.add(Optional.empty());
        }
        return list;
    }

    private static Loadout fromStrings(List<String> saved) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            String text = i < saved.size() ? saved.get(i) : "";
            ResourceLocation id = text.isEmpty() ? null : ResourceLocation.tryParse(text);
            list.add(Optional.ofNullable(id));
        }
        return new Loadout(list);
    }

    public static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOTS;
    }

    public Optional<ResourceLocation> get(int slot) {
        return slots.get(slot);
    }

    public Loadout with(int slot, @Nullable ResourceLocation id) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        list.set(slot, Optional.ofNullable(id));
        return new Loadout(list);
    }

    public boolean contains(ResourceLocation id) {
        return slots.contains(Optional.of(id));
    }
}
