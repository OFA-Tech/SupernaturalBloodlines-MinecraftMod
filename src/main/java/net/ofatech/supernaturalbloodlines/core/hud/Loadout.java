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
 * nothing in any slot: Bite goes into slot 1 once, when the player first becomes a type that can bite ({@link #withBiteOffered}). Once cleared or moved
 * it is never put back on its own, so the saved form always lists all six slots.
 *
 * @param biteOffered Bite has already been given to this player (or deliberately not, because slot 1 was in use): it is never offered again. A save made
 *                    before this flag existed has it set, so a Bite the player cleared stays cleared.
 */
public record Loadout(List<Optional<ResourceLocation>> slots, boolean biteOffered) {
    public static final int SLOTS = 6;
    /** The pseudo action our own Bite uses. */
    public static final ResourceLocation BITE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bite");

    public static final Loadout DEFAULT = new Loadout(emptySlots(), false);

    /** An empty string is an empty slot, so a cleared Bite survives a save; an id that does not parse is an empty slot too. */
    public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots").forGetter(l -> Optional.of(l.slots().stream().map(s -> s.map(ResourceLocation::toString).orElse("")).toList())),
            Codec.BOOL.optionalFieldOf("bite_offered").forGetter(l -> Optional.of(l.biteOffered()))
    ).apply(i, (saved, offered) -> saved.map(s -> fromStrings(s, offered.orElse(true))).orElse(DEFAULT)));

    public Loadout {
        if (slots.size() != SLOTS) {
            throw new IllegalArgumentException("a loadout has " + SLOTS + " slots, not " + slots.size());
        }
        slots = List.copyOf(slots);
    }

    private static List<Optional<ResourceLocation>> emptySlots() {
        List<Optional<ResourceLocation>> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            list.add(Optional.empty());
        }
        return list;
    }

    private static Loadout fromStrings(List<String> saved, boolean biteOffered) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            String text = i < saved.size() ? saved.get(i) : "";
            ResourceLocation id = text.isEmpty() ? null : ResourceLocation.tryParse(text);
            list.add(Optional.ofNullable(id));
        }
        return new Loadout(list, biteOffered);
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
        return new Loadout(list, biteOffered);
    }

    /**
     * Called when the player can bite: Bite goes into slot 1 the first time, and only if that slot is empty (a slot the player set is never overwritten).
     * Either way it is not offered again, so a Bite the player clears later stays cleared. The same loadout when it was offered before.
     */
    public Loadout withBiteOffered() {
        if (biteOffered) {
            return this;
        }
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        if (list.get(0).isEmpty()) {
            list.set(0, Optional.of(BITE));
        }
        return new Loadout(list, true);
    }

    /**
     * The fallback for a server that runs the hud part without the supernatural part (so there is no hub to assign slots in): the empty slots 2 to 6 show,
     * in order, the unlocked actions that are not already in the list. Slot 1 stays Bite or empty. Never stored: server and client both compute it.
     */
    public Loadout withFallback(List<ResourceLocation> unlocked) {
        List<Optional<ResourceLocation>> list = new ArrayList<>(slots);
        int next = 0;
        for (int i = 1; i < SLOTS; i++) {
            if (list.get(i).isPresent()) {
                continue;
            }
            while (next < unlocked.size() && list.contains(Optional.of(unlocked.get(next)))) {
                next++;
            }
            if (next >= unlocked.size()) {
                break;
            }
            list.set(i, Optional.of(unlocked.get(next++)));
        }
        return new Loadout(list, biteOffered);
    }

    public boolean contains(ResourceLocation id) {
        return slots.contains(Optional.of(id));
    }
}
